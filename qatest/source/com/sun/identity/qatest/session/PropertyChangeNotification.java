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
 * $Id: PropertyChangeNotification.java,v 1.1 2008-09-04 16:33:36 rmisra Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.session;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenEvent;
import com.iplanet.sso.SSOTokenListener;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.HashSet;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Reporter;

/**
 * This class tests setting the following property change notifications in an
 * ssotoken:
 * (a) Protected property
 * (b) Custom property
 * It requires property change notification enabled and protected and custom
 * properties be added to notification list porperties in the Session service
 * under global configuration.
 */
public class PropertyChangeNotification extends TestCommon implements
        SSOTokenListener {

    private SSOToken ssotoken;
    private SSOToken adminToken;
    private String strTestSPCN = "testSPCN";
    private String strURL;
    private IDMCommon idmc;
    private int iCount = 0;
    private Set setNum = new HashSet();
    private ResourceBundle rb;

   /**
    * Empty constructor
    */
    public PropertyChangeNotification() throws Exception {
        super("SessionProtectedProperty");
        idmc = new IDMCommon();
        rb = ResourceBundle.getBundle("session" + fileseparator +
                "SessionProperty");
        adminToken = getToken(adminUser, adminPassword, basedn);
    }

    /**
     * Initialization method. Setup:
     * (a) Creates user Identities

     * @throws java.lang.Exception
     */
    @BeforeClass(groups = {"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void setup()
    throws Exception {
        entering("setup", null);
        try {
            idmc.createDummyUser(adminToken, realm, "", strTestSPCN);
            log(Level.FINE,"setup", "Created user " + strTestSPCN +
                    " identity");
            strURL = protocol + "://" + host + ":" + port + uri;
            log(Level.FINEST, "setup", "Server URL: " + strURL);
        } catch(Exception e) {
            cleanup();
            log(Level.SEVERE, "setup", e.getMessage());
            cleanup();
            e.printStackTrace();
            throw e;
        }
        exiting("setup");
    }

    /**
     * Test property change notification in an sso token for a protected
     * property
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testProtectedPropertyChange()
    throws Exception {
        entering("testProtectedPropertyChange", null);
        Reporter.log("Test property change notification in an sso token for" +
                " a protected session property.");
        iCount = 0;
        if (setNum.size() != 0) {
            setNum.clear();
        }
        try {
            try {
                ssotoken = getToken(strTestSPCN, strTestSPCN, basedn);
                ssotoken.addSSOTokenListener(this);
                
                String strTokID = ssotoken.getTokenID().toString();
                log(Level.FINEST, "testProtectedPropertyChange",
                        "Unencrypted token id: " + strTokID);
                
                String strEncTokID = c66EncodeSidString(strTokID);
                log(Level.FINEST, "testProtectedPropertyChange",
                        "Encrypted token id: " + strEncTokID);

                WebClient webClient = new WebClient();
                HtmlPage page = (HtmlPage)webClient.getPage(strURL +
                        "/testSessionPropChange.jsp?IDToken=" + strEncTokID);
                log(Level.FINEST, "testProtectedPropertyChange",
                        "Page returned from testSessionPropChange.jsp: " +
                        page.asXml());
                Thread.sleep(notificationSleepTime);
            } catch(SSOException e) {
                       System.out.println("Protected property change failed." +
                       " This is not the expected result.");
            }
        } catch(Exception e) {
            log(Level.SEVERE, "testProtectedPropertyChange",
                    e.getMessage());
            cleanup();
            e.printStackTrace();
            throw e;
        } finally {
            log(Level.FINEST, "testProtectedPropertyChange", "iCount: " +
                    iCount);
            log(Level.FINEST, "testProtectedPropertyChange", "setNum set: " +
                    setNum.toString());
            destroyToken(ssotoken);
            if (iCount != 3  && setNum.size() != 3 && !setNum.contains("1-4") &&
                    !setNum.contains("2-4") && !setNum.contains("3-4")) {
                log(Level.SEVERE, "testProtectedPropertyChange", "Property " +
                        "change notification statistics is incorrect.");
                assert false;
            }       
        }
        exiting("testProtectedPropertyChange");
    }

    /**
     * Test property change notification in an sso token for a custom
     * property
     */
    @Test(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void testCustomPropertyChange()
    throws Exception {
        entering("testCustomPropertyChange", null);
        Reporter.log("Test property change notification in an sso token for" +
                " a custom session property.");        
        iCount = 0;
        if (setNum.size() != 0) {
            setNum.clear();
        }        
        try {
            try {
                ssotoken = getToken(strTestSPCN, strTestSPCN, basedn);
                ssotoken.addSSOTokenListener(this);
                
                String strCusProp =
                        rb.getString("SessionProperty.customPropertyName");
                log(Level.FINEST, "testCustomPropertyChange", "Custom" +
                        " property name: " + strCusProp);
                
                ssotoken.setProperty(strCusProp, "cus-0");
                String oldServerid = ssotoken.getProperty(strCusProp);
                log(Level.FINEST, "testCustomPropertyChange", "Custom" +
                        " property initial value: " + oldServerid);

                ssotoken.setProperty(strCusProp, "cus-1");
                String newServerid = ssotoken.getProperty(strCusProp);
                log(Level.FINEST, "testCustomPropertyChange", "Custom" +
                        " property value after first change: " + newServerid);
                
                ssotoken.setProperty(strCusProp, "cus-2");
                newServerid = ssotoken.getProperty(strCusProp);
                log(Level.FINEST, "testCustomPropertyChange", "Custom" +
                        " property value after second change: " + newServerid);

                Thread.sleep(notificationSleepTime);
            } catch(SSOException e) {
                       log(Level.SEVERE, "testCustomPropertyChange", "Custom" +
                               " property change failed. This is not the" +
                               " expected result.");
            }
        } catch(Exception e) {
            log(Level.SEVERE, "testCustomPropertyChange",
                    e.getMessage());
            cleanup();
            e.printStackTrace();
            throw e;
        } finally {
            log(Level.FINEST, "testProtectedPropertyChange", "iCount: " +
                    iCount);
            log(Level.FINEST, "testProtectedPropertyChange", "setNum set: " +
                    setNum.toString());
            destroyToken(ssotoken);
            if (iCount != 3  && setNum.size() != 3 && !setNum.contains("1-4") &&
                    !setNum.contains("2-4") && !setNum.contains("3-4")) {
                log(Level.SEVERE, "testCustomPropertyChange", "Property " +
                        "change notification statistics is incorrect.");
                assert false;
            }       
        }
        exiting("testCustomPropertyChange");
    }
    
    /**
     * Cleans up the testcase attributes set in setup
     *
     * @throws java.lang.Exception
     */
    @AfterClass(groups = {"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void cleanup()
    throws Exception {
        entering("cleanup", null);
        try {
            log(Level.FINEST, "cleanup", "Deleting User: "
                    + strTestSPCN);
            idmc.deleteIdentity(adminToken, realm, IdType.USER, strTestSPCN);
        } catch (Exception e) {
            log(Level.SEVERE, "cleanup", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(adminToken);
        }
        exiting("cleanup");
    }

    /**
     * Method implementation to recieve notifications for SSOTokenListener
     * interface
     * @param evt
     */
    public void ssoTokenChanged(SSOTokenEvent evt) {
       try {
           log(Level.FINEST, "ssoTokenChanged", "EVENT TYPE: " + evt.getType());
           iCount++;
           setNum.add(iCount + "-" + evt.getType());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
