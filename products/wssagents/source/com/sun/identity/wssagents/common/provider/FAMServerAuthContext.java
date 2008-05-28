/*
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
 * $Id: FAMServerAuthContext.java,v 1.1 2008-05-28 19:50:52 mrudul_uchil Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.wssagents.common.provider;

import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthContext;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.server.WSEndpoint;

public class FAMServerAuthContext implements ServerAuthContext {
    
    private CallbackHandler handler = null;
    
    //***************AuthModule Instance**********
    FAMServerAuthModule  authModule = null;
    
    /** Creates a new instance of FAMServerAuthContext */
    public FAMServerAuthContext(String operation, Subject subject, Map map, 
        CallbackHandler callbackHandler) {
        //System.out.println("FAMServerAuthContext operation : " + operation);
        //System.out.println("FAMServerAuthContext subject : " + subject);
        //System.out.println("FAMServerAuthContext map : " + map);
        //System.out.println("FAMServerAuthContext callbackHandler : " + 
        //    callbackHandler);
        //initialize the AuthModules and keep references to them
        this.handler = callbackHandler;
        
        // TBD : Following code will be changed to use only one way of getting
        // service end point once Metro / JAX-WS team gives the fix for
        // "appContext" issue.
        String appContext = (String)map.get("providername");
        System.out.println("FAMServerAuthContext appContext : " + appContext);
        
        WSDLPort port2 = (WSDLPort)map.get("WSDL_MODEL");
        String endpoint2 = port2.getAddress().getURL().toString();
        System.out.println("FAMServerAuthContext endpoint from WSDL model : " + 
            endpoint2);
        
        WSEndpoint endPoint = (WSEndpoint)map.get("ENDPOINT");
        WSDLPort port = endPoint.getPort();
        String endpoint = port.getAddress().getURL().toString();
        System.out.println("FAMServerAuthContext WSP endpoint : " + endpoint);
        
        String providerName = endpoint;
        
        System.out.println("FAMServerAuthContext providerName : " + 
            providerName);
        
        authModule = new FAMServerAuthModule();
        map.put("providername", providerName);
        try {
            authModule.initialize(null, null, null,map);
        } catch (AuthException e) {
            System.out.println("FAMServerAuthContext : serverAuthModule : " + 
                "Initialize ERROR : " + e.toString());
            e.printStackTrace();
        }
        
    }
    
    public AuthStatus validateRequest(MessageInfo messageInfo, 
        Subject clientSubject, Subject serviceSubject) throws AuthException {
        
        try {
            return authModule.validateRequest(messageInfo, clientSubject, 
                serviceSubject);
        } catch (AuthException e) {
            System.out.println("FAMServerAuthContext : serverAuthModule : " + 
                "validateRequest ERROR : " + e.toString());
            e.printStackTrace();
            return AuthStatus.SEND_FAILURE;
        }
        
    }
    
    public AuthStatus secureResponse(MessageInfo messageInfo, 
        Subject serviceSubject) throws AuthException {
        
        try {
            return authModule.secureResponse(messageInfo, serviceSubject);
        } catch (AuthException e) {
            System.out.println("FAMServerAuthContext : serverAuthModule : " + 
                "secureResponse ERROR : " + e.toString());
            e.printStackTrace();
            return AuthStatus.SEND_FAILURE;
        }
        
    }
    
    public void cleanSubject(MessageInfo messageInfo, Subject subject) 
        throws AuthException {
        try {
            authModule.cleanSubject(messageInfo, subject);
        } catch (AuthException e) {
            System.out.println("FAMServerAuthContext : serverAuthModule : " + 
                "cleanSubject ERROR.");
            e.printStackTrace();
        }
    }
    
               
}