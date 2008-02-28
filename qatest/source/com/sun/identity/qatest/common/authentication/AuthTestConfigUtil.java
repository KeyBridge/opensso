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
 * $Id: AuthTestConfigUtil.java,v 1.8 2008-02-28 04:04:50 inthanga Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.authentication;

import com.gargoylesoftware.htmlunit.WebClient;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.TestCommon;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;

/**
 * Authentication Test Utility class <code>AuthTestUtil</code>
 * extends <code>TestCommon</code> helper class contains utility
 * methods to create module instances,services,users,gets the default
 * module related configuration data
 */
public class AuthTestConfigUtil extends TestCommon {
    
    private String moduleName;
    private Map<String, String> mapModData;
    private String moduleService;
    private String moduleSubConfigName;
    private ResourceBundle configdata;
    private static final String moduleSubconfigid = "serverconfig";
    private String testbaseDir;
    private String url;
    private String famadmURL;
    private String logoutURL;
    private String configInfo;
    private String testConfigRealm = "/";
    private FederationManager am;
    private WebClient webClient;
    
    /**
     * Default Contructor
     * @param configuration data file name
     */
    public AuthTestConfigUtil(String config){
        super("AuthTestConfigUtil");
        this.configInfo = config;
        this.configdata =  ResourceBundle.getBundle(configInfo);
        url = getLoginURL("/");
        logoutURL = protocol + ":" + "//" + host + ":" + port +
                        uri + "/UI/Logout";
        famadmURL = protocol + ":" + "//" + host + ":" + port +
                        uri;
        am = new FederationManager(famadmURL);
    }
    
    /**
     * Returns the module configuration data
     * @param moduleName
     * @return module configuration data as a Map
     */
    public Map getModuleData(String modName){
        moduleName = modName;
        mapModData = new HashMap<String, String>();
        moduleService = configdata.getString(modName
                + ".module-service-name");
        moduleSubConfigName = configdata.getString(modName
                + ".module-subconfig-name");
        Enumeration bundleKeys = configdata.getKeys();
        while (bundleKeys.hasMoreElements()) {
            String key = (String)bundleKeys.nextElement();
            if (key.startsWith(moduleName)) {
                String value  = configdata.getString(key);
                mapModData.put(key, value);
            }
        }
        log(Level.FINEST, "getModuleData", "ModuleData:" + mapModData);
        return mapModData;
    }

    /**
     * Returns the module configuration data
     * @param moduleName
     * @return module configuration data as a List
     */
    public List getModuleDataAsList(String modName) {
        moduleName = modName;
        List mapModDataList = new ArrayList();
        moduleService = configdata.getString(modName
                + ".module-service-name");
        moduleSubConfigName = configdata.getString(modName
                + ".module-subconfig-name");
        Enumeration bundleKeys = configdata.getKeys();
        while (bundleKeys.hasMoreElements()) {
            String key = (String)bundleKeys.nextElement();
            if (key.startsWith(moduleName) && !(key.contains("module-"))) {
                String actualKey  = key.substring(key.indexOf(".") + 1,
                        key.length());
                String value  = configdata.getString(key);
                mapModDataList.add(actualKey + "=" + value);
            }
        }
        log(Level.FINEST, "getModuleDataAsList", "ModuleData: " +
                mapModDataList);
        return mapModDataList;
    }
    
    /**
     * Returns test URL for this test instance
     * @return url
     */
    public String getURL(){
        return url;
    }
    
    /**
     * Returns Module Service Name
     * @return moduleServiceName
     *
     */
    public String getModuleServiceName(){
        return  moduleService;
    }
    
    /**
     * Returns the module SubconfigName
     * @return moduleSubConfigName
     */
    public String getModuleSubConfigName(){
        return moduleSubConfigName;
    }
    
    /**
     * Returns module Sunconfig ID
     * @return module subconfig Id
     */
    public String getModuleSubConfigId(){
        return moduleSubconfigid;
    }
    
    /**
     * Sets test realm
     * @param realm name
     */
    public void setTestConfigRealm(String realmName){
        if (!(realmName.equals("/")))
            testConfigRealm = testConfigRealm + realmName;
    }

