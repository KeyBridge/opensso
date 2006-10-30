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
 * $Id: IDPSSOFederate.java,v 1.1 2006-10-30 23:16:35 qcheng Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.saml2.profile;

import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.common.QuerySignatureUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class handles the federation and/or single sign on request
 * from a service provider. It processes the <code>AuthnRequest</code>
 * sent by the service provider and generates a proper 
 * <code>Response</code> that contains an <code>Assertion</code>.
 * It sends back a <code>Response</code> containing error status if
 * something is wrong during the request processing.
 */

public class IDPSSOFederate {

    private static final String REQ_ID = "ReqID";

    private IDPSSOFederate() {
    }
 
    /**
     * This method processes the <code>AuthnRequest</code> coming 
     * from a service provider via HTTP Redirect.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     *
     */
    public static void doSSOFederate(HttpServletRequest request,
                                     HttpServletResponse response) {

        String classMethod = "IDPSSOFederate.doSSOFederate: ";
        Object session = null;
   
        try { 
        String idpMetaAlias = request.getParameter(
                              SAML2MetaManager.NAME_META_ALIAS_IN_URI);
        if ((idpMetaAlias == null)
            || (idpMetaAlias.trim().length() == 0)) {
            idpMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
                                            request.getRequestURI());
        }
        if ((idpMetaAlias == null) 
            || (idpMetaAlias.trim().length() == 0)) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "unable to get IDP meta alias from request.");
            }
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                SAML2Utils.bundle.getString("IDPMetaAliasNotFound"));
            return;
        }
      
        // retrieve IDP entity id from meta alias
        String idpEntityID = null;
        String realm = null;
        try {
            if (IDPSSOUtil.metaManager == null) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to get meta manager.");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                   SAML2Utils.bundle.getString("errorMetaManager"));
                return;
            }
            idpEntityID = IDPSSOUtil.metaManager.getEntityByMetaAlias(
                                                         idpMetaAlias);
            if ((idpEntityID == null) 
                || (idpEntityID.trim().length() == 0)) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP Entity ID from meta.");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                   SAML2Utils.bundle.getString("nullIDPEntityID"));
                return;
            }
            realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
        } catch (SAML2MetaException sme) {
            SAML2Utils.debug.error(classMethod +
                "Unable to get IDP Entity ID from meta.");
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
               SAML2Utils.bundle.getString("nullIDPEntityID"));
            return;
        }

        // get the request id query parameter from the request. If this
        // is the first visit then the request id is not set; if it is 
        // coming back from a successful authentication, then request 
        // id should be there.
        String reqID = request.getParameter(REQ_ID);
        if ((reqID != null) && (reqID.trim().length() == 0)) { 
            reqID = null;
        }

        AuthnRequest authnReq = null;
        String relayState = null;
        if (reqID == null) {
            // this is the first time visit 
            // get the saml request query parameter from the request
            String samlRequest = 
                   request.getParameter(SAML2Constants.SAML_REQUEST);
    
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "saml request=" + samlRequest);
            }
            if (samlRequest == null) { 
                SAML2Utils.debug.error(classMethod + "samlRequest is null");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                   SAML2Utils.bundle.getString("InvalidSAMLRequest"));
                return;
            }

            // get the AuthnRequest from the saml request
            authnReq = getAuthnRequest(samlRequest);
            if (authnReq == null) {
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                   SAML2Utils.bundle.getString("InvalidSAMLRequest"));
                return;
            }
       
            if (!SAML2Utils.isSourceSiteValid(
                  authnReq.getIssuer(), realm, idpEntityID)) {
                if (SAML2Utils.debug.warningEnabled()) {
                    SAML2Utils.debug.warning(classMethod + 
                        "Issuer in Request is not valid.");
                }
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                   SAML2Utils.bundle.getString("InvalidSAMLRequest"));
                return;
            }
 
            // verify the signature of the query string if applicable
            IDPSSODescriptorElement idpSSODescriptor = null;
            try {
                idpSSODescriptor = IDPSSOUtil.metaManager.
                             getIDPSSODescriptor(realm, idpEntityID);
            } catch (SAML2MetaException sme) {
                SAML2Utils.debug.error(classMethod, sme);
                idpSSODescriptor = null;
            }
            if (idpSSODescriptor == null) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP SSO Descriptor from meta.");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    SAML2Utils.bundle.getString("metaDataError"));
                return;
            }
            if (idpSSODescriptor.isWantAuthnRequestsSigned()) {
                // need to verify the query string containing authnRequest
                String spEntityID = authnReq.getIssuer().getValue();
                if ((spEntityID == null) || 
                    (spEntityID.trim().length() == 0)) {
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        SAML2Utils.bundle.getString("InvalidSAMLRequest"));
                    return;
                }
                SPSSODescriptorElement spSSODescriptor = null;
                try {
                    spSSODescriptor = IDPSSOUtil.metaManager.
                              getSPSSODescriptor(realm, spEntityID);
                } catch (SAML2MetaException sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                    spSSODescriptor = null;
                }
                if (spSSODescriptor == null) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get SP SSO Descriptor from meta.");
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        SAML2Utils.bundle.getString("metaDataError"));
                    return;
                }
                X509Certificate spCert = KeyUtil.getVerificationCert(
                    spSSODescriptor, spEntityID, false);
                String queryString = request.getQueryString();
                try {
                    if (!QuerySignatureUtil.verify(queryString, spCert)) {
                        SAML2Utils.debug.error(classMethod +
                            "authn request verification failed.");
                        response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                            SAML2Utils.bundle.getString(
                                "invalidSignInRequest"));
                        return;
                    }
                } catch (SAML2Exception se) {
                    SAML2Utils.debug.error(classMethod +
                        "authn request verification failed.", se);
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                        SAML2Utils.bundle.getString("invalidSignInRequest"));
                    return;
                } 
                
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + 
                        "Authn Request signature verification is successful.");
                }
            }

            reqID = authnReq.getID();
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "request id=" + reqID);
            }
            if (reqID == null) {
                SAML2Utils.debug.error(classMethod + "Request id is null");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                   SAML2Utils.bundle.getString("InvalidSAMLRequestID"));
                return;
            }
            // get the user sso session from the request
            try {
                session = SessionManager.getProvider().getSession(request);
            } catch (SessionException se) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(
                        classMethod + "Unable to retrieve user session.");
                }
                session = null;
            }
            // need to check if the forceAuth is true. if so, do auth 
            if (authnReq.isForceAuthn() == Boolean.TRUE) { 
                if (session != null) {
                    try {
                        SessionManager.getProvider().invalidateSession(
                            session, request, response);
                    } catch (SessionException ssoe) {
                        SAML2Utils.debug.error(classMethod +
                            "Unable to invalidate the sso session.");
                    }
                    session = null;
                }
            }

            // get the relay state query parameter from the request
            relayState = request.getParameter(SAML2Constants.RELAY_STATE);
    
            if (session == null) {
                // the user has not logged in yet, redirect to auth
    
                // TODO: need to verify the signature of the AuthnRequest

                // save the AuthnRequest in the IDPCache so that it can be
                // retrieved later when the user successfully authenticates
                IDPCache.authnRequestCache.put(reqID, authnReq);
    
                // save the relay state in the IDPCache so that it can be
                // retrieved later when the user successfully authenticates
                if (relayState != null && relayState.trim().length() != 0) {
                    IDPCache.relayStateCache.put(reqID, relayState);
                }
   
                // redirect to the authentication service
                try {
                    redirectAuthentication(request, response, authnReq,
                                     reqID, realm, idpEntityID);
                } catch (IOException ioe) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to redirect to authentication.", ioe);
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                       SAML2Utils.bundle.getString("UnableToRedirectToAuth"));
                } catch (SAML2Exception se) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to redirect to authentication.", se);
                    response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                       SAML2Utils.bundle.getString("UnableToRedirectToAuth"));
                } 
                return;
            } else {
                // generate assertion response
                sendResponseToACS(
                    request, response, authnReq, idpMetaAlias, relayState);
            }
        } else {
            // the second visit, the user has already authenticated
            // retrieve the cache authn request and relay state
            authnReq = (AuthnRequest)IDPCache.authnRequestCache.remove(reqID);
            relayState = (String)IDPCache.relayStateCache.remove(reqID);
            if (authnReq == null) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to get AuthnRequest from cache.");
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                    SAML2Utils.bundle.getString("UnableToGetAuthnReq"));
                return;
            }
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    classMethod + "RequestID=" + reqID);
            }
            // generate assertion response
            sendResponseToACS(
                request, response, authnReq, idpMetaAlias, relayState);
        }
        } catch (IOException ioe) {
            SAML2Utils.debug.error(classMethod + "I/O rrror", ioe);
        }
    }

 
    /**
     * Sends <code>Response</code> containing an <code>Assertion</code>
     * back to the requesting service provider
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param authnReq the <code>AuthnRequest</code> object
     * @param idpMetaAlias the meta alias of the identity provider
     * @param relayState the relay state 
     * 
     */
    private static void sendResponseToACS(
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   AuthnRequest authnReq,
                                   String idpMetaAlias,
                                   String relayState) 
        throws IOException {

        String classMethod = "IDPSSOFederate.sendResponeToACS: " ;
        String spEntityID = authnReq.getIssuer().getValue();
    
        String nameIDFormat = null;
        NameIDPolicy policy = authnReq.getNameIDPolicy();
        if (policy != null) {
            nameIDFormat = policy.getFormat();
        }
        try {
            IDPSSOUtil.doSSOFederate(request, response, authnReq, 
                  spEntityID, idpMetaAlias, nameIDFormat, relayState); 
        } catch (SAML2Exception se) {
            SAML2Utils.debug.error(classMethod +
                "Unable to do sso or federation.", se);
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                SAML2Utils.bundle.getString("UnableToDOSSOOrFederation"));
       
        }
    }

    /**
     *  Returns the <code>AuthnRequest</code> from saml request string
     */
    private static AuthnRequest getAuthnRequest(String compressedReq) {
    
        AuthnRequest authnReq = null;
        String outputString = SAML2Utils.decodeFromRedirect(compressedReq);
        if (outputString != null) {
            try {
                authnReq = (AuthnRequest)ProtocolFactory.getInstance().
                    createAuthnRequest(outputString);
            } catch (SAML2Exception se) {
                SAML2Utils.debug.error(
                "IDPSSOFederate.getAuthnRequest(): cannot construct "
                + "a AuthnRequest object from the SAMLRequest value:", se);
            }
        }
        return authnReq;
    }

    /**
     * Redirect to authenticate service
     */
    private static void redirectAuthentication(
                             HttpServletRequest request,
                             HttpServletResponse response,
                             AuthnRequest authnReq,
                             String reqID,
                             String realm,
                             String idpEntityID) 
        throws SAML2Exception, IOException {
        String classMethod = "IDPSSOFederate.redirectAuthentication: ";
        // get the authentication service url 
        StringBuffer newURL = new StringBuffer(
                                IDPSSOUtil.getAuthenticationServiceURL(
                                realm, idpEntityID, request));
        // find out the authentication method, e.g. module=LDAP, from
        // authn context mapping 
        IDPAuthnContextMapper idpAuthnContextMapper = 
            IDPSSOUtil.getIDPAuthnContextMapper(realm, idpEntityID);
        
        IDPAuthnContextInfo info = 
            idpAuthnContextMapper.getIDPAuthnContextInfo(
                authnReq, idpEntityID, realm);
        Set authnTypeAndValues = info.getAuthnTypeAndValues();
        if ((authnTypeAndValues != null) 
            && (!authnTypeAndValues.isEmpty())) { 
            Iterator iter = authnTypeAndValues.iterator();
            StringBuffer authSB = new StringBuffer((String)iter.next());
            while (iter.hasNext()) {
                authSB.append("&"); 
                authSB.append((String)iter.next());
            }
            if (newURL.indexOf("?") == -1) {
                newURL.append("?");
            } else {
                newURL.append("&");
            }
            newURL.append(authSB.toString());
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(classMethod +
                    "authString=" + authSB.toString());
            }
        }
        if (newURL.indexOf("?") == -1) {
            newURL.append("?goto=");
        } else {
            newURL.append("&goto=");
        }
        newURL.append(URLEncDec.encode(request.getRequestURL().
                       append("?ReqID=").append(reqID).toString()));
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(classMethod +
                "New URL for authentication: " + newURL.toString());
        }
        // TODO: here we should check if the new URL is one
        //       the same web container, if yes, forward,
        //       if not, redirect
        response.sendRedirect(newURL.toString());
        return;
    }
}
