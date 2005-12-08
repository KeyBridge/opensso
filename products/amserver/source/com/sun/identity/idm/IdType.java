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
 * $Id: IdType.java,v 1.2 2005-12-08 01:16:43 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.idm;

import java.util.Collections;
import java.util.Set;

/**
 * The class <code>IdType</code> defines the types of supported identities,
 * and provides static constants for these identities. Currently defined
 * identities are <code>IdType.USER</code>, <code>
 * IdType.ROLE</code>,
 * <code>IdType.GROUP</code> and <code> IdType.AGENT
 * </code>. The usage of
 * the respective types are defined along with their declaration.
 *
 * @supported.all.api
 */
public class IdType {
    private String idType;

    protected IdType(String type) {
        idType = type;
    }

    /**
     * Identity type of USER
     */
    public static final IdType USER = new IdType("user");

    /**
     * Identity type of ROLE
     */
    public static final IdType ROLE = new IdType("role");

    /**
     * Identity type of GROUP
     */
    public static final IdType GROUP = new IdType("group");

    /**
     * Identity type of AGENT
     */
    public static final IdType AGENT = new IdType("agent");

    /**
     * Identity type of filter role.
     */
    public static final IdType FILTEREDROLE = new IdType("filteredrole");

    public static final IdType REALM = new IdType("realm");

    public boolean equals(Object type) {
        if (type instanceof IdType) {
            return (((IdType) type).idType.equalsIgnoreCase(this.idType));
        }
        return (false);
    }

    public String toString() {
        return ("IdType: " + idType);
    }

    /**
     * Returns the hash code of the object
     */
    public int hashCode() {
        return idType.hashCode();
    }

    /**
     * Returns the name of this type, for example <code> user </code> for type
     * User.
     * 
     * @return Name of the this type.
     */
    public String getName() {
        return idType;
    }

    /**
     * Returns a set of types of identities this type can hav as its' members.
     * 
     * @return Set of <code>IdType</code> which can be members of this
     *         identity type.
     */
    public Set canHaveMembers() {
        Set results = (Set) IdUtils.typesCanHaveMembers.get(getName());
        return (results == null) ? Collections.EMPTY_SET : results;
    }

    /**
     * Returns a set of types of identities that this type can be a member of.
     * 
     * @return Set of <code>IdType</code>.
     */
    public Set canBeMemberOf() {
        Set results = (Set) IdUtils.typesCanBeMemberOf.get(getName());
        return (results == null) ? Collections.EMPTY_SET : results;
    }

    /**
     * Returns a set of types of identities that this type can add as members.
     * 
     * @return Set of <code>IdType</code>.
     */
    public Set canAddMembers() {
        Set results = (Set) IdUtils.typesCanAddMembers.get(getName());
        return (results == null) ? Collections.EMPTY_SET : results;
    }
}
