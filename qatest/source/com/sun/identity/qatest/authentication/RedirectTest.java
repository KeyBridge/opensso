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
 * $Id: RedirectTest.java,v 1.1 2007-05-25 21:59:02 sridharev Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.authentication;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.authentication.AuthTestConfigUtil;
import com.sun.identity.qatest.common.authentication.AuthTestsValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class is called by the <code>RedirectTestFactory</code>.
 * Each RedirectTest will have a module instance name and the realm
 * associated with it to perform the test.
 * This class does the following :
 * - Create the realm
 * - Create the module instances for that realm
 * - Create users to Login if required to create.
 * - Validates for module based authentication pass and failure case
 * - Validates for the goto and gotoOnFail URL for this instance.
 */
public class RedirectTest extends TestCommon {
    
    private ResourceBundle testResources;
    private String strRealm;
    private String moduleOnSuccess;
    private String moduleOnFail;
    private String modulePassMsg;
    private String moduleFailMsg;
    private String moduleGotoPassMsg;
    private String moduleOnFailPassMsg;
    private String createUserProp;
    private boolean userExists;
    private boolean isSubrealm = false;
    private String cleanupFlag;
    private boolean debug;
    private String userName;
    private String password;
    private String redirectURL;
    private String testRealm;
    private String uniqueIdentifier;
    private List moduleConfigData;
    private String moduleServiceName;
    private String moduleSubConfig;
    private String moduleSubConfigId;
    private String testModName;
    private String configrbName = "authenticationConfigData";
    private List<String> testUserList = new ArrayList<String>();
    
    /**
     * Default Constructor
     **/
    public RedirectTest() {
        super("RedirectTest");
    }
    
    /**
     * <code>RedirectTest</code> class has the implementation of the
     * url redirecrt for module/go/gotoOnFail based authentication.
     * Tests reads and configures the system based on the configuration
     * details provided for test scenario and performs/validates the tests.
     * If for any reason configuration fails the tests will not run.
     * @param test resources bundle holding the test data
     * @param module type
     * @param realmName
     */
    public RedirectTest(ResourceBundle rbName, String modName,
            String realmName) {
        super("RedirectTest");
        this.testResources = rbName;
        this.testModName = modName;
        this.testRealm = realmName;
    }
    
    /**
     * Reads the necessary test configuration and prepares the system
     * for module/goto/gotoOnFail redirection tests.The following are done
     * in the setup
     * - Create module instances
     * - Create realm
     * - Create Users , If needed
     */
    @BeforeClass(groups = {"client"})
    public void setup(){
        entering("setup", null);
        moduleOnSuccess = testResources.getString("am-auth-test-" +
                testModName + "-goto");
        moduleOnFail = testResources.getString("am-auth-test-" +
                testModName + "-gotoOnFail");
        modulePassMsg = testResources.getString("am-auth-test-" +
                testModName + "-module-passmsg");
        moduleFailMsg = testResources.getString("am-auth-test-" +
                testModName + "-module-failmsg");
        moduleGotoPassMsg = testResources.getString("am-auth-test-" +
                testModName + "-goto-passmsg");
        moduleOnFailPassMsg = testResources.getString("am-auth-test-" +
                testModName + "-gotoOnFail-passmsg");
        userName = testResources.getString("am-auth-test-" +
                testModName + "-user");
        userName.trim();
        password = testResources.getString("am-auth-test-" +
                testModName + "-password");
        password.trim();
        cleanupFlag = testResources.getString("am-auth-test-debug");
        debug = new Boolean(cleanupFlag).booleanValue();
        log(logLevel, "setup", "testModuleName " + testModName);
        log(logLevel, "setup", "testRealmName " + testRealm);
        log(logLevel, "setup", "moduleOnpass " + moduleOnSuccess);
        log(logLevel, "setup", "moduleOnFail " + moduleOnFail);
        log(logLevel, "setup", "modulePassMsg" + modulePassMsg);
        log(logLevel, "setup", "moduleFailMsg" + moduleFailMsg);
        log(logLevel, "setup", "modulePassMsg" + moduleGotoPassMsg);
        log(logLevel, "setup", "modulePassMsg" + moduleOnFailPassMsg);
        log(logLevel, "setup", "userName" + userName);
        log(logLevel, "setup", "password" + password);
        Reporter.log("testModuleName " + testModName);
        Reporter.log("testModuleName " + testRealm);
        Reporter.log("testModuleName " + moduleOnSuccess);
        Reporter.log("moduleOnFail " + moduleOnFail);
        Reporter.log("modulePassMsg " + modulePassMsg);
        Reporter.log("moduleFailMsg " + moduleFailMsg);
        Reporter.log("moduleFailMsg " + moduleGotoPassMsg);
        Reporter.log("moduleFailMsg " + moduleOnFailPassMsg);
        Reporter.log("moduleuseName " + userName);
        Reporter.log("modulepassword " + password);
        createModule(testModName);
        log(logLevel, "setup", "Module Created");
        createUserProp = testResources.getString("am-auth-test-" +
                testModName + "-createTestUser");
        userExists = new Boolean(createUserProp).booleanValue();
        if (!userExists) {
            createUser(userName, password);
        }
        if (!testRealm.equals("/")) {
            createTestRealm(testRealm);
            isSubrealm = true;
            uniqueIdentifier = testRealm + "_" + testModName;
            redirectURL = getLoginURL(testRealm) + "&amp;"
                    + "module=" + moduleSubConfig;
        } else {
            redirectURL = getLoginURL(testRealm) + "?" + "module="
                    + moduleSubConfig;
            uniqueIdentifier = "rootrealm" + "_" + testModName;
        }
        exiting("setup");
    }
    
