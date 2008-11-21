/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
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
 * $Id: WebsphereConfigDirFinder.java,v 1.1 2008-11-21 22:21:55 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere.v61;

import java.net.InetAddress;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.IDefaultValueFinder;
import com.sun.identity.agents.tools.websphere.IConstants;
import com.sun.identity.install.tools.util.Debug;

/**
 * This class locates the config directory of Websphere instance.
 */
public class WebsphereConfigDirFinder implements IDefaultValueFinder,IConstants{
    
    public String getDefaultValue(String key, IStateAccess state, 
            String value) {
        
        String result = null;
        if (value != null) {
            result = value;
        } else {
            result = getDefaultWASConfigDirPath();
        }
        
        return result;
    }
    
    
    private String getDefaultWASConfigDirPath() {
        String result = null;
        String osName = System.getProperty(STR_OS_NAME_PROPERTY);
        String localHost = "localhost";
        try {
            localHost = InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            // Better not throw an ugly exception while trying to get
            // default value
            Debug.log("WebsphereConfigDirFinder.getDefaultWASConfigDirPath " +
                    " exception caught : " + ex.getMessage());
        }
        
        if (osName.toLowerCase().startsWith(STR_WINDOWS)) {
            result = "C:\\Program Files\\IBM\\WebSphere\\AppServer\\"
                    + "profiles\\wp_profile\\config\\cells\\"
                    + "cell01\\nodes\\node01\\servers\\server1";
        } else {
            result = "/opt/IBM/WebSphere/AppServer/profiles/wp_profile/" +
                    "config/cells/cell01/nodes/node01/servers/server1";
        }
        
        return result;
    }
    
}
