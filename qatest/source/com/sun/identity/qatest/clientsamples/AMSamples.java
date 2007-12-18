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
 * $Id: AMSamples.java,v 1.1 2007-12-18 23:49:44 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.clientsamples;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.PolicyCommon;
import com.sun.identity.qatest.common.TestConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.webtest.DefaultTaskHandler;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class tests the Acccess Manager Client Samples
 */
public class AMSamples extends TestCommon {
    
    private ResourceBundle rb_client;
    private ResourceBundle rb_ams;
    private ResourceBundle rb_amconfig;
    private String clientURL;
    private String serviceconfigURL;
    private String userprofileURL;
    private String policyURL;
    private String ssovalidationURL;
    private String baseDir;
    private String userName;
    private String resourceName;
    private String clientDomain;
    private String polName = "clientSamplePolicyTest";
    private DefaultTaskHandler task;
    private HtmlPage page;
    private SSOToken admintoken;
    private IDMCommon idmc;
    private PolicyCommon pc;
    private WebClient webClient;

    /**
     * Creates a new instance of AMSamples and instantiates common objects and
     * classes.
     */
    public AMSamples()
    throws Exception {
        super("AMSamples");
        rb_amconfig = ResourceBundle.getBundle("AMConfig");
        rb_client = ResourceBundle.getBundle("clientsamplesGlobal");
        rb_ams = ResourceBundle.getBundle("AMSamples");
        admintoken = getToken(adminUser, adminPassword, basedn);
        idmc = new IDMCommon();
        pc = new PolicyCommon();
        baseDir = getBaseDir() + System.getProperty("file.separator")
            + rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME)
            + System.getProperty("file.separator") + "built"
            + System.getProperty("file.separator") + "classes"
            + System.getProperty("file.separator");
    }
    
    /**
     *  Creates required users and policy.
     */
    @BeforeClass(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void setup()
    throws Exception {
        entering("setup", null);

        clientDomain = rb_client.getString("client_domain_name");
        log(Level.FINE, "setup", "Client host domain name: " + clientDomain);

        String deployPort = rb_client.getString("deploy_port");
        log(Level.FINE, "setup", "Deploy port: " + deployPort);

        String deployURI = rb_client.getString("deploy_uri");
        log(Level.FINE, "setup", "Deploy URI: " + deployURI);

        InetAddress addr = InetAddress.getLocalHost();
        String hostname = addr.getCanonicalHostName();

        clientURL = protocol + "://" + hostname +  clientDomain + ":" +
                deployPort + deployURI;
        log(Level.FINE, "setup", "Client URL: " + clientURL);

        serviceconfigURL = clientURL + rb_ams.getString("serviceconfig_uri");
        log(Level.FINE, "setup", "Service Configuration Sample Servlet URL: " +
                serviceconfigURL);

        userprofileURL = clientURL + rb_ams.getString("userprofile_uri");
        log(Level.FINE, "setup", "User Profile (Attributes) Sample Servlet" +
                " URL: " + userprofileURL);

        policyURL = clientURL + rb_ams.getString("policy_uri");
        log(Level.FINE, "setup", "Policy Evaluator Client Sample Servlet" +
                " URL: " + policyURL);

        ssovalidationURL = clientURL + rb_ams.getString("ssovalidation_uri");
        log(Level.FINE, "setup", "Single Sign On Token Verification Servlet" +
                " URL: " + ssovalidationURL);

        Map map = new HashMap();
        Set set = new HashSet();
        userName = rb_ams.getString("cs_username");
        set.add(userName);
        map.put("sn", set);
        set = new HashSet();
        set.add(userName);
        map.put("cn", set);
        set = new HashSet();
        String userPassword = rb_ams.getString("cs_password");
        set.add(userPassword);
        map.put("userpassword", set);
        set = new HashSet();
        set.add("Active");
        map.put("inetuserstatus", set);

        idmc.createIdentity(admintoken, realm, IdType.USER, userName, map);

        resourceName = rb_ams.getString("policy_resource");
        String xmlFile = "client-samples-policy-test.xml";
        createPolicyXML(xmlFile);
        pc.createPolicy(xmlFile, realm);

        exiting("setup");
    }

    /**
     * This test validates the user attributes for a super admin user.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testUserProfileAdminUser()
    throws Exception {
        entering("testUserProfileAdminUser", null);
        try {
            String res = rb_ams.getString("userprofile_pass");
            String xmlFile = "testUserProfileAdminUser.xml";
            generateUserProfileXML(realm, adminUser, adminPassword, xmlFile,
                    res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, adminUser) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileAdminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a super" +
                    " admin user.");
        }
        exiting("testUserProfileAdminUser");
    }

    /**
     * This test validates the user attributes for a user with no admin
     * privilages
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testUserProfileNonadminUser()
    throws Exception {
        entering("testUserProfileNonadminUser", null);
        try {
            String res = rb_ams.getString("userprofile_pass");
            String xmlFile = "testUserProfileNonadminUser.xml";
            generateUserProfileXML(realm, userName, userName, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, userName) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileNonadminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a user" +
                    " with no admin privilages.");
        }
        exiting("testUserProfileNonadminUser");
    }

    /**
     * This test validates the user attributes for a non existing user.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testUserProfileNonExistingUser()
    throws Exception {
        entering("testUserProfileNonExistingUser", null);
        try {
            String res = rb_ams.getString("userprofile_error");
            String xmlFile = "testUserProfileExistingUser.xml";
            generateUserProfileXML(realm, "doesnotexist", "doesnotexist",
                    xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileNonExistingUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a non" +
                    " existing user.");
        }
        exiting("testUserProfileNonExistingUser");
    }

    /**
     * This test validates the user attributes for a user with incorrect
     * credentials (incorrect password).
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testUserProfileInvalidUserCredential()
    throws Exception {
        entering("testUserProfileInvalidUserCredential", null);
        try {
            String res = rb_ams.getString("userprofile_error");
            String xmlFile = "testUserProfileInvalidUserCredential.xml";
            generateUserProfileXML(realm, adminUser, adminPassword + "wrong",
                    xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testUserProfileInvalidUserCredential",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the user attributes for a user" +
                    " with incorrect credentials (incorrect password).");
        }
        exiting("testUserProfileInvalidUserCredential");
    }

    /**
     * This test validates the service configuration for DAI service for
     * configuration type set to Schema.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testServiceConfigDAISchema()
    throws Exception {
        entering("testServiceConfigDAISchema", null);
        try {
            String res = rb_ams.getString("serviceconfig_dai_schema_pass");
            String xmlFile = "testServiceConfigDAISchema.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword, "DAI",
                    "globalSchema", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigDAISchema", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " DAI service for configuration type set to Schema.");
        }
        exiting("testServiceConfigDAISchema");
    }

    /**
     * This test validates the service configuration for DAI service for
     * configuration type set to Config.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testServiceConfigDAIConfig()
    throws Exception {
        entering("testServiceConfigDAIConfig", null);
        try {
            String res = rb_ams.getString("serviceconfig_config_pass");
            String xmlFile = "testServiceConfigDAIConfig.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword, "DAI",
                    "globalConfig", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigDAIConfig", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " DAI service for configuration type set to Config.");
        }
        exiting("testUserProfileAdminUser");
    }

    /**
     * This test validates the service configuration for
     * iplanetAMPlatformService service for configuration type set to Schema.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testServiceConfigPlatformSvcSchema()
    throws Exception {
        entering("testServiceConfigPlatformSvcSchema", null);
        try {
            String res = rb_ams.getString("serviceconfig_schema_pass");
            String xmlFile = "testServiceConfigPlatformSvcSchema.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMPlatformService", "globalSchema", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigPlatformSvcSchema",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " iplanetAMPlatformService service for configuration type" +
                    " set to Schema.");
        }
        exiting("testServiceConfigPlatformSvcSchema");
    }

    /**
     * This test validates the service configuration for
     * iplanetAMPlatformService service for configuration type set to Config.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testServiceConfigPlatformSvcConfig()
    throws Exception {
        entering("testServiceConfigPlatformSvcConfig", null);
        try {
            String res = rb_ams.getString("serviceconfig_config_pass");
            String xmlFile = "testServiceConfigPlatformSvcConfig.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMPlatformService", "globalConfig", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigPlatformSvcConfig",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " iplanetAMPlatformService service for configuration type" +
                    " set to Config.");
        }
        exiting("testServiceConfigPlatformSvcConfig");
    }

    /**
     * This test validates the service configuration for not existing service
     * for configuration type set to Schema.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testServiceConfigSvcNotExistSchema()
    throws Exception {
        entering("testServiceConfigSvcNotExistSchema", null);
        try {
            String res = rb_ams.getString("serviceconfig_error");
            String xmlFile = "testServiceConfigSvcNotExistSchema.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMSvcNotExistSchema", "globalSchema", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigSvcNotExistSchema",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " not existing service for configuration type set to" +
                    " Schema.");
        }
        exiting("testServiceConfigSvcNotExistSchema");
    }

    /**
     * This test validates the service configuration for not existing service
     * for configuration type set to Config.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testServiceConfigSvcNotExistConfig()
    throws Exception {
        entering("testServiceConfigSvcNotExistConfig", null);
        try {
            String res = rb_ams.getString("serviceconfig_error");
            String xmlFile = "testServiceConfigSvcNotExistConfig.xml";
            generateServiceConfigXML(realm, adminUser, adminPassword,
                    "iplanetAMSvcNotExistConfig", "globalConfig", xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testServiceConfigSvcNotExistConfig",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates the service configuration for" +
                    " not existing service for configuration type set to" +
                    " Config.");
        }
        exiting("testServiceConfigSvcNotExistConfig");
    }

    /**
     * This test validates valid authorization (allow access) to a policy
     * resource for a super admin user.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testPolicyEvalPassAdminUser()
    throws Exception {
        entering("testPolicyEvalPassAdminUser", null);
        try {
            String res = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalPassAdminUser.xml";
            generatePolicyEvalXML(realm, adminUser, adminPassword,
                    "iPlanetAMWebAgentService", resourceName, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalPassAdminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (allow access) to a policy resource for a super admin" +
                    " user.");
        }
        exiting("testPolicyEvalPassAdminUser");
    }

    /** his test validates valid authorization(deny access) to a policy
     * resource for a super admin user.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testPolicyEvalFailAdminUser()
    throws Exception {
        entering("testPolicyEvalFailAdminUser", null);
        try {
            String res = rb_ams.getString("policy_error");
            String resP = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalFailAdminUser.xml";
            generatePolicyEvalXML(realm, adminUser, adminPassword,
                    "iPlanetAMWebAgentService", resourceName + "Fail",
                    xmlFile, "");
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, resP) != -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalFailAdminUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (deny access) to a policy resource for a super admin" +
                    " user.");
        }
        exiting("testPolicyEvalFailAdminUser");
    }

    /**
     * This test validates valid authorization (allow access) to a policy
     * resource for a user with no admin privilages.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testPolicyEvalPassTestUser()
    throws Exception {
        entering("testPolicyEvalPassTestUser", null);
        try {
            String res = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalPassTestUser.xml";
            generatePolicyEvalXML(realm, userName, userName,
                    "iPlanetAMWebAgentService", resourceName, xmlFile, res);
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalPassTestUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (allow access) to a policy resource for a user with no" +
                    " admin privilages.");
        }
        exiting("testPolicyEvalPassTestUser");
    }

    /**
     * This test validates valid authorization (deny access) to a policy
     * resource for a user with no admin privilage.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testPolicyEvalFailTestUser()
    throws Exception {
        entering("testPolicyEvalFailTestUser", null);
        try {
            String res = rb_ams.getString("policy_error");
            String resP = rb_ams.getString("policy_pass");
            String xmlFile = "testPolicyEvalFailTestUser.xml";
            generatePolicyEvalXML(realm, userName, userName,
                    "iPlanetAMWebAgentService", resourceName + "Fail",
                    xmlFile, "");
            task = new DefaultTaskHandler(baseDir + xmlFile);
            webClient = new WebClient();
            page = task.execute(webClient);
            if (getHtmlPageStringIndex(page, resP) != -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testPolicyEvalFailTestUser", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates valid authorization" +
                    " (deny access) to a policy resource for a user with no" +
                    " admin privilage.");
        }
        exiting("testPolicyEvalFailTestUser");
    }

    /**
     * This test validates SSO Token for a super admin user.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testSSOVerificationServletAdminUser()
    throws Exception {
        entering("testSSOVerificationServletAdminUser", null);
        String loginURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Login";
        String logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        webClient = new WebClient();
        try {
            consoleLogin(webClient, loginURL, adminUser, adminPassword);
            Thread.sleep(5000);
            page = (HtmlPage)webClient.getPage(ssovalidationURL);
            if (clientDomain.equals(cookieDomain)) {
                if (getHtmlPageStringIndex(page, adminUser) == -1)
                    assert false;
            } else {
                if (getHtmlPageStringIndex(page, adminUser) != -1)
                    assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testSSOVerificationServletAdminUser",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
            Reporter.log("This test validates SSO Token for a super admin" +
                    " user.");
        }
        exiting("testSSOVerificationServletAdminUser");
    }

    /**
     * This test validates SSO Token for a user with no admin privilages.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testSSOVerificationServletTestUser()
    throws Exception {
        entering("testSSOVerificationServletTestUser", null);
        String loginURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Login";
        String logoutURL = protocol + ":" + "//" + host + ":" + port + uri +
                "/UI/Logout";
        webClient = new WebClient();
        try {
            consoleLogin(webClient, loginURL, userName, userName);
            Thread.sleep(5000);
            page = (HtmlPage)webClient.getPage(ssovalidationURL);
            if (clientDomain.equals(cookieDomain)) {
                if (getHtmlPageStringIndex(page, userName) == -1)
                    assert false;
            } else {
                if (getHtmlPageStringIndex(page, userName) != -1)
                    assert false;
            }
        } catch (Exception e) {
            log(Level.SEVERE, "testSSOVerificationServletTestUser",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, logoutURL);
            Reporter.log("This test validates SSO Token for a user with no" +
                    " admin privilages.");
        }
        exiting("testSSOVerificationServletTestUser");
    }

    /**
     * This test validates SSO Token failure when no user token is available.
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testSSOVerificationServletError()
    throws Exception {
        entering("testSSOVerificationServletError", null);
        try {
            String res = rb_ams.getString("ssovalidation_error");
            webClient = new WebClient();
            page = (HtmlPage)webClient.getPage(ssovalidationURL);
            if (getHtmlPageStringIndex(page, res) == -1)
                assert false;
        } catch (Exception e) {
            log(Level.SEVERE, "testSSOVerificationServletError",
                    e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            Reporter.log("This test validates SSO Token failure when no user" +
                    " token is available.");
        }
        exiting("testSSOVerificationServletError");
    }

    /**
     * Cleanup method. This method:
     * (a) Delete users
     * (b) Deletes policies
     */
    @AfterClass(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            log(Level.FINEST, "cleanup", "UserName:" + userName);
            Reporter.log("User name:" + userName);
            Reporter.log("Policy name:" + polName);
            idmc.deleteIdentity(admintoken, realm, IdType.USER, userName);
            pc.deletePolicy(polName, realm);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        exiting("cleanup");
    }

    /**
     * Generate the XML for User Profile testcases.
     */
    private void generateUserProfileXML(String org, String username,
            String password, String xmlFile, String result)
    throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);

        log(Level.FINEST, "generateUserProfileXML", "Organization: " + org);
        log(Level.FINEST, "generateUserProfileXML", "Username: " + username);
        log(Level.FINEST, "generateUserProfileXML", "Password: " + password);
        log(Level.FINEST, "generateUserProfileXML", "XML File: " + xmlFile);

        out.write("<url href=\"" + userprofileURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"form2\" buttonName=\"submit\" >");
        out.write(newline);
        out.write("<input name=\"orgname\" value=\"" + org + "\"/>");
        out.write(newline);
        out.write("<input name=\"username\" value=\"" + username + "\"/>");
        out.write(newline);
        out.write("<input name=\"password\" value=\"" + password + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + result + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }

    /**
     * Generates XML for Service Configuration testacses
     */
    private void generateServiceConfigXML(String org, String username,
            String password, String svcName, String configType,
            String xmlFile, String result)
    throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);

        log(Level.FINEST, "generateServiceConfigXML", "Organization: " + org);
        log(Level.FINEST, "generateServiceConfigXML", "Username: " + username);
        log(Level.FINEST, "generateServiceConfigXML", "Password: " + password);
        log(Level.FINEST, "generateServiceConfigXML", "Service Name: " +
                svcName);
        log(Level.FINEST, "generateServiceConfigXML", "Configuration Type: " +
                configType);
        log(Level.FINEST, "generateServiceConfigXML", "XML File: " + xmlFile);

        out.write("<url href=\"" + serviceconfigURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"form2\" buttonName=\"submit\">");
        out.write(newline);
        out.write("<input name=\"orgname\" value=\"" + org + "\"/>");
        out.write(newline);
        out.write("<input name=\"username\" value=\"" + username + "\"/>");
        out.write(newline);
        out.write("<input name=\"password\" value=\"" + password + "\"/>");
        out.write(newline);
        out.write("<input name=\"service\" value=\"" + svcName + "\"/>");
        out.write(newline);
        out.write("<dynamicinput name=\"method\" value=\"" + configType);
        out.write("\"/>");
        out.write(newline);
        out.write("<result text=\"" + result + "\" />");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }

    /**
     * Generates XML for Policy testacses.
     */
    private void generatePolicyEvalXML(String org, String username,
            String password, String svcName, String resName, String xmlFile,
            String result)
    throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);

        log(Level.FINEST, "generatePolicyEvalXML", "Organization: " + org);
        log(Level.FINEST, "generatePolicyEvalXML", "Username: " + username);
        log(Level.FINEST, "generatePolicyEvalXML", "Password: " + password);
        log(Level.FINEST, "generatePolicyEvalXML", "Service Name: " + svcName);
        log(Level.FINEST, "generatePolicyEvalXML", "Resource: " + resName);
        log(Level.FINEST, "generatePolicyEvalXML", "XML File: " + xmlFile);

        out.write("<url href=\"" + policyURL);
        out.write("\">");
        out.write(newline);
        out.write("<form name=\"form2\" buttonName=\"submit\">");
        out.write(newline);
        out.write("<input name=\"orgname\" value=\"" + org + "\"/>");
        out.write(newline);
        out.write("<input name=\"username\" value=\"" + username + "\"/>");
        out.write(newline);
        out.write("<input name=\"password\" value=\"" + password + "\"/>");
        out.write(newline);
        out.write("<input name=\"servicename\" value=\"" + svcName + "\"/>");
        out.write(newline);
        out.write("<input name=\"resource\" value=\"" + resName + "\"/>");
        out.write(newline);
        out.write("<result text=\"" + result + "\"/>");
        out.write(newline);
        out.write("</form>");
        out.write(newline);
        out.write("</url>");
        out.write(newline);
        out.close();
    }

    /**
     * Generates XML for creating the policy.
     */
    private void createPolicyXML(String xmlFile)
    throws Exception {
        FileWriter fstream = new FileWriter(baseDir + xmlFile);
        BufferedWriter out = new BufferedWriter(fstream);

        out.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        out.write(newline);
        out.write("<!DOCTYPE Policies");
        out.write(newline);
        out.write("PUBLIC \"-//Sun Java System Access Manager 7.1 2006Q3");
        out.write("Admin CLI DTD//EN\"");
        out.write(newline);
        out.write("\"jar://com/sun/identity/policy/policyAdmin.dtd\">");
        out.write(newline);

        out.write("<Policies>");
        out.write(newline);

        out.write("<Policy name=\"" + polName + "\" referralPolicy=\"false\"");
        out.write(" active=\"true\">");
        out.write(newline);

        out.write("<Rule name=\"csrule\">");
        out.write(newline);
        out.write("<ServiceName name=\"iPlanetAMWebAgentService\"/>");
        out.write(newline);
        out.write("<ResourceName name=\"" + resourceName + "\"/>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"POST\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("<AttributeValuePair>");
        out.write(newline);
        out.write("<Attribute name=\"GET\"/>");
        out.write(newline);
        out.write("<Value>allow</Value>");
        out.write(newline);
        out.write("</AttributeValuePair>");
        out.write(newline);
        out.write("</Rule>");
        out.write(newline);

        out.write("<Subjects name=\"cssubjects\" description=\"\">");
        out.write(newline);
        out.write("<Subject name=\"cssubj\" type=\"AuthenticatedUsers\"");
        out.write(" includeType=\"inclusive\">");
        out.write(newline);
        out.write("</Subject>");
        out.write(newline);
        out.write("</Subjects>");
        out.write(newline);

        out.write("</Policy>");
        out.write(newline);
        out.write("</Policies>");
        out.close();
    }
}
