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
 * $Id: AuthClientUtils.java,v 1.9 2008-03-21 06:23:18 manish_rustagi Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.authentication.client;

import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.IOException;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Iterator;
import java.util.ResourceBundle;

import java.net.URL;
import java.net.URLDecoder;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;

import java.security.AccessController;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;

import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.share.SessionEncodeURL;
import com.sun.identity.session.util.SessionUtils;

import com.iplanet.am.util.Debug;
import com.iplanet.am.util.AMClientDetector;
import com.iplanet.am.util.Locale;
import com.iplanet.am.util.SystemProperties;

import com.iplanet.services.cdm.Client;
import com.iplanet.services.cdm.AuthClient;
import com.iplanet.services.cdm.ClientsManager;
import com.iplanet.services.util.Crypt;
import com.iplanet.services.util.CookieUtils;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.services.util.Base64;

import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.security.EncodeAction;

import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AMAuthErrorCode;

import com.sun.identity.common.ResourceLookup;
import com.sun.identity.common.Constants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.FQDNUtils;
import com.sun.identity.common.HttpURLConnectionManager;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.common.ISLocaleContext;

import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSEntry;

import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.policy.plugins.AuthSchemeCondition;

public class AuthClientUtils {

    public static final String  DEFAULT_CLIENT_TYPE ="genericHTML";
    private static final String  DEFAULT_CONTENT_TYPE="text/html";
    private static final String  DEFAULT_FILE_PATH="html";
    private static final String  DSAME_VERSION="7.0";
    public static final String ERROR_MESSAGE = "Error_Message";
    public static final String ERROR_TEMPLATE = "Error_Template";
    public static final String MSG_DELIMITER= "|";
    public static final String BUNDLE_NAME="amAuth";   

    private static boolean setRequestEncoding = false;

    private static AMClientDetector clientDetector;
    private static Client defaultClient;
    private static FQDNUtils fqdnUtils;
    private static ResourceBundle bundle;
    private static final boolean urlRewriteInPath =
        Boolean.valueOf(SystemProperties.get(
        Constants.REWRITE_AS_PATH,"")).booleanValue();
    public static final String templatePath =
        new StringBuffer().append(Constants.FILE_SEPARATOR)
    .append(ISAuthConstants.CONFIG_DIR)
    .append(Constants.FILE_SEPARATOR)
    .append(ISAuthConstants.AUTH_DIR).toString();
    private static final String rootSuffix = SMSEntry.getRootSuffix();

    // dsame version
    private static String dsameVersion =
        SystemProperties.get(Constants.AM_VERSION,DSAME_VERSION);

    /* Constants.AM_COOKIE_NAME is the AM Cookie which
     * gets set when the user has authenticated
     */
    private static String cookieName=
        SystemProperties.get(Constants.AM_COOKIE_NAME);

    /* Constants.AM_AUTH_COOKIE_NAME is the Auth Cookie which
     * gets set during the authentication process.
     */
    private static String authCookieName=
        SystemProperties.get(Constants.AM_AUTH_COOKIE_NAME,
        ISAuthConstants.AUTH_COOKIE_NAME);
    private static String loadBalanceCookieName = null;
    private static String persistentCookieName=
        SystemProperties.get(Constants.AM_PCOOKIE_NAME);
    private static String loadBalanceCookieValue=
        SystemProperties.get(Constants.AM_LB_COOKIE_VALUE);
    private static String serviceURI = SystemProperties.get(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) + "/UI/Login";

    // Name of the webcontainer
    private static String webContainer =
        SystemProperties.get(Constants.IDENTITY_WEB_CONTAINER);
    private static String serverURL = null;
    static Debug utilDebug = Debug.getInstance("amAuthClientUtils");
    private static boolean useCache = Boolean.getBoolean(SystemProperties.get(
        com.sun.identity.shared.Constants.URL_CONNECTION_USE_CACHE, "false"));

    static {
        // Initialzing variables
        String installTime =
            SystemProperties.get(AdminTokenAction.AMADMIN_MODE, "false");
        if (installTime.equalsIgnoreCase("false")) {
            clientDetector = new AMClientDetector();
            if (isClientDetectionEnabled()) {
                defaultClient = ClientsManager.getDefaultInstance();
            }
        }
        fqdnUtils = new FQDNUtils();
        bundle = Locale.getInstallResourceBundle(BUNDLE_NAME);
        if (webContainer != null && webContainer.length() > 0) {
            if (webContainer.indexOf("BEA") >= 0 ||
                webContainer.indexOf("IBM5.1") >= 0 ) {
                setRequestEncoding = true;
            }
        }
        if (SystemProperties.isServerMode()) {
            loadBalanceCookieName =
                SystemProperties.get(Constants.AM_LB_COOKIE_NAME,"amlbcookie");
        } else {
            loadBalanceCookieName =
                SystemProperties.get(Constants.AM_LB_COOKIE_NAME);
        }
        String proto = SystemProperties.get(Constants.DISTAUTH_SERVER_PROTOCOL);
        String host = null;
        String port = null;
        if (proto != null && proto.length() != 0 ) {
            host = SystemProperties.get(Constants.DISTAUTH_SERVER_HOST);
            port = SystemProperties.get(Constants.DISTAUTH_SERVER_PORT);
        } else {
            proto = SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
            host = SystemProperties.get(Constants.AM_SERVER_HOST);
            port = SystemProperties.get(Constants.AM_SERVER_PORT);
        }
        serverURL = proto + "://" + host + ":" + port;
    }

    /*
     * Protected constructor to prevent any instances being created
     * Needs to be protected to allow subclass AuthUtils
     */
    protected AuthClientUtils() {
    }        

    public static Hashtable parseRequestParameters(
        HttpServletRequest request) {

        Enumeration requestEnum = request.getParameterNames();

        return (decodeHash(request,requestEnum));

    }

