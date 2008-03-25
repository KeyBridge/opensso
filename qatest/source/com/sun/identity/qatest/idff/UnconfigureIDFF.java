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
 * $Id: UnconfigureIDFF.java,v 1.9 2008-03-25 22:46:22 mrudulahg Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.idff;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sun.identity.qatest.common.FederationManager;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;

/**
 * This class removes the configuration on SP & IDP 
 * It removes SP & IDP entities on both sides, and also removes the COT. 
 */
public class UnconfigureIDFF extends TestCommon {
    private WebClient webClient;
    private Map<String, String> configMap;
    private String spurl;
    private String idpurl;
    
    /** Creates a new instance of UnconfigureIDFF */
    public UnconfigureIDFF() {
        super("UnconfigureIDFF");
    }
    
    /**
     * Create the webClient which should be run before each test.
     */
    private void getWebClient() 
    throws Exception {
        try {
            webClient = new WebClient(BrowserVersion.MOZILLA_1_0);
        } catch(Exception e) {
            log(Level.SEVERE, "getWebClient", e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Configure sp & idp
     * @DocTest: IDFF|Unconfigure SP & IDP by deleting entities & COT's 
     */
    @AfterSuite(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void UnconfigureIDFF(String strGroupName)
    throws Exception {
        Object[] params = {strGroupName};
        entering("UnconfigureIDFF", params);
        String spurl;
        String idpurl;
        try {
            //Upload global properties file in configMap
            configMap = new HashMap<String, String>();
            getWebClient();
            
            configMap = getMapFromResourceBundle("idffTestConfigData");
            log(Level.FINEST, "UnconfigureIDFF", "Map:" + configMap);
            
            spurl = configMap.get(TestConstants.KEY_SP_PROTOCOL) + "://" + 
                    configMap.get(TestConstants.KEY_SP_HOST) + ":" + 
                    configMap.get(TestConstants.KEY_SP_PORT) + 
                    configMap.get(TestConstants.KEY_SP_DEPLOYMENT_URI);
            idpurl = configMap.get(TestConstants.KEY_IDP_PROTOCOL) + "://" + 
                    configMap.get(TestConstants.KEY_IDP_HOST) + ":" + 
                    configMap.get(TestConstants.KEY_IDP_PORT) + 
                    configMap.get(TestConstants.KEY_IDP_DEPLOYMENT_URI);
            
            consoleLogin(webClient, spurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_SP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_SP_AMADMIN_PASSWORD));
            consoleLogin(webClient, idpurl + "/UI/Login",
                    (String)configMap.get(TestConstants.KEY_IDP_AMADMIN_USER),
                    (String)configMap.get(
                    TestConstants.KEY_IDP_AMADMIN_PASSWORD));
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureIDFF", e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        try{
            FederationManager spfm = new FederationManager(spurl);
            FederationManager idpfm = new FederationManager(idpurl);
            
            HtmlPage idpEntityPage = idpfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), "idff");
            if (FederationManager.getExitCode(idpEntityPage) != 0) {
               log(Level.SEVERE, "UnconfigureIDFF", "listEntities famadm" +
                       " command failed");
               assert false;
            }
            //Delete IDP & SP entities on IDP
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "UnconfigureIDFF", "IDP entity exists at" +
                        " IDP.");
                if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false,
                        "idff")) == 0) {
                    log(Level.FINEST, "UnconfigureIDFF", "Delete IDP entity" +
                            " on IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureIDFF", "Couldnt delete IDP " +
                            "entity on IDP side");
                    log(Level.SEVERE, "UnconfigureIDFF", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            if (idpEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureIDFF", "SP entity exists at" +
                        " IDP.");
                if (FederationManager.getExitCode(idpfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM), false,
                        "idff")) == 0) {
                    log(Level.FINEST, "UnconfigureIDFF", "Deleted SP entity on " +
                            "IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureIDFF", "Couldnt delete SP " +
                            "entity on IDP side");
                    log(Level.SEVERE, "UnconfigureIDFF", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            //Delete COT on IDP side.
            HtmlPage idpcotPage = idpfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM));
            if (FederationManager.getExitCode(idpcotPage) != 0) {
               log(Level.SEVERE, "UnconfigureIDFF", "listCots famadm command" +
                       " failed");
               assert false;
            }
            if (idpcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_COT))) {
                log(Level.FINEST, "UnconfigureIDFF", "COT exists at IDP side");
                if (FederationManager.getExitCode(idpfm.deleteCot(webClient,
                        configMap.get(TestConstants.KEY_IDP_COT),
                        configMap.get(TestConstants.KEY_IDP_EXECUTION_REALM))) == 0) {
                    log(Level.FINEST, "UnconfigureIDFF", "Couldn't delete " +
                            "COT at IDP side");
                } else {
                    log(Level.SEVERE, "UnconfigureIDFF", "Deleted COT " +
                            "at IDP side");                    
                    log(Level.SEVERE, "UnconfigureIDFF", "deleteCot famadm" +
                            " command failed");
                }
            }
            
            HtmlPage spEntityPage = spfm.listEntities(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), "idff");
            if (FederationManager.getExitCode(spEntityPage) != 0) {
               log(Level.SEVERE, "UnconfigureIDFF", "listEntities famadm" +
                       " command failed");
               assert false;
            }
            //Delete SP & IDP entities on sp
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_ENTITY_NAME))) {
                log(Level.FINEST, "UnconfigureIDFF", "SP entity exists at" +
                        " SP.");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_SP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false,
                        "idff")) == 0) {
                    log(Level.FINEST, "UnconfigureIDFF", "Deleted SP entity" +
                            " on SP side");
                } else {
                    log(Level.SEVERE, "UnconfigureIDFF", "Couldnt delete idp " +
                            "entity on SP side");
                    log(Level.SEVERE, "UnconfigureIDFF", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            if (spEntityPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_IDP_ENTITY_NAME)))
            {
                log(Level.FINEST, "UnconfigureIDFF", "IDP entity exists at" +
                        " SP.");
                if (FederationManager.getExitCode(spfm.deleteEntity(webClient,
                        configMap.get(TestConstants.KEY_IDP_ENTITY_NAME),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM), false,
                        "idff")) == 0) {
                    log(Level.FINEST, "UnconfigureIDFF", "Deleted IDP entity" +
                            " on SP side");
                } else {
                    log(Level.FINEST, "UnconfigureIDFF", "Couldnt delete IDP " +
                            "entity on SP side");
                    log(Level.SEVERE, "UnconfigureIDFF", "deleteEntity famadm" +
                            " command failed");
                    assert false;
                }
            }
            
            //Delete COT on sp side.
            HtmlPage spcotPage = spfm.listCots(webClient,
                    configMap.get(TestConstants.KEY_SP_EXECUTION_REALM));
            if (FederationManager.getExitCode(spcotPage) != 0) {
               log(Level.SEVERE, "UnconfigureIDFF", "listCots famadm" +
                       " command failed");
               assert false;
            }
            if (spcotPage.getWebResponse().getContentAsString().
                    contains(configMap.get(TestConstants.KEY_SP_COT))) {
                if (FederationManager.getExitCode(spfm.deleteCot(webClient,
                        configMap.get(TestConstants.KEY_SP_COT),
                        configMap.get(TestConstants.KEY_SP_EXECUTION_REALM))) != 0) {
                    log(Level.SEVERE, "UnconfigureIDFF", "Couldn't delete " +
                            "COT at SP side");
                    log(Level.SEVERE, "UnconfigureIDFF", "deleteCot famadm" +
                            " command failed");
                } else {
                    log(Level.FINEST, "UnconfigureIDFF", "Deleted COT " +
                            "at SP side");                    
                }
            }
            
            if (strGroupName.contains("sec")) {
                    log(Level.FINEST, "UnconfigureIDFF", "Disable XML signing.");
                    List<String> arrList = new ArrayList();
                    arrList.add("XMLSigningOn=false");
                    if (FederationManager.getExitCode(idpfm.setAttrDefs(
                            webClient, "sunFAMIDFFConfiguration", 
                            "Global", "", arrList)) != 0) {
                        log(Level.SEVERE, "UnconfigureIDFF", "Couldn't set " +
                                "XMLSigningOn=true on IDP side ");
                    } else {
                        log(Level.FINEST, "UnconfigureIDFF", "Successfully " +
                                "set XMLSigningOn=false on IDP side ");
                    }
                    if (FederationManager.getExitCode(spfm.setAttrDefs(
                            webClient, "sunFAMIDFFConfiguration", 
                            "Global", "", arrList)) != 0) {
                        log(Level.SEVERE, "UnconfigureIDFF", "Couldn't set " +
                                "XMLSigningOn=true on SP side ");
                    } else {
                        log(Level.FINEST, "UnconfigureIDFF", "Successfully " +
                                "set XMLSigningOn=false on SP side ");
                    }
            }

            IDMCommon idmC = new IDMCommon();
            //If execution_realm is different than root realm (/) 
            //then delete the realm at SP side
            idmC.deleteSubRealms(webClient, spfm, configMap.get(TestConstants.
                    KEY_SP_EXECUTION_REALM), configMap.get(TestConstants.
                    KEY_SP_SUBREALM_RECURSIVE_DELETE));
            
            //If execution_realm is different than root realm (/) 
            //then create the realm at IDP side
            idmC.deleteSubRealms(webClient, idpfm, configMap.get(TestConstants.
                    KEY_IDP_EXECUTION_REALM), configMap.get(TestConstants.
                    KEY_IDP_SUBREALM_RECURSIVE_DELETE));
        } catch(Exception e) {
            log(Level.SEVERE, "UnconfigureIDFF", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            consoleLogout(webClient, spurl);
            consoleLogout(webClient, idpurl);
        }
        exiting("UnconfigureIDFF");
    }
}
