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
 * $Id: EmbeddedOpenDS.java,v 1.5 2007-12-07 21:25:59 rajeevangal Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.setup;

import com.sun.identity.shared.debug.Debug;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletContext;


import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.extensions.ConfigFileHandler;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.InitializationException;
import org.opends.messages.Message;
import org.opends.server.util.EmbeddedUtils;
import org.opends.server.types.DirectoryEnvironmentConfig;
import org.opends.server.extensions.SaltedSHA512PasswordStorageScheme;

// Until we have apis to setup replication we will use the clin interface.
import org.opends.guitools.replicationcli.ReplicationCliMain;

/**
  * This class encapsulates all <code>OpenDS</code>  dependencies.
  * All the interfaces are invoked from <code>AMSetupServlet</code> class
  * at different points : initial installation, normal startup and
  * normal shutdown of the embedded <code>OpenDS</code> instance.
  */
public class EmbeddedOpenDS {

    private static boolean serverStarted = false;

    /**
     * Returns <code>true</code> if the server has already been started.
     *
     * @return <code>true</code> if the server has already been started.
     */ 
    public static boolean isStarted()
    {
        return serverStarted;
    }

    /**
     * Sets up embedded opends during initial installation :
     * <ul>
     * <li>lays out the filesystem directory structure needed by opends
     * <li>sets up port numbers for ldap and replication
     * <li>invokes <code>EmbeddedUtils</code> to start the embedded server.
     * </ul>
     *
     *  @param map Map of properties collected by the configurator.
     *  @param servletCtx Servlet Context to read deployed war contents.
     *  @throws Exception on encountering errors.
     */
    public static void setup(Map map, ServletContext servletCtx)
        throws Exception {
        String basedir = (String)map.get(SetupConstants.CONFIG_VAR_BASE_DIR);
        String odsRoot = basedir + "/" +
            SetupConstants.SMS_OPENDS_DATASTORE;
        new File(basedir).mkdir();
        new File(odsRoot).mkdir();
        String[] subDirectories =
        { "adminDb", "bak", "bin", "changelogDb", "classes",
          "config", "db", "db_verify", "ldif", "lib",
          "locks", "logs", "db_rebuild", "db_unindexed",
          "db_index_test", "db_import_test", "config/schema", 
          "config/upgrade" };

        // create sub dirs
        for (int i = 0; i < subDirectories.length; i++) {
            new File(odsRoot, subDirectories[i]).mkdir();
        }

        // copy files
        String[] files = {
            //"config/upgrade/schema.ldif.3056",
            //"config/upgrade/config.ldif.3056",
            "config/upgrade/schema.ldif.3463",
            "config/upgrade/config.ldif.3463",
            "config/config.ldif",
            "config/admin-backend.ldif",
            "config/famsuffix.ldif",
            "config/schema/00-core.ldif",
            "config/schema/01-pwpolicy.ldif",
            "config/schema/02-config.ldif",
            "config/schema/03-changelog.ldif",
            "config/schema/03-rfc2713.ldif",
            "config/schema/03-rfc2714.ldif",
            "config/schema/03-rfc2739.ldif",
            "config/schema/03-rfc2926.ldif",
            "config/schema/03-rfc3112.ldif",
            "config/schema/03-rfc3712.ldif",
            "config/schema/03-uddiv3.ldif",
            "config/schema/04-rfc2307bis.ldif"
        };
        for (int i = 0 ; i < files.length; i++) {
            String file = "/WEB-INF/template/opends/"+files[i];
            InputStreamReader fin = new InputStreamReader(
                servletCtx.getResourceAsStream(file));

            StringBuffer sbuf = new StringBuffer();
            char[] cbuf = new char[1024];
            int len;
            while ((len = fin.read(cbuf)) > 0) {
                sbuf.append(cbuf, 0, len);
            }
            FileWriter fout = null;

            try {
                fout = new FileWriter(odsRoot + "/" + files[i]);
                String inpStr = sbuf.toString();
                fout.write(ServicesDefaultValues.tagSwap(inpStr));
            } catch (IOException e) {
                Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                    "EmbeddedOpenDS.setup(). Error loading ldifs:", e);
                throw e;
            } finally {
                if (fin != null) {
                    try {
                        fin.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }  
                if (fout != null) {
                    try {
                        fout.close();
                    } catch (Exception ex) {
                        //No handling requried
                    }
                }
            }
        }
        SetupProgress.reportStart("emb.installingemb", odsRoot);
        EmbeddedOpenDS.startServer(odsRoot);
        SetupProgress.reportEnd("emb.done", null);
        //java.lang.Thread.sleep(5000);

        // Check: If adding a new server to a existing cluster

        if (!isMultiServer(map)) {
            // Default: single / first server.
            SetupProgress.reportStart("emb.creatingfamsuffix", null);
            EmbeddedOpenDS.shutdownServer("to load ldif");
            EmbeddedOpenDS.loadLDIF(odsRoot, odsRoot+ "/config/famsuffix.ldif");
            EmbeddedOpenDS.startServer(odsRoot);
            SetupProgress.reportEnd("emb.done", null);
        }
        //java.lang.Thread.sleep(5000);
    }