    /*
     * Validate the module based authentication for the module
     * under test for correct user and password behaviour
     */
    @Test(groups = {"client"})
    public void validateModuleTestsPositive()
    throws Exception {
        entering("validateModuleTestsPositive", null);
        Map executeMap = new HashMap();
        executeMap.put("redirectURL", redirectURL);
        executeMap.put("userName", userName);
        executeMap.put("password", password);
        executeMap.put("modulePassMsg", modulePassMsg);
        executeMap.put("moduleFailMsg", moduleFailMsg);
        executeMap.put("uniqueIdentifier", uniqueIdentifier);
        AuthTestsValidator authTestValidator =
                new AuthTestsValidator(executeMap);
        authTestValidator.testModulebasedPostive();
        exiting("validateModuleTestsPositive");
    }
    
    /*
     * Validate the module based authentication for the module
     * under test for incorrect user and password behaviour
     */
    @Test(groups = {"client"})
    public void validateModuleTestsNegative()
    throws Exception {
        entering("validateModuleTestsNegative", null);
        Map executeMap = new HashMap();
        executeMap.put("redirectURL", redirectURL);
        executeMap.put("userName", userName);
        executeMap.put("password", password);
        executeMap.put("modulePassMsg", modulePassMsg);
        executeMap.put("moduleFailMsg", moduleFailMsg);
        executeMap.put("uniqueIdentifier", uniqueIdentifier);
        AuthTestsValidator authTestValidator =
                new AuthTestsValidator(executeMap);
        authTestValidator.testModulebasedNegative();
        exiting("validateModuleTestsNegative");
    }
    
    /*
     * Validate the module based authentication for the module
     * under test for "goto" param when auth success behaviour
     */
    @Test(groups = {"client"})
    public void validateGotoTests()
    throws Exception {
        entering("validateGotoTests", null);
        Map executeMap = new HashMap();
        String gotoURL = redirectURL + "&amp;goto=" + moduleOnSuccess;
        executeMap.put("redirectURL", redirectURL);
        executeMap.put("userName", userName);
        executeMap.put("password", password);
        executeMap.put("modulePassMsg", moduleGotoPassMsg);
        executeMap.put("moduleFailMsg", moduleOnFailPassMsg);
        executeMap.put("gotoURL", gotoURL);
        executeMap.put("uniqueIdentifier", uniqueIdentifier);
        AuthTestsValidator authTestValidator =
                new AuthTestsValidator(executeMap);
        authTestValidator.testModuleGoto();
        exiting("validateGotoTests");
    }
    
    /*
     * Validate the module based authentication for the module
     * under test for "GotoOnfail" param with unsuccessful authentication
     * behaviour
     */
    @Test(groups = {"client"})
    public void validateGotoOnFailTests()
    throws Exception {
        entering("validateGotoOnFailTests", null);
        Map executeMap = new HashMap();
        String gotoURL = redirectURL + "&amp;gotoOnFail=" + moduleOnFail;
        executeMap.put("redirectURL", redirectURL);
        executeMap.put("userName", userName);
        executeMap.put("password", password);
        executeMap.put("modulePassMsg", moduleGotoPassMsg);
        executeMap.put("moduleFailMsg", moduleOnFailPassMsg);
        executeMap.put("gotoURL", gotoURL);
        executeMap.put("uniqueIdentifier", uniqueIdentifier);
        AuthTestsValidator authTestValidator = new AuthTestsValidator(executeMap);
        authTestValidator.testModuleGotoOnFail();
        exiting("validateGotoOnFailTests");
    }
    
