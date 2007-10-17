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
 * $Id: IDPSSOFederate.java,v 1.9 2007-10-17 18:46:36 weisun2 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.saml2.profile;

import com.sun.identity.saml2.protocol.Scoping;
import com.sun.identity.multiprotocol.MultiProtocolUtils;
import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.common.QuerySignatureUtil;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.IDPAuthnContextInfo;
import com.sun.identity.saml2.plugins.IDPAuthnContextMapper;
import com.sun.identity.saml2.plugins.IDPECPSessionMapper;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import org.w3c.dom.Element;

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
        doSSOFederate(request, response, false);
    }
    /**
     * This method processes the <code>AuthnRequest</code> coming 
     * from a service provider via HTTP Redirect.
     *
     * @param request the <code>HttpServletRequest</code> object
     * @param response the <code>HttpServletResponse</code> object
     * @param isFromECP true if the request comes from ECP
     *
     */
    public static void doSSOFederate(HttpServletRequest request,
        HttpServletResponse response, boolean isFromECP) {

        String classMethod = "IDPSSOFederate.doSSOFederate: ";
        Object session = null;
        SPSSODescriptorElement spSSODescriptor = null;
        try { 
            String idpMetaAlias = request.getParameter(
                SAML2MetaManager.NAME_META_ALIAS_IN_URI);
            if ((idpMetaAlias == null) || (idpMetaAlias.trim().length() == 0)) {
                idpMetaAlias = SAML2MetaUtils.getMetaAliasByUri(
                    request.getRequestURI());
            }
            if ((idpMetaAlias == null) || (idpMetaAlias.trim().length() == 0)) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                        "unable to get IDP meta alias from request.");
                }
                sendError(response, SAML2Constants.CLIENT_FAULT,
                    "IDPMetaAliasNotFound", null, isFromECP);
                return;
            }
      
            // retrieve IDP entity id from meta alias
            String idpEntityID = null;
            String realm = null;
            try {
                if (IDPSSOUtil.metaManager == null) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get meta manager.");
                    sendError(response, SAML2Constants.SERVER_FAULT,
                        "errorMetaManager", null, isFromECP);
                    return;
                }
                idpEntityID = IDPSSOUtil.metaManager.getEntityByMetaAlias(
                    idpMetaAlias);
                if ((idpEntityID == null) ||
                    (idpEntityID.trim().length() == 0)) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get IDP Entity ID from meta.");
                    String[] data = { idpEntityID };
                    LogUtil.error(Level.INFO, LogUtil.INVALID_IDP, data, null);
                    sendError(response, SAML2Constants.CLIENT_FAULT,
                        "nullIDPEntityID", null, isFromECP);
                    return;
                }
                realm = SAML2MetaUtils.getRealmByMetaAlias(idpMetaAlias);
            } catch (SAML2MetaException sme) {
                SAML2Utils.debug.error(classMethod +
                    "Unable to get IDP Entity ID from meta.");
                String[] data = { idpMetaAlias };
                LogUtil.error(Level.INFO, LogUtil.IDP_METADATA_ERROR, data,
                    null);
                sendError(response, SAML2Constants.SERVER_FAULT,
                    "nullIDPEntityID", sme.getMessage(), isFromECP);
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
                authnReq = getAuthnRequest(request, isFromECP);
                if (authnReq == null) {
                    sendError(response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequest", null, isFromECP);
                    return;
                }

                String spEntityID = authnReq.getIssuer().getValue();
                try {
                    String authnRequestStr = authnReq.toXMLString();
                    String[] logdata = { spEntityID, idpMetaAlias,
                        authnRequestStr };
                    String logId = isFromECP ?
                        LogUtil.RECEIVED_AUTHN_REQUEST_ECP :
                        LogUtil.RECEIVED_AUTHN_REQUEST;
                    LogUtil.access(Level.INFO, logId, logdata, null);
                } catch (SAML2Exception saml2ex) {
                    SAML2Utils.debug.error(classMethod, saml2ex);
                    sendError(response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequest", saml2ex.getMessage(), isFromECP);
                    return;
                }

                if (!SAML2Utils.isSourceSiteValid(
                    authnReq.getIssuer(), realm, idpEntityID)) {
                    if (SAML2Utils.debug.warningEnabled()) {
                        SAML2Utils.debug.warning(classMethod + 
                            "Issuer in Request is not valid.");
                    }
                    sendError(response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequest", null, isFromECP);
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
                    sendError(response, SAML2Constants.SERVER_FAULT,
                        "metaDataError", null, isFromECP);
                    return;
                } 
                try {
                    spSSODescriptor = IDPSSOUtil.metaManager.
                        getSPSSODescriptor(realm, spEntityID);
                } catch (SAML2MetaException sme) {
                    SAML2Utils.debug.error(classMethod, sme);
                    spSSODescriptor = null;
                }

                if (isFromECP || idpSSODescriptor.isWantAuthnRequestsSigned()) {
                    // need to verify the query string containing authnRequest
                    if ((spEntityID == null) || 
                        (spEntityID.trim().length() == 0)) {
                        sendError(response, SAML2Constants.CLIENT_FAULT,
                            "InvalidSAMLRequest", null, isFromECP);
                        return;
                    }
                
                    if (spSSODescriptor == null) {
                        SAML2Utils.debug.error(classMethod +
                            "Unable to get SP SSO Descriptor from meta.");
                        sendError(response, SAML2Constants.SERVER_FAULT,
                            "metaDataError", null, isFromECP);
                        return;
                    }
                    X509Certificate spCert = KeyUtil.getVerificationCert(
                        spSSODescriptor, spEntityID, false);

                    try {
                        boolean isSignatureOK = false;
                        if (isFromECP) {
                            isSignatureOK = authnReq.isSignatureValid(spCert);
                        } else {
                            String queryString = request.getQueryString();
                            isSignatureOK =
                                QuerySignatureUtil.verify(queryString, spCert);
                        }
                        if (!isSignatureOK) {
                            SAML2Utils.debug.error(classMethod +
                                "authn request verification failed.");
                            sendError(response, SAML2Constants.CLIENT_FAULT,
                                "invalidSignInRequest", null, isFromECP);
                            return;
                        }

                        // In ECP profile, sp doesn't know idp.
                        if (!isFromECP) {
                            // verify Destination
                            List ssoServiceList =
                                idpSSODescriptor.getSingleSignOnService();
                            String ssoURL =
                                SPSSOFederate.getSSOURL(ssoServiceList,
                                SAML2Constants.HTTP_REDIRECT);
                            if (!SAML2Utils.verifyDestination(
                                authnReq.getDestination(), ssoURL)) {
                                SAML2Utils.debug.error(classMethod + "authn " +
                                    "request destination verification failed.");
                                sendError(response, SAML2Constants.CLIENT_FAULT,
                                    "invalidDestination", null, isFromECP);
                                return;
                            }
                        }
                    } catch (SAML2Exception se) {
                        SAML2Utils.debug.error(classMethod +
                            "authn request verification failed.", se);
                        sendError(response, SAML2Constants.CLIENT_FAULT,
                            "invalidSignInRequest", null, isFromECP);
                        return;
                    } 
                
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod + "authn " +
                            "request signature verification is successful.");
                    }
                }

                reqID = authnReq.getID();
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod +
                        "request id=" + reqID);
                }
                if (reqID == null) {
                    SAML2Utils.debug.error(classMethod + "Request id is null");
                    sendError(response, SAML2Constants.CLIENT_FAULT,
                        "InvalidSAMLRequestID", null, isFromECP);
                    return;
                }

                if (isFromECP) {
                    try {
                        IDPECPSessionMapper idpECPSessonMapper = 
                            IDPSSOUtil.getIDPECPSessionMapper(realm,
                            idpEntityID);
                        session = idpECPSessonMapper.getSession(request,
                            response);
                    } catch (SAML2Exception se) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod +
                                "Unable to retrieve user session.");
                        }
                    }
                } else {
                    // get the user sso session from the request
                    try {
                        session = SessionManager.getProvider().getSession(
                            request);
                    } catch (SessionException se) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod +
                                "Unable to retrieve user session.");
                        }
                        session = null;
                    }
                }
                // need to check if the forceAuth is true. if so, do auth 
                if (Boolean.TRUE.equals(authnReq.isForceAuthn())) {
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
                    synchronized (IDPCache.authnRequestCache) { 
                        IDPCache.authnRequestCache.put(reqID,
                            new CacheObject(authnReq));
                    }
    
                    // save the relay state in the IDPCache so that it can be
                    // retrieved later when the user successfully authenticates
                    if (relayState != null && relayState.trim().length() != 0) {
                        IDPCache.relayStateCache.put(reqID, relayState);
                    }
     
                    //Initiate proxying
                    try {
                        boolean isProxy = IDPProxyUtil.isIDPProxyEnabled(
                            authnReq, realm);
                        if (isProxy) { 
                            String preferredIDP = IDPProxyUtil.getPreferredIDP(
                                authnReq,idpEntityID, realm, request, response);
                            if (preferredIDP != null) {
                                if (SAML2Utils.debug.messageEnabled()) {
                                    SAML2Utils.debug.message(classMethod +
                                        "IDP to be proxied" +  preferredIDP);
                                }
                                String tmp = IDPProxyUtil.sendProxyAuthnRequest(
                                    authnReq, preferredIDP, spSSODescriptor,
                                    idpEntityID, request, response, realm,
                                    relayState);
                                response.sendRedirect(tmp);
                                return;
                            }
                        }
                        //else continue for the local authentication.    
                    } catch (SAML2Exception re) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod +
                                "Redirecting for the proxy handling error: "
                                + re.getMessage());
                        }
                        sendError(response, SAML2Constants.SERVER_FAULT,
                            "UnableToRedirectToPreferredIDP", re.getMessage(),
                            isFromECP);
                    }
   
                    // redirect to the authentication service
                    try {
                        redirectAuthentication(request, response, authnReq,
                            reqID, realm, idpEntityID);
                    } catch (IOException ioe) {
                        SAML2Utils.debug.error(classMethod +
                            "Unable to redirect to authentication.", ioe);
                        sendError(response, SAML2Constants.SERVER_FAULT,
                            "UnableToRedirectToAuth", ioe.getMessage(),
                            isFromECP);
                    } catch (SAML2Exception se) {
                        SAML2Utils.debug.error(classMethod +
                            "Unable to redirect to authentication.", se);
                        sendError(response, SAML2Constants.SERVER_FAULT,
                            "UnableToRedirectToAuth", se.getMessage(),
                            isFromECP);
                    } 
                    return;
                } else {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod + 
                            "Session is valid");
                    }
                    RequestedAuthnContext requestAuthnContext =
                        authnReq.getRequestedAuthnContext();
                    boolean sessionUpgrade = true;
                    IDPSession oldIDPSession = null;
                    String sessionIndex = null;
                    sessionIndex = IDPSSOUtil.getSessionIndex(session);
                    sessionUpgrade =
                        isSessionUpgrade(requestAuthnContext,sessionIndex);

                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(classMethod +
                            "Session Upgrade is :" + sessionUpgrade);
                    }
                    if (sessionUpgrade) {
                        // Save the original IDP Session
                        oldIDPSession = (IDPSession) 
                            IDPCache.idpSessionsByIndices.get(sessionIndex); 
                        IDPCache.oldIDPSessionCache.put(reqID,oldIDPSession);
                        // Save the new requestId and AuthnRequest
                        IDPCache.authnRequestCache.put(reqID, 
                            new CacheObject(authnReq));
                        // save if the request was an Session Upgrade case.
                        IDPCache.isSessionUpgradeCache.add(reqID);
                        // redirect to the authentication service

                        // save the relay state in the IDPCache so that it can
                        // be retrieved later when the user successfully
                        // authenticates
                        if ((relayState != null) &&
                            (relayState.trim().length() != 0)) {
                            IDPCache.relayStateCache.put(reqID, relayState);
                        }

                        try {
                             redirectAuthentication(request, response, authnReq,
                                 reqID, realm, idpEntityID);
                             return;
                        } catch (IOException ioe) {
                            SAML2Utils.debug.error(classMethod +
                                 "Unable to redirect to authentication.", ioe);
                            sessionUpgrade = false;
                            cleanUpCache(reqID);
                            sendError(response, SAML2Constants.SERVER_FAULT,
                                "UnableToRedirectToAuth", ioe.getMessage(),
                                isFromECP);
                        } catch (SAML2Exception se) {
                            SAML2Utils.debug.error(classMethod +
                                "Unable to redirect to authentication.", se);
                            sessionUpgrade = false;
                            cleanUpCache(reqID);
                            sendError(response, SAML2Constants.SERVER_FAULT,
                                "UnableToRedirectToAuth", se.getMessage(),
                                isFromECP);
                        }
                    } 
                    // comes here if either no session upgrade or error
                    // redirecting to authentication url.
                    // generate assertion response
                    if (!sessionUpgrade) {
                         // call multi-federation protocol to set the protocol
                         MultiProtocolUtils.addFederationProtocol(session, 
                             SingleLogoutManager.SAML2);
                        NameIDPolicy policy = authnReq.getNameIDPolicy();
                        String nameIDFormat =
                            (policy == null) ? null : policy.getFormat();
                        try {
                            IDPSSOUtil.sendResponseToACS(request, response,
                                session, authnReq, spEntityID, idpEntityID,
                                idpMetaAlias, realm, nameIDFormat, relayState);
                        } catch (SAML2Exception se) {
                            SAML2Utils.debug.error(classMethod +
                                "Unable to do sso or federation.", se);
                            sendError(response, SAML2Constants.SERVER_FAULT,
                                "UnableToDOSSOOrFederation", se.getMessage(),
                                isFromECP);
                        }
                    }
                }
            } else {
                // the second visit, the user has already authenticated
                // retrieve the cache authn request and relay state
                CacheObject cacheObj = null;
                synchronized (IDPCache.authnRequestCache) {
                    cacheObj =
                        (CacheObject)IDPCache.authnRequestCache.remove(reqID);
                }
                if (cacheObj != null) {
                    authnReq = (AuthnRequest)cacheObj.getObject();
                }
                relayState = (String)IDPCache.relayStateCache.remove(reqID);
                if (authnReq == null) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to get AuthnRequest from cache.");
                    sendError(response, SAML2Constants.SERVER_FAULT,
                        "UnableToGetAuthnReq", null, isFromECP);
                    return;
                }
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message(classMethod + "RequestID=" +
                        reqID);
                }
                boolean isSessionUpgrade = false;

                if (IDPCache.isSessionUpgradeCache != null 
                    && !IDPCache.isSessionUpgradeCache.isEmpty()) {
                    if (IDPCache.isSessionUpgradeCache.contains(reqID)) {
                        isSessionUpgrade =  true;
                    }
                }
                SessionProvider sessionProvider = SessionManager.getProvider();
                session = sessionProvider.getSession(request);
                if (isSessionUpgrade) {
                    IDPSession oldSess = 
                        (IDPSession)IDPCache.oldIDPSessionCache.remove(reqID);
                    String sessionIndex = IDPSSOUtil.getSessionIndex(session);
                    if (sessionIndex != null && (sessionIndex.length() != 0 )) { 
                        IDPCache.idpSessionsByIndices.put(sessionIndex,oldSess);
                    }
                }
                if (session != null) {
                    // call multi-federation protocol to set the protocol
                    MultiProtocolUtils.addFederationProtocol(session, 
                        SingleLogoutManager.SAML2);
                }
                // generate assertion response
                String spEntityID = authnReq.getIssuer().getValue();
                NameIDPolicy policy = authnReq.getNameIDPolicy();
                String nameIDFormat =
                    (policy == null) ? null : policy.getFormat();
                try {
                    IDPSSOUtil.sendResponseToACS(request, response, session,
                        authnReq, spEntityID, idpEntityID, idpMetaAlias, realm,
                        nameIDFormat, relayState);
                } catch (SAML2Exception se) {
                    SAML2Utils.debug.error(classMethod +
                        "Unable to do sso or federation.", se);
                    sendError(response, SAML2Constants.SERVER_FAULT,
                        "UnableToDOSSOOrFederation", se.getMessage(),
                        isFromECP);
                }
            }
        } catch (IOException ioe) {
            SAML2Utils.debug.error(classMethod + "I/O rrror", ioe);
        } catch (SessionException sso) {
            SAML2Utils.debug.error("SSOException : " , sso);
        } catch (SOAPException soapex) {
            SAML2Utils.debug.error("IDPSSOFederate.doSSOFederate:" , soapex);
        }
    }

    private static void sendError(HttpServletResponse response,
        String faultCode, String rbKey, String detail, boolean isFromECP)
        throws IOException, SOAPException {

        if (isFromECP) {
            SOAPMessage soapFault = SAML2Utils.createSOAPFault(faultCode,
                rbKey, detail);
            if (soapFault != null) {
                //  Need to call saveChanges because we're
                // going to use the MimeHeaders to set HTTP
                // response information. These MimeHeaders
                // are generated as part of the save.
                if (soapFault.saveRequired()) {
                    soapFault.saveChanges();
                }
                response.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(soapFault.getMimeHeaders(), response);
                // Write out the message on the response stream
                OutputStream os = response.getOutputStream();
                soapFault.writeTo(os);
                os.flush();
                os.close();
            } else {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                SAML2Utils.bundle.getString(rbKey));
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
     *  Returns the <code>AuthnRequest</code> from HttpServletRequest
     */
    private static AuthnRequest getAuthnRequest(HttpServletRequest request,
        boolean isFromECP) {

        if (isFromECP) {
            MimeHeaders headers = SAML2Utils.getHeaders(request);
            try {
                InputStream is = request.getInputStream();
                SOAPMessage msg = SAML2Utils.mf.createMessage(headers, is);
                Element elem = SAML2Utils.getSamlpElement(msg, 
                    SAML2Constants.AUTHNREQUEST);
                return ProtocolFactory.getInstance().createAuthnRequest(elem);
	    } catch (Exception ex) {
                SAML2Utils.debug.error("IDPSSOFederate.getAuthnRequest:", ex);
            }
            return null;
        } else {
            String samlRequest = 
                request.getParameter(SAML2Constants.SAML_REQUEST);
    
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("IDPSSOFederate.getAuthnRequest: " +
                    "saml request = " + samlRequest);
            }
            if (samlRequest == null) { 
                return null;
            }
            return getAuthnRequest(samlRequest);
        }
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
    
       /**
     * Iterates through the RequestedAuthnContext from Service Provider and
     * check if user has already authenticated with the same AuthnContext.
     * If RequestAuthnContext is not found in the authenticated AuthnContext
     * then session upgrade will be done .
     *
     * @param requestAuthnContext the <code>RequestAuthnContext</code> object.
     * @param sessionIndex the Session Index of the active session.
     * @return true if the requester requires to reauthenticate
     */
    private static boolean isSessionUpgrade(
                               RequestedAuthnContext requestAuthnContext,
                               String sessionIndex) {
        String classMethod = "IDPSSOFederate.isSessionUpgrade: ";

        if (sessionIndex == null) {
            return false;
        }

        boolean sessionUpgrade=true;
        if (requestAuthnContext != null) {
            // Get the AuthContext 
            Set authContextSet = 
                (HashSet) IDPCache.authnContextCache.remove(sessionIndex);
            if (authContextSet != null && !authContextSet.isEmpty()) {
                Iterator authCtxIterator = authContextSet.iterator();
                List authnCtxRefList = 
                    requestAuthnContext.getAuthnContextClassRef();
                if (authnCtxRefList != null &&  !authnCtxRefList.isEmpty()){
                    Iterator i = authnCtxRefList.iterator();
                    while (i.hasNext()) {
                        String authClass = (String)i.next();
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(classMethod 
                                        + "SP authClassReference: " 
                                        + authClass);
                        }
                        while (authCtxIterator.hasNext()) {
                            AuthnContext authnContext = 
                                  (AuthnContext)authCtxIterator.next();
                            String origAuthnCtxClassRef = 
                                  authnContext.getAuthnContextClassRef();
                            if (SAML2Utils.debug.messageEnabled()) {
                                SAML2Utils.debug.message(classMethod 
                                  +  "SP Original authClassReference: "
                                  + origAuthnCtxClassRef);
                            }
                            if (authClass != null && 
                                authClass.equals(origAuthnCtxClassRef)) {
                                sessionUpgrade = false;
                                break;
                            }
                         }
                     }
                 } else { 
                     List authnCtxDeclRef = 
                         requestAuthnContext.getAuthnContextDeclRef();
                     if (authnCtxDeclRef != null && 
                                   !authnCtxDeclRef.isEmpty()) {
                         Iterator i = authnCtxDeclRef.iterator();
                         while(i.hasNext()) {
                             String authClass = (String)i.next();
                             if (SAML2Utils.debug.messageEnabled()) {
                                 SAML2Utils.debug.message(classMethod +
                                     "authDeclReference from SP is :" + 
                                      authClass);
                             }
                             while (authCtxIterator.hasNext()) {
                                 AuthnContext authnContext = 
                                     (AuthnContext)authCtxIterator.next();
                                 String origDeclRef = 
                                     authnContext.getAuthnContextDeclRef();
                                 if (SAML2Utils.debug.messageEnabled()) {
                                     SAML2Utils.debug.message(classMethod 
                                     +"Original authDeclRef from  SP is : " 
                                     + origDeclRef);
                                 }
                                 if ((authClass != null) 
                                     && (authClass.equals(origDeclRef))) {
                                     sessionUpgrade=false;
                                     break;
                                 }
                             }
                          }
                      }
                 }
            } else {
                // no authcontext to compare with 
                // so no session upgrade.
                 sessionUpgrade=false;
            }
         }
         return sessionUpgrade;
    }

    /**
     * clean up the cache created for session upgrade.
     */
    private static void cleanUpCache(String reqID) {
        IDPCache.oldIDPSessionCache.remove(reqID);
        IDPCache.authnRequestCache.remove(reqID);
        IDPCache.isSessionUpgradeCache.remove(reqID); 
    }
}