    /**
     *  Starts the embedded <code>OpenDS</code> instance.
     *
     *  @param odsRoot File system directory where <code>OpenDS</code> 
     *                 is installed.
     *
     *  @throws Exception upon encountering errors.
     */
    public static void startServer(String odsRoot) throws Exception {
        if (isStarted()) {
            return;
        }
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        debug.message("EmbeddedOpenDS.startServer("+odsRoot+")");

        DirectoryEnvironmentConfig config = new DirectoryEnvironmentConfig();
        config.setServerRoot(new File(odsRoot));
        config.setForceDaemonThreads(true);
        config.setConfigClass(ConfigFileHandler.class);
        config.setConfigFile(new File(odsRoot+"/config", "config.ldif"));
        debug.message("EmbeddedOpenDS.startServer:starting DS Server...");
        EmbeddedUtils.startServer(config);
        debug.message("...EmbeddedOpenDS.startServer:DS Server started.");

        serverStarted = true;
    }
    

    /**
     *  Gracefully shuts down the embedded opends instance.
     *
     *  @param reason  string representing reasn why shutdown was called.
     *
     *  @throws Exception on encountering errors.
     */
    public static void shutdownServer(String reason) throws Exception {
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        if (isStarted()) {
            debug.message("EmbeddedOpenDS.shutdown server...");
            DirectoryServer.shutDown(
                "com.sun.identity.setup.EmbeddedOpenDS",
                Message.EMPTY);
            debug.message("EmbeddedOpenDS.shutdown server success.");
            serverStarted = false;
        }
    }

