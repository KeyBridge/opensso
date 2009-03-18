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
 * $Id: PriviligeUtilsTest.java,v 1.5 2009-03-18 01:15:25 dillidorai Exp $
 */
package com.sun.identity.policy;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.UserSubject;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.IPCondition;
import com.sun.identity.entitlement.OrSubject;
import com.sun.identity.entitlement.Privilige;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.policy.interfaces.Subject;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.unittest.UnittestLog;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.Test;

/**
 *
 * @author dillidorai
 */
public class PriviligeUtilsTest {

    @Test
    public void testPriviligeToPolicy() throws Exception {
        String BASE_DN = "dc=opensso,dc=java,dc=net";
        Map<String, Object> actionValues = new HashMap<String, Object>();
        Set<String> getValues = new HashSet<String>();
        getValues.add("allow");
        actionValues.put("GET", getValues);
        Set<String> postValues = new HashSet<String>();
        postValues.add("allow");
        actionValues.put("POST", postValues);
        String resourceName = "http://www.sun.com";
        Entitlement entitlement = new Entitlement("iPlanetAMWebAgentService",
                resourceName, actionValues);
        entitlement.setName("ent1");
        Set<Entitlement> entitlements = new HashSet<Entitlement>();
        entitlements.add(entitlement);
        String user11 = "id=user11,ou=user," + BASE_DN;
        String user12 = "id=user12,ou=user," + BASE_DN;
        EntitlementSubject us1 = new UserSubject(user11);
        EntitlementSubject us2 = new UserSubject(user12);
        Set<EntitlementSubject> subjects = new HashSet<EntitlementSubject>();
        subjects.add(us1);
        subjects.add(us2);
        OrSubject os = new OrSubject(subjects);
        EntitlementCondition ipc = new IPCondition("*.sun.com");
        Privilige privilige = new Privilige(
                "TestPrivilige",
                entitlements,
                us1, //orSubject
                ipc, //entitlementCondition
                null); //attributes

        UnittestLog.logMessage(
                "PriviligeUtilsTest.testPriviligeToPolicy():" + "privilige=" 
                + privilige);
        Policy policy = PriviligeUtils.priviligeToPolicy(privilige);
        
        UnittestLog.logMessage(
                "PriviligeUtilsTest.testPriviligeToPolicy():" + "policy="
                + policy.toXML());
         

    }

    private Rule createRule(String ruleName) throws PolicyException {
        Map<String, Set<String>> actionMap = new HashMap<String, Set<String>>();
        Set<String> getValues = new HashSet<String>();
        getValues.add("allow");
        actionMap.put("GET", getValues);
        Set<String> postValues = new HashSet<String>();
        postValues.add("deny");
        actionMap.put("POST", postValues);
        return new Rule(ruleName,
                "iPlanetAMWebAgentService",
                "http://sample.com/" + ruleName,
                actionMap);
    }

    private Subject createUsersSubject(
            PolicyManager pm,
            String... userNames) throws PolicyException {
        SubjectTypeManager mgr = pm.getSubjectTypeManager();
        Subject subject = mgr.getSubject("AMIdentitySubject");
        Set<String> values = new HashSet<String>();
        for (String value : userNames) {
            String uuid = "id=" + value + ",ou=user," + ServiceManager.getBaseDN();
            values.add(uuid);
        }
        subject.setValues(values);
        return subject;
    }

    private void createUsers(
            SSOToken adminToken,
            String... names)
            throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
                adminToken, "/");
        for (String name : names) {
            Map attrMap = new HashMap();

            Set cnVals = new HashSet();
            cnVals.add(name);
            attrMap.put("cn", cnVals);

            Set snVals = new HashSet();
            snVals.add(name);
            attrMap.put("sn", snVals);

            Set nameVals = new HashSet();
            nameVals.add(name);
            attrMap.put("givenname", nameVals);

            Set passworVals = new HashSet();
            passworVals.add(name);
            attrMap.put("userpassword", passworVals);

            amir.createIdentity(IdType.USER, name, attrMap);
        }
    }

    private void deleteUsers(SSOToken adminToken,
            String... names)
            throws IdRepoException, SSOException {
        AMIdentityRepository amir = new AMIdentityRepository(
                adminToken, "/");
        Set identities = new HashSet();
        for (String name : names) {
            String uuid = "id=" + name + ",ou=user," + ServiceManager.getBaseDN();
            identities.add(IdUtils.getIdentity(adminToken, uuid));
        }
        amir.deleteIdentities(identities);
    }

    public static void main(String[] args) throws Exception {
        new PriviligeUtilsTest().testPriviligeToPolicy();
        UnittestLog.flush(new Date().toString());
    }

}
