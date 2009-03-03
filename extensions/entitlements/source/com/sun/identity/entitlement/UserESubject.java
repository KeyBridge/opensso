/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: UserESubject.java,v 1.3 2009-02-27 16:58:44 dillidorai Exp $
 */
package com.sun.identity.entitlement;

import com.sun.identity.shared.debug.Debug;

import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * ESubject to represent user identity for membership check
 * @author dorai
 */
public class UserESubject implements ESubject {

    private String user;
    private String pSubjectName;

    /**
     * Constructs an UserESubject
     */
    public UserESubject() {
    }

    /**
     * Constructs UserESubject
     * @param user the uuid of the user who is member of the ESubject
     */
    public UserESubject(String user) {
        this.user = user;
    }

    /**
     * Constructs UserESubject
     * @param user the uuid of the user who is member of the ESubject
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when UserESubject was created from
     * OpenSSO policy Subject
     */
    public UserESubject(String user, String pSubjectName) {
        this.user = user;
        this.pSubjectName = pSubjectName;
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            user = jo.optString("user");
            pSubjectName = jo.optString("pSubjectName");
        } catch (JSONException joe) {
        }
    }

    /**
     * Returns state of the object
     * @return state of the object encoded as string
     */
    public String getState() {
        return toString();
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping  of the object
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("user", user);
        jo.put("pSubjectName", pSubjectName);
        return jo;
    }

    /**
     * Returns string representation of the object
     * @return string representation of the object
     */
    public String toString() {
        String s = null;
        try {
            JSONObject jo = new JSONObject();
            jo.put("user", user);
            jo.put("pSubjectName", pSubjectName);
            s = jo.toString(2);
        } catch (JSONException joe) {
            Debug debug = Debug.getInstance("Entitlement");
            debug.error("UserESubject.toString(), JSONException:" +
                    joe.getMessage());
        }
        return s;
    }

    /**
     * Returns <code>SubjectDecision</code> of
     * <code>ESubject</code> evaluation
     * @param subject ESubject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>SubjectDecision</code> of
     * <code>ESubject</code> evaluation
     * @throws com.sun.identity.entitlement,  EntitlementException in case
     * of any error
     */
    public SubjectDecision evaluate(
            Subject subject,
            String resourceName,
            Map<String, Set<String>> environment)
            throws EntitlementException {
        return null;
    }

    /**
     * Sets the member user of the object
     * @param user the uuid of the member user
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the member user of the object
     * @return  the uuid of the member user
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets OpenSSO policy subject name of the object
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when UserESubject was created from
     * OpenSSO policy Subject
     */
    public void setPSubjectName(String pSubjectName) {
        this.pSubjectName = pSubjectName;
    }

    /**
     * Returns OpenSSO policy subject name of the object
     * @return subject name as used in OpenSSO policy,
     * this is releavant only when UserESubject was created from
     * OpenSSO policy Subject
     */
    public String getPSubjectName() {
        return pSubjectName;
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return  <code>true</code> if the passed in object is equal to this object
     */
    public boolean equals(Object obj) {
        boolean equalled = true;
        if (obj == null) {
            equalled = false;
        }
        if (!getClass().equals(obj.getClass())) {
            equalled = false;
        }
        UserESubject object = (UserESubject) obj;
        if (user == null) {
            if (object.getUser() != null) {
                equalled = false;
            }
        } else {
            if (!user.equals(object.getUser())) {
                equalled = false;
            }
        }
        if (pSubjectName == null) {
            if (object.getPSubjectName() != null) {
                equalled = false;
            }
        } else {
            if (!pSubjectName.equals(object.getPSubjectName())) {
                equalled = false;
            }
        }
        return equalled;
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    public int hashCode() {
        int code = 0;
        if (user != null) {
            code += user.hashCode();
        }
        if (pSubjectName != null) {
             code += pSubjectName.hashCode();
        }
        return code;
    }
}