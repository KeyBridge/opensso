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
 * $Id: TestCommon.java,v 1.50 2008-06-26 20:10:39 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPasswordInput;
import com.gargoylesoftware.htmlunit.html.HtmlRadioButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.jaxrpc.JAXRPCUtil;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.testng.Reporter;

/**
 * This class is the base for all <code>OpenSSO</code> QA testcases.
 * It has commonly used methods.
 */
public class TestCommon implements TestConstants {
    
    private String className;
    static private ResourceBundle rb_amconfig;
    static protected String adminUser;
    static protected String adminPassword;
    static protected String basedn;
    static protected String host;
    static protected String protocol;
    static protected String port;
    static protected String uri;
    static protected String realm;
    static protected String serverName;
    static protected String cookieDomain;
    static protected int notificationSleepTime;
    static protected Level logLevel;
    static protected boolean distAuthEnabled = false;
    static private Logger logger;
    static private String logEntryTemplate;
    static private Server server;
    private String productSetupResult;
    static private String uriseparator = "/";
 
    protected static String newline = System.getProperty("line.separator");
    protected static String fileseparator =
            System.getProperty("file.separator");
    private static String tableContents;
    
    static {
        try {
            rb_amconfig = ResourceBundle.getBundle(
                    TestConstants.TEST_PROPERTY_AMCONFIG);
            logger = Logger.getLogger("com.sun.identity.qatest");
            serverName = rb_amconfig.getString(
                    TestConstants.KEY_ATT_SERVER_NAME);
            FileHandler fileH = new FileHandler(serverName + fileseparator +
                    "logs");
            SimpleFormatter simpleF = new SimpleFormatter();
            fileH.setFormatter(simpleF);
            logger.addHandler(fileH);
            String logL = rb_amconfig.getString(
                    TestConstants.KEY_ATT_LOG_LEVEL);
            if ((logL != null) && !logL.equals("")) {
                logger.setLevel(Level.parse(logL));
            } else {
                logger.setLevel(Level.FINE);
            }
            logLevel = logger.getLevel();
            adminUser = rb_amconfig.getString(
                    TestConstants.KEY_ATT_AMADMIN_USER);
            adminPassword = rb_amconfig.getString(
                    TestConstants.KEY_ATT_AMADMIN_PASSWORD);
            basedn = rb_amconfig.getString(TestConstants.KEY_AMC_BASEDN);
            distAuthEnabled = ((String)rb_amconfig.getString(
                    TestConstants.KEY_DIST_AUTH_ENABLED)).equals("true");
            if (!distAuthEnabled) {
                protocol = rb_amconfig.getString(
                        TestConstants.KEY_AMC_PROTOCOL);
                host = rb_amconfig.getString(TestConstants.KEY_AMC_HOST);
                port = rb_amconfig.getString(TestConstants.KEY_AMC_PORT);
                uri = rb_amconfig.getString(TestConstants.KEY_AMC_URI);
            } else {
                String strDistAuthURL = rb_amconfig.getString(
                        TestConstants.KEY_DIST_AUTH_NOTIFICATION_SVC);
                
                int iFirstSep = strDistAuthURL.indexOf(":");
                protocol = strDistAuthURL.substring(0, iFirstSep);
                
                int iSecondSep = strDistAuthURL.indexOf(":", iFirstSep + 1);
                host = strDistAuthURL.substring(iFirstSep + 3, iSecondSep);
                
                int iThirdSep = strDistAuthURL.indexOf(uriseparator,
                        iSecondSep + 1);
                port = strDistAuthURL.substring(iSecondSep + 1, iThirdSep);
                
                int iFourthSep = strDistAuthURL.indexOf(uriseparator,
                        iThirdSep + 1);
                uri = uriseparator +
                        strDistAuthURL.substring(iThirdSep + 1, iFourthSep);
            }
            realm = rb_amconfig.getString(TestConstants.KEY_ATT_REALM);
            cookieDomain = rb_amconfig.getString(
                    TestConstants.KEY_ATT_COOKIE_DOMAIN);
            notificationSleepTime = new Integer(rb_amconfig.getString(
                    TestConstants.KEY_ATT_NOTIFICATION_SLEEP)).intValue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private TestCommon() {
    }
    
    protected TestCommon(String componentName) {
        logEntryTemplate = this.getClass().getName() + ".{0}: {1}";
        className = this.getClass().getName();
        productSetupResult = rb_amconfig.getString(
                TestConstants.KEY_ATT_PRODUCT_SETUP_RESULT);
        if (productSetupResult.equals("fail")) {
            log(Level.SEVERE, "TestCommon", "Product setup failed. Check logs" +
                    " for more detail...");
            assert false;
        }
    }
    
    /**
     * Writes a log entry for entering a test method.
     */
    protected void entering(String methodName, Object[] params) {
        if (params != null) {
            logger.entering(className, methodName, params);
        } else {
            logger.entering(className, methodName);
        }
    }
    
    /**
     * Writes a log entry for exiting a test method.
     */
    protected void exiting(String methodName) {
        logger.exiting(className, methodName);
    }
    
    /**
     * Writes a log entry.
     */
    protected static void log(Level level, String methodName, Object message) {
        Object[] args = {methodName, message};
        logger.log(level, MessageFormat.format(logEntryTemplate, args));
    }
    
    /**
     * Writes a log entry.
     */
    protected void log(
            Level level,
            String methodName,
            String message,
            Object[] params
            ) {
        Object[] args = {methodName, message};
        logger.log(level, MessageFormat.format(logEntryTemplate, args), params);
    }
    
    /**
     * Writes a log entry for testng report
     */
    protected void logTestngReport(Map m) {
        Set s = m.keySet();
        Iterator it = s.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            String value = (String)m.get(key);
            Reporter.log(key + "=" + value);
        }
    }
    
    /**
     * Returns single sign on token.
     */
    protected SSOToken getToken(String name, String password, String basedn)
    throws Exception {
        entering("SSOToken", null);
        log(Level.FINEST, "getToken", name);
        log(Level.FINEST, "getToken", password);
        log(Level.FINEST, "getToken", basedn);
        AuthContext authcontext = new AuthContext(basedn);
        authcontext.login();
        javax.security.auth.callback.Callback acallback[] =
                authcontext.getRequirements();
        for (int i = 0; i < acallback.length; i++){
            if (acallback[i] instanceof NameCallback) {
                NameCallback namecallback = (NameCallback)acallback[i];
                namecallback.setName(name);
            }
            if (acallback[i] instanceof PasswordCallback) {
                PasswordCallback passwordcallback =
                        (PasswordCallback)acallback[i];
                passwordcallback.setPassword(password.toCharArray());
            }
        }
        
        authcontext.submitRequirements(acallback);
        if (authcontext.getStatus() ==
                com.sun.identity.authentication.AuthContext.Status.SUCCESS)
            log(Level.FINEST, "getToken", "Successful authentication ....... ");
        SSOToken ssotoken = authcontext.getSSOToken();
        log(Level.FINEST, "getToken",
                (new StringBuilder()).append("TOKENCREATED>>> ").
                append(ssotoken).toString());
        exiting("SSOToken");
        return ssotoken;
    }
    
    /**
     * Validate single sign on token.
     */
    protected boolean validateToken(SSOToken ssotoken)
    throws Exception {
        entering("validateToken", null);
        SSOTokenManager stMgr = SSOTokenManager.getInstance();
        boolean bVal = stMgr.isValidToken(ssotoken);
        if (bVal)
            log(Level.FINE, "validateToken", "Token is Valid");
        else
            log(Level.FINE, "validateToken", "Token is Invalid");
        exiting("validateToken");
        return bVal;
    }
    
    /**
     * Destroys single sign on token.
     */
    protected void destroyToken(SSOToken ssotoken)
    throws Exception {
        destroyToken(null, ssotoken);
    }
    
    /**
     * Destroys single sign on token.
     */
    protected void destroyToken(SSOToken requester, SSOToken ssotoken)
    throws Exception {
        entering("destroyToken", null);
        if (validateToken(ssotoken)) {
            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            if (requester != null)
                stMgr.destroyToken(requester, ssotoken);
            else
                stMgr.destroyToken(ssotoken);
        }
        exiting("destroyToken");
    }
    
    /**
     * Returns the base directory where code base is
     * checked out.
     */
    protected String getBaseDir()
    throws Exception {
        entering("getBaseDir", null);
        String strCD =  System.getProperty("user.dir");
        log(Level.FINEST, "getBaseDir", "Current Directory: " + strCD);
        exiting("getBaseDir");
        return (strCD);
    }
    
    /**
     * Reads a file containing data-value pairs and returns that as a list
     * object.
     */
    protected List getListFromFile(String fileName)
    throws Exception {
        entering("getListFromFile", null);
        ArrayList list = null;
        if (fileName != null) {
            list = new ArrayList();
            BufferedReader input = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line=input.readLine()) != null) {
                if ((line.indexOf("=")) != -1)
                    list.add(line);
            }
            log(Level.FINEST, "getListFromFile", "List: " + list);
            if (input != null)
                input.close();
        }
        exiting("getListFromFile");
        return (list);
    }
    
    /**
     * Login to admin console using htmlunit
     */
    protected HtmlPage consoleLogin(
            WebClient webclient,
            String amUrl,
            String amadmUser,
            String amadmPassword)
            throws Exception {
        entering("consoleLogin", null);
        log(Level.FINEST, "consoleLogin", "JavaScript Enabled: " +
                webclient.isJavaScriptEnabled());
        log(Level.FINEST, "consoleLogin", "Redirect Enabled: " +
                webclient.isRedirectEnabled());
        log(Level.FINEST, "consoleLogin", "URL: " + amUrl);
        URL url = new URL(amUrl);
        HtmlPage page = (HtmlPage)webclient.getPage(amUrl);
        log(Level.FINEST, "consoleLogin", "BEFORE CONSOLE LOGIN: " +
                page.getTitleText());
        HtmlForm form = page.getFormByName("Login");
        HtmlHiddenInput txt1 =
                (HtmlHiddenInput)form.getInputByName("IDToken1");
        txt1.setValueAttribute(amadmUser);
        HtmlHiddenInput txt2 =
                (HtmlHiddenInput)form.getInputByName("IDToken2");
        txt2.setValueAttribute(amadmPassword);
        page = (HtmlPage)form.submit();
        log(Level.FINEST, "consoleLogin", "AFTER CONSOLE LOGIN: " +
                page.getTitleText());
        exiting("consoleLogin");
        return (page);
    }
    
    /**
     * Creates a map object and adds all the configutaion properties to that.
     */
    protected Map getConfigurationMap(String rb, String strProtocol,
            String strHost, String strPort, String strURI)
            throws Exception {
        entering("getConfigurationMap", null);
        
        ResourceBundle cfg = ResourceBundle.getBundle(rb);
        Map<String, String> map = new HashMap<String, String>();
        map.put("serverurl", strProtocol + ":" + "//" + strHost + ":" +
                strPort);
        map.put("serveruri", strURI);
        map.put(TestConstants.KEY_ATT_COOKIE_DOMAIN, cfg.getString(
                TestConstants.KEY_ATT_COOKIE_DOMAIN));
        map.put(TestConstants.KEY_ATT_AMADMIN_USER, cfg.getString(
                TestConstants.KEY_ATT_AMADMIN_USER));
        map.put(TestConstants.KEY_ATT_AMADMIN_PASSWORD, cfg.getString(
                TestConstants.KEY_ATT_AMADMIN_PASSWORD));
        map.put(TestConstants.KEY_AMC_SERVICE_PASSWORD, cfg.getString(
                TestConstants.KEY_AMC_SERVICE_PASSWORD));
        map.put(TestConstants.KEY_ATT_CONFIG_DIR, cfg.getString(
                TestConstants.KEY_ATT_CONFIG_DIR));
        map.put(TestConstants.KEY_ATT_CONFIG_DATASTORE, cfg.getString(
                TestConstants.KEY_ATT_CONFIG_DATASTORE));
        map.put(TestConstants.KEY_ATT_AM_ENC_PWD,
                cfg.getString(TestConstants.KEY_ATT_AM_ENC_PWD));
        map.put(TestConstants.KEY_ATT_DIRECTORY_SERVER, cfg.getString(
                TestConstants.KEY_ATT_DIRECTORY_SERVER));
        map.put(TestConstants.KEY_ATT_DIRECTORY_PORT, cfg.getString(
                TestConstants.KEY_ATT_DIRECTORY_PORT));
        map.put(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX,
                cfg.getString(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX));
        map.put(TestConstants.KEY_ATT_DS_DIRMGRDN, cfg.getString(
                TestConstants.KEY_ATT_DS_DIRMGRDN));
        map.put(TestConstants.KEY_ATT_DS_DIRMGRPASSWD,
                cfg.getString(TestConstants.KEY_ATT_DS_DIRMGRPASSWD));
        map.put(TestConstants.KEY_ATT_LOAD_UMS, cfg.getString(
                TestConstants.KEY_ATT_LOAD_UMS));
        
        exiting("getConfigurationMap");
        
        return map;
    }
    
    /**
     * Creates a map object and adds all the configutaion properties to that.
     */
    protected Map getConfigurationMap(String rb)
    throws Exception {
        return (getConfigurationMap(rb, protocol, host, port, uri));
    }
    
    /**
     * Configures opensso using the configurator page. It map needs to set the
     * following values:
     * serverurl                 <protocol + ":" + "//" + host + ":" + port>
     * serveruri                 <URI for configured instance>
     * cookiedomain              <full cookie domain name>
     * amadmin_password          <password for amadmin user>
     * urlaccessagent_password   <password for UrlAccessAgent user>
     * config_dir                <directory where product will be installed>
     * datastore                 <type of statstore: faltfile, dirServer or
     *                            activeDir>
     * directory_server          <directory server hostname>
     * directory_port            <directory server port>
     * config_root_suffix        <suffix under which configuration data will
     *                            be stored>
     * sm_root_suffix            <suffix where sms data will be stored>
     * ds_dirmgrdn               <directory user with administration
     *                            privilages>
     * ds_dirmgrpasswd           <password for directory user with
     *                            administration privilages>
     * load_ums                  <to load user schema or not(yes or no)>
     */
    protected boolean configureProduct(Map map)
    throws Exception {
        entering("configureProduct", null);
        
        log(Level.FINEST, "configureProduct", "Configuration Map: " + map);
        
        WebClient webclient = new WebClient();
        String strURL = (String)map.get("serverurl") +
                (String)map.get("serveruri") + "/configurator.jsp?type=custom";
        log(Level.FINEST, "configureProduct", "strURL: " + strURL);
        URL url = new URL(strURL);
        HtmlPage page = null;
        int pageIter = 0;
        try {
            // THIS WHILE IS WRITTEN BECUASE IT TAKES SOME TIME FOR INITIAL
            // CONFIGURATOR PAGE TO LOAD AND WEBCLIENT CALL DOES NOT WAIT
            // FOR SUCH A DURATION.
            while (page == null && pageIter <= 30) {
                try {
                    page = (HtmlPage)webclient.getPage(url);
                    Thread.sleep(10000);
                    pageIter++;
                } catch (com.gargoylesoftware.htmlunit.ScriptException e) {
                }
            }
        } catch(com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException e) {
            log(Level.SEVERE, "configureProduct", strURL +
                    " cannot be reached.");
            return false;
        }
        
        if (pageIter > 30) {
            log(Level.SEVERE, "configureProduct",
                    "Product Configuration was not" +
                    " successfull." + strURL + "was not found." +
                    " Please check if war is deployed properly.");
            exiting("configureProduct");
            return false;
        }
        
        if (getHtmlPageStringIndex(page, "Not Found") != -1) {
            log(Level.SEVERE, "configureProduct",
                    "Product Configuration was not" +
                    " successfull." + strURL + "was not found." +
                    " Please check if war is deployed properly.");
            exiting("configureProduct");
            return false;
        }
        
        if (getHtmlPageStringIndex(page, "configurator.jsp") != -1) {
            log(Level.FINE, "configureProduct", "Inside configurator.");
            
            HtmlForm form = (HtmlForm)page.getForms().get(0);
            
            HtmlTextInput txtServer =
                    (HtmlTextInput)form.getInputByName("SERVER_URL");
            txtServer.setValueAttribute((String)map.get("serverurl"));
            
            HtmlTextInput txtCookieDomain =
                    (HtmlTextInput)form.getInputByName("COOKIE_DOMAIN");
            txtCookieDomain.setValueAttribute((String)map.get(
                    TestConstants.KEY_ATT_COOKIE_DOMAIN));
            
            HtmlPasswordInput txtAmadminPassword =
                    (HtmlPasswordInput)form.getInputByName("ADMIN_PWD");
            txtAmadminPassword.setValueAttribute((String)map.get(
                    TestConstants.KEY_ATT_AMADMIN_PASSWORD));
            HtmlPasswordInput txtAmadminPasswordR =
                    (HtmlPasswordInput)form.getInputByName("ADMIN_CONFIRM_PWD");
            txtAmadminPasswordR.setValueAttribute((String)map.get(
                    TestConstants.KEY_ATT_AMADMIN_PASSWORD));
            
            HtmlPasswordInput txtUrlAccessAgentPassword =
                    (HtmlPasswordInput)form.getInputByName("AMLDAPUSERPASSWD");
            txtUrlAccessAgentPassword.setValueAttribute((String)map.get(
                    TestConstants.KEY_AMC_SERVICE_PASSWORD));
            HtmlPasswordInput txtUrlAccessAgentPasswordR =
                    (HtmlPasswordInput)form.getInputByName(
                    "AMLDAPUSERPASSWD_CONFIRM");
            txtUrlAccessAgentPasswordR.setValueAttribute((String)map.get(
                    TestConstants.KEY_AMC_SERVICE_PASSWORD));
            
            HtmlTextInput txtConfigDir =
                    (HtmlTextInput)form.getInputByName("BASE_DIR");
            txtConfigDir.setValueAttribute((String)map.get(
                    TestConstants.KEY_ATT_CONFIG_DIR));
            
            HtmlTextInput txtEncryptionKey =
                    (HtmlTextInput)form.getInputByName("AM_ENC_KEY");
            String strEncryptKey = (String)map.get(
                    TestConstants.KEY_ATT_AM_ENC_PWD);
            if (!(strEncryptKey.equals(null)) && !(strEncryptKey.equals("")))
                txtEncryptionKey.setValueAttribute(strEncryptKey);
            
            String strConfigStore = (String)map.get(
                    TestConstants.KEY_ATT_CONFIG_DATASTORE);
            log(Level.FINE, "configureProduct", "Config store is: " +
                    strConfigStore);
            
            HtmlRadioButtonInput rbDataStore =
                    (HtmlRadioButtonInput)form.getInputByName("DATA_STORE");
            rbDataStore.setDefaultValue(strConfigStore);
            
            if (strConfigStore.equals("embedded")) {
                log(Level.FINE, "configureProduct",
                        "Doing embedded System configuration.");
                
                HtmlTextInput txtDirServerPort =
                        (HtmlTextInput)form.getInputByName("DIRECTORY_PORT");
                txtDirServerPort.
                        setValueAttribute((String)map.get(
                        TestConstants.KEY_ATT_DIRECTORY_PORT));
                
                HtmlTextInput txtDirConfigData =
                        (HtmlTextInput)form.getInputByName("ROOT_SUFFIX");
                txtDirConfigData.setValueAttribute((String)map.
                        get(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX));
                
                HtmlPasswordInput txtDirAdminPassword =
                        (HtmlPasswordInput)form.
                        getInputByName("DS_DIRMGRPASSWD");
                txtDirAdminPassword.setValueAttribute((String)map.
                        get(TestConstants.KEY_ATT_DS_DIRMGRPASSWD));
            } else {
                log(Level.FINE, "configureProduct",
                        "Doing directory configuration.");
                
                HtmlTextInput txtDirServerName =
                        (HtmlTextInput)form.getInputByName("DIRECTORY_SERVER");
                txtDirServerName.
                        setValueAttribute((String)map.get(
                        TestConstants.KEY_ATT_DIRECTORY_SERVER));
                
                HtmlTextInput txtDirServerPort =
                        (HtmlTextInput)form.getInputByName("DIRECTORY_PORT");
                txtDirServerPort.
                        setValueAttribute((String)map.get(
                        TestConstants.KEY_ATT_DIRECTORY_PORT));
                
                HtmlTextInput txtDirConfigData =
                        (HtmlTextInput)form.getInputByName("ROOT_SUFFIX");
                txtDirConfigData.setValueAttribute((String)map.
                        get(TestConstants.KEY_ATT_CONFIG_ROOT_SUFFIX));
                
                HtmlTextInput txtDirAdminDN =
                        (HtmlTextInput)form.getInputByName("DS_DIRMGRDN");
                txtDirAdminDN.setValueAttribute((String)map.
                        get(TestConstants.KEY_ATT_DS_DIRMGRDN));
                
                HtmlPasswordInput txtDirAdminPassword =
                        (HtmlPasswordInput)form.
                        getInputByName("DS_DIRMGRPASSWD");
                txtDirAdminPassword.setValueAttribute((String)map.
                        get(TestConstants.KEY_ATT_DS_DIRMGRPASSWD));
                
                HtmlCheckBoxInput chkLoadUMS =
                        (HtmlCheckBoxInput)form.getInputByName("DS_UM_SCHEMA");
                if (((String)map.get(TestConstants.KEY_ATT_LOAD_UMS)).
                        equals("yes"))
                    chkLoadUMS.setChecked(true);
                else
                    chkLoadUMS.setChecked(false);
            }
            try {
                page = (HtmlPage)form.submit();
                log(Level.FINEST, "configureProduct", "Returned Page:\n" +
                        page.asXml());
            } catch (com.gargoylesoftware.htmlunit.ScriptException e) {
            }
            if ((getHtmlPageStringIndex(page, "Status: Failed") != -1)) {
                log(Level.SEVERE, "configureProduct",
                        "Product Configuration was" +
                        " not successfull. Configuration failed.");
                exiting("configureProduct");
                return false;
            }
            String strNewURL = (String)map.get("serverurl") +
                    (String)map.get("serveruri") + "/UI/Login" + "?" +
                    "IDToken1=" + map.get(TestConstants.KEY_ATT_AMADMIN_USER) +
                    "&IDToken2=" +
                    map.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD);
            log(Level.FINE, "configureProduct", "strNewURL: " + strNewURL);
            url = new URL(strNewURL);
            try {
                page = (HtmlPage)webclient.getPage(url);
            } catch (com.gargoylesoftware.htmlunit.ScriptException e) {
            }
            if ((getHtmlPageStringIndex(page, "Authentication Failed") != -1) ||
                    (getHtmlPageStringIndex(page, "configurator.jsp") != -1)) {
                log(Level.SEVERE, "configureProduct",
                        "Product Configuration was" +
                        " not successfull. Configuration failed.");
                exiting("configureProduct");
                return false;
            } else {
                log(Level.FINE, "configureProduct",
                        "Product Configuration was" +
                        " successfull. New bits were successfully configured.");
                strNewURL = (String)map.get("serverurl") +
                        (String)map.get("serveruri") + "/UI/Logout";
                consoleLogout(webclient, strNewURL);
                exiting("configureProduct");
                return true;
            }
        } else {
            String strNewURL = (String)map.get("serverurl") +
                    (String)map.get("serveruri") + "/UI/Login" + "?" +
                    "IDToken1=" + adminUser + "&IDToken2=" +
                    map.get(TestConstants.KEY_ATT_AMADMIN_PASSWORD);
            log(Level.FINE, "configureProduct", "strNewURL: " +
                    strNewURL);
            url = new URL(strNewURL);
            page = (HtmlPage)webclient.getPage(url);
            if (getHtmlPageStringIndex(page,
                    "Authentication Failed") != -1) {
                log(Level.FINE, "configureProduct",
                        "Product was already configured. " +
                        "Super admin login failed.");
                exiting("configureProduct");
                return false;
            } else {
                log(Level.FINE, "configureProduct", "Product was " +
                        "already configured. " +
                        "Super admin login successful.");
                strNewURL = (String)map.get("serverurl") +
                        (String)map.get("serveruri") + "/UI/Logout";
                consoleLogout(webclient, strNewURL);
                exiting("configureProduct");
                return true;
            }
        }
    }
    
    /**
     * Logout from admin console using htmlunit
     */
    protected void consoleLogout(
            WebClient webclient,
            String amUrl)
            throws Exception {
        entering("consoleLogout", null);
        log(Level.FINEST, "consoleLogout", "JavaScript Enabled: " +
                webclient.isJavaScriptEnabled());
        log(Level.FINEST, "consoleLogout", "Redirect Enabled: " +
                webclient.isRedirectEnabled());
        log(Level.FINEST, "consoleLogout", "URL: " + amUrl);
        URL url = new URL(amUrl);
        HtmlPage page = (HtmlPage)webclient.getPage(amUrl);
        log(Level.FINEST, "consoleLogout", "Page title after logout: " +
                page.getTitleText());
        exiting("consoleLogout");
    }
    
    /**
     * Checks whether the string exists on the page
     */
    protected int getHtmlPageStringIndex(
            HtmlPage page,
            String searchStr)
            throws Exception {
        entering("getHtmlPageStringIndex", null);
        String strPage;
        try {
            strPage = page.asXml();
        } catch (java.lang.NullPointerException npe) {
            log(Level.FINEST, "getHtmlPageStringIndex", "Page object is NULL");
            return 0;
        }
        log(Level.FINEST, "getHtmlPageStringIndex", "Search string: " +
                searchStr);
        log(Level.FINEST, "getHtmlPageStringIndex", "Search page\n:" +
                strPage);
        int iIdx = strPage.indexOf(searchStr);
        if (iIdx != -1)
            log(Level.FINEST, "getHtmlPageStringIndex",
                    "Search string found on page: " + iIdx);
        else
            log(Level.FINEST, "getHtmlPageStringIndex",
                    "Search string not found on page: " + iIdx);
        exiting("getHtmlPageStringIndex");
        return iIdx;
    }
    
    /**
     * Checks whether the string exists on the page and optionally logs page
     */
    protected int getHtmlPageStringIndex(
            HtmlPage page,
            String searchStr,
            boolean isLog)
            throws Exception {
        entering("getHtmlPageStringIndex", null);
        String strPage;
        try {
            strPage = page.asXml();
        } catch (java.lang.NullPointerException npe) {
            log(Level.FINEST, "getHtmlPageStringIndex", "Page object is NULL");
            return 0;
        }
        int iIdx = strPage.indexOf(searchStr);
        if (isLog){
            log(Level.FINEST, "getHtmlPageStringIndex", "Search page\n:" +
                    strPage);
            if (iIdx != -1)
                log(Level.FINEST, "getHtmlPageStringIndex",
                        "Search string found on page: " + iIdx);
            else
                log(Level.FINEST, "getHtmlPageStringIndex",
                        "Search string not found on page: " + iIdx);
        }
        exiting("getHtmlPageStringIndex");
        return iIdx;
    }
    
    /**
     * Reads data from a Map object, creates a new file and writes data to that
     * file
     */
    protected void createFileFromMap(Map properties, String fileName)
    throws Exception {
        entering("createFileFromMap", null);
        log(Level.FINEST, "createFileFromMap", "Map: " + properties);
        log(Level.FINEST, "createFileFromMap", "fileName: " + fileName);
        StringBuffer buff = new StringBuffer();
        for (Iterator i = properties.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            String valueString = entry.getValue().toString();
            buff.append(entry.getKey());
            buff.append("=");
            if (valueString.length() != 0)
                buff.append(valueString.substring(0, valueString.length()));
            buff.append("\n");
        }
        
        BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
        out.write(buff.toString());
        out.close();
        exiting("createFileFromMap");
    }
    
    /**
     * Reads data from a ResourceBundle object and creates a Map containing all
     * the attribute keys and values. It also takes in a Set of attribute key
     * names, which if specified, are not put into the Map. This is to ensure
     * selective selection of attribute key and value pairs. One can further
     * specify to search for a key containig a specific string (str).
     */
    protected Map getMapFromResourceBundle(String rbName, String str, Set set)
    throws Exception {
        entering("getMapFromResourceBundle", null);
        Map map = new HashMap();
        ResourceBundle rb = ResourceBundle.getBundle(rbName);
        for (Enumeration e = rb.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            String value = (String)rb.getString(key);
            if (set != null) {
                if (!set.contains(key)) {
                    if (str != null) { 
                       if (key.indexOf(str) != -1)
                            map.put(key, value);
                    } else
                        map.put(key, value);
                }
            } else {
                if (str != null) {
                    if (key.indexOf(str) != -1)
                        map.put(key, value);
                } else
                    map.put(key, value);
            }
        }
        exiting("getMapFromResourceBundle");
        return (map);
    }
    
    /**
     * Reads data from a ResourceBundle object and creates a Map containing all
     * the attribute keys and values.
     * @param resourcebundle name
     */
    protected Map getMapFromResourceBundle(String rbName)
    throws Exception {
        return (getMapFromResourceBundle(rbName, null, null));
    }

    /**
     * Reads data from a ResourceBundle object and creates a Map containing all
     * the attribute keys and values. One can further specify to search for a
     * key containig a specific string (str)
     * @param resourcebundle name
     * @param str string to match contained in the key
     */
    protected Map getMapFromResourceBundle(String rbName, String str)
    throws Exception {
        return(getMapFromResourceBundle(rbName, str, null));
    }
    
    /**
     * Returns a map of String to Set of String from a formatted string.
     * The format is
     * <pre>
     * &lt;key1&gt;=&lt;value11&gt;,&lt;value12&gt;...,&lt;value13&gt;;
     * &lt;key2&gt;=&lt;value21&gt;,&lt;value22&gt;...,&lt;value23&gt;; ...
     * &lt;keyn&gt;=&lt;valuen1&gt;,&lt;valuen2&gt;...,&lt;valuen3&gt;
     * </pre>
     */
    protected Map<String, Set<String>> parseStringToMap(String str)
    throws Exception {
        entering("parseStringToMap", null);
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        StringTokenizer st = new StringTokenizer(str, ";");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int idx = token.indexOf("=");
            if (idx != -1) {
                Set<String> set = new HashSet<String>();
                map.put(token.substring(0, idx).trim(), set);
                StringTokenizer st1 = new StringTokenizer(
                        token.substring(idx+1), ",");
                while (st1.hasMoreTokens()) {
                    set.add(st1.nextToken().trim());
                }
            }
        }
        exiting("parseStringToMap");
        return map;
    }
    
    /**
     * Returns set of string. This is a convenient method for adding a set of
     * string into a map. In this project, we usually have the
     * <code>Map&lt;String, Set&lt;String&gt;&gt; and many times, we just
     * want to add a string to the map.
     */
    protected Set<String> putSetIntoMap(
            String key,
            Map<String, Set<String>> map,
            String value
            )
    throws Exception {
        entering("putSetIntoMap", null);
        Set<String> set = new HashSet<String>();
        set.add(value);
        map.put(key, set);
        exiting("putSetIntoMap");
        return set;
    }
    
    /**
     * Returns LoginURL based on the realm under test
     * @param realm
     * @return loginURL
     */
    protected String getLoginURL(String strOrg) {
        entering("getLoginURL", null);
        String loginURL;
        if ((strOrg.equals("")) || (strOrg.equalsIgnoreCase("/"))) {
            loginURL = protocol + ":" + "//" + host + ":" + port + uri
                    + "/UI/Login";
        } else {
            loginURL = protocol + ":" + "//" + host + ":" + port + uri
                    + "/UI/Login" + "?org=" + strOrg ;
        }
        exiting("getLoginURL");
        return loginURL;
    }
    
    /**
     * Returns the List for the given tokens
     * @param string tokens
     * @return list of the tokens
     */
    protected List getListFromTokens(StringTokenizer strTokens)
    throws Exception {
        entering("getListFromTokens", null);
        List<String> list = new ArrayList<String>();
        while (strTokens.hasMoreTokens()) {
            list.add(strTokens.nextToken());
        }
        exiting("getListFromTokens");
        return list;
    }
    
    /*
     * Gets the baseDirectory to create the XML files
     */
    protected String getTestBase()
    throws Exception {
        entering("getTestBase", null);
        String testbaseDir = null;
        ResourceBundle rbamconfig = ResourceBundle.getBundle(
                TestConstants.TEST_PROPERTY_AMCONFIG);
        testbaseDir = getBaseDir() + fileseparator
                + rbamconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
                + fileseparator + "built"
                + fileseparator + "classes"
                + fileseparator ;
        exiting("getTestBase");
        return (testbaseDir);
    }
    
    /**
     * Takes a token separated string and returns each individual
     * token as part of a list.
     */
    protected List getAttributeList(String strList, String token)
    throws Exception {
        entering("getAttributeList", null);
        StringTokenizer stk = new StringTokenizer(strList, token);
        List<String> attList = new ArrayList<String>();
        while (stk.hasMoreTokens()) {
            attList.add(stk.nextToken());
        }
        exiting("getAttributeList");
        return (attList);
    }
    
    /**
     * Takes a token separated string and returns each individual
     * token as part of a Map.
     */
    protected Map getAttributeMap(String strList, String token)
    throws Exception {
        entering("getAttributeMap", null);
        StringTokenizer stk = new StringTokenizer(strList, token);
        Map map = new HashMap();
        int idx;
        String strToken;
        while (stk.hasMoreTokens()) {
            strToken = stk.nextToken();
            idx = strToken.indexOf("=");
            map.put(strToken.substring(0, idx), strToken.substring(idx + 1,
                    strToken.length()));
        }
        log(Level.FINEST, "getAttributeMap", map);
        exiting("getAttributeMap");
        return (map);
    }
    
    /**
     * Returns set of string. This is a convenient method for adding multipe set
     * of string into a map. The value contains the multiple string sepearete by
     * token string.
     */
    protected Set<String> putSetIntoMap(
            String key,
            Map<String, Set<String>> map,
            String value,
            String token
            )
            throws Exception {
        entering("putSetIntoMap", null);
        StringTokenizer stk = new StringTokenizer(value, token);
        Set<String> setValue = new HashSet<String>();
        while (stk.hasMoreTokens()) {
            setValue.add(stk.nextToken());
        }
        map.put(key, setValue);
        exiting("putSetIntoMap");
        return setValue;
    }
    
    /**
     * Concatenates second set to a first set
     * @param set1 first set
     * @param set2 second set to be concatenated with the first set
     */
    protected void concatSet(Set set1, Set set2)
    throws Exception {
        entering("concatSet", null);
        Iterator keyIter = set2.iterator();
        String item;
        while (keyIter.hasNext()) {
            item = (String)keyIter.next();
            set1.add(item);
        }
        exiting("concatSet");
    }
    
    /**
     * Returns true if the value set contained in the Set
     * contains the requested string.
     */
    protected boolean setValuesHasString(Set set, String str)
    throws Exception {
        entering("setValuesHasString", null);
        log(Level.FINEST, "setValuesHasString", "The values in the set are:\n" +
                set);
        boolean res = false;
        Iterator keyIter = set.iterator();
        String item;
        Object obj;
        while (keyIter.hasNext()) {
            obj = (Object)keyIter.next();
            item = obj.toString();
            if (item.indexOf(str) != 0) {
                res = true;
                break;
            }
        }
        exiting("setValuesHasString");
        return res;
    }
    
    /**
     * Returns protocol, host, port and uri from a given url.
     * Map contains value pairs in the form of:
     * protocol, protocol value
     * host, host value
     * port, port value
     * uri, uri value
     */
    protected Map getURLComponents(String strNamingURL)
    throws Exception {
        entering("getURLComponents", null);
        Map map = new HashMap();
        int iFirstSep = strNamingURL.indexOf(":");
        String strProtocol = strNamingURL.substring(0, iFirstSep);
        map.put("protocol", strProtocol);
        
        int iSecondSep = strNamingURL.indexOf(":", iFirstSep + 1);
        String strHost = strNamingURL.substring(iFirstSep + 3, iSecondSep);
        map.put("host", strHost);
        
        int iThirdSep = strNamingURL.indexOf(uriseparator, iSecondSep + 1);
        String strPort = strNamingURL.substring(iSecondSep + 1, iThirdSep);
        map.put("port", strPort);
        
        int iFourthSep = strNamingURL.indexOf(uriseparator, iThirdSep + 1);
        String strURI = uriseparator + strNamingURL.substring(iThirdSep + 1,
                iFourthSep);
        map.put("uri", strURI);
        exiting("getURLComponents");
        
        return (map);
    }
    
    /**
     * Replace all tags in a file with actual value that are defined in a map
     * @param inFile input file name to be replaced with the tag
     * @param outFile output file name with actual value
     * @param valMap a map contains tag name and value i.e.
     * [ROOT_SUFFIX, dc=sun,dc=com]
     */
    protected void replaceStringInFile(String inFile, String outFile,
            Map valMap)
    throws Exception {
        entering("replaceStringInFile", null);
        String key = null;
        String value = null;
        String outputStr = null;
        Iterator keyIter;
        BufferedReader buff = new BufferedReader(new FileReader(inFile));
        StringBuffer sb = new StringBuffer();
        for (String inputStr = buff.readLine(); (inputStr != null);
        inputStr = buff.readLine()) {
            keyIter = valMap.keySet().iterator();
            while (keyIter.hasNext()) {
                key = (String)keyIter.next();
                value = (String)valMap.get(key);
                inputStr = inputStr.replaceAll(key, value);
            }
            sb.append(inputStr + "\n");
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
        out.write(sb.toString());
        out.close();
        exiting("replaceStringInFile");
    }
    
    /**
     * Returns the SSOToken of a user.
     */
    protected SSOToken getUserToken(SSOToken requester, String userId)
    throws Exception {
        entering("getUserToken", null);
        SSOToken stok = null;
        if (validateToken(requester)) {
            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            Set set = stMgr.getValidSessions(requester, host);
            Iterator it = set.iterator();
            String strLocUserID;
            while (it.hasNext()) {
                stok = (SSOToken)it.next();
                strLocUserID = stok.getProperty("UserId");
                log(Level.FINEST, "getUserToken", "UserID: " + strLocUserID);
                if (strLocUserID.equals(userId))
                    break;
            }
        }
        exiting("getUserToken");
        return (stok);
    }
    
    /**
     * Start the notification (jetty) server for getting notifications from the
     * server.
     */
    protected Map startNotificationServer()
    throws Exception {
        Map map = new HashMap();
        String strNotURL = rb_amconfig.getString(
                TestConstants.KEY_AMC_NOTIFICATION_URL);
        log(Level.FINEST, "startNotificationServer", "Notification URI: " +
                strNotURL);
        Map notificationURLMap = getURLComponents(strNotURL + "/");

        String  strPort = (String)notificationURLMap.get("port");
        String  strURI = (String)notificationURLMap.get("uri");

        log(Level.FINEST, "startNotificationServer", "Notification Port: " +
                strPort + ", uri is " + strURI);

        int deployPort  = new Integer(strPort).intValue();
        server = new Server(deployPort);
        log(Level.FINE, "startNotificationServer", "Starting the notification" +
                " (jetty) server");

        String deployURI = rb_amconfig.getString(TestConstants.
                KEY_INTERNAL_WEBAPP_URI);
        log(Level.FINE, "startNotificationServer", "Deploy URI: " +
                deployURI);

        WebAppContext wac = new WebAppContext();
        wac.setContextPath(deployURI);
        String warFile = getBaseDir() + "/data/common/internalwebapp.war";
        log(Level.FINE, "startNotificationServer", "WAR File: " +
                warFile);
        wac.setWar(warFile);
        log(Level.FINE, "startNotificationServer", "Deploy URI: " +
                deployURI);
        server.setHandler(wac);
        server.start();

        map = registerNotificationServerURL();
        log(Level.FINE, "startNotificationServer", "Registered the " +
               "notification url");
        return map;
    }

    /**
     * Stop the notification (jetty) server for getting notifications from the
     * server.
     */
    protected void stopNotificationServer(Map notificationIDMap)
    throws Exception {
       log(Level.FINE, "stopNotificationServer", "Stopping the notification" +
                " (jetty) server");
        String strNotURL = rb_amconfig.getString(
                TestConstants.KEY_AMC_NOTIFICATION_URL);
        server.stop();
        WebClient wc = new WebClient();
        HtmlPage jettypage = (HtmlPage)wc.getPage(strNotURL);
        int i = 0;
        while((jettypage.getWebResponse().getContentAsString().contains("jetty"))
                && (i < 60)){
            log(Level.FINE, "startNotificationServer", "Jetty server is up. " +
                    "Waiting for the jetty process to die");
            Thread.sleep(5000);
            jettypage = (HtmlPage)wc.getPage(strNotURL);
            i++;
        }
        if (jettypage.getWebResponse().getContentAsString().contains("jetty")) {
             log(Level.SEVERE, "startNotificationServer", "Jetty server is " +
                     "Still up. Couldn't shut down the server");
        }
        deregisterNotificationServerURL(notificationIDMap);
    }

    /**
     * Register the notification (jetty) server for getting notifications from
     * the server.
     */
    protected Map registerNotificationServerURL()
    throws Exception {
        Map notificationIDMap = new HashMap();
        log(Level.FINE, "registerNotificationServerURL", "Register the " +
                "notification (jetty) server");
        String strNotURL = rb_amconfig.getString(
                TestConstants.KEY_AMC_NOTIFICATION_URL);
         //For SMS
        SOAPClient client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);
        String strNotificationID1 = (String)client.
                send("registerNotificationURL", strNotURL, null, null);
        notificationIDMap.put("ID1", strNotificationID1);
        //For IDRepo
        client = new SOAPClient("DirectoryManagerIF");
        // Register for AMSDK notifications
        String strNotificationID2 = (String)client.
                send("registerNotificationURL", strNotURL, null, null);
        notificationIDMap.put("ID2", strNotificationID2);
        // Register for IdRepo Service
        String strNotificationID3 = (String)client.
                send("registerNotificationURL_idrepo", strNotURL, null, null);
        notificationIDMap.put("ID3", strNotificationID3);
        return notificationIDMap;
    }

    /**
     * Deregister the notification (jetty) server for getting notifications from
     * the server.
     */
    protected void deregisterNotificationServerURL(Map notificationIDMap)
    throws Exception {
        log(Level.FINE, "deregisterNotificationServerURL", "Deregister the " +
                "notification (jetty) server");
        String strID1 = (String)notificationIDMap.get("ID1");
        SOAPClient client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);
        client.send("deRegisterNotificationURL", strID1, null, null);
        //For IDRepo
        client = new SOAPClient("DirectoryManagerIF");
        // Register for AMSDK notifications
        String strID2 = (String)notificationIDMap.get("ID2");
        client.send("deRegisterNotificationURL", strID2, null, null);
        // Register for IdRepo Service
        String strID3 = (String)notificationIDMap.get("ID3");
        client.send("deRegisterNotificationURL_idrepo", strID3, null, null);
        log(Level.FINE, "deregisterNotificationServerURL", "Completed " +
                "deregistering the notification url's");
    }

    /**
     * Replaces the Redirect uri & the search strings in the authentication
     * properites files under the directory
     * <QATESTHOME>/<SERVER_NAME1>/built/classes
     */
    public void replaceRedirectURIs(String strModule)
    throws Exception {
        entering("replaceRedirectURIs", null);
        try {
            File directory;
            String[] files;
            String ext = "properties";
            String directoryName;
            String fileName;
            String absFileName;
            String redirecturi[] = new String[6];
            for (int i = 0; i < 6; i++) {
                redirecturi[i] = "redirecturl" + i + ".html";
            }

            String strNotURL = rb_amconfig.getString(TestConstants.
                    KEY_AMC_NOTIFICATION_URL);
            Map notificationURLMap = getURLComponents(strNotURL + "/");
            log(Level.FINEST, "replaceRedirectURIs", "notificationURLMap: " +
                    notificationURLMap.toString());

            String  deployProto = (String)notificationURLMap.get("protocol");
            String  strPort = (String)notificationURLMap.get("port");
            String strHostname = (String)notificationURLMap.get("host");
            int deployPort  = new Integer(strPort).intValue();
            String deployURI = rb_amconfig.getString(TestConstants.
                    KEY_INTERNAL_WEBAPP_URI);

            String clientURL;
            if (strModule.equals("samlv2") ||
                    strModule.equals("samlv2idpproxy") ||
                    strModule.equals("idff"))
                clientURL = deployProto + "://" + strHostname +  ":" +
                        deployPort + deployURI + uriseparator + "federation";
            else
                clientURL = deployProto + "://" + strHostname +  ":" +
                        deployPort + deployURI + uriseparator + strModule;
            log(Level.FINEST, "replaceRedirectURIs", "clientURL: " + clientURL);

            Map replaceVals = new HashMap();
            for (int j = 0; j < 6; j++) {
                replaceVals.put("REDIRECT_URI" + j, clientURL + "/" +
                        redirecturi[j]);
              replaceVals.put("REDIRECT_URI_SEARCH_STRING" + j, redirecturi[j]);
            }
            log(Level.FINEST, "replaceRedirectURIs", "replaceVals: " +
                    replaceVals.toString());

            directoryName = getTestBase() + strModule;
            log(Level.FINEST, "replaceRedirectURIs", "directoryName: " +
                    directoryName);
            directory = new File(directoryName);
            assert (directory.exists());
            files = directory.list();

            for (int i = 0; i < files.length; i++) {
                fileName = files[i];
                if (fileName.endsWith(ext.trim())) {
                    absFileName = directoryName + fileseparator + fileName;
                    log(Level.FINEST, "replaceRedirectURIs", "Replacing the" +
                            " file :" + absFileName);
                    replaceString(absFileName, replaceVals, absFileName);
                }
            }
        } catch (Exception e) {
            log(Level.SEVERE, "replaceRedirectURIs", e.getMessage());
            e.printStackTrace();
        }
        exiting("replaceRedirectURIs");
    }

    
    /**
     * Converts attrValPair into Map containing attrName as key
     * and values as set.
     * @param attrValPair Attribute value pair in the format
     * attrval1=val1,val2|attrval2=val11,val12
     * @return Map containing attrName and values as set.
     */
    protected Map attributesToMap(String attrValPair)
    throws Exception {
        entering("attributesToMap", null);
        Map attrMap = new HashMap();
        if ((attrValPair != null) && (attrValPair.length() > 0)) {
            StringTokenizer tokens = new StringTokenizer(attrValPair, "|");
            while (tokens.hasMoreTokens()) {
                StringTokenizer attrToken =
                        new StringTokenizer(tokens.nextToken(), "=");
                String attrName = attrToken.nextToken();
                Set valSet = new HashSet();
                StringTokenizer valueTokens =
                        new StringTokenizer(attrToken.nextToken(), ",");
                if (valueTokens.countTokens() <= 0) {
                    valSet.add(attrToken.nextToken());
                } else {
                    while (valueTokens.hasMoreTokens()) {
                        valSet.add(valueTokens.nextToken());
                    }
                }
                attrMap.put(attrName, valSet);
            }
        } else {
            throw new RuntimeException("Attributes value pair cannot be null");
        }
        exiting("attributesToMap");
        return attrMap;
    }
    
    /**
     * Compares two maps returns true if both are equal else false
     * @param newValMap Atrribute values this should be subset of updateValMap
     * @param updateValMap Atrribute values
     * @return true if bothe the maps are equal.
     */
    protected boolean isAttrValuesEqual(Map newValMap, Map updateValMap)
    throws Exception {
        entering("isAttrValuesEqual", null);
        boolean equal;
        if (newValMap != null && updateValMap != null){
            Set updatedKeys = newValMap.keySet();
            Iterator itr1 = updatedKeys.iterator();
            while (itr1.hasNext()) {
                String key = (String)itr1.next();
                Set val1Set = (Set)newValMap.get(key);
                Set val2Set = (Set)updateValMap.get(key);
                equal = val1Set.equals(val2Set);
                if (!equal) {
                    return false;
                }
            }
        } else {
            return false;
        }
        exiting("isAttrValuesEqual");
        return true;
    }
    
    /**
     * Returns set of properties from a given resource file
     * @param file resource file
     * @return set of properties
     */
    protected Properties getProperties(String file)
    throws MissingResourceException {
        entering("getProperties", null);
        Properties properties = new Properties();
        ResourceBundle bundle = ResourceBundle.getBundle(file);
        Enumeration e = bundle.getKeys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = bundle.getString(key);
            properties.put(key, value);
        }
        exiting("getProperties");
        return properties;
    }
    
    /**
     * Returns all SSOTokens of a user.
     */
    protected Set getAllUserTokens(SSOToken requester, String userId)
    throws Exception {
        entering("getAllUserTokens", null);
        SSOToken stok = null;
        Set setAllToken = new HashSet();
        if (validateToken(requester)) {
            SSOTokenManager stMgr = SSOTokenManager.getInstance();
            Set set = stMgr.getValidSessions(requester, host);
            Iterator it = set.iterator();
            String strLocUserID;
            while (it.hasNext()) {
                stok = (SSOToken)it.next();
                strLocUserID = stok.getProperty("UserId");
                log(Level.FINEST, "getAllUserTokens", "UserID: " +
                        strLocUserID);
                if (strLocUserID.equalsIgnoreCase(userId)) {
                    setAllToken.add(stok);
                }
            }
        }
        exiting("getAllUserTokens");
        return setAllToken;
    }
    
    /**
     * Replaces the strings given in the map in the input file and wirtes to the
     * output file
     * @param file input file (absolute file path)
     * @param Map with name value pairs like ("SM_SUFFIX", basedn)
     * @param file output file (absolute file path)
     * @param encoding
     */
    protected void replaceString(String inputFN, Map nvp,
            String outputFN, String enc) 
    throws Exception {        
        entering("replaceString", null);
        FileInputStream in = null;
        BufferedWriter out = null;
        try {
            File file = new File(inputFN);
            byte[] data = new byte[(int)file.length()];
            in = new FileInputStream(file);
            in.read(data);
            StringBuffer buf = new StringBuffer(new String(data));
            Set keys = nvp.keySet();
            Object iter[] = sort(keys.toArray());
            for (int i = 0; i < iter.length; i++){
                String key = (String)iter[i];
                String value = (String)nvp.get(key);
                replaceToken(buf, key, value);
            }
            if(outputFN != null) {
                try {
                    out = new BufferedWriter(new OutputStreamWriter
                            (new FileOutputStream(outputFN),enc));
                } catch (java.io.UnsupportedEncodingException ex) {
                    out = new BufferedWriter(new OutputStreamWriter
                            (new FileOutputStream(outputFN),"ISO8859_1"));
                }
                String str = buf.toString();
                out.write(str, 0, str.length());
                out.flush();
            }
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            in.close();
            out.close();
        }
        exiting("replaceString");
        return;
    }
    
    /**
     * Replaces the strings given in the map in the input file and wirtes to the
     * output file
     * @param file input file (absolute file path)
     * @param Map with name value pairs like ("SM_SUFFIX", basedn)
     * @param file output file (absolute file path)
     */
    protected void replaceString(String inputFN, Map nvp,
            String outputFN) 
    throws Exception {
        String enc = System.getProperty("file.encoding");
        replaceString(inputFN, nvp, outputFN, enc);
    }
    
    /**
     * Replaces the strings in a string buffer
     * @param String buffer
     * @param key to be replaced
     * @param value to be replaced
     */
    protected void replaceToken(StringBuffer buf, String key,
            String value) {
        entering("replaceToken", null);
        if (key == null || value == null || buf == null)
            return;
        int loc = 0, keyLen = key.length(), valLen = value.length();
        while ((loc = buf.toString().indexOf(key, loc)) != -1) {
            buf.replace(loc, loc + keyLen, value);
            loc = loc + valLen;
        }
        exiting("replaceToken");
        return;
    }
    
    protected Set getListFromHtmlPage(HtmlPage page)
    throws Exception {
        entering("getListFromHtmlPage", null);
        Set set = new HashSet();
        String strP = page.asXml();
        int iExit = strP.indexOf("<!-- CLI Exit Code: 0 -->");
        int iColon = strP.lastIndexOf(":", iExit);
        String strSub = strP.substring(iColon +1, iExit).trim();
        StringTokenizer st = new StringTokenizer(strSub, "\n");
        while (st.hasMoreTokens()) {
            set.add(st.nextToken());
        }
        log(Level.FINEST, "getListFromHtmlPage", "The list is as follows: " +
                set.toString());
        exiting("getListFromHtmlPage");
        return (set);
    }

    /**
     * Sorts the keys in the map
     * returns sorted object []
     */
    protected Object[] sort(Object objArray[])
    throws Exception {
        entering("sort", null);
        for (int i = objArray.length; --i >= 0; ) {
            for (int j = 0; j < i; j++) {
                if (((String)objArray[j]).length() <
                        ((String)objArray[j+1]).length()) {
                    Object T = (String)objArray[j+1];
                    objArray[j+1] = objArray[j];
                    objArray[j] = T;
                }
            }
        }
        exiting("setValuesHasString");
        return objArray;
    }

    /**
     * Get the property value of a property in showServerConfig.jsp
     * @webClient - the WebClient object that will used to emulate the client
     * browser
     * @propName - the name of the property whose value should be retrieved
     * from the contents of showServerConfig.jsp
     */
    public String getServerConfigValue(WebClient webClient, String propName)
    throws Exception {
        String propValue = null;
        boolean accessConsole = false;
        try {
            if (tableContents == null) {
                accessConsole = true;
                HtmlPage consolePage = consoleLogin(webClient,
                        getLoginURL(realm), adminUser, adminPassword);
                if (consolePage != null) {
                    String configJSPUrl = protocol + ":" + "//" + host + ":" +
                            port + uri + "/showServerConfig.jsp";
                    HtmlPage configJSPPage =
                            (HtmlPage) webClient.getPage(configJSPUrl);
                    if (configJSPPage != null) {
                        String jspContents =
                                configJSPPage.getWebResponse().
                                getContentAsString();
                        int tableStartIndex =
                                jspContents.indexOf("<table border=\"1\">");
                        if (tableStartIndex != -1) {
                            int tableEndIndex = jspContents.indexOf("</table>",
                                    tableStartIndex);
                            if (tableEndIndex != -1) {
                                tableContents =
                                        jspContents.substring(tableStartIndex,
                                        tableEndIndex);
                            } else {
                                log(Level.SEVERE, "getServerConfigValue",
                                        "Did not find the end of the table " +
                                        "in " + configJSPPage + ".");
                            }
                        } else {
                            log(Level.SEVERE, "getServerConfigValue",
                                    "Did not find the start of the table " +
                                    "in " + configJSPPage + ".");
                        }
                    } else {
                        log(Level.SEVERE, "getServerConfigValue",
                        "Unable to access " + configJSPPage + ".");
                    }
                } else {
                    log(Level.SEVERE, "getServerConfigValue",
                            "Unable to login to the console");
                }
            }

            int propIndex = tableContents.indexOf(propName);
            if (propIndex != -1) {
                int valueStartIndex =
                        tableContents.indexOf("<td>", propIndex +
                        propName.length());
                if (valueStartIndex != -1) {
                    int valueEndIndex =
                            tableContents.indexOf("</td>",
                            valueStartIndex);
                    if (valueEndIndex != -1) {
                        propValue =
                                tableContents.substring(
                                valueStartIndex + 4,
                                valueEndIndex).trim();
                        propValue = propValue.replace('\n', ' ').
                                replace("\r", "");

                        log(Level.FINEST,
                                "getServerConfigValue",
                                "The value of " + propName +
                                " is " + propValue  + ".");
                    } else {
                        log(Level.SEVERE,
                                "getServerConfigValue",
                                "Did not find end tag for " +
                                "property " + propName + ".");
                    }
                } else {
                    log(Level.SEVERE, "getServerConfigValue",
                            "Did not find start tag for " +
                            "property " + propName + ".");
                }
            } else {
                log(Level.SEVERE, "getServerConfigValue",
                        "Did not find the configuration " +
                        "property " + propName + ".");
            }
        } catch (Exception e) {
            log(Level.SEVERE, "setup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            if (accessConsole) {
                String logoutURL = protocol + ":" + "//" + host + ":" + port +
                            uri + "/UI/Logout";
                consoleLogout(webClient, logoutURL);
            }
            return propValue;
        }
    }
}
