/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PrivilegeManager.java,v 1.11 2009-05-05 16:28:05 veiming Exp $
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.IPolicyDataStore;
import com.sun.identity.entitlement.util.DebugFactory;
import com.sun.identity.entitlement.util.PrivilegeSearchFilter;
import com.sun.identity.shared.debug.IDebug;
import java.security.Principal;
import java.util.Date;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * Class to manage entitlement privileges: to add, remove, modify privilege
 */
public abstract class PrivilegeManager {
    /**
     * Debug for Policy Administration Point classes
     */
    public static IDebug debug = DebugFactory.getDebug("Entitlement");

    private Subject adminSubject;

    /**
     * Returns instance of configured <code>PrivilegeManager</code>
     * @param subject subject that would be used for the privilege management 
     * operations
     * @return instance of configured <code>PrivilegeManager</code>
     */
    static public PrivilegeManager getInstance(Subject subject) {
        PrivilegeManager pm = null;
        try {
            //TODO: read the class name from configuration
            Class clazz = Class.forName(
                "com.sun.identity.entitlement.opensso.PolicyPrivilegeManager");
            pm = (PrivilegeManager)clazz.newInstance();
            pm.initialize(subject);
        } catch (ClassNotFoundException e) {
            debug.error("PrivilegeManager.getInstance", e);
        } catch (InstantiationException e) {
            debug.error("PrivilegeManager.getInstance", e);
        } catch (IllegalAccessException e) {
            debug.error("PrivilegeManager.getInstance", e);
        }
        return pm;
    }

    /**
     * Constructor.
     */
    public PrivilegeManager() {
    }

    /**
     * Initializes the object
     * @param subject subject to initilialize the privilege manager with
     */
    public void initialize(Subject subject) {
        this.adminSubject = subject;
    }

    /**
     * Returns a privilege.
     *
     * @param privilegeName name for the privilege to be returned
     * @throws EntitlementException if privilege is not found.
     */
    public abstract Privilege getPrivilege(String privilegeName)
            throws EntitlementException;

    /**
     * Adds a privilege.
     *
     * @param privilege privilege to be added
     * @throws EntitlementException if the privilege could not be added
     */
    public void addPrivilege(Privilege privilege)
        throws EntitlementException {
        Date date = new Date();
        privilege.setCreationDate(date.getTime());
        privilege.setLastModifiedDate(date.getTime());

        Set<Principal> principals = adminSubject.getPrincipals();
        String principalName = ((principals != null) && !principals.isEmpty()) ?
            principals.iterator().next().getName() : null;

        if (principalName != null) {
            privilege.setCreatedBy(principalName);
            privilege.setLastModifiedBy(principalName);
        }
    }

    /**
     * Removes a privilege.
     *
     * @param privilegeName name of the privilege to be removed
     * @throws EntitlementException if privilege cannot be removed.
     */
    public void removePrivilege(String privilegeName)
        throws EntitlementException {
    }

    /**
     * Modifies a privilege.
     *
     * @param privilege the privilege to be modified
     * @throws EntitlementException if privilege cannot be modified.
     */
    public void modifyPrivilege(Privilege privilege)
        throws EntitlementException {
        Privilege origPrivilege = getPrivilege(privilege.getName());
        if (origPrivilege != null) {
            privilege.setCreatedBy(origPrivilege.getCreatedBy());
            privilege.setCreationDate(origPrivilege.getCreationDate());
        }
        Date date = new Date();
        privilege.setLastModifiedDate(date.getTime());

        Set<Principal> principals = adminSubject.getPrincipals();
        if ((principals != null) && !principals.isEmpty()) {
            privilege.setLastModifiedBy(principals.iterator().next().getName());
        }
    }

    /**
     * Returns privilege names.
     *
     * @return privilege names.
     * @throws EntitlementException if there are errors obtaining privilege
     *         names.
     */
    public abstract Set<String> getPrivilegeNames() throws EntitlementException;


    /**
     * Returns a set of privilege names for a given search criteria.
     *
     * @param filter Set of search filter.
     * @param boolAnd <code>true</code> for AND-ing the search filter.
     * @return a set of privilege names for a given search criteria.
     * @throws EntitlementException if search failed.
     */
    public Set<String> searchPrivilegeNames(
        String realm,
        Set<PrivilegeSearchFilter> filter,
        boolean boolAnd
    ) throws EntitlementException {
        IPolicyDataStore datastore =
            PolicyDataStoreFactory.getInstance().getDataStore();
        return datastore.searchPrivilegeNames(
            realm, filter, boolAnd, 0, false, false);//TOFIX
    }

    /**
     * Returns privilege names matching the pattern.
     *
     * @param pattern pattern to match the privilege names.
     * @return privilege names matching the pattern.
     * @throws EntitlementException if there are errors obtaining privilege
     *         names.
     */
    public abstract Set<String> getPrivilegeNames(String pattern)
        throws EntitlementException;


    /**
     * Returns the XML representation of this privilege.
     *
     * @param name Name of Privilege.
     * @return XML representation of this privilege.
     * @throws EntitlementException if privilege is not found, or cannot
     * be obtained.
     */
    public abstract String getPrivilegeXML(String name)
        throws EntitlementException;
}
