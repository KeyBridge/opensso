/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms of the Common
 * Development and Distribution License (the License). You may not use
 * this file except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each
 * file and include the License file at opensso/legal/CDDLv1.0.txt. If
 * applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ListenerResource.java,v 1.4 2009-11-26 17:06:06 veiming Exp $
 */

package com.sun.identity.rest;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementListener;
import com.sun.identity.entitlement.ListenerManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import org.json.JSONException;

/**
 * Exposes the entitlement listener REST resource.
 */
@Path("/1/entitlement/listener")
public class ListenerResource extends ResourceBase {
    @POST
    @Produces("application/json")
    public String addListener(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @FormParam("url") String url,
        @FormParam("resources") List<String> resources,
        @FormParam("application") @DefaultValue("iPlanetAMWebAgentService")
            String application
    ) {
        try {
            Subject caller = getCaller(request);
            URL urlObj = new URL(url);
            EntitlementListener l = new EntitlementListener(urlObj,
                application, resources);
            ListenerManager.getInstance().addListener(caller, l);
            return createResponseJSONString(201, headers, "Created");
        } catch (RestException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (JSONException e) {
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (EntitlementException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (MalformedURLException e) {
            Object[] param = {url};
            throw getWebApplicationException(headers, 400, 426, param);
        }
    }

    @DELETE
    @Produces("application/json")
    @Path("/{url}")
    public String deleteListener(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @PathParam("url") String url
    ) {
        try {
            Subject caller = getCaller(request);
            URL urlObj = new URL(url);
            ListenerManager.getInstance().removeListener(caller, urlObj);
            return createResponseJSONString(200, headers, "OK");
        } catch (RestException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (JSONException e) {
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (EntitlementException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (MalformedURLException e) {
            throw getWebApplicationException(426, e, MimeType.JSON);
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{url}")
    public String getListener(
        @Context HttpHeaders headers,
        @Context HttpServletRequest request,
        @PathParam("url") String url
    ) {
        try {
            Subject caller = getCaller(request);
            URL urlObj = new URL(url);
            EntitlementListener listener = ListenerManager.getInstance()
                .getListener(caller, urlObj);
            if (listener == null) {
                String[] param = {url.toString()};
                throw new EntitlementException(427, param);
            }
            return createResponseJSONString(200, headers,
                listener.toJSON());
        } catch (JSONException e) {
            throw getWebApplicationException(e, MimeType.JSON);
        } catch (RestException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (EntitlementException e) {
            throw getWebApplicationException(headers, e, MimeType.JSON);
        } catch (MalformedURLException e) {
            throw getWebApplicationException(426, e, MimeType.JSON);
        }
    }
}

