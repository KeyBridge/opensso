<%--
   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: SA_SP.jsp,v 1.3 2007-12-15 09:24:06 rajeevangal Exp $

   Copyright 2007 Sun Microsystems Inc. All Rights Reserved
--%>

<%@ page language="java" 
import="java.util.*,
java.io.*,
java.util.logging.Level,
com.sun.identity.plugin.session.SessionProvider,
com.sun.identity.plugin.session.SessionManager,
com.sun.identity.plugin.session.SessionException,
com.sun.identity.saml2.common.SAML2Constants,
com.sun.identity.saml2.common.SAML2Utils,
com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement,
com.sun.identity.saml2.logging.LogUtil,
com.sun.identity.saml2.meta.SAML2MetaManager,
com.sun.identity.saml2.meta.SAML2MetaException,
com.sun.identity.saml2.meta.SAML2MetaUtils,
com.sun.identity.sae.api.SecureAttrs,
com.sun.identity.sae.api.Utils"
%>

<%
    //  Setup http GET/POST to sp app URL
    String action = "POST";
    // Resolve FM-SP's metaAlias
    String spMetaAlias = 
               SAML2MetaUtils.getMetaAliasByUri(request.getRequestURI()) ;

    String servletPath = request.getServletPath();
    String gotoUrl = request.getRequestURL().toString();
    String appBase = gotoUrl.substring(0, gotoUrl.lastIndexOf(servletPath)+1);
    String errorUrl = appBase+"saml2/jsp/saeerror.jsp";
    String ipaddr = request.getRemoteAddr();
    String userid = null;

    // This FM-SP's entity ID
    String realm = null;
    String spEntityId = null;


    Object token = null;
    SessionProvider provider = null;

    if (spMetaAlias == null) {
        String errStr = errorUrl+"?errorcode=14&errorstring=SP_NullMetaAlias";
	SAML2Utils.debug.error(errStr);
        String data[] = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    // Check if a user is already authenticated
    boolean loggedIn = false;
    String loggedinPrincipal = null;
    try {
        provider = SessionManager.getProvider();
        token = provider.getSession(request);
        if((token != null) && (provider.isValid(token))) {
            loggedIn = true;
            loggedinPrincipal = provider.getPrincipalName(token);
        } else {
            token  = null; // for logging
        }
    } catch (SessionException e) {
        SAML2Utils.debug.message("SA_SP:sessionvalidation exc:ignored" , e);
        token  = null; // for logging
        // Assumed not logged in
    }

    if (loggedIn == false ) {
        String errStr = errorUrl+"?errorcode=5&errorstring=SP_No_SSOToken";
	SAML2Utils.debug.error(errStr);
        String data[] = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }
    String spApp = (String) request.getParameter(
                                      SecureAttrs.SAE_PARAM_SPAPPURL);
    if (spApp == null ) {
        String errStr = errorUrl+"?errorcode=6&errorstring=SP_NOSPAppURL";
	SAML2Utils.debug.error(errStr);
        String data[] = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }
    HashMap map = new HashMap();
    try {
        realm = SAML2MetaUtils.getRealmByMetaAlias(spMetaAlias);
        SAML2MetaManager mm = SAML2Utils.getSAML2MetaManager();
        spEntityId = mm.getEntityByMetaAlias(spMetaAlias);
        // get attr list from configuration
        SPSSOConfigElement spConfig = mm.getSPSSOConfig(realm, spEntityId);
        if (spConfig != null) {
            Map attrs = SAML2MetaUtils.getAttributes(spConfig);
            if (attrs != null) {
                List value = (List) attrs.get(SAML2Constants.ATTRIBUTE_MAP);
                if (value != null && !value.isEmpty()) {
                    Iterator valueIter = value.iterator();
                    while (valueIter.hasNext()) {
                        String entry = (String) valueIter.next();
                        StringTokenizer st = new StringTokenizer(entry, "=");
                        if (st.countTokens() != 2) {
                            continue;
                        }
                        st.nextToken();
                        String localAttr = st.nextToken();
                        String[] vals = provider.getProperty(token,localAttr);
                        if ((vals != null) && (vals.length > 0)) {
                            map.put(localAttr, vals[0]);
                        }
                    }
                }
            }
        }
    } catch (SAML2MetaException se) {
    }
    
    // Get SAE attributes relating to FM-SP and SPApp
    Map hp = SAML2Utils.getSAEAttrs(
        realm, spEntityId, SAML2Constants.SP_ROLE, spApp);

    if (hp == null) {
        String errStr = errorUrl
                        +"?errorcode=13&errorstring=Error_invalid_SAEAttrs:"
                        +spApp;
	SAML2Utils.debug.error(errStr);
        String data[] = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    String secret = null;
    String cryptoType = (String) hp.get(SecureAttrs.SAE_CRYPTO_TYPE);
    if (SecureAttrs.SAE_CRYPTO_TYPE_SYM.equals(cryptoType)) {
        secret = (String) hp.get(SecureAttrs.SAE_CONFIG_SHARED_SECRET);
    } else if (SecureAttrs.SAE_CRYPTO_TYPE_ASYM.equals(cryptoType)) {
        secret = (String) hp.get(SecureAttrs.SAE_CONFIG_PRIVATE_KEY_ALIAS);
    }
    if (secret == null || secret.length() == 0) {
        String errStr = errorUrl
                        +"?errorcode=13&errorstring=Error_invalid_sharedSecret:"
                        +spApp;
	SAML2Utils.debug.error(errStr);
        String data[] = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }

    // Make sure we are in FM mode.
    SecureAttrs.setServerFlag(true);
    SecureAttrs.init(new Properties());
    
    SecureAttrs sa = SecureAttrs.getInstance(cryptoType);
    String encodedString = sa.getEncodedString(map, secret);
    if (encodedString == null) {
       String errStr = errorUrl
                       +"?errorcode=7&errorstring=Couldnt_secure_attrs:"
                       +map;
	SAML2Utils.debug.error(errStr);
        String data[] = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR, 
                   data, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }
    String ssoUrl = null;
    HashMap sParams = null;
    if (action.equals("GET")) {
        if (spApp.indexOf("?") > 0) {
            ssoUrl = spApp+"&"+SecureAttrs.SAE_PARAM_DATA+"="+encodedString;
        } else {
            ssoUrl = spApp+"?"+SecureAttrs.SAE_PARAM_DATA+"="+encodedString;
        }
    } else {
        ssoUrl = spApp;
        sParams = new HashMap();
        sParams.put(SecureAttrs.SAE_PARAM_DATA, encodedString);
    }

    String data[] = {map.toString()};
    SAML2Utils.logAccess(Level.INFO, LogUtil.SAE_SP_SUCCESS, 
               data, token, ipaddr, userid, realm, "SAE", null);
    // Comment this redirect and uncomment the below href for debugging. 
    // The href at the bottom will take effect
    try {
        Utils.redirect(response, ssoUrl, sParams, action);
    } catch (Exception ex) {
       String errStr = errorUrl
                       +"?errorcode=7&errorstring=Couldnt_redirect:"+ex
                       +" Map="+map;
	SAML2Utils.debug.error(errStr);
        String data1[] = {errStr};
        SAML2Utils.logError(Level.INFO, LogUtil.SAE_SP_ERROR, 
                   data1, token, ipaddr, userid, realm, "SAE", null);
        response.sendRedirect(errStr);
        return;
    }
/*
%>
    <br> DEBUG : We are in SAE handler deployed on FM in IDP role.
    <br> Click <a href=<%=ssoUrl%>>here </a> to continue SSO with SP.
    <br> Edit saml2/jsp/SA_IDP.jsp and uncomment the "send.Redirect(..)" line to execute a redirect automatically so that the user doesnt see this debug page.
<%
*/
%>
