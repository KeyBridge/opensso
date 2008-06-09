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
 * $Id: Step3.java,v 1.20 2008-06-09 22:07:16 veiming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.config.wizard;

import com.sun.identity.setup.AMSetupServlet;
import net.sf.click.control.ActionLink;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.setup.BootstrapData;
import java.util.Map;
import com.sun.identity.setup.ConfiguratorException;

/**
 * Step 3 is for selecting the embedded or external configuration store 
 */
public class Step3 extends LDAPStoreWizardPage {

    public static final String LDAP_STORE_SESSION_KEY = "wizardCustomConfigStore";
    public ActionLink validateRootSuffixLink = 
        new ActionLink("validateRootSuffix", this, "validateRootSuffix");
    public ActionLink setReplicationLink = 
        new ActionLink("setReplication", this, "setReplication");
    public ActionLink validateHostNameLink = 
        new ActionLink("validateHostName", this, "validateHostName");
    public ActionLink setConfigType = 
        new ActionLink("setConfigType", this, "setConfigType");
    public ActionLink validateLocalPortLink = 
        new ActionLink("validateLocalPort", this, "validateLocalPort");
    
    private static final String QUOTE = "\"";
    private static final String SEPARATOR = "\" : \"";
    private String localRepPort;
    
    public Step3() {
    }
    
    public void onInit() {
        String val = getAttribute("rootSuffix", Wizard.defaultRootSuffix);
        addModel("rootSuffix", val);

        val = getAttribute("encryptionKey", AMSetupServlet.getRandomString());
        addModel("encryptionKey", val);
        
        val = getAttribute("configStorePort", getAvailablePort(50389));
        addModel("configStorePort", val);
        addModel("localConfigPort", val);

        localRepPort = getAttribute("localRepPort", getAvailablePort(58989));
        addModel("localRepPort", localRepPort);

        val = getAttribute("existingPort", getAvailablePort(50389));        
        addModel("existingPort", val);

        val = getAttribute("existingRepPort", getAvailablePort(58990));
        addModel("existingRepPort", val);

        val = getAttribute("configStoreSSL", "SIMPLE");
        addModel("configStoreSSL", val);
        
        if (val.equals("SSL")) {
            addModel("selectConfigStoreSSL", "checked=\"checked\"");
        } else {
            addModel("selectConfigStoreSSL", "");
        }

        // initialize the data store type being used
        val = getAttribute(
            SetupConstants.CONFIG_VAR_DATA_STORE, 
            SetupConstants.SMS_EMBED_DATASTORE);
        addModel(SetupConstants.CONFIG_VAR_DATA_STORE, val);

        if (val.equals(SetupConstants.SMS_EMBED_DATASTORE)) {
            addModel("selectEmbedded", "checked=\"checked\"");
            addModel("selectExternal", "");
        } else {
            addModel("selectEmbedded", "");
            addModel("selectExternal", "checked=\"checked\"");
        }

        val = getAttribute("configStoreHost", "localhost");
        addModel("configStoreHost", val);

        val = getAttribute("configStorePassword", Wizard.defaultPassword);
        addModel("configStorePassword", val);

        val = getAttribute("configStoreLoginId", Wizard.defaultUserName);
        addModel("configStoreLoginId", val);

         val = getAttribute(SetupConstants.DS_EMB_REPL_FLAG, "");
         if (val.equals(SetupConstants.DS_EMP_REPL_FLAG_VAL)) {
             addModel("FIRST_INSTANCE", "1");
             addModel("selectFirstSetup", "");
             addModel("selectExistingSetup", "checked=\"checked\"");
         } else {
             addModel("FIRST_INSTANCE", "0");
             addModel("selectFirstSetup", "checked=\"checked\"");
             addModel("selectExistingSetup", "");
         }

        super.onInit();
    }       

    public boolean setConfigType() {
        String type = toString("type");
        if (type.equals("remote")) {
            type = SetupConstants.SMS_DS_DATASTORE;
        } else {
            type = SetupConstants.SMS_EMBED_DATASTORE;
        } 
        getContext().setSessionAttribute( 
            SetupConstants.CONFIG_VAR_DATA_STORE, type);
        return true;
    }

    public boolean setReplication() {
        String type = toString("multi");
        if (type.equals("enable")) {
            type = SetupConstants.DS_EMP_REPL_FLAG_VAL;
        } 
        getContext().setSessionAttribute(
            SetupConstants.DS_EMB_REPL_FLAG, type);
        return true;
    }

