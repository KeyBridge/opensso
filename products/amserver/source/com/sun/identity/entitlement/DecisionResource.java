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
 * $Id: DecisionResource.java,v 1.7 2009-09-10 16:35:38 veiming Exp $
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.util.AuthSPrincipal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Exposes the entitlement decision REST resource.
 * 
 * @author Paul C. Bryan <pbryan@sun.com>
 * @author Ravi Hingarajiya <ravi.hingarajiya@sun.com>
 */
@Path("/1/entitlement")
public class DecisionResource {
    public static final String JSON_DECISION_ARRAY_KEY = "results";

    private enum Permission {
        deny, allow
    }

    private Subject getCaller() {
        //TOFIX: hardcoded to amadmin
        return toSubject(
            "id=amAdmin,ou=user,dc=opensso,dc=java,dc=net");
    }

    private Evaluator getEvaluator(Subject caller, String application)
        throws EntitlementException {
        return ((application == null) || (application.length() == 0))
            ? new Evaluator(caller) : new Evaluator(caller, application);
    }

    /**
     * Returns entitlement decision of a given user.
     *
     * @param realm Realm name.
     * @param subject Subject of interest.
     * @param action Action to be evaluated.
     * @param resource Resource to be evaluated.
     * @param application Application name.
     * @param environment environment parameters.
     * @return entitlement decision of a given user. Either "deny" or "allow".
     */
    @GET
    @Produces("text/plain")
    @Path("/decision")
    public String decision(
        @Context HttpHeaders headers,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("subject") String subject,
        @QueryParam("action") String action,
        @QueryParam("resource") String resource,
        @QueryParam("application") String application,
        @QueryParam("env") List<String> environment
    ) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }
        Subject caller = getCaller();
        Map env = getMap(environment);

        try {
            validateSubjectAndResource(subject, resource);

            if ((action == null) || (action.trim().length() == 0)) {
                throw new EntitlementException(422);
            }
            Evaluator evaluator = getEvaluator(caller, application);
            return permission(evaluator.hasEntitlement(realm,
                toSubject(subject), toEntitlement(resource, action),
                env));
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.decision", e);
            throw getWebApplicationException(headers, e);
        }
    }

    /**
     * Returns the entitlements of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resources to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/decisions")
    public String decisions(
        @Context HttpHeaders headers,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("subject") String subject,
        @QueryParam("resources") List<String> resources,
        @QueryParam("application") String application,
        @QueryParam("env") List<String> environment
    ) {
        try {
            if (!realm.startsWith("/")) {
                realm = "/" + realm;
            }
            if ((resources == null) || resources.isEmpty()) {
                throw new EntitlementException(424);
            }

            Map env = getMap(environment);
            Set<String> setResources = new HashSet<String>();
            setResources.addAll(resources);
            validateSubject(subject);
            Evaluator evaluator = getEvaluator( getCaller(), application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, toSubject(subject), setResources, env);

            List<JSONObject> results = new ArrayList<JSONObject>();
            if (entitlements != null) {
                for (Entitlement e : entitlements) {
                    Map<String, Boolean> actionValues = e.getActionValues();
                    if ((actionValues != null) && !actionValues.isEmpty()) {
                        JSONEntitlement je = new JSONEntitlement(
                            e.getResourceName(), actionValues, e.getAdvices(),
                            e.getAttributes());
                        results.add(je.toJSONObject());
                    }
                }
            }

            JSONObject jo = new JSONObject();
            jo.put(JSON_DECISION_ARRAY_KEY, results);
            return jo.toString();
        } catch (JSONException e) {
            PrivilegeManager.debug.warning("DecisionResource.decisions", e);
            throw getWebApplicationException(e);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.decisions", e);
            throw getWebApplicationException(headers, e);
        }
    }

    /**
     * Returns the entitlement of a given subject.
     *
     * @param realm Realm Name.
     * @param subject Subject of interest.
     * @param action action to be evaluated.
     * @param resource resource to be evaluated
     * @param application application name.
     * @param environment environment parameters.
     * @return entitlement of a given subject (in JSON string).
     */
    @GET
    @Produces("application/json")
    @Path("/entitlement")
    public String entitlement(
        @Context HttpHeaders headers,
        @QueryParam("realm") @DefaultValue("/") String realm,
        @QueryParam("subject") String subject,
        @QueryParam("resource") String resource,
        @QueryParam("application") String application,
        @QueryParam("env") List<String> environment
    ) {
        if (!realm.startsWith("/")) {
            realm = "/" + realm;
        }

        Map env = getMap(environment);
        Subject caller = getCaller();

        try {
            validateSubjectAndResource(subject, resource);
            Evaluator evaluator = getEvaluator(caller, application);
            List<Entitlement> entitlements = evaluator.evaluate(
                realm, toSubject(subject), resource, env, false);

            Entitlement e = entitlements.get(0);
            JSONEntitlement jsonE = new JSONEntitlement(e.getResourceName(),
                e.getActionValues(), e.getAdvices(), e.getAttributes());
            return jsonE.toJSONObject().toString();
        } catch (JSONException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(e);
        } catch (EntitlementException e) {
            PrivilegeManager.debug.warning("DecisionResource.evaluate", e);
            throw getWebApplicationException(headers, e);
        }
    }

    private Map<String, Set<String>> getMap(List<String> list) {
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        if ((list != null) && !list.isEmpty()) {
            for (String l : list) {
                if (l.contains("=")) {
                    String[] cond = l.split("=", 2);
                    Set<String> set = env.get(cond[0]);

                    if (set == null) {
                        set = new HashSet<String>();
                        env.put(cond[0], set);
                    }

                    set.add(cond[1]);
                }
            }
        }
        
        return env;
    }

    private Entitlement toEntitlement(String resource, String action) {
        Set<String> set = new HashSet<String>();
        set.add(action);
        return new Entitlement(resource, set);
    }

    private Subject toSubject(Principal principal) {
        if (principal == null) {
            return null;
        }
        Set<Principal> set = new HashSet<Principal>();
        set.add(principal);
        return new Subject(false, set, new HashSet(), new HashSet());
    }

    private Subject toSubject(String subject) {
        return (subject == null) ? null :
            toSubject(new AuthSPrincipal(subject));
    }

    private String permission(boolean b) {
        return (b ? Permission.allow.toString() : Permission.deny.toString());
    }

    private WebApplicationException getWebApplicationException(
        HttpHeaders headers,
        EntitlementException e) {
        throw new WebApplicationException(
              Response.status(e.getErrorCode())
              .entity(e.getLocalizedMessage(getUserLocale(headers)))
              .type("text/plain; charset=UTF-8").build());
    }

    private WebApplicationException getWebApplicationException(
        JSONException e) {
        throw new WebApplicationException(
              Response.status(425)
              .entity(e.getLocalizedMessage())
              .type("text/plain; charset=UTF-8").build());
    }
    
    private Locale getUserLocale(HttpHeaders headers) {
        List<Locale> locales = headers.getAcceptableLanguages();
        return ((locales == null) || locales.isEmpty()) ? Locale.getDefault() :
            locales.get(0);
    }

    private void validateSubjectAndResource(String subject, String resource)
        throws EntitlementException {
        validateSubject(subject);
        validateResource(resource);
    }

    private void validateSubject(String subject)
        throws EntitlementException {
        if ((subject == null) || (subject.trim().length() == 0)) {
            throw new EntitlementException(421);
        }
    }

    private void validateResource(String resource)
        throws EntitlementException {
        if ((resource == null) || (resource.trim().length() == 0)) {
            throw new EntitlementException(420);
        }
    }
}

