/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LoginServlet.java,v 1.7 2008-06-25 05:41:49 qcheng Exp $
 *
 */



package com.sun.identity.authentication.UI;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.jato.CompleteRequestException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestContextImpl;
import com.iplanet.jato.ViewBeanManager;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;
import java.net.URL;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is the front controller of authentication UI
 */
public class LoginServlet
extends com.sun.identity.authentication.UI.AuthenticationServletBase {
    /**
     * Creates <code>LoginServlet</code> object.
     */
    public LoginServlet() {
        super();
    }
    
    /**
     *
     *
     */
    protected void initializeRequestContext(RequestContext requestContext) {
        super.initializeRequestContext(requestContext);
        Debug debug = Debug.getInstance("amLoginServlet");
        // Set a view bean manager in the request context.  This must be
        // done at the module level because the view bean manager is
        // module specifc.
        ViewBeanManager viewBeanManager =
        new ViewBeanManager(requestContext,PACKAGE_NAME);
        
        ((RequestContextImpl)requestContext).setViewBeanManager(
        viewBeanManager);
        
        HttpServletRequest request = requestContext.getRequest();
        HttpServletResponse response = requestContext.getResponse();
        
        // Check content length
        try {
            RequestUtils.checkContentLength(request);
        } catch (L10NMessageImpl e) {
            if (debug.messageEnabled()) {
                ISLocaleContext localeContext = new ISLocaleContext();
                localeContext.setLocale(request);
                java.util.Locale locale = localeContext.getLocale();
                debug.message("LoginServlet: " + e.getL10NMessage(locale));
            }
            AuthExceptionViewBean vb = (AuthExceptionViewBean)
            viewBeanManager.getViewBean(
            com.sun.identity.authentication.UI.AuthExceptionViewBean.class);
            vb.forwardTo(requestContext);
            throw new CompleteRequestException();
        }
        
        // Check if the hostname in the URL is an FQDN else
        // redirect to the fqdn
        String client_type = AuthUtils.getClientType(request);
        if (debug.messageEnabled()) {
            debug.message("Client Type = " + client_type);
        }
        String hostName = AuthUtils.getHostName(request);
        if (!AuthUtils.isValidFQDNRequest(hostName)) {
            try {
                String newHN =
                AuthUtils.getValidFQDNResource(hostName,request);
                if (debug.messageEnabled()) {
                    debug.message("FQDN = " + newHN);
                }
                if (AuthUtils.isGenericHTMLClient(client_type)) {
                    debug.message("This is HTML");
                    response.sendRedirect(newHN);
                } else {
                    String fileName = AuthUtils.getDefaultFileName(
                        request, REDIRECT_JSP);
                    if (debug.messageEnabled()) {
                        debug.message("Forward to : " + fileName);
                    }
                    RequestDispatcher dispatcher =
                    request.getRequestDispatcher(fileName);
                    dispatcher.forward(request, response);
                }
            } catch (Exception e) {
                // came here continue
            }
            throw new CompleteRequestException();
        }
        
        // Check whether this is the correct server to accept the client
        // response.
        String authCookieValue = AuthUtils.getAuthCookieValue(request);
        if ((authCookieValue != null) && (authCookieValue.length() != 0) &&
            (!authCookieValue.equalsIgnoreCase("LOGOUT"))) {
            //if cookie server does not match to this local server then
            //send Auth request to cookie (original) server
            String cookieURL = null;
            try {
                SessionID sessionID = new SessionID(authCookieValue);
                URL sessionServerURL = Session.getSessionServiceURL(sessionID);
                cookieURL = sessionServerURL.getProtocol()
                + "://" + sessionServerURL.getHost() + ":"
                    + Integer.toString(sessionServerURL.getPort()) + serviceURI;
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("LoginServlet error in Session : "
                        + e.toString());
                }
            }
            if (debug.messageEnabled()) {
                debug.message("cookieURL : " + cookieURL);
            }
            if ((cookieURL != null) && (cookieURL.length() != 0) &&
                (!AuthUtils.isLocalServer(cookieURL,true))) {
                debug.message("Routing the request to Original Auth server");
                try {
                    HashMap origRequestData =
                        AuthUtils.sendAuthRequestToOrigServer(
                        request,response,cookieURL);                    
                    String redirect_url = null;
                    String clientType = null;
                    String output_data = null;
                    if (origRequestData != null && !origRequestData.isEmpty()) {
                        redirect_url =
                            (String)origRequestData.get("AM_REDIRECT_URL");
                        output_data =
                            (String)origRequestData.get("OUTPUT_DATA");
                        clientType =
                            (String)origRequestData.get("AM_CLIENT_TYPE");
                    }
                    if (debug.messageEnabled()) {
                        debug.message("redirect_url : " + redirect_url);
                        debug.message("clientType : " + clientType);
                    }
                    if (((redirect_url != null) && !redirect_url.equals("")) &&
                        (AuthUtils.isGenericHTMLClient(clientType))
                    ) {
                        debug.message("Redirecting the response");
                        response.sendRedirect(redirect_url);
                    }
                    if ((output_data != null) && (!output_data.equals(""))) {
                        debug.message("Printing the forwarded response");
                        java.io.PrintWriter outP = response.getWriter();
                        outP.println(output_data);
                    }
                   if ((redirect_url == null || (redirect_url.length() == 0))
                           && (output_data == null || (output_data.length() 
                           == 0))) {
                       if (debug.messageEnabled()) {
                           debug.message("LoginServlet:initializeRequestContext"
                               + " No Response from original Auth server");
                       }
                       String refererURL = request.getHeader("Referer");
                       String refererDomain = null;
                       if(refererURL!=null && !(refererURL.length() == 0)) {
                           URL u =new URL(refererURL);
                           int pos = u.getHost().indexOf(".");
                           if(pos!=-1) {
                               refererDomain= u.getHost().substring(pos);
                           }
                       } else {
                           refererURL = request.getRequestURL().toString();
                           if (request.getQueryString() != null) {
                               refererURL = refererURL + "?" + 
                                   request.getQueryString();
                           }
                       }
                       if (debug.messageEnabled()) {
                           debug.message("LoginServlet:initializeRequestContext"
                               + " referer domain is " + refererDomain); 
                       }
                       //remove amAuthCookie and amLBCookie cookies
                       Cookie[] cookies = request.getCookies();
                       Set domains = AuthUtils.getCookieDomains();
                       if (cookies != null && cookies.length > 0) {
                           for (int i = 0; i < cookies.length; i++) {
                               if (cookies[i].getName().equalsIgnoreCase(
                                   AuthUtils.getAuthCookieName())
                                   || cookies[i].getName().equalsIgnoreCase
                                   (AuthUtils.getlbCookieName())) {
                                   if (debug.messageEnabled()) {
                                       debug.message("LoginServlet:" 
                                           + "initializeRequestContext removing"
                                           + "cookie "+ cookies[i].getName());
                                   }
                                   cookies[i].setValue("");
                                   cookies[i].setMaxAge(0);
                                   response.addCookie(cookies[i]);
                                   if (!domains.isEmpty()) {
                                       for (Iterator it = domains.iterator(); 
                                           it.hasNext(); ) {
                                           String domain = (String)it.next();
                                           if (debug.messageEnabled()) {
                                               debug.message("LoginServlet:" 
                                                   + "initializeRequestContext"
                                                   + " removing cookie "+ 
                                                   domain);
                                           }
                                           Cookie cookie = AuthUtils.
                                               createCookie(
                                               cookies[i].getName(),"",domain);
                                           cookie.setMaxAge(0);
                                           response.addCookie(cookie);
                                       } //end for
                                   } else {
                                       //using domain name from referer
                                       if (refererDomain != null) {
                                           Cookie cookie = AuthUtils.
                                               createCookie(cookies[i].
                                               getName(),"", refererDomain);
                                           cookie.setMaxAge(0);
                                           response.addCookie(cookie);
                                       }             
                                   }
                               } 
                           }
                       }
                       if (debug.messageEnabled()) {
                           debug.message("LoginServlet:initializeRequestContext"
                               + "redirecting to: " + refererURL);
                       }
                       response.sendRedirect(refererURL);
                   } 
                } catch (Exception e) {
                    if (debug.messageEnabled()) {
                        debug.message("LoginServlet error in Request Routing : "
                            + e.toString());
                    }
                }
                throw new CompleteRequestException();
            }
        }
    }    
    
    /**
     * Returns url for auth module.
     * @return url for auth module.
     */
    public String getModuleURL() {
        // The superclass can be configured from init params specified at
        // deployment time.  If the superclass has been configured with
        // a different module URL, it will return a non-null value here.
        // If it has not been configured with a different URL, we use our
        // (hopefully) sensible default.
        String result = super.getModuleURL();
        if (result != null)
            return result;
        else
            return DEFAULT_MODULE_URL;
    }
    
    /**
     *
     *
     */
    protected void onSessionTimeout(RequestContext requestContext)
    throws ServletException {
        // Do nothing
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Class variables
    ////////////////////////////////////////////////////////////////////////////
    
    /** Default module uri. */
    public static final String DEFAULT_MODULE_URL="../UI";
    /** Confiured page name for configured servlet */
    public static String PACKAGE_NAME=
    getPackageName(LoginServlet.class.getName());
    
    private static final String REDIRECT_JSP = "Redirect.jsp";
    
    // the debug file
    private Debug debug;
    
    private static String serviceURI = SystemProperties.get(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) + "/UI/Login";    
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Instance variables
    ////////////////////////////////////////////////////////////////////////////
}