    public static void setupReplication(Map map) throws Exception
    {
        // Setup replication
        SetupProgress.reportStart("emb.creatingreplica", null);
        int ret = setupReplicationEnable(map);
        if (ret == 0)
            ret = setupReplicationInitialize(map);
        if (ret == 0) {
            SetupProgress.reportEnd("emb.success", null);
            Debug.getInstance(SetupConstants.DEBUG_NAME).message(
                "EmbeddedOpenDS.setupReplication(). replication setup succeeded.");
        } else {
            SetupProgress.reportEnd("emb.failed", new Integer(ret));
            Debug.getInstance(SetupConstants.DEBUG_NAME).error(
                "EmbeddedOpenDS.setupReplication(). Error setting up replication");
            throw new ConfiguratorException(
                    "configurator.embreplfailed");
        }
    }
    /**
      * Setups replication between two opends sms and user stores.
      * $ dsreplication enable
      *    --no-prompt
      *    --host1 host1 --port1 1389 --bindDN1 "cn=Directory Manager"
      *    --bindPassword1 password --replicationPort1 8989
      *    --host2 host2 --port2 2389 --bindDN2 "cn=Directory Manager"
      *    --bindPassword2 password --replicationPort2 8990 
      *    --adminUID admin --adminPassword password 
      *    --baseDN "dc=example,dc=com"
      *
      *
      *  @param map Map of properties collected by the configurator.
      *  @return status : 0 == success, !0 == failure
      */
    public static int setupReplicationEnable(Map map)
    {
        String[] enableCmd= {
            "enable",                // 0
            "--no-prompt",           // 1
            "--host1",               // 2
            "host1val",              // 3
            "--port1",               // 4
            "port1ival",             // 5
            "--bindDN1",             // 6
            "cn=Directory Manager",  // 7
            "--bindPassword1",       // 8
            "password",              // 9
            "--replicationPort1",    // 10
            "8989",                  // 11
            "--host2",               // 12
            "host2val",              // 13
            "--port2",               // 14
            "port2ival",             // 15
            "--bindDN2",             // 16
            "cn=Directory Manager",  // 17
            "--bindPassword2",       // 18
            "password",              // 19
            "--replicationPort2",    // 20
            "8989",                  // 21
            "--adminUID",            // 22
            "admin",                 // 23
            "--adminPassword",       // 24
            "password",              // 25 
            "--baseDN",              // 26
            "dc=example,dc=com"      // 27
        };
        enableCmd[3] = (String) map.get(SetupConstants.DS_EMB_REPL_HOST2);
        enableCmd[5] = (String) map.get(SetupConstants.DS_EMB_REPL_PORT2);
        enableCmd[9] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        enableCmd[11] = (String) map.get(SetupConstants.DS_EMB_REPL_REPLPORT2);

        enableCmd[13] = "localhost";
        enableCmd[15] = (String) map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT);
        enableCmd[19] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        enableCmd[21] = (String) map.get(SetupConstants.DS_EMB_REPL_REPLPORT1);
        enableCmd[25] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        enableCmd[27] = (String)map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX);

        SetupProgress.reportStart("emb.replcommand",concat(enableCmd));

        int ret = ReplicationCliMain.mainCLI(enableCmd, false, 
                                            SetupProgress.getOutputStream(), 
                                            SetupProgress.getOutputStream(), 
                                            null); 
        if (ret == 0)
            SetupProgress.reportEnd("emb.success", null);
        else
            SetupProgress.reportEnd("emb.failed", null);
        return ret;
    }
    /**
      * Syncs replication data between two opends sms and user stores.
      * $ dsreplication initialize 
      *     --baseDN "dc=example,dc=com" --adminUID admin --adminPassword pass
      *     --hostSource host1 --portSource 1389
      *     --hostDestination host2 --portDestination 2389
      *
      *  @param map Map of properties collected by the configurator.
      *  @return status : 0 == success, !0 == failure
      */
    public static int setupReplicationInitialize(Map map)
    {
        String[] initializeCmd= {
            "initialize",                 // 0
            "--no-prompt",                // 1
            "--baseDN",                   // 2
            "dc=opensso,dc=java,dc=net",  // 3
            "--adminUID",                 // 4
            "admin",                      // 5
            "--adminPassword",            // 6
            "password",                   // 7
            "--hostSource",               // 8
            "localhost",                  // 9
            "--portSource",               // 10
            "50389",                      // 11
            "--hostDestination",          // 12
            "localhost",                  // 13
            "--portDestination",          // 14
            "51389"                       // 15
        };
        initializeCmd[3] = (String)map.get(SetupConstants.CONFIG_VAR_ROOT_SUFFIX);
        initializeCmd[7] = (String) map.get(SetupConstants.CONFIG_VAR_DS_MGR_PWD);
        initializeCmd[9] = (String) map.get(SetupConstants.DS_EMB_REPL_HOST2);
        initializeCmd[11] = (String) map.get(SetupConstants.DS_EMB_REPL_PORT2);
        initializeCmd[13] = "localhost";
        initializeCmd[15] = (String) map.get(SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT);

        SetupProgress.reportStart("emb.replcommand",concat(initializeCmd));
        int ret = ReplicationCliMain.mainCLI(initializeCmd, false, 
                                            SetupProgress.getOutputStream(), 
                                            SetupProgress.getOutputStream(), 
                                            null); 
        if (ret == 0)
            SetupProgress.reportEnd("emb.success", null);
        else
            SetupProgress.reportEnd("emb.failed", null);
        return ret;
    }
 
    /**
      * @return true if multi server option is selected in the configurator.
      */
    public static boolean isMultiServer(Map map) 
    {
        String replFlag = (String) map.get(SetupConstants.DS_EMB_REPL_FLAG); 
        if (replFlag != null && replFlag.startsWith(
              SetupConstants.DS_EMP_REPL_FLAG_VAL)) {
            return true;
        }
        return false;
    }
    private static String concat(String[] args)
    {
        String ret = "";
        for (int i = 0; i < args.length; i++)
           ret += args[i]+" ";
        return ret;
    }

    /**
     *  Utility function to preload data in the embedded instance.
     *  Must be called when the directory instance is shutdown.
     *
     *  @param odsRoot Local directory where <code>OpenDS</code> is installed.
     *  @param ldif Full path of the ldif file to be loaded.
     *
     */
    public static void loadLDIF(String odsRoot, String ldif)
    {
        Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
        try {
            debug.message("EmbeddedOpenDS:loadLDIF("+ldif+")");
            String[] args1 = 
            { 
                "-C", "org.opends.server.extensions.ConfigFileHandler",
                "-f", odsRoot+"/config/config.ldif",
                "-n", "userRoot",
                "-l", ldif,
                "-Q" 
            };
            org.opends.server.tools.ImportLDIF.mainImportLDIF(args1);
            debug.message("EmbeddedOpenDS:loadLDIF Success");
        } catch (Exception ex) {
              debug.error("EmbeddedOpenDS:loadLDIF:ex="+ex);
        }
    } 

    /**
      * Returns a one-way hash for passwd using SSHA512 scheme.
      *
      * @param p Clear password string
      * @return hash value
      */
    public static String hash(String p)
    {
        String str = null;
        try {
            byte[] bb = p.getBytes();
            str = SaltedSHA512PasswordStorageScheme.encodeOffline(bb);
        } catch (Exception ex) {
            Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
            debug.error("EmbeddedOpenDS.hash failed : ex="+ex);
        }
        return str;
    }

}
