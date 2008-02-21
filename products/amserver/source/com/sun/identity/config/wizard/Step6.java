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
 * $Id: Step6.java,v 1.7 2008-02-21 22:35:45 jonnelson Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.config.wizard;

import com.sun.identity.config.pojos.LDAPStore;
import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.setup.SetupConstants;
import net.sf.click.Page;


public class Step6 extends AjaxPage {

    public Step6(){}

    public void onInit() {
        String hostName = (String)getContext().getSessionAttribute(
            SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_HOST);
        if (hostName != null) {                    
            add("hostName", hostName);            
            add("hostPort", (String)getContext().getSessionAttribute(
                SetupConstants.CONFIG_VAR_DIRECTORY_SERVER_PORT));
            add("userDN", (String)getContext().getSessionAttribute(
                SetupConstants.CONFIG_VAR_DS_MGR_DN));
            add("baseDN", (String)getContext().getSessionAttribute(
                SetupConstants.CONFIG_VAR_ROOT_SUFFIX));
        }
        
        String tmp =(String)getContext().getSessionAttribute("configDirectory");
        add("configDirectory", tmp);
        
        tmp = (String)getContext().getSessionAttribute("configStoreHost");
        add("configStoreHost", tmp);

        tmp = (String)getContext().getSessionAttribute("rootSuffix");
        add("rootSuffix", tmp);

        tmp = (String)getContext().getSessionAttribute("configStorePort");
        add("configStorePort", tmp);

        tmp = (String)getContext().getSessionAttribute("configStoreLoginId");
        add("configStoreLoginId", tmp);


        LDAPStore configStore = (LDAPStore)getContext().getSessionAttribute(
            Step3.LDAP_STORE_SESSION_KEY);
        add("configStore", configStore);

        LDAPStore userStore = (LDAPStore)getContext().getSessionAttribute(
            Step4.LDAP_STORE_SESSION_KEY);
        add( "userStore", userStore);

        add("loadBalancerHost", 
            (String)getContext().getSessionAttribute(SetupConstants.LB_SITE_NAME));
        add("loadBalancerPort", 
            (String)getContext().getSessionAttribute(SetupConstants.LB_PRIMARY_URL));

        super.onInit();
    }

    protected void add(String key, Object value) {
        if (value != null) {
            addModel(key, value);
        }
    }
}