    /**
     * Creates the module instances
     * @param moduleServicename
     * @param moduleSubconfig
     * @param moduleConfigdata
     * @param moduleConfigId
     */
    public void createModuleInstances(String modServname, String modSubconf,
            List modConfdata, String modConfId) 
    throws Exception {
        createModuleInstances(realm, modServname, modSubconf, modConfdata, 
                modConfId);
    }    
    /**
     * Creates the module instances
     * @param realm
     * @param moduleServicename
     * @param moduleSubconfig
     * @param moduleConfigdata
     * @param moduleConfigId
     */
    public void createModuleInstances(String modRealm, String modServname, 
            String modSubconf, List modConfdata, String modConfId) 
    throws Exception {
        try {
            log(Level.FINEST, "createModuleInstances", "moduleServiceName: " +
                    modServname);
            log(Level.FINEST, "createModuleInstances", 
                    "moduleSubconfiguration: " + modSubconf);
            log(Level.FINEST, "createModuleInstances", "moduleConfigurationId: "
                    + modConfId);
            log(Level.FINEST, "createModuleInstances", "realm: " + modRealm);

            webClient = new WebClient();
            consoleLogin(webClient, url, adminUser, adminPassword);
            String cmdRealm = modRealm;
            if (!modRealm.equals("/")) {
                cmdRealm = "/" + modRealm;
            }            
            log(Level.FINE, "createModuleInstances", 
                    "Creating module sub-configuration " + modSubconf + 
                    " in service " + modServname + " in realm " + cmdRealm + 
                    " ...");
            if (FederationManager.getExitCode(
                    am.createSubCfg(webClient, modServname, modSubconf, 
                    modConfdata, cmdRealm, modConfId, "0")) != 0) {
                log(Level.SEVERE, "createModuleInstances", 
                        "createSubCfg (Module) famadm command failed");
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "createModuleInstances", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }
    
    /**
     * Deletes the module instances
     */
    public void deleteModuleInstances(String modServname, String modSubconf)
    throws Exception {
        deleteModuleInstances(realm, modServname, modSubconf);
    }
    
    /**
     * Deletes the module instances
     * @param modRealm
     * @param moduleServicename
     * @param moduleSubconfig
     * @param moduleConfigId
     */
    public void deleteModuleInstances(String modRealm, String modServname, 
            String modSubconf) 
    throws Exception {
        try {
            log(Level.FINEST, "deleteModuleInstances", "moduleServiceName: " +
                    modServname);
            log(Level.FINEST, "deleteModuleInstances", 
                    "moduleSubconfiguration: " + modSubconf);
            log(Level.FINEST, "deleteModuleInstances", "realm: " + modRealm);

            webClient = new WebClient();
            consoleLogin(webClient, url, adminUser, adminPassword);
            String cmdRealm = modRealm;
            if (!modRealm.equals("/")) {
                cmdRealm = "/" + modRealm;
            }            
            log(Level.FINE, "deleteModuleInstances", 
                    "Deleting module sub-configuration " + modSubconf + 
                    " in service " + modServname + " in realm " + cmdRealm + 
                    " ...");
            if (FederationManager.getExitCode(
                    am.deleteSubCfg(webClient, modServname, modSubconf, 
                    cmdRealm)) != 0) {
                log(Level.SEVERE, "deleteModuleInstances", 
                        "deleteSubCfg (Module) famadm command failed");
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "deleteModuleInstances", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }    
    
    
    /**
     * Create Services
     * @param chainname
     * @param List service_data
     */
    public void createServices(String chainname, List servData)
    throws Exception {
        try {
            String servicename = "iPlanetAMAuthConfiguration";
            String subconfigid = "NamedConfiguration";
            String subconfigname = "Configurations/" + chainname;
            log(Level.FINEST, "createServices", "servicename: " + servicename);
            log(Level.FINEST, "createServices", "subconfig: " + subconfigid);
            log(Level.FINEST, "createServices", "subconfigname: " + 
                    subconfigname);
            webClient = new WebClient();
            consoleLogin(webClient, url, adminUser, adminPassword);
            log(Level.FINE, "createModuleInstances", 
                    "Creating service sub-configuration " + subconfigname + 
                    " ...");            
            if (FederationManager.getExitCode(
                    am.createSubCfg(webClient, servicename, subconfigname, 
                    servData, realm, subconfigid, "0")) != 0) {
                log(Level.SEVERE, "createServices", 
                        "createSubCfg (Service) famadm command failed");
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "createServices", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }
    
    /**
     * Create Users
     * @param List user list
     * @param username
     */
    public void createUser(List userList, String uname)
    throws Exception{
        try {
            webClient = new WebClient();
            consoleLogin(webClient, url, adminUser, adminPassword);
            log(Level.FINE, "createUser", "Creating user " + uname + " ...");
            if (FederationManager.getExitCode(
                    am.createIdentity(webClient, realm, uname, "User", 
                    userList)) != 0) {
                log(Level.SEVERE, "createUser", 
                        "createIdentity (User) famadm command failed");
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "createUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }
    
    /**
     * Creates the realm
     * @param realmname
     */
    public void createRealms(String realmName)
    throws Exception{
        try {
            webClient = new WebClient();
            consoleLogin(webClient, url, adminUser, adminPassword);
            log(Level.FINE, "createRealms", "Creating realm " + realmName + 
                    "...");
            if (FederationManager.getExitCode(
                    am.createRealm(webClient, realmName)) != 0) {
                log(Level.SEVERE, "createRealms", 
                        "createRealm famadm command failed");
                assert false;
            }
        } catch(Exception e) {
            log(Level.SEVERE, "createRealms", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
        }
    }
    
    /**
     * Get the list of users from Map, to create the
     * users.This is need for the <code>FederationManager</code> to
     * create users on the System
     * @param Map of users to be creared
     * @param moduleName
     */
    public List getListFromMap(Map lMap, String moduleName) {
        Object escapeModServiceName = moduleName + ".module-service-name";
        Object escapeModSubConfigName = moduleName + ".module-subconfig-name";
        lMap.remove(escapeModServiceName);
        lMap.remove(escapeModSubConfigName);
        List<String> list = new ArrayList<String>();
        for (Iterator iter = lMap.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = ( Map.Entry)iter.next();
            String userkey = (String)entry.getKey();
            int sindex = userkey.indexOf(".");
            CharSequence cseq = userkey.subSequence(0, sindex+1);
            userkey = userkey.replace(cseq , "");
            userkey.trim();
            String removeModname = moduleName + ".";
            String userval = (String)entry.getValue();
            String uadd = userkey + "=" + userval;
            uadd.trim();
            list.add(uadd);
        }
        log(Level.FINEST, "getListFromMap", "List: " + list);        
        return list;
    }
}