    public boolean validateRootSuffix() {
        String rootsuffix = toString("rootSuffix");
        
        if (rootsuffix == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else {
            writeToResponse("true");
        }
        setPath(null);
        return false;    
    }
    
    
    public boolean validateLocalPort() {
        String port = toString("port");
        
        if (port == null) {
            writeToResponse(getLocalizedString("missing.required.field"));
        } else {
            try {
                int val = Integer.parseInt(port);
                if (val < 1 || val > 65535 ) {
                    writeToResponse(getLocalizedString("invalid.port.number"));
                } else {
                    getContext().setSessionAttribute("configStorePort", port);
                    writeToResponse("true");
                }
            } catch (NumberFormatException e) {
                 writeToResponse(getLocalizedString("invalid.port.number"));
            }
        }
        setPath(null);        
        return false;    
    }
        
        
    /*
     * a call is made to the fam url entered in the browser. If the FAM server
     * exists a <code>Map</code> of data will be returned which contains the
     * information about the existing servers data store, including any 
     * replication ports if its embedded.
     * Information to control the UI is returned in a JSON object of the form
     * { 
     *   "param1" : "value1", 
     *   "param2" : "value2"
     * }
     * The JS on the browser will interpret the above and make the necessary
     * changes to prompt the user for any more details required.
     */
    public boolean validateHostName() {
        StringBuffer sb = new StringBuffer();
        String hostName = toString("hostName");
        
        if (hostName == null) {            
            addObject(sb, "code", "100");
            addObject(sb, "message",
                getLocalizedString("missing.required.field"));
        } else {
            // try to retrieve the remote FAM information
            String admin = "amadmin";
            String password = (String)getContext().getSessionAttribute(
                SetupConstants.CONFIG_VAR_ADMIN_PWD);
            
            try { 
                String dsType;
                Map data = AMSetupServlet.getRemoteServerInfo(
                    hostName, admin, password);
                
                // data returned from existing FAM server
                if (data != null && !data.isEmpty()) {                    
                    addObject(sb, "code", "100");
                    addObject(sb, "message", getLocalizedString("ok.string"));
                    
                    setupDSParams(data);
                    
                    String key = (String)data.get("enckey");
                    getContext().setSessionAttribute("encryptionKey",key);

                    getContext().setSessionAttribute("ENCLDAPUSERPASSWD",
                         (String)data.get("ENCLDAPUSERPASSWD"));
                    
                    // true for embedded, false for sunds
                    String embedded = 
                        (String)data.get(BootstrapData.DS_ISEMBEDDED);
                    addObject(sb, "embedded", embedded);           
                    String host = (String)data.get(BootstrapData.DS_HOST);
                    
                    if (embedded.equals("true")) {
                        getContext().setSessionAttribute(
                            "configStoreHost", getHostName());
                        addObject(sb, "configStoreHost", "localhost");

                        // set the multi embedded flag 
                        getContext().setSessionAttribute(
                            SetupConstants.CONFIG_VAR_DATA_STORE, 
                            SetupConstants.SMS_EMBED_DATASTORE); 
                        
                        getContext().setSessionAttribute(
                            SetupConstants.DS_EMB_REPL_FLAG,
                            SetupConstants.DS_EMP_REPL_FLAG_VAL); 
      
                        // get the existing replication ports if any
                        String replAvailable = (String)data.get(
                            BootstrapData.DS_REPLICATIONPORT_AVAILABLE);
                        if (replAvailable == null) {
                            replAvailable = "false";
                        }
                        addObject(sb, "replication", replAvailable);
                        String existingRep = (String)data.get(
                            BootstrapData.DS_REPLICATIONPORT);
                        getContext().setSessionAttribute(
                            "existingRepPort", existingRep);
                        addObject(sb, "replicationPort", existingRep);
                    } else {
                        getContext().setSessionAttribute("configStorePort", 
                            (String) data.get(BootstrapData.DS_PORT));
                        getContext().setSessionAttribute(
                            "configStoreHost", host);   
                        addObject(sb, "configStoreHost", host);
                    }

                    // set the replication ports pulled from the remote
                    // server in the session and pass back to the client
                    String existing = (String)data.get(
                        BootstrapData.DS_PORT);
                    getContext().setSessionAttribute(
                        "existingPort", existing);
                    addObject(sb, "existingPort", existing);

                    // set the configuration store port
                    getContext().setSessionAttribute(
                        "existingStorePort", existing);   
                    addObject(sb, "existingStorePort", existing);
                    
                    getContext().setSessionAttribute("existingHost",host);

                    // set the configuration store host
                    getContext().setSessionAttribute(
                        "existingStoreHost", host);   
                    addObject(sb, "existingStoreHost", host);

                    // set the configuration store port
                    getContext().setSessionAttribute(
                        "localRepPort", localRepPort);

                    // dsmgr password is same as amadmin for embedded
                    getContext().setSessionAttribute(
                        "configStorePassword", password);     
                }
            } catch (ConfiguratorException c) {
                String code = c.getErrorCode();
                String message = getLocalizedString(code);
                if (code == null) {
                    code = "999";
                    message = c.getMessage();
                }
                addObject(sb, "code", code);
                addObject(sb, "message", message);                                                       
            }
        }
        sb.append(" }");           
        writeToResponse(sb.toString());
        setPath(null);
        return false;
    }
        
    private void addObject(StringBuffer sb, String key, String value) {
        if (sb.length() < 1) {
            // add first object
            sb.append("{ ");
        } else {
            sb.append(",");
        }
        sb.append(QUOTE)
          .append(key)
          .append(SEPARATOR)
          .append(value)
          .append(QUOTE);                         
    }
    
    /*
     * the following value have been pulled from an existing fam server
     * which was configured to use an external DS. We need to set the DS 
     * values in the request so they can be used to configure the exisiting
     * FAM server.
     */
    private void setupDSParams(Map data) {             
        String tmp = (String)data.get(BootstrapData.DS_BASE_DN);
        getContext().setSessionAttribute("rootSuffix", tmp);        
        
        tmp = (String)data.get(BootstrapData.DS_MGR);
        getContext().setSessionAttribute("configStoreLoginId", tmp);        
        
        tmp = (String)data.get(BootstrapData.DS_PWD);
        getContext().setSessionAttribute("configStorePassword", tmp);
       
        getContext().setSessionAttribute(
            SetupConstants.CONFIG_VAR_DATA_STORE, 
            SetupConstants.SMS_DS_DATASTORE);    
    }
}