    /**
     * Clean up is called post execution of each module and realm .
     * This is done to maintain the system in a clean state
     * after executing each test scenario, in this case each module based
     * authentication tests for all possible cases.
     * This processed in this method are:
     * 1. Delete the authentication module
     * 2. Delete the realm involved only if it is not root realm
     * 3. Delete the users involved/created for this test if any.
     */
    @AfterClass(groups={"client"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        if (!debug) {
            try {
                String url = protocol + ":" + "//" + host + ":" + port + uri;
                log(logLevel, "cleanup", url);
                FederationManager am = new FederationManager(url);
                WebClient webClient = new WebClient();
                log(logLevel, "cleanup", "Users:" + testUserList);
                if (!isSubrealm) {
                    List<String> listModInstance = new ArrayList<String>();
                    listModInstance.add(moduleSubConfig);
                    log(logLevel, "cleanup", "listModInstance" + listModInstance);
                    consoleLogin(webClient, url, adminUser, adminPassword);
                    HtmlPage page = (HtmlPage)am.deleteAuthInstances(webClient,
                            realm, listModInstance);
                    log(logLevel, "cleanup", "Page" + page.asXml());
                    am.deleteIdentities(webClient, testRealm, testUserList, "User");
                } else {
                    consoleLogin(webClient, url, adminUser, adminPassword);
                    am.deleteIdentities(webClient, testRealm, testUserList, "User");
                    String delRealm = "/" + testRealm;
                    am.deleteRealm(webClient, delRealm, true);
                }
                url = url + "/UI/Logout";
                consoleLogout(webClient, url);
            } catch(Exception e) {
                log(Level.SEVERE, "cleanup", e.getMessage(), null);
                e.printStackTrace();
                throw e;
            }
        }
        exiting("cleanup");
    }
    
    /**
     * Call Authentication Utility class to create the module instances
     * for a given module instance name
     * @param moduleName
     */
    private void createModule(String mName) {
        try {
            AuthTestConfigUtil moduleConfig =
                    new AuthTestConfigUtil(configrbName);
            moduleConfig.setTestConfigRealm(testRealm);
            Map modMap = moduleConfig.getModuleData(mName);
            moduleServiceName = moduleConfig.getModuleServiceName();
            moduleSubConfig = moduleConfig.getModuleSubConfigName();
            moduleSubConfigId = moduleConfig.getModuleSubConfigId();
            log(logLevel, "createModule", "ModuleServiceName :" +
                    moduleServiceName);
            log(logLevel, "createModule", "ModuleSubConfig :" +
                    moduleSubConfig);
            log(logLevel, "createModule", "ModuleSubConfigId :" +
                    moduleSubConfigId);
            moduleConfigData = getListFromMap(modMap, mName);
            moduleConfig.createModuleInstances(moduleServiceName,
                    moduleSubConfig, moduleConfigData, moduleSubConfigId);
        } catch(Exception e) {
            log(Level.SEVERE, "createModule", e.getMessage(), null);
            e.printStackTrace();
        }
    }
    
    /**
     * Creates the required test users on the system for each
     * Chain to be executed
     * @param user map to be created
     * @param ChainName
     **/
    private void createUser(String newUser, String userpassword) {
        List<String> userList = new ArrayList<String>();
        userList.add("sn=" + newUser);
        userList.add("cn=" + newUser);
        userList.add("userpassword=" + userpassword);
        userList.add("inetuserstatus=Active");
        log(logLevel, "createUser", "userList " + userList);
        testUserList.add(newUser);
        try {
            AuthTestConfigUtil userConfig =
                    new AuthTestConfigUtil(configrbName);
            userConfig.setTestConfigRealm(testRealm);
            userConfig.createUser(userList, newUser);
        } catch(Exception e) {
            log(Level.SEVERE, "createUsers", e.getMessage(), null);
            e.printStackTrace();
        }
    }
    
    /**
     * Create the test realm
     * @param realm
     */
    private void createTestRealm(String newRealm){
        try {
            AuthTestConfigUtil realmConfig =
                    new AuthTestConfigUtil(configrbName);
            realmConfig.createRealms("/" + newRealm);
        } catch(Exception e) {
            log(Level.SEVERE, "createTestRealm", e.getMessage(), null);
            e.printStackTrace();
        }
    }
    
    /**
     * Get the list of users from Map, to create the
     * users.This is need for the <code>FederationManager</code> to
     * create users on the System
     * @param Map of users to be creared
     * @param moduleName
     */
    private List getListFromMap(Map lMap, String moduleName){
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
            log(logLevel, "getListFromMap", "UserList" + list);
        }
        return list;
    }
}
