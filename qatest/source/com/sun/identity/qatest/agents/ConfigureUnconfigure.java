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
 * $Id: ConfigureUnconfigure.java,v 1.6 2008-02-26 01:15:41 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.agents;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.common.IDMCommon;
import com.sun.identity.qatest.common.SMSCommon;
import com.sun.identity.qatest.common.TestCommon;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

/**
 * Contains setup and cleanup methods required to be executed
 * before and after the execution of agents suite.
 */
public class ConfigureUnconfigure extends TestCommon {

    private SSOToken admintoken;
    private ResourceBundle rbg;
    private IDMCommon idmc;
    private SMSCommon smsc;
    private String agentId;
    private String agentPassword;
    private String strGblRB = "agentsGlobal";

    /**
     * Class constructor. Instantiates the ResourceBundles and
     * creates common objects required by tests.
     */
    public ConfigureUnconfigure()
    throws Exception{
        super("ConfigureUnconfigure");
        rbg = ResourceBundle.getBundle(strGblRB);
        agentId = rbg.getString(strGblRB + ".agentId");
        agentPassword = rbg.getString(strGblRB + ".agentPassword");
        idmc = new IDMCommon();
    }

    /**
     * Creates agent profile for the agent and start the notification server.
     */
    @BeforeSuite(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void createAgentProfile()
    throws Exception {
        entering("createAgentProfile", null);
        try {
            startNotificationServer();
            Map map = new HashMap();
            Set set = new HashSet();
            set.add(agentPassword);
            map.put("userpassword", set);
            set = new HashSet();
            set.add("Active");
            map.put("sunIdentityServerDeviceStatus", set);
            log(Level.FINEST, "createAgentProfile", "Create the agent " +
                    "with ID " + agentId);
            admintoken = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(admintoken);
            if (smsc.isAMDIT()) {
                if (!setValuesHasString(idmc.searchIdentities(admintoken,
                        agentId, IdType.AGENT), agentId))
                    idmc.createIdentity(admintoken, realm, IdType.AGENT,
                            agentId,  map);
            } else {
                set = new HashSet();
                set.add("2.2_Agent");
                map.put("AgentType",set);
                if (!setValuesHasString(idmc.searchIdentities(admintoken,
                        agentId, IdType.AGENTONLY), agentId))
                    idmc.createIdentity(admintoken, realm, IdType.AGENTONLY,
                            agentId, map);
            }
        } catch (Exception e) {
            stopNotificationServer();
            log(Level.SEVERE, "createAgentProfile", e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("createAgentProfile");
    }

    /**
     * Deletes agent profile for the agent and stops the notification server.
     */
    @AfterSuite(groups={"ds_ds", "ds_ds_sec", "ff_ds", "ff_ds_sec"})
    public void deleteAgentProfile()
    throws Exception {
        entering("deleteAgentProfile", null);
        try {
            admintoken = getToken(adminUser, adminPassword, basedn);
            smsc = new SMSCommon(admintoken);
            if (smsc.isAMDIT())
                if (setValuesHasString(idmc.searchIdentities(admintoken,
                agentId, IdType.AGENT), agentId))
                    idmc.deleteIdentity(admintoken, realm, IdType.AGENT,
                            agentId);
            else
                if (setValuesHasString(idmc.searchIdentities(admintoken,
                agentId, IdType.AGENTONLY), agentId))
                    idmc.deleteIdentity(admintoken, realm, IdType.AGENTONLY,
                            agentId);
            stopNotificationServer();
        } catch (Exception e) {
            stopNotificationServer();
            log(Level.SEVERE, "deleteAgentProfile", e.getMessage());
            throw e;
        } finally {
            destroyToken(admintoken);
        }
        exiting("deleteAgentProfile");
    }
}
