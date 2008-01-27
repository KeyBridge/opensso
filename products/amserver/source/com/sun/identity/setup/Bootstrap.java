/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Bootstrap.java,v 1.8 2008-01-27 08:01:11 veiming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.setup;

import com.iplanet.am.util.AdminUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.authentication.internal.InvalidAuthContextException;
import com.sun.identity.authentication.internal.server.SMSAuthModule;
import com.sun.identity.common.DebugPropertiesObserver;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.security.auth.login.LoginException;

/**
 * This class is responsible for bootstrapping the WAR.
 */
public class Bootstrap {
    /**
     * Directory where bootstrap file resides.
     */
    public static final String JVM_OPT_BOOTSTRAP = "bootstrap.dir";
    private final static String BOOTSTRAP = "bootstrap";
    private static boolean isBootstrap;

    private Bootstrap() {
    }
    
    /**
     * Loads System Property with the bootstrap file that is
     * found in <code>JVM_OPT_BOOTSTRAP</code> stated directory.
     *
     * @throws Exception if properties cannot be loaded.
     */
    public synchronized static void load()
        throws Exception
    {   
        if (!isBootstrap) {
            String basedir = System.getProperty(JVM_OPT_BOOTSTRAP);
            load(basedir);
            SystemProperties.initializeProperties("com.iplanet.am.naming.url",
                SystemProperties.getServerInstanceName() + "/namingservice");
        }
    }

    /**
     * Loads System Property with the bootstrap file that is
     * found in a directory.
     *
     * @param basedir Directory where bootstrap file resides.
     *        bootstrap file can contain either an URL where 
     *        we can go fetch the server configuration properties
     *        or a file that contains the properties.
     * @throws Exception if properties cannot be loaded.
     */
    public static Properties load(String basedir)
        throws Exception {
        if (!basedir.endsWith(File.separator)) {
            basedir = basedir + File.separator;
        }

        Properties prop = null;
        String amConfigProperties = basedir +
            SetupConstants.AMCONFIG_PROPERTIES;
        File file = new File(amConfigProperties);
        if (file.exists()) {
            prop = new Properties();
            InputStream propIn = new FileInputStream(amConfigProperties);
            try {
                prop.load(propIn);
            } finally {
                propIn.close();
            }
        } else {
            isBootstrap = true;
            BootstrapData bData = new BootstrapData(basedir + BOOTSTRAP);
            prop = getConfiguration(bData, true);
        }
        
        return prop;
    }

    static boolean load(BootstrapData bootstrap, boolean reinit)
        throws Exception {
        boolean configured = false;  
        if (bootstrap != null) {
            configured = bootstrap(bootstrap, reinit);
            isBootstrap = true;
        }
        return configured;
    }
    
    /**
     * Returns <code>true</code> if able to bootstrap the system.
     *
     * @param bootstrapInfo object that contains information on how to
     *        fetch the server configuration properties.
     * @param reinit <code>true</code> to re initialize the system.
     * @throws Exception if there are errors in bootstrapping.
     */
    public static boolean bootstrap(BootstrapData bootstrapInfo, boolean reinit) 
        throws Exception {
        return (getConfiguration(bootstrapInfo, reinit) != null);
    }

    /**
     * Returns System Property with an URL.
     *
     * @param bootstrapInfo an URL that contains information on how to
     *        fetch the server configuration properties.
     * @param reinit <code>true</code> to re initialize the system.
     * @throws Exception if properties cannot be loaded.
     */
    private static Properties getConfiguration(
        BootstrapData bootstrapData,
        boolean reinit
    ) throws Exception {
        Properties properties = null;
        bootstrapData.initSMS();
        if (reinit) {
            AdminUtils.initialize();
            SMSAuthModule.initialize();
        }

        String dsbasedn = bootstrapData.getBaseDN();
        String pwd = bootstrapData.getDsameUserPassword();
        String dsameUser = "cn=dsameuser,ou=DSAME Users," + dsbasedn;
        String instanceName = bootstrapData.getInstanceName();

        SSOToken ssoToken = getSSOToken(dsbasedn, dsameUser,
            Crypt.decode(pwd, Crypt.getHardcodedKeyEncryptor()));
        try {
            properties = ServerConfiguration.getServerInstance(
                ssoToken, instanceName);
            if (properties != null) {
                SystemProperties.initializeProperties(
                    properties, true, false);
                String serverConfigXML =
                    ServerConfiguration.getServerConfigXML(
                    ssoToken, instanceName);
                Crypt.reinitialize();
                BootstrapData.loadServerConfigXML(serverConfigXML);
                AdminUtils.initialize();
                SMSAuthModule.initialize();
                SystemProperties.initializeProperties(
                    properties, true, true);
                DebugPropertiesObserver.getInstance().notifyChanges();
                SystemProperties.setServerInstanceName(instanceName);

                ServiceConfigManager scm = new ServiceConfigManager(
                    Constants.SVC_NAME_PLATFORM, (SSOToken)
                        AccessController.doPrivileged(
                        AdminTokenAction.getInstance()));
                scm.addListener(ConfigurationObserver.getInstance());
            }
        } catch (SMSException e) {
            //ignore. product is not configured yet.
            properties = null;
        }
        return properties;
    }
   
    private static SSOToken getSSOToken(
        String basedn,
        String bindUser, 
        String bindPwd
    ) throws LoginException, InvalidAuthContextException {
        SSOToken ssoToken = null;
        AuthPrincipal principal = new AuthPrincipal(bindUser);
        AuthContext ac = new AuthContext(
            basedn, principal, bindPwd.toCharArray());
        if (ac.getLoginStatus() == AuthContext.AUTH_SUCCESS) {
            ssoToken = ac.getSSOToken();
        }
        return ssoToken;
    }

    private static List readFile(String file) 
        throws IOException
    {
        List list = new ArrayList();
        BufferedReader in = null;
        String str = null;
        
        try {
            in = new BufferedReader(new FileReader(file));
            if (in.ready()) {
                str = in.readLine();
                while (str != null) {
                    list.add(str);
                    str = in.readLine();
                }
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                    //No handling requried
                }
            }
        }
        return list;
    }
    
    /**
     * Modifies the <code>dsameuser</code> password in bootstrap file.
     *
     * @param password New Password.
     * @throws IOException if modification fails.
     */
    public static void modifyDSAMEUserPassword(String password) 
        throws IOException {
        String baseDir = SystemProperties.get(SystemProperties.CONFIG_PATH);
        String bootstrapFile = baseDir + "/" + AMSetupServlet.BOOTSTRAP_EXTRA;
        List urls = readFile(bootstrapFile);
        StringBuffer buff = new StringBuffer();
        for (Iterator i = urls.iterator(); i.hasNext(); ) {
            String url = (String)i.next();
            buff.append(modifyDSAMEUserPassword(url, password)).append("\n");
        }
        
        AMSetupServlet.writeToFile(bootstrapFile, buff.toString());
    }
        
    private static String modifyDSAMEUserPassword(String url, String password) 
        throws IOException {
        int start = url.indexOf("&" + BootstrapData.PWD + "=");
        if (start == -1) {
            start = url.indexOf("?" + BootstrapData.PWD + "=");
        }
        if (start != -1) {
            String encPassword = URLEncoder.encode(Crypt.encode(password, 
                Crypt.getHardcodedKeyEncryptor()), "UTF-8");
            int end = url.indexOf("&", start+1);
            if (end == -1) {
                url = url.substring(0, start + 5) + encPassword;
            } else {
                url = url.substring(0, start + 5) + encPassword + 
                    url.substring(end);
            }
        }
        return url;
    }
}