    private static Hashtable decodeHash(
        HttpServletRequest request, Enumeration names) {

        Hashtable data = new Hashtable();
        String enc = request.getParameter("gx_charset");
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils::decodeHash:enc = "+enc);
        }
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String value = request.getParameter(name);
            if (setRequestEncoding) {
                data.put(name, Locale.URLDecodeField(value, enc, utilDebug));
            } else {
                data.put(name, value);
            }
        }// while
        return (data);
    }           

    /**
     * Returns the Logout cookie.
     *
     * @param sid Session ID.
     * @param cookieDomain Cookie domain.
     * @return logout cookie string.
     */
    public static Cookie getLogoutCookie(SessionID sid, String cookieDomain) {
        String logoutCookieString = getLogoutCookieString(sid);
        Cookie logoutCookie = createCookie(logoutCookieString,cookieDomain);
        logoutCookie.setMaxAge(0);
        return (logoutCookie);
    }


    /**
     * Returns the encrpted Logout cookie string .
     * The format of this cookie is:
     * <code>LOGOUT@protocol@servername@serverport@sessiondomain</code>.
     *
     * @param sid the SessionID
     * @return encrypted logout cookie string.
     */
    public static String getLogoutCookieString(SessionID sid) {
        String logout_cookie = null;
        try {
            logout_cookie = (String) AccessController.doPrivileged(
                new EncodeAction(
                "LOGOUT" + "@" +
                sid.getSessionServerProtocol() + "@" +
                sid.getSessionServer() + "@" +
                sid.getSessionServerPort() + "@" +
                sid.getSessionDomain(), Crypt.getHardcodedKeyEncryptor()));
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Logout cookie : " + logout_cookie);
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error creating cookie : " + e.getMessage());
            }
        }
        return (logout_cookie );
    }

    /**
     * Returns Cookie to be set in the response.
     *
     * @param cookieValue value of cookie
     * @param cookieDomain domain for which cookie will be set.
     * @return Cookie object.
     */
    public static Cookie createCookie(String cookieValue, String cookieDomain) {
        String cookieName = getCookieName();
        if (utilDebug.messageEnabled()) {
            utilDebug.message("cookieName : " + cookieName);
            utilDebug.message("cookieValue : " + cookieValue);
            utilDebug.message("cookieDomain : " + cookieDomain);
        }
        return (createCookie(cookieName,cookieValue,cookieDomain));
    }    

    public static String getQueryOrgName(HttpServletRequest request,
        String org) {
        String queryOrg = null;
        if ((org != null) && (org.length() != 0)) {
            queryOrg = org;
        } else {
            if (request != null) {
                queryOrg = request.getServerName();
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("queryOrg is :" + queryOrg);
        }
        return (queryOrg);
    }   


    // print cookies in the request
    // use for debugging purposes

    public static void printCookies(HttpServletRequest req) {
        Cookie ck[] = req.getCookies();
        if (ck == null) {
            utilDebug.message("No Cookie in header");
            return;
        }
        for (int i = 0; i < ck.length; ++i) {
            if ( utilDebug.messageEnabled()) {
                utilDebug.message("Received Cookie:" + ck[i].getName() + 
                                  " = " + ck[i].getValue());
            }
        }
    }    

    public static void printHash(Hashtable reqParameters) {
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthRequest: In printHash" + reqParameters);
            }
            if (reqParameters == null) {
                return;
            }
            Enumeration Edata = reqParameters.keys();
            while (Edata.hasMoreElements()) {
                Object key =  Edata.nextElement();
                Object value = reqParameters.get(key);
                utilDebug.message("printHash Key is : " + key);
                if (value instanceof String[]) {
                    String tmp[] = (String[])value;
                    for (int ii=0; ii < tmp.length; ii++) {
                        if (utilDebug.messageEnabled()) {
                            utilDebug.message("printHash : String[] keyname ("+ 
                                              key + ") = " + tmp[ii]);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.warning("Exception: printHash :" , e);
            }
        }
    }

    public static void setlbCookie(
        HttpServletResponse response) throws AuthException {
        String cookieName = getlbCookieName();
        if (cookieName != null && cookieName.length() != 0) {
            Set domains = getCookieDomains();
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createlbCookie(domain);
                    response.addCookie(cookie);
                }
            } else {
                response.addCookie(createlbCookie(null));
            }
        }
    }

    /**
     * Creates a Cookie with the <code>cookieName</code>,
     * <code>cookieValue</code> for the cookie domains specified.
     *
     * @param cookieName is the name of the cookie
     * @param cookieValue is the value fo the cookie
     * @param cookieDomain Domain for which the cookie is to be set.
     * @return the cookie object.
     */
    public static Cookie createCookie(
        String cookieName,
        String cookieValue,
        String cookieDomain
    ) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("cookieName   : " + cookieName);
            utilDebug.message("cookieValue  : " + cookieValue);
            utilDebug.message("cookieDomain : " + cookieDomain);
        }

        Cookie cookie = null;
        try {
            // hardcoded need to read from attribute and set cookie
            // for all domains
            cookie = CookieUtils.newCookie(cookieName, cookieValue,
                "/", cookieDomain);
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error creating cookie. : " 
                                  + e.getMessage());
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("createCookie Cookie is set : " + cookie);
        }
        return (cookie);
    }    

    public static void clearlbCookie(HttpServletResponse response){
        String cookieName = getlbCookieName();
        if (cookieName != null && cookieName.length() != 0) {
            Set domains = getCookieDomains();
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie =
                        createPersistentCookie(
                            cookieName, "LOGOUT", 0, domain);
                    response.addCookie(cookie);
                }
            } else {
                response.addCookie(
                    createPersistentCookie(cookieName, "LOGOUT", 0, null));
            }
        }
    }          

    /* return the the error message for the error code */
    public static String getErrorMessage(String errorCode) {
        String errorMessage = getErrorVal(errorCode,ERROR_MESSAGE);
        return (errorMessage);
    }

    /* return the the error template for the error code */
    public static String getErrorTemplate(String errorCode) {
        String errorTemplate = getErrorVal(errorCode,ERROR_TEMPLATE);
        return (errorTemplate);
    } 

    public static boolean checkForCookies(HttpServletRequest req) {

        // came here if cookie not found , return false
        return(
            (CookieUtils.getCookieValueFromReq(req,getAuthCookieName()) != null)
            ||
            (CookieUtils.getCookieValueFromReq(req,getCookieName()) !=null));
    }       

    // Get Original Redirect URL for Auth to redirect the Login request
    public static String getOrigRedirectURL(HttpServletRequest request,
        SessionID sessID) {
        try {
            String sidString = null;
            if (sessID != null) {
                sidString = sessID.toString();
            }
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(sidString);
            if (manager.isValidToken(ssoToken)) {
                utilDebug.message("Valid SSOToken");
                String origRedirectURL = ssoToken.getProperty("successURL");
                String gotoURL = request.getParameter("goto");
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Original successURL : " 
                                      + origRedirectURL);
                    utilDebug.message("Request gotoURL : " + gotoURL);
                }
                if ((gotoURL != null) && (gotoURL.length() != 0) && 
                    (!gotoURL.equalsIgnoreCase("null"))) {
                    String encoded = request.getParameter("encoded");
                    if (encoded != null && encoded.equals("true")) {
                        origRedirectURL = getBase64DecodedValue(gotoURL);
                    } else {
                        origRedirectURL = gotoURL;
                    }
                }
                return (origRedirectURL);
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getOrigRedirectURL:"
                                  + e.toString());
            }
            return (null);
        }
        return (null);
    }     

    /**
     * Adds Logout cookie to URL.
     *
     * @param url is the url to be rewritten with the logout cookie
     * @param logoutCookie is the logoutCookie String
     * @param isCookieSupported is a boolean which indicates whether
     *        cookie support is true or false
     * @return URL with the logout cookie appended to it.
     */
    public static String addLogoutCookieToURL(
        String url,
        String logoutCookie,
        boolean isCookieSupported) {
        String logoutURL = null;

        if ((logoutCookie == null) || (isCookieSupported)) {
            logoutURL = url;
        } else {
            StringBuffer cookieString = new StringBuffer();
            cookieString.append(URLEncDec.encode(getCookieName()))
            .append("=").append(URLEncDec.encode(logoutCookie));

            StringBuffer encodedURL = new StringBuffer();
            if (url.indexOf("?") != -1) {
                cookieString.insert(0,"&amp;");
            } else {
                cookieString.insert(0,"?");
            }

            cookieString.insert(0,url);
            logoutURL = cookieString.toString();

            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieString is : "+ cookieString);
            }
        }

        /*if (utilDebug.messageEnabled()) {
         *  utilDebug.message("logoutURL is : "+ logoutURL);
         *}
         */

        return (logoutURL);
    }                 

    /**
     * Returns the Session ID for the request.
     * The cookie in the request for invalid sessions
     * is in authentication cookie, <code>com.iplanet.am.auth.cookie</code>,
     * and for active/inactive sessions in <code>com.iplanet.am.cookie</code>.
     *
     *  @param request HttpServletRequest object.
     *  @return session id for this request.
     */
    private static SessionID getSidFromCookie(HttpServletRequest request) {
        SessionID sessionID = null;
        String authCookieName = getAuthCookieName();
        String sidValue =
            CookieUtils.getCookieValueFromReq(request,authCookieName);
        if (sidValue == null) {
            sidValue =
                SessionEncodeURL.getSidFromURL(request,authCookieName);
        }
        if (sidValue != null) {
            sessionID = new SessionID(sidValue);
            utilDebug.message("sidValue from Auth Cookie");
        }
        return (sessionID);
    }

    /**
     * Returns the Session ID for this request.  If Authetnication Cookie and
     * Valid AM Cookie are there and request method is GET then use Valid
     * AM Cookie else use Auth Cookie. The cookie in the request for invalid
     * sessions is in auth cookie, <code>com.iplanet.am.auth.cookie</code>,
     * and for active/inactive sessions in <code>com.iplanet.am.cookie</code>.
     *
     * @param request HTTP Servlet Request.
     * @return Session ID for this request.
     */
    public static SessionID getSessionIDFromRequest(HttpServletRequest request) {
        boolean isGetRequest= (request !=null &&
            request.getMethod().equalsIgnoreCase("GET"));
        SessionID amCookieSid = new SessionID(request);
        SessionID authCookieSid = getSidFromCookie(request);
        SessionID sessionID;
        if (authCookieSid == null) {
            sessionID = amCookieSid;
        } else {
            if (isGetRequest) {
                sessionID = amCookieSid;
            } else {
                sessionID = authCookieSid;
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils:returning sessionID:" + sessionID);
        }
        return (sessionID);
    }    

    /**
     * Returns <code>true</code> if the request has the
     * <code>arg=newsession</code> query parameter.
     *
     * @param reqDataHash Request Data Hashtable.
     * returns <code>true</code> if this parameter is present.
     */
    public static boolean newSessionArgExists(Hashtable reqDataHash) {
        String arg = (String) reqDataHash.get("arg");
        boolean newSessionArgExists =
            (arg != null) && arg.equals("newsession");
        if (utilDebug.messageEnabled()) {
            utilDebug.message("newSessionArgExists : " + newSessionArgExists);
        }
        return (newSessionArgExists);
    }

    // Get the AuthContext.IndexType given string index type value
    public static AuthContext.IndexType getIndexType(String strIndexType) {
        AuthContext.IndexType indexType = null;
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getIndexType : strIndexType = " + strIndexType);
        }
        if (strIndexType != null) {
            if (strIndexType.equalsIgnoreCase("user")) {
                indexType = AuthContext.IndexType.USER;
            } else if (strIndexType.equalsIgnoreCase("role")) {
                indexType = AuthContext.IndexType.ROLE;
            } else if (strIndexType.equalsIgnoreCase("service")) {
                indexType = AuthContext.IndexType.SERVICE;
            } else if (strIndexType.equalsIgnoreCase("module_instance")) {
                indexType = AuthContext.IndexType.MODULE_INSTANCE;
            } else if (strIndexType.equalsIgnoreCase("level")) {
                indexType = AuthContext.IndexType.LEVEL;
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getIndexType : IndexType = " + indexType);
        }
        return (indexType);
    }

    // Get the index name given index type from the existing valid session
    public static String getIndexName(SSOToken ssoToken,
        AuthContext.IndexType indexType) {
        String indexName = "";
        try {
            if (indexType == AuthContext.IndexType.USER) {
                indexName = ssoToken.getProperty("UserToken");
            } else if (indexType == AuthContext.IndexType.ROLE) {
                indexName = ssoToken.getProperty("Role");
            } else if (indexType == AuthContext.IndexType.SERVICE) {
                indexName = ssoToken.getProperty("Service");
            } else if (indexType == AuthContext.IndexType.MODULE_INSTANCE) {
                indexName = 
                    getLatestIndexName(ssoToken.getProperty("AuthType"));
            } else if (indexType == AuthContext.IndexType.LEVEL) {
                indexName = ssoToken.getProperty("AuthLevel");
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getIndexName :"+ e.toString());
            }
            return (indexName);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getIndexName : IndexType = " + indexType);
            utilDebug.message("getIndexName : IndexName = " + indexName);
        }
        return (indexName);
    }

    // Get the first or latest index name from the string of index names
    // separated by "|".
    private static String getLatestIndexName(String indexName) {
        String firstIndexName = indexName;
        if (indexName != null) {
            StringTokenizer st = new StringTokenizer(indexName,"|");
            if (st.hasMoreTokens()) {
                firstIndexName = (String)st.nextToken();
            }
        }
        return (firstIndexName);
    }

    // search valve in the String
    private static boolean isContain(String value, String key) {
        if (value == null) {
            return (false);
        }
        try {
            if (value.indexOf("|") != -1) {
                StringTokenizer st = new StringTokenizer(value, "|");
                while (st.hasMoreTokens()) {
                    if ((st.nextToken()).equals(key)) {
                        return (true);
                    }
                }
            } else {
                if (value.trim().equals(key.trim())) {
                    return (true);
                }
            }
        } catch (Exception e) {
            utilDebug.error("error : " + e.toString());
        }
        return (false);
    }

    // Method to check if this is Session Upgrade
    public static boolean checkSessionUpgrade(
        SSOToken ssoToken,Hashtable reqDataHash) {
        utilDebug.message("Check Session upgrade!");
        String tmp = null;
        String value = null;
        boolean upgrade = false;
        try {
            if (reqDataHash.get("user")!=null) {
                tmp = (String) reqDataHash.get("user");
                value = ssoToken.getProperty("UserToken");
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("user : " + tmp);
                    utilDebug.message("userToken : " + value);
                }
                if (!tmp.equals(value)) {
                    upgrade = true;
                }
            } else if (reqDataHash.get("role")!=null) {
                tmp = (String) reqDataHash.get("role");
                value = ssoToken.getProperty("Role");
                if (!isContain(value, tmp)) {
                    upgrade = true;
                }
            } else if (reqDataHash.get("service")!=null) {
                tmp = (String) reqDataHash.get("service");
                value = ssoToken.getProperty("Service");
                if (!isContain(value, tmp)) {
                    upgrade = true;
                }
            } else if (reqDataHash.get("module")!=null) {
                tmp = (String) reqDataHash.get("module");
                value = ssoToken.getProperty("AuthType");
                if (!isContain(value, tmp)) {
                    upgrade = true;
                }
            } else if (reqDataHash.get("authlevel")!=null) {
                int i = Integer.parseInt((String)reqDataHash.get("authlevel"));
                if (i>Integer.parseInt(ssoToken.getProperty("AuthLevel"))) {
                    upgrade = true;
                }
            } else if ( reqDataHash.get(Constants.COMPOSITE_ADVICE) != null ) {
                upgrade = true;
            }
        } catch (Exception e) {
            utilDebug.message("Exception in checkSessionUpgrade : " , e);
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("Check session upgrade : " + upgrade);
        }
        return (upgrade);
    }   

    public static boolean isClientDetectionEnabled() {
        boolean clientDetectionEnabled = false;

        if (clientDetector != null) {
            String detectionEnabled = clientDetector.detectionEnabled();
            clientDetectionEnabled = detectionEnabled.equalsIgnoreCase("true");
        } else {
            utilDebug.message("getClientDetector,Service does not exist");
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("clientDetectionEnabled = " 
                              + clientDetectionEnabled);
        }
        return (clientDetectionEnabled);
    }

    /**
     * Returns the client type. If client detection is enabled then
     * client type is determined by the <code>ClientDetector</code> class otherwise
     * <code>defaultClientType</code> set in
     * <code>iplanet-am-client-detection-default-client-type</code>
     * is assumed to be the client type.
     *
     * @param req HTTP Servlet Request.
     * @return client type.
     */
    public static String getClientType(HttpServletRequest req) {
        if (isClientDetectionEnabled() && (clientDetector != null)) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("clienttype = "
                    +clientDetector.getClientType(req));
            }
            return (clientDetector.getClientType(req));
        }
        return (getDefaultClientType());
    }

    /**
     * Get default client
     */
    public static String getDefaultClientType() {
        String defaultClientType = DEFAULT_CLIENT_TYPE;
        if (defaultClient != null) {
            try {
                defaultClientType = defaultClient.getClientType();
                // add observer, so auth will be notified if the client changed
                // defClient.addObserver(this);
            } catch (Exception e) {
                utilDebug.error("getDefaultClientType Error : " 
                                + e.toString());
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getDefaultClientType, ClientType = " +
                defaultClientType);
        }
        return (defaultClientType);
    }

    /**
     * return the client Object associated with a clientType
     * default instance is returned if the instance could not be found
     */
    private static Client getClientInstance(String clientType) {
        if (!clientType.equals(getDefaultClientType())) {
            try {
                return (AuthClient.getInstance(clientType,null));
            } catch (Exception ce) {
                utilDebug.warning("getClientInstance()" , ce);
            }
        }
        return (defaultClient);
    }

    /**
     * Returns the requested property from clientData (example fileIdentifer).
     *
     * @param clientType
     * @param property
     * @return the requested property from clientData.
     */
    private static String getProperty(String clientType, String property) {

        try {
            return (getClientInstance(clientType).getProperty(property));
        } catch (Exception ce) {
            // which means we did not get the client Property
            utilDebug.warning("Error retrieving Client Data : " + property + 
                ce.toString());
            // if this was not the default client type then lets
            // try to get the default client Property
            return (getDefaultProperty(property));
        }
    }

    /**
     * return the requested property for default client
     */
    public static String getDefaultProperty(String property) {        
        try {
            return (defaultClient.getProperty(property));
        } catch (Exception ce) {
            utilDebug.warning("Could not get " + property + ce.toString());
        }
        return (null);
    }

    /**
     * return the charset associated with the clientType
     */
    public static String getCharSet(String clientType,java.util.Locale locale) {
        String charset = Client.CDM_DEFAULT_CHARSET;
        try {
            charset = getClientInstance(clientType).getCharset(locale);
        } catch (Exception ce) {
            utilDebug.warning("AuthClientUtils.getCharSet:Client data was "+
                "not found, setting charset to UTF-8.");
            charset = Constants.CONSOLE_UI_DEFAULT_CHARSET;
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthClientUtils.getCharSet: Charset from"+
                " Client is : " + charset);
        }
        return (charset);
    }

    /**
     * return the filePath associated with a clientType
     */
    public static String getFilePath(String clientType) {
        String filePath = getProperty(clientType,"filePath");
        if (filePath == null) {
            return (DEFAULT_FILE_PATH);
        }
        return (filePath);
    }

    /**
     * return the contentType associated with a clientType
     * if no contentType found then return the default
     */
    public static String getContentType(String clientType) {

        String contentType = getProperty(clientType,"contentType");
        if (contentType == null) {
            return (DEFAULT_CONTENT_TYPE);
        }
        return (contentType);
    }

    /**
     * for url rewriting with session id we need to know whether
     * cookies are supported
     * RFE 4412286
     */
    public static String getCookieSupport(String clientType) {
        String cookieSup = getProperty(clientType,"cookieSupport");
        return (cookieSup);
    }

    /**
     * determine if this client is an html client
     */
    public static boolean isGenericHTMLClient(String clientType) {
        String type = getProperty(clientType,"genericHTML");
        return(type == null) || type.equals("true");
    }    

    /* return true if cookiSupport is true or cookieDetection
     * mode has been detected .This is used to determine
     * whether cookie should be set in response or not.
     */
    public static boolean isSetCookie(String clientType) {
        boolean setCookie =  setCookieVal(clientType,"true");

        if (utilDebug.messageEnabled()) {
            utilDebug.message("setCookie : " + setCookie);
        }

        return (setCookie);
    }

    /* checks the cookieDetect , cookieSupport values to
     * determine if cookie should be rewritten or set.
     */
    public static boolean setCookieVal(String clientType,String value) {

        String cookieSupport = getCookieSupport(clientType);
        boolean cookieDetect = getCookieDetect(cookieSupport);

        boolean cookieSup =  ((cookieSupport !=null) &&
            (cookieSupport.equalsIgnoreCase(value) ||
            cookieSupport.equalsIgnoreCase(
            ISAuthConstants.COOKIE_DETECT_PROPERTY)));
        boolean setCookie = (cookieSup || cookieDetect) ;

        if (utilDebug.messageEnabled()) {
            utilDebug.message("cookieSupport : " + cookieSupport);
            utilDebug.message("cookieDetect : " + cookieDetect);
            utilDebug.message(" setCookie is : " +  setCookie);
        }

        return (setCookie);
    }

    /** Returns true if cookieDetect mode else false.
     *  @param cookieSupport , whether cookie is supported or not.
     *  @return true if cookieDetect mode else false
     */
    public static boolean getCookieDetect(String cookieSupport) {
        boolean cookieDetect
            = ((cookieSupport == null) ||
            (cookieSupport.equalsIgnoreCase(
            ISAuthConstants.COOKIE_DETECT_PROPERTY)));
        if (utilDebug.messageEnabled()) {
            utilDebug.message("CookieDetect : " + cookieDetect);
        }
        return (cookieDetect);
    }

    /**
     * Extracts the client URL from the String passed
     * URL passed is in the format clientType | URL
     * @param urlString is a String , a URL
     * @param index is the position of delimiter "|"
     * @return Returns the client URL.
     */
    public static String getClientURLFromString(String urlString,int index,
        HttpServletRequest request) {
        String clientURL = null;
        if (urlString != null) {
            String clientTypeInUrl = urlString.substring(0,index);
            if ((clientTypeInUrl != null) &&
                (clientTypeInUrl.equals(getClientType(request)))) {
                if (urlString.length() > index) {
                    clientURL = urlString.substring(index+1);
                }
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Client URL is :" + clientURL);
        }
        return (clientURL);
    }

    /* return true if cookieSupport is false and cookie Detect
     * mode (which is rewrite as well as set cookie the first
     * time). This determines whether url should be rewritten
     * or not.
     */
    public static boolean isUrlRewrite(String clientType) {

        boolean rewriteURL = setCookieVal(clientType,"false");
        if (utilDebug.messageEnabled()) {
            utilDebug.message("rewriteURL : " + rewriteURL);
        }

        return (rewriteURL);
    }

    public static String getDSAMEVersion() {
        return (dsameVersion);
    }

    /**Returns the Auth Cookie Name.
     *
     * @return authCookieName, a String,the auth cookie name.
     */
    public static String getAuthCookieName() {
        return (authCookieName);
    }

    public static String getCookieName() {
        return (cookieName);
    }

    public static String getPersistentCookieName() {
        return (persistentCookieName);
    }

    public static String getlbCookieName() {
        return (loadBalanceCookieName);
    }

    public static String getlbCookieValue() {
        if (SystemProperties.isServerMode()) {
            try {
                return (WebtopNaming.getAMServerID());
            } catch (Exception e) {
                return (null);
            }
        }
        return (loadBalanceCookieValue);
    }

    public static Set getCookieDomains() {
        Set cookieDomains = Collections.EMPTY_SET;
        try {
            SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                ServiceSchemaManager scm  = new ServiceSchemaManager(
                    "iPlanetAMPlatformService",token);
                ServiceSchema psc = scm.getGlobalSchema();
                Map attrs = psc.getAttributeDefaults();
                cookieDomains =
                    (Set)attrs.get(ISAuthConstants.PLATFORM_COOKIE_DOMAIN_ATTR);
            } catch (SMSException ex) {
                // Ignore the exception and leave cookieDomains empty;
                utilDebug.message("getCookieDomains - SMSException ");
            }
            if (cookieDomains == null) {
                cookieDomains = Collections.EMPTY_SET;
            }
        } catch (SSOException ex) {
            // unable to get SSOToken
            utilDebug.message("getCookieDomains - SSOException ");
        }
        if (utilDebug.messageEnabled() && (!cookieDomains.isEmpty())) {
            utilDebug.message("CookieDomains : ");
            Iterator iter = cookieDomains.iterator();
            while (iter.hasNext()) {
                utilDebug.message("  " + (String)iter.next());
            }
        }
        return (cookieDomains);
    }

    /* This method returns the organization DN.
     * The organization DN is deteremined based on
     * the query parameters "org" OR "domain" OR
     * the server host name. For backward compatibility
     * the orgname will be determined from requestURI
     * in the case where either query params OR server host
     * name are not valid and orgDN cannot be found.
     * The orgDN is determined based on and in order,by the SDK:
     * 1. OrgDN - organization dn.
     * 2. Domain - check if org is a domain by trying to get
     *    domain component
     * 3  Org path- check if the orgName passed is a path (eg."/suborg1")
     * 4. URL - check if the orgName passed is a DNS alias (URL).
     * 5. If no orgDN is found null is returned.
     * @param orgParam is the org or domain query param ,
     *        or the server host name
     * @param noQueryParam is a boolean indicating that the
     *        the request did not have query.
     * @param request is the HttpServletRequest object
     * @return A String which is the organization DN
     */
    public static String getOrganizationDN(String orgParam,boolean noQueryParam,
        HttpServletRequest request) {
        String orgName = null;
        SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        // try to get the host name if org or domain Param is null
        try {
            orgName = IdUtils.getOrganization(token,orgParam);
            if ((orgName != null) && (orgName.length() != 0)) {
                orgName = orgName.toLowerCase();
            }
        } catch (Exception oe) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Could not get orgName",oe);
            }
        }

        // if orgName is null then match the DNS Alias Name
        // to the full url ie. proto:/server/amserver/UI/Login
        // This is for backward compatibility

        if (((orgName == null) || orgName.length() == 0) && (noQueryParam)) {
            if (request != null) {
                String url = request.getRequestURL().toString();
                int index  = url.indexOf(";");
                if (index != -1) {
                    orgParam = stripPort(url.substring(0,index));
                } else {
                    orgParam = stripPort(url);
                }

                try {
                    orgName = IdUtils.getOrganization(token,orgParam);
                } catch (Exception e) {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("Could not get orgName"+orgParam,e);
                    }
                }
            }
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("getOrganizationDN : orgParam... :" + orgParam);
            utilDebug.message("getOrganizationDN : orgDN ... :" + orgName);
        }
        return (orgName);
    }

    /** This method determines the org parameter
     * and determines the organization DN based on
     * query parameters.
     * The organization DN is determined based on
     * the query parameters "org" OR "domain" OR
     * the server host name. For backward compatibility
     * the orgname will be determined from requestURI
     * in the case where either query params OR server host
     * name are not valid and orgDN cannot be found.
     * The orgDN is determined based on and in order,by the SDK:
     * 1. OrgDN - organization dn.
     * 2. Domain - check if org is a domain by trying to get
     *    domain component
     * 3  Org path- check if the orgName passed is a path (eg."/suborg1")
     * 4. URL - check if the orgName passed is a DNS alias (URL).
     * 5. If no orgDN is found null is returned.
     *
     * @param request HTTP Servlet Request object.
     * @param requestHash Query Hashtable.
     * @return Organization DN.
     */
    public static String getDomainNameByRequest(
        HttpServletRequest request,
        Hashtable requestHash) {
        boolean noQueryParam=false;
        String orgParam = getOrgParam(requestHash);

        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgParam is.. :" + orgParam);
        }

        // try to get the host name if org or domain Param is null
        if ((orgParam == null) || (orgParam.length() == 0)) {
            noQueryParam= true;
            orgParam = request.getServerName();
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Hostname : " + orgParam);
            }
        }
        String orgDN = getOrganizationDN(orgParam,noQueryParam,request);

        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgDN is " + orgDN);
        }

        return (orgDN);
    }

    /**
     * Returns the org or domain parameter passed as a query in the request.
     *
     * @param requestHash Hashtable containing the query parameters
     * @return organization name.
     */
    public static String getOrgParam(Hashtable requestHash) {
        String orgParam = null;
        if ((requestHash != null) && !requestHash.isEmpty()) {
            orgParam = (String) requestHash.get(ISAuthConstants.DOMAIN_PARAM);
            if ((orgParam == null) || (orgParam.length() == 0)) {
                orgParam = (String)requestHash.get(ISAuthConstants.ORG_PARAM);
            }
            if ((orgParam == null) || (orgParam.length() == 0)) {
                orgParam = 
                    (String)requestHash.get(ISAuthConstants.REALM_PARAM);
            }
            if ((orgParam != null) && (orgParam.length() != 0)) {
                String encoded = (String) requestHash.get("encoded");
                String new_org = (String) requestHash.get("new_org");
                if ((new_org != null && new_org.equals("true")) &&
                    (encoded != null && encoded.equals("true"))) {
                    orgParam = getBase64DecodedValue(orgParam);
                }
            }
        }
        return (orgParam);
    }

    static String stripPort(String in) {
        try {
            URL url = new URL(in);
            return(url.getProtocol() + "://" + url.getHost()+ url.getFile());
        } catch (MalformedURLException ex) {
            return (in);
        }
    }

    /**
     * Returns <code>true</code> if the host name in the URL is valid.
     *
     * @param hostName Host name.
     * @return <code>true</code> if the host name in the URL is valid.
     */
    public static boolean isValidFQDNRequest(String hostName) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("hostName is : " + hostName);
        }

        boolean retVal = fqdnUtils.isHostnameValid(hostName);

        if (retVal) {
            utilDebug.message("hostname  and fqdnDefault match returning true");
        } else {
            utilDebug.message("hostname and fqdnDefault don't match");
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("retVal is : " + retVal);
        }
        return (retVal);
    }

    /**
     * Returns the valid hostname from the fqdn map and constructs the correct
     * URL. The request will be forwarded to the new URL.
     *
     * @param partialHostName Partial host name.
     * @param servletRequest HTTP Servlet Request.
     */
    public static String getValidFQDNResource(
        String partialHostName,
        HttpServletRequest servletRequest
    ) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Get mapping for " + partialHostName);
        }

        // get mapping from table
        String validHostName =
            fqdnUtils.getFullyQualifiedHostName(partialHostName);

        if (validHostName == null) {
            validHostName = partialHostName;
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("fully qualified hostname :"+ validHostName);
        }

        String requestURL = constructURL(validHostName,servletRequest);

        if (utilDebug.messageEnabled()) {
            utilDebug.message("Request URL :"+ requestURL);
        }
        return (requestURL);
    }

    /* get the host name from the servlet request's host header or
     * get it using servletRequest:getServerName() in the case
     * where host header is not found
     */
    public static String getHostName(HttpServletRequest servletRequest) {
        // get the host header
        String hostname = servletRequest.getHeader("host");
        if (hostname != null) {
            int i = hostname.indexOf(":");
            if (i != -1) {
                hostname = hostname.substring(0,i);
            }
        } else {
            hostname = servletRequest.getServerName();
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("Returning host name : " + hostname);
        }
        return (hostname);

    }

    /* construct the url */
    static String constructURL(String validHostName,
        HttpServletRequest servletRequest) {
        String scheme =
            RequestUtils.getRedirectProtocol(
                servletRequest.getScheme(),validHostName);
        int port = servletRequest.getServerPort();
        String requestURI = servletRequest.getRequestURI();
        String queryString = servletRequest.getQueryString();

        StringBuffer urlBuffer = new StringBuffer();
        urlBuffer.append(scheme).append("://")
        .append(validHostName).append(":")
        .append(port).append(requestURI);

        if (queryString != null) {
            urlBuffer.append("?")
            .append(queryString);
        }

        String urlString = urlBuffer.toString();

        if (utilDebug.messageEnabled()) {
            utilDebug.message("returning new url : " + urlString);
        }

        return (urlString);
    }

    public static String constructLoginURL(HttpServletRequest request) {
        StringBuffer loginURL = new StringBuffer(serviceURI);
        String qString = request.getQueryString();
        if ((qString != null) && (qString.length() != 0)) {
            loginURL.append("?");
            loginURL.append(qString);
        }
        return(loginURL.toString());
    }

    // Get Original Redirect URL for Auth to redirect the Login request
    public static SSOToken getExistingValidSSOToken(SessionID sessID) {
        SSOToken ssoToken = null;
        try {
            if (sessID != null) {
                String sidString = sessID.toString();
                SSOTokenManager manager = SSOTokenManager.getInstance();
                SSOToken currentToken = manager.createSSOToken(sidString);
                if (manager.isValidToken(currentToken)) {
                    ssoToken = currentToken;
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error in getExistingValidSSOToken :"
                                  + e.toString());
            }
            return (ssoToken);
        }
        return (ssoToken);
    }

    // Check for Session Timed Out
    // If Session is Timed Out Exception is thrown
    public static boolean isTimedOut(SessionID sessID) {
        boolean isTimedOut = false;
        try {
            if (sessID != null) {
                String sidString = sessID.toString();
                SSOTokenManager manager = SSOTokenManager.getInstance();
                SSOToken currentToken = manager.createSSOToken(sidString);
                if (manager.isValidToken(currentToken)) {
                      isTimedOut = false;
                }
            }
        } catch (Exception e) {
            if (e.getMessage().indexOf("Session timed out") != -1) {
                isTimedOut = true;
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Session Timed Out :"+ isTimedOut);
        }
        return isTimedOut;
    }

    public static String getErrorVal(String errorCode,String type) {
        String errorMsg=null;
        String templateName=null;
        String resProperty = bundle.getString(errorCode);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("errorCod is.. : " + errorCode);
            utilDebug.message("resProperty is.. : " + resProperty);
        }
        if ((resProperty != null) && (resProperty.length() != 0)) {
            int commaIndex = resProperty.indexOf(MSG_DELIMITER);
            if (commaIndex != -1) {
                templateName = 
                    resProperty.substring(commaIndex+1,resProperty.length());
                errorMsg = resProperty.substring(0,commaIndex);
            } else {
                errorMsg = resProperty;
            }
        }

        if (type.equals(ERROR_MESSAGE)) {
            return (errorMsg);
        } else if (type.equals(ERROR_TEMPLATE)) {
            return (templateName);
        } else {
            return (null);
        }
    }

    public static boolean isCookieSupported(HttpServletRequest req) {
        boolean cookieSupported = true;
        String cookieSupport = getCookieSupport(getClientType(req));
        if ((cookieSupport != null) && cookieSupport.equals("false")) {
            cookieSupported = false;
        }
        return (cookieSupported);
    }

    public static boolean isCookieSet(HttpServletRequest req) {
        boolean cookieSet = false;
        String cookieSupport = getCookieSupport(getClientType(req));
        boolean cookieDetect = getCookieDetect(cookieSupport);
        if (isClientDetectionEnabled() && cookieDetect) {
            cookieSet = true;
        }
        return (cookieSet);
    }

    /*create Persistent Cookie */
    public static Cookie createPersistentCookie(String name, String value,
        int maxAge, String cookieDomain) {

        Cookie pCookie = CookieUtils.newCookie(name, value, "/", cookieDomain);
        if (maxAge >= 0) {
            pCookie.setMaxAge(maxAge);
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("pCookie is.. :" + pCookie);
        }

        return (pCookie);
    }

    public static Cookie createlbCookie(String cookieDomain) throws AuthException {
        Cookie lbCookie = null;
        try {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("cookieDomain : " + cookieDomain);
            }
            String cookieName = getlbCookieName();
            String cookieValue = getlbCookieValue();
            lbCookie = 
                createPersistentCookie(
                    cookieName, cookieValue, -1, cookieDomain);
            return (lbCookie);
        } catch (Exception e) {
            utilDebug.message("Unable to create Load Balance Cookie");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
    }

    /**
     * Returns the Cookie object created based on the <code>cookieName</code>,
     * Session ID and <code>cookieDomain</code>.
     * If <code>AuthContext,/code> status is not <code>SUCCESS</code> then
     * cookie is created with authentication cookie Name, else AM Cookie Name
     * will be used to create cookie.
     *
     * @param ac the AuthContext object
     * @param cookieDomain the cookie domain for creating cookie.
     * @return Cookie object.
     */
    public static Cookie getCookieString(AuthContext ac, String cookieDomain) {
        Cookie cookie = null;
        String cookieName = getAuthCookieName();
        String cookieValue = serverURL + serviceURI;
        try {
            if (ac.getStatus() == AuthContext.Status.SUCCESS) {
                cookieName = getCookieName();
                cookieValue = ac.getAuthIdentifier();
                utilDebug.message("Create AM cookie");
            }
            cookie = createCookie(cookieName,cookieValue,cookieDomain);
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error getCookieString : " 
                                  + e.getMessage());
            }
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Cookie is : " + cookie);
        }
        return (cookie);
    }

    /**
     ( Returns URL with the cookie value in the URL. The cookie in the
     * re-written URL will have the AM cookie if session is active/inactive
     * and authentication cookie if session is invalid.
     *
     * @param url URL to be encoded.
     * @param request HTTP Servlet Request.
     * @param ac Authentication Context.
     * @return the encoded URL.
     */
    public static String encodeURL(
        String url,
        HttpServletRequest request,
        AuthContext ac) {
        if (isCookieSupported(request)) {
            return (url);
        }

        String cookieName = getAuthCookieName();
        if (ac.getStatus() == AuthContext.Status.SUCCESS) {
            cookieName = getCookieName();
        }

        String encodedURL = url;
        if (urlRewriteInPath) {
            encodedURL =
                encodeURL(url,SessionUtils.SEMICOLON,false,
                          cookieName,ac.getAuthIdentifier());
        } else {
            encodedURL =
                encodeURL(url,SessionUtils.QUERY,true,
                          cookieName,ac.getAuthIdentifier());
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("encodeURL : URL = " + url +
                ", \nRewritten URL = " + encodedURL);
        }
        return(encodedURL);
    }

    private static String encodeURL(String url,short encodingScheme,boolean escape,
        String cookieName, String strSessionID) {
        String encodedURL = url;
        String cookieStr = 
            SessionEncodeURL.createCookieString(cookieName,strSessionID);
        encodedURL = SessionEncodeURL.encodeURL(cookieStr,url,
            encodingScheme,escape);
        return (encodedURL);
    }

    /**
     * Returns the resource based on the default values.
     *
     * @param request HTTP Servlet Request.
     * @param fileName name of the file
     * @param locale Locale used for the search.
     * @param servletContext Servlet Context for server
     * @return Path to the resource.
     */
    public static String getDefaultFileName(
        HttpServletRequest request,
        String fileName,
        java.util.Locale locale, 
        ServletContext servletContext) {

        String strlocale = "";
        if (locale != null) {
            strlocale = locale.toString();
        }
        String filePath = getFilePath(getClientType(request));
        String fileRoot = ISAuthConstants.DEFAULT_DIR;

        String templateFile = null;
        try {
            templateFile = ResourceLookup.getFirstExisting(
                servletContext,
                fileRoot,strlocale,null,filePath,fileName,
                templatePath,true);
        } catch (Exception e) {
            templateFile = new StringBuffer().append(templatePath)
            .append(fileRoot).append(Constants.FILE_SEPARATOR)
            .append(fileName).toString();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getDefaultFileName:templateFile is :" +
                templateFile);
        }
        return (templateFile);
    }

    /* get the root suffix , eg. o= isp */
    public static String getRootSuffix() {
        // rootSuffix is already normalized in SMSEntry
        return (rootSuffix);
    }

    /* get the root dir to start lookup from./<default org>
     * default is /default
     */
    private static String getFileRoot() {
        String fileRoot = ISAuthConstants.DEFAULT_DIR;
        String rootOrgName = DNUtils.DNtoName(rootSuffix);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("rootOrgName is : " + rootOrgName);
        }
        if (rootOrgName != null) {
            fileRoot = rootOrgName;
        }
        return (fileRoot);
    }

    /* insert chartset in the filename */
    private static String getCharsetFileName(String fileName) {
        ISLocaleContext localeContext = new ISLocaleContext();
        String charset = localeContext.getMIMECharset();
        if (fileName == null) {
            return (null);
        }

        int i = fileName.indexOf(".");
        String charsetFilename = null;
        if (i != -1) {
            charsetFilename = fileName.substring(0, i) + "_" + charset +
                fileName.substring(i);
        } else {
            charsetFilename = fileName + "_" + charset;
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("charsetFilename is : "+ charsetFilename);
        }
        return (charsetFilename);
    }

    /* retrieve the resource (file) using resource lookup */
    public static String getResourceLocation(String fileRoot, String localeName,
        String orgFilePath,String filePath,String filename,String templatePath,
        ServletContext servletContext,HttpServletRequest request) {
        String resourceName = null;
        String clientType = getClientType(request);
        if ((clientType != null) &&
            (!clientType.equals(getDefaultClientType()))) {
            // non-HTML client
            String charsetFileName = getCharsetFileName(filename);
            resourceName =
                ResourceLookup.getFirstExisting(servletContext,fileRoot,
                localeName,orgFilePath,
                filePath,charsetFileName,
                templatePath,true);
        }
        if (resourceName == null) {
            resourceName = ResourceLookup.getFirstExisting(servletContext,
                fileRoot,localeName,
                orgFilePath,
                filePath,filename,
                templatePath,true);
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("Resource is.. " + resourceName);
        }
        return (resourceName);
    }

    /* constructs the filePath parameter for FileLookUp
     * filePath = indexName (service name) + clientPath (eg. html).
     */
    public static String getFilePath(HttpServletRequest request,
        AuthContext.IndexType indexType, String indexName) {
        String filePath = getFilePath(getClientType(request));
        String serviceName = null;
        StringBuffer filePathBuffer = new StringBuffer();
        // only if index name is service type then need it
        // as part of the filePath since  service can have
        // have different auth template

        if ((indexType != null) &&
            (indexType.equals(AuthContext.IndexType.SERVICE))) {
            serviceName = indexName;
        }

        if ((filePath == null) && (serviceName == null)) {
            return (null);
        }

        if ((filePath != null) && (filePath.length() > 0)) {
            filePathBuffer.append(Constants.FILE_SEPARATOR)
            .append(filePath);
        }

        if ((serviceName != null) && (serviceName.length() >0)) {
            filePathBuffer.append(Constants.FILE_SEPARATOR)
            .append(serviceName);
        }

        String newFilePath = filePathBuffer.toString();
        if (utilDebug.messageEnabled()) {
            utilDebug.message("FilePath is.. :" + newFilePath);
        }

        return (newFilePath);
    }

    /* retrieves the org path to search resource
     * eg. if orgDN = o=org1,o=org11,o=org12,dc=iplanet,dc=com
     * then orgFilePath will be org12/org11/org1
     */
    static String getOrgFilePath(String orgDN) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("getOrgFilePath : orgDN is: " + orgDN);
        }
        String normOrgDN = DNUtils.normalizeDN(orgDN);
        String orgPath = null;

        if (normOrgDN != null) {
            StringBuffer orgFilePath = new StringBuffer();
            String remOrgDN = normOrgDN;
            String orgName = null;
            while ((remOrgDN != null) && (remOrgDN.length() != 0)
                && !remOrgDN.equals(getRootSuffix())) {
                orgName = DNUtils.DNtoName(remOrgDN);
                orgFilePath = orgFilePath.insert(0,
                    Constants.FILE_SEPARATOR+
                    orgName);
                int i = remOrgDN.indexOf(",");
                if (i != -1) {
                    remOrgDN = remOrgDN.substring(i+1);
                }
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("remOrgDN is : "+ remOrgDN);
                }
            }
            orgPath = orgFilePath.toString();
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("getOrgFilePath: orgPath is : " + orgPath);
        }
        return (orgPath);
    }

    /**
     * Returns the File name based on the given input values.
     *
     * @param fileName Name of the file.
     * @param localeName Locale name.
     * @param orgDN Organization distinguished name.
     * @param servletRequest HTTP Servlet Request.
     * @param servletContext Servlet Context for server.
     * @param indexType AuthContext Index Type.
     * @param indexName index name associated with the index type.
     * @return File name of the resource.
     */
    public static String getFileName(
        String fileName,
        String localeName,
        String orgDN,
        HttpServletRequest servletRequest,
        ServletContext servletContext,
        AuthContext.IndexType indexType,
        String indexName
    ) {
        String fileRoot = getFileRoot();
        String templateFile = null;
        try {
            // get the filePath  Client filePath + serviceName
            String filePath = getFilePath(servletRequest,indexType,indexName);
            String orgFilePath = getOrgFilePath(orgDN);

            if (utilDebug.messageEnabled()) {
                utilDebug.message("Calling ResourceLookup: filename = " 
                                  + fileName + ", defaultOrg = " + fileRoot 
                                  + ", locale = " + localeName + 
                                  ", filePath = " + filePath + 
                                  ", orgPath = " + orgFilePath);
            }

            templateFile = getResourceLocation(fileRoot,localeName,orgFilePath,
                filePath,fileName,templatePath,servletContext,servletRequest);
        } catch (Exception e) {
            utilDebug.message("Error getting File : " + e.getMessage());
            templateFile = new StringBuffer().append(templatePath)
            .append(Constants.FILE_SEPARATOR)
            .append(ISAuthConstants.DEFAULT_DIR)
            .append(Constants.FILE_SEPARATOR)
            .append(fileName).toString();
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("File/Resource is : " + templateFile);
        }
        return (templateFile);
    }

    public static String getAuthCookieValue(HttpServletRequest req) {
        return (CookieUtils.getCookieValueFromReq(req,getAuthCookieName()));
    }

    public static String getDomainNameByRequest(Hashtable requestHash) {
        String orgParam = getOrgParam(requestHash);
        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgParam is.. :" + orgParam);
        }
        // try to get the host name if org or domain Param is null
        if ((orgParam == null) || (orgParam.length() == 0)) {
            orgParam = "/";
            if (utilDebug.messageEnabled()) {
                utilDebug.message("defaultOrg : " + orgParam);
            }
        }
        String orgDN = getOrganizationDN(orgParam,false,null);

        if (utilDebug.messageEnabled()) {
            utilDebug.message("orgDN is " + orgDN);
        }
        return (orgDN);
    }          

    // Check whether the request is coming to the server who created the
    // original Auth request or session
    public static boolean isLocalServer(String cookieURL, boolean isServer) {
        boolean local = false;
        try {
            String urlStr   = serverURL + serviceURI;

            if (utilDebug.messageEnabled()) {
                utilDebug.message("This server URL : " + urlStr);
                utilDebug.message("Server URL from cookie : " + cookieURL);
            }

            if ((urlStr != null) && (cookieURL != null) &&
                (cookieURL.equalsIgnoreCase(urlStr))) {
                local = true;
            }
            if (!local && isServer && (cookieURL != null)) {
                int uriIndex = cookieURL.indexOf(serviceURI);
                String tmpCookieURL = cookieURL;
                if (uriIndex != -1) {
                    tmpCookieURL = cookieURL.substring(0,uriIndex);
                }
                Vector platformList = WebtopNaming.getPlatformServerList();
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("search CookieURL : " + tmpCookieURL);
                    utilDebug.message("platform server List : " 
                                      + platformList);
                }
                // if cookie URL is not in the Platform server list then
                // consider as new authentication for that local server
                if (!platformList.contains(tmpCookieURL)) {
                    local = true;
                }
            }
        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Error isLocalServer : " + e.getMessage());
            }
        }
        return (local);
    }
    // Check whether the request is coming to the server who created the
    // original Auth request or session
    // This method needs to be merged with the one above.
    public static boolean isLocalServer(String cookieURL, String inputURI) {
        int uriIndex = cookieURL.indexOf(inputURI);
        String tmpCookieURL = cookieURL;
        if (uriIndex != -1) {
            tmpCookieURL = cookieURL.substring(0,uriIndex);
        }
        return (isLocalServer(tmpCookieURL+serviceURI, true));
    }

    /**
     * Sends the request to the original Auth server and receives the result
     * data.
     *
     * @param request HttpServletRequest to be sent
     * @param response HttpServletResponse to be received
     * @param cookieURL URL of the original authentication server to be
     * connected
     *
     * @return HashMap of the result data from the original server's response
     *
     */
    public static HashMap sendAuthRequestToOrigServer(HttpServletRequest request,
        HttpServletResponse response, String cookieURL) {
        HashMap origRequestData = new HashMap();

        // Print request Headers
        if (utilDebug.messageEnabled()) {
            Enumeration requestHeaders = request.getHeaderNames();
            while (requestHeaders.hasMoreElements()) {
                String name = (String) requestHeaders.nextElement();
                Enumeration value = (Enumeration)request.getHeaders(name);
                utilDebug.message("Header name = " + name + 
                                  " Value = " + value);
            }// w
        }

        // Open URL connection
        HttpURLConnection conn = null;
        OutputStream  out = null;
        String strCookies = null;
        try {
            URL authURL = new URL(cookieURL);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Connecting to : " + authURL);
            }
            conn = HttpURLConnectionManager.getConnection(authURL);
            conn.setDoOutput( true );
            conn.setUseCaches( useCache );
            conn.setRequestMethod("POST");
            conn.setFollowRedirects(false);
            conn.setInstanceFollowRedirects(false);

            // replay cookies
            strCookies = getCookiesString(request);
            if (strCookies != null) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Sending cookies : " + strCookies);
                }
                conn.setRequestProperty("Cookie", strCookies);
            }
            conn.setRequestProperty(
                "Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty(
                "Content-Length", request.getHeader("content-length"));
            conn.setRequestProperty("Host", request.getHeader("host"));

            // Sending Output to Original Auth server...
            utilDebug.message("SENDING DATA ... ");
            String in_requestData = getFormData(request);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Request data : " + in_requestData);
            }
            out = conn.getOutputStream();
            PrintWriter pw = new PrintWriter(out);
            pw.print(in_requestData); // here we "send" the request body
            pw.flush();
            pw.close();

            // Receiving input from Original Auth server...
            utilDebug.message("RECEIVING DATA ... ");
            if (utilDebug.messageEnabled()) {
                utilDebug.message("Response Code: " + conn.getResponseCode());
                utilDebug.message("Response Message: " 
                                  + conn.getResponseMessage());
                utilDebug.message("Follow redirect : " 
                                  + conn.getFollowRedirects());
            }

            // Check response code
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {

                // Input from Original servlet...
                StringBuffer in_buf = new StringBuffer();
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
                int len;
                char[] buf = new char[1024];
                while ((len = in.read(buf,0,buf.length)) != -1) {
                    in_buf.append(buf,0,len);
                }
                String in_string = in_buf.toString();
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Received response data : " + in_string);
                }
                origRequestData.put("OUTPUT_DATA",in_string);

            } else {
                utilDebug.message("Response code NOT OK");
            }

            String client_type = conn.getHeaderField("AM_CLIENT_TYPE");
            if (client_type != null) {
                origRequestData.put("AM_CLIENT_TYPE", client_type);
            }
            String redirect_url = conn.getHeaderField("Location");
            if (redirect_url != null) {
                origRequestData.put("AM_REDIRECT_URL", redirect_url);
            }

            // retrieves cookies from the response
            Map headers = conn.getHeaderFields();
            processCookies(headers, response);

            out.flush();

        } catch (Exception e) {
            if (utilDebug.messageEnabled()) {
                utilDebug.message("send exception : " , e);
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ioe) {
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("send IOException : " 
                                          + ioe.toString());
                    }
                }
            }
        }

        return (origRequestData);
    }

    // Gets the request form data in the form of string
    private static String getFormData(HttpServletRequest request) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("");
        Enumeration requestEnum = request.getParameterNames();
        while (requestEnum.hasMoreElements()) {
            String name = (String) requestEnum.nextElement();
            String value = request.getParameter(name);
            buffer.append(URLEncDec.encode(name));
            buffer.append('=');
            buffer.append(URLEncDec.encode(value));
            if (requestEnum.hasMoreElements()) {
                buffer.append('&');
            }
        }
        return(buffer.toString());
    }

    // parses the cookies from the response header and adds them in
    // the HTTP response.
    private static void processCookies(Map headers, HttpServletResponse response) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("processCookies : headers : " + headers);
        }

        if (headers == null || headers.isEmpty()) {
            return;
        }

        for (Iterator hrs = headers.entrySet().iterator(); hrs.hasNext();) {
            Map.Entry me = (Map.Entry)hrs.next();
            String key = (String) me.getKey();
            if (key != null && (key.equalsIgnoreCase("Set-cookie") ||
                (key.equalsIgnoreCase("Cookie")))) {
                List list = (List)me.getValue();
                if (list == null || list.isEmpty()) {
                    continue;
                }
                Cookie cookie = null;
                String domain = null;
                String path = null;
                for (Iterator it = list.iterator(); it.hasNext(); ) {
                    String cookieStr = (String)it.next();
                    if (utilDebug.messageEnabled()) {
                        utilDebug.message("processCookies : cookie : " 
                                          + cookieStr);
                    }
                    StringTokenizer stz = new StringTokenizer(cookieStr, ";");
                    if (stz.hasMoreTokens()) {
                        String nameValue = (String)stz.nextToken();
                        int index = nameValue.indexOf("=");
                        if (index == -1) {
                            continue;
                        }
                        String tmpName = nameValue.substring(0, index).trim();
                        String value = nameValue.substring(index + 1);
                        Set domains = getCookieDomains();
                        if (!domains.isEmpty()) {
                            for (Iterator itcd = 
                                 domains.iterator(); itcd.hasNext(); ) {
                                domain = (String)itcd.next();
                                cookie = createCookie(tmpName, value, domain);
                                response.addCookie(cookie);
                            }
                        } else {
                            cookie = createCookie(tmpName, value, null);
                            response.addCookie(cookie);
                        }
                    }
                }
            }
        }
    }

    // Get cookies string from HTTP request object
    private static String getCookiesString(HttpServletRequest request) {
        Cookie cookies[] = request.getCookies();
        StringBuffer cookieStr = null;
        String strCookies = null;
        // Process Cookies
        if (cookies != null) {
            for (int nCookie = 0; nCookie < cookies.length; nCookie++) {
                if (utilDebug.messageEnabled()) {
                    utilDebug.message("Cookie name = " + 
                                      cookies[nCookie].getName());
                    utilDebug.message("Cookie value = " + 
                                      cookies[nCookie].getValue());
                }
                if (cookieStr == null) {
                    cookieStr = new StringBuffer();
                } else {
                    cookieStr.append(";");
                }
                cookieStr.append(cookies[nCookie].getName())
                .append("=")
                .append(cookies[nCookie].getValue());
            }
        }
        if (cookieStr != null) {
            strCookies = cookieStr.toString();
        }
        return (strCookies);
    }
    /**
    * Sets server cookie to <code>HttpServletResponse</code> object
    * @param aCookie auth context associated with lb cookie
    * @param response <code>true</code> if it is persistent
    * @throws AuthException if it fails to create pcookie
    */

    public static void setServerCookie(Cookie aCookie, HttpServletResponse response)
    throws AuthException {
        String cookieName = aCookie.getName();
        String cookieValue = aCookie.getValue();
        if (cookieName != null && cookieName.length() != 0) {
            Set domains = getCookieDomains();
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createCookie(cookieName, cookieValue,
                        domain);
                    response.addCookie(cookie);
                }
            } else {
                response.addCookie(createCookie(cookieName,cookieValue,null));
            }
        }
    } 

    /**
     * Sets the redirectBackUrlCookie to be set as Access Manager 
     * server URL when redirecting to external web site during authentication
     * process.
     * @param cookieName auth context associated with lb cookie
     * @param cookieValue auth context associated with lb cookie
     * @param response <code>true</code> if it is persistent
     * @throws AuthException if it fails to create this cookie
     */
    public static void setRedirectBackServerCookie(String cookieName, 
        String cookieValue, HttpServletResponse response) 
        throws AuthException {

        if (cookieName != null && cookieName.length() != 0) {
            Set domains = getCookieDomains();
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie = createCookie(cookieName, cookieValue,
                        domain);
                    response.addCookie(cookie);
                }
            } else {
                response.addCookie(createCookie(cookieName,cookieValue,null));
            }
        }
    }

    /**
     * Clears server cookie.
     *
     * @param cookieName Cookie Name.
     * @param response HTTP Servlet Response.
     */
    public static void clearServerCookie(
        String cookieName,
        HttpServletResponse response) {
        if (utilDebug.messageEnabled()) {
            utilDebug.message("In clear server Cookie = " +  cookieName);
        }
        if (cookieName != null && cookieName.length() != 0) {
            Set domains = getCookieDomains();
            if (!domains.isEmpty()) {
                for (Iterator it = domains.iterator(); it.hasNext(); ) {
                    String domain = (String)it.next();
                    Cookie cookie =
                        createPersistentCookie(cookieName, "LOGOUT", 0, domain);
                    response.addCookie(cookie);
                    utilDebug.message("In clear server Cookie added cookie");
                }
            } else {
                response.addCookie(
                    createPersistentCookie(cookieName, "LOGOUT", 0, null));
                utilDebug.message("In clear server added cookie no domain");
            }
        }
    }

    // Returns Query String from request parameters Map
    public static String getQueryStrFromParameters(Map paramMap) {
        StringBuffer buff = new StringBuffer();
        boolean first = true;

        if (paramMap != null && !paramMap.isEmpty()) {
            for (Iterator i = paramMap.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry me = (Map.Entry)i.next();
                String key = (String)me.getKey();
                String value = (String)me.getValue();

                if (first) {
                    buff.append("?");
                    first = false;
                } else {
                    buff.append("&");
                }

                buff.append(key).append("=").append(value);
            }
        }
        return (buff.toString());
    } 

    /** 
     * Returns the Base64 decoded value.
     * @param encodedStr Base64 encoded string
     * @return a String the Base64 decoded string value
     */
    public static String getBase64DecodedValue(String encodedStr) {
        String returnValue = null;

        if (encodedStr != null && encodedStr.length() != 0) {
            try {
                returnValue = new String(Base64.decode(encodedStr), "UTF-8");
            } catch (RuntimeException rtex) {
                utilDebug.warning("getBase64DecodedValue:RuntimeException");
            } catch (java.io.UnsupportedEncodingException ueex) {
                utilDebug.warning("getBase64DecodedValue:" + 
                    "UnsupportedEncodingException");
            }
        }

        if (utilDebug.messageEnabled()) {
            utilDebug.message("getBase64DecodedValue:returnValue : " 
                + returnValue);
        }
        return (returnValue);
    }

    /**
     * Returns true if the request has the ForceAuth=<code>true</code>
     * query parameter or composite advise.
     *
     * @return true if this parameter is present otherwise false.
     */
    public static boolean forceAuthFlagExists(Hashtable reqDataHash) {
        String force = (String) reqDataHash.get("ForceAuth");
        boolean forceFlag = (Boolean.valueOf(force)).booleanValue();
        if (utilDebug.messageEnabled()) {
             utilDebug.message("AuthUtils.forceFlagExists : " + forceFlag);
        }
        if (forceFlag == false) {
            if ( reqDataHash.get(Constants.COMPOSITE_ADVICE) != null ) {
                String tmp = (String)reqDataHash.
                    get(Constants.COMPOSITE_ADVICE);
                forceFlag =
                checkForForcedAuth(tmp);
            }
        }
        return forceFlag;
    }

    /**
     * Returns true if the composite Advice has the ForceAuth element
     *
     * @return true if this parameter is present otherwise false.
     */
    public static boolean checkForForcedAuth(String xmlCompositeAdvice) {
        boolean returnForcedAuth = false;
        try {
            String decodedAdviceXML = URLDecoder.decode(xmlCompositeAdvice);
            Map adviceMap = PolicyUtils.parseAdvicesXML(decodedAdviceXML);
            if (utilDebug.messageEnabled()) {
                utilDebug.message("AuthUtils.checkForForcedAuth : decoded XML "
                    +"= " + decodedAdviceXML);
                utilDebug.message("AuthUtils.checkForForcedAuth : result Map = "
                + adviceMap);
            }
            if (adviceMap != null) {
                if (adviceMap.containsKey(AuthSchemeCondition.
                    FORCE_AUTH_ADVICE)) {
                    returnForcedAuth = true;
                }
            }
        } catch  (com.sun.identity.policy.PolicyException polExp) {
            utilDebug.error("AuthUtils.checkForForcedAuth : Error in "
                + "Policy  XML parsing ",polExp );
        }
        if (utilDebug.messageEnabled()) {
            utilDebug.message("AuthUtils.checkForForcedAuth: returnForcedAuth"+
                "= " + returnForcedAuth);
        }
        return returnForcedAuth;
    }
    
    /**
     *  Sets the service URI
     */    
    public static void setServiceURI(String strServiceURI) {
    	serviceURI =  strServiceURI;  	
    }

    /** 
     * Returns the service URI
     * @return a String the Service URI
     */
    public static String getServiceURI(){
    	return serviceURI;
    }
}
