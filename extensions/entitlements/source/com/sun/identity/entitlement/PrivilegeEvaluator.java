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
 * $Id: PrivilegeEvaluator.java,v 1.16 2009-05-19 23:50:14 veiming Exp $
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.IThreadPool;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.security.auth.Subject;

/**
 * This class evaluates entitlements of a subject for a given resource
 * and a environment paramaters.
 */
class PrivilegeEvaluator {
    private Subject adminSubject;
    private Subject subject;
    private String applicationName;
    private String resourceName;
    private Map<String, Set<String>> envParameters;
    private ResourceSearchIndexes indexes;
    private List<List<Entitlement>> resultQ = new
        LinkedList<List<Entitlement>>();
    private Application application;
    private Set<String> actionNames;
    private EntitlementCombiner entitlementCombiner;
    private boolean recursive;
    private IThreadPool threadPool;
    private EntitlementException eException;
    private final Lock lock = new ReentrantLock();
    private Condition hasResults = lock.newCondition();

    /**
     * Initializes the evaluator.
     *
     * @param adminSubject Administrator subject which is used for evcaluation.
     * @param subject Subject to be evaluated.
     * @param applicationName Application Name.
     * @param resourceName Rsource name.
     * @param actions Action names.
     * @param envParameters Environment parameters.
     * @param recursive <code>true</code> for sub tree evaluation
     * @throws com.sun.identity.entitlement.EntitlementException if
     * initialization fails.
     */
    private void init(
        Subject adminSubject,
        Subject subject,
        String applicationName,
        String resourceName,
        Set<String> actions,
        Map<String, Set<String>> envParameters,
        boolean recursive
    ) throws EntitlementException {
        this.adminSubject = adminSubject;
        this.subject = subject;
        this.applicationName = applicationName;
        this.resourceName = resourceName;
        this.envParameters = envParameters;
        
        this.actionNames = new HashSet<String>();
        if ((actions == null) || actions.isEmpty()) {
            this.actionNames.addAll(getApplication().getActions().keySet());
        } else {
            this.actionNames.addAll(actions);
        }

        entitlementCombiner = getApplication().getEntitlementCombiner();
        entitlementCombiner.init(applicationName, resourceName,
            this.actionNames, recursive);
        this.recursive = recursive;
        threadPool = new EntitlementThreadPool(); //TOFIX
    }

    /**
     * Returrns <code>true</code> if the subject has privilege to have the
     * given entitlement.
     *
     * @param adminSubject Administrator subject which is used for evcaluation.
     * @param subject Subject to be evaluated.
     * @param applicationName Application Name.
     * @param entitlement Entitlement to be evaluated.
     * @param envParameters Environment parameters.
     * @return <code>true</code> if the subject has privilege to have the
     * given entitlement.
     * @throws com.sun.identity.entitlement.EntitlementException if
     * evaluation fails.
     */
    public boolean hasEntitlement(
        String realm,
        Subject adminSubject,
        Subject subject,
        String applicationName,
        Entitlement entitlement,
        Map<String, Set<String>> envParameters
    ) throws EntitlementException {
        init(adminSubject, subject, applicationName,
            entitlement.getResourceName(), 
            entitlement.getActionValues().keySet(), envParameters, false);

        indexes = entitlement.getResourceSearchIndexes();
        List<Entitlement> results = evaluate(realm);
        Entitlement result = results.get(0);
        for (String action : entitlement.getActionValues().keySet()) {
            Boolean b = result.getActionValue(action);
            if ((b == null) || !b.booleanValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returrns list of entitlements which is entitled to a subject.
     *
     * @param adminSubject Administrator subject which is used for evcaluation.
     * @param subject Subject to be evaluated.
     * @param applicationName Application Name.
     * @param resourceName Resource name.
     * @param envParameters Environment parameters.
     * @param recursive <code>true</code> for sub tree evaluation.
     * @return <code>true</code> if the subject has privilege to have the
     * given entitlement.
     * @throws com.sun.identity.entitlement.EntitlementException if
     * evaluation fails.
     */
    public List<Entitlement> evaluate(
        String realm,
        Subject adminSubject,
        Subject subject,
        String applicationName,
        String resourceName,
        Map<String, Set<String>> envParameters,
        boolean recursive
    ) throws EntitlementException {
        init(adminSubject, subject, applicationName,
            resourceName, null, envParameters, recursive);
        indexes = getApplication().getResourceSearchIndex(resourceName);
        return evaluate(realm);
    }

    private List<Entitlement> evaluate(String realm)
        throws EntitlementException {

        int totalCount = 0;
        PrivilegeIndexStore pis = PrivilegeIndexStore.getInstance(realm);
        Iterator<Privilege> i = pis.search(indexes,
            SubjectAttributesManager.getSubjectSearchFilter(
                subject, applicationName), recursive, threadPool);
        Set<Privilege> privileges = new HashSet<Privilege>(20);
        while (i.hasNext()) {
            privileges.add(i.next());
            totalCount++;
            if ((totalCount % 10) == 0) {
                threadPool.submit(new PrivilegeTask(this, privileges));
                privileges.clear();
            }
        }
        if (!privileges.isEmpty()) {
            threadPool.submit(new PrivilegeTask(this, privileges));
        }

        int counter = 0;
        lock.lock();
        boolean isDone = (eException != null);

        try {
            while (!isDone && (counter < totalCount)) {
                if (resultQ.isEmpty()) {
                    hasResults.await();
                }
                while (!resultQ.isEmpty() && !isDone) {
                    entitlementCombiner.add(resultQ.remove(0));
                    isDone = entitlementCombiner.isDone();
                    counter++;
                }
            }
        } catch (InterruptedException ex) {
            Evaluator.debug.error("PrivilegeEvaluator.evaluate", ex);
        } finally {
            lock.unlock();
        }

        if (eException != null) {
            throw eException;
        }

        return entitlementCombiner.getResults();
    }

    
    private Application getApplication() {
        if (application == null) {
            application = ApplicationManager.getApplication(
                "/", applicationName); //TOFIX: realm
        }
        return application;
    }

    class PrivilegeTask implements Runnable {
        final PrivilegeEvaluator parent;
        private Set<Privilege> privileges;

        PrivilegeTask(PrivilegeEvaluator parent, Set<Privilege> privileges) {
            this.parent = parent;
            this.privileges = new HashSet(privileges.size() *2);
            this.privileges.addAll(privileges);
        }

        public void run() {
            try {
                for (Privilege privilege : privileges) {
                    List<Entitlement> entitlements = privilege.evaluate(
                        parent.subject, parent.resourceName, parent.actionNames,
                        parent.envParameters, parent.recursive);

                    try {
                        parent.lock.lock();
                        parent.resultQ.add(entitlements);
                        parent.hasResults.signal();
                    } finally {
                        parent.lock.unlock();
                    }
                }
            } catch (EntitlementException ex) {
                try {
                    parent.lock.lock();
                    parent.eException = ex;
                    parent.hasResults.signal();
                } finally {
                    parent.lock.unlock();
                }
            }
        }
    }
}
