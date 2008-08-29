/**
 * $Id: SafeWordPrincipal.java,v 1.1 2008-08-29 22:06:42 kevinserwin Exp $
 * Copyright � 2005 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this document.
 * In particular, and without limitation, these intellectual property rights
 * may include one or more of the U.S. patents listed at
 * http://www.sun.com/patents and one or more additional patents or pending
 * patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software.  Government users are subject
 * to the Sun Microsystems, Inc. standard license agreement and applicable
 * provisions of the FAR and its supplements.
 *
 * Use is subject to license terms.
 *
 * This distribution may include materials developed by third parties.Sun,
 * Sun Microsystems and  the Sun logo are trademarks or registered trademarks
 * of Sun Microsystems, Inc. in the U.S. and other countries.  
 *
 * Copyright � 2005 Sun Microsystems, Inc. Tous droits r�serv�s.
 * Sun Microsystems, Inc. d�tient les droits de propri�t� intellectuels relatifs
 * � la technologie incorpor�e dans le produit qui est d�crit dans ce document.
 * En particulier, et ce sans limitation, ces droits de propri�t�
 * intellectuelle peuvent inclure un ou plus des brevets am�ricains list�s
 * � l'adresse http://www.sun.com/patents et un ou les brevets suppl�mentaires
 * ou les applications de brevet en attente aux Etats - Unis et dans les
 * autres pays.
 *
 * L'utilisation est soumise aux termes du contrat de licence.
 *
 * Cette distribution peut comprendre des composants d�velopp�s par des
 * tierces parties.
 *
 * Sun,  Sun Microsystems et  le logo Sun sont des marques de fabrique ou des
 * marques d�pos�es de Sun Microsystems, Inc. aux Etats-Unis et dans
 * d'autres pays.
 */


package com.sun.identity.authentication.modules.safeword;

import java.security.Principal;


/**
 * <p> This class implements the <code>Principal</code> interface
 * and represents an SafeWord user.
 *
 * <p> Principals such as this <code>SafeWordPrincipal</code>
 * may be associated with a particular <code>Subject</code>
 * to augment that <code>Subject</code> with an additional
 * identity.  Refer to the <code>Subject</code> class for more information
 * on how to achieve this.  Authorization decisions can then be based upon 
 * the Principals associated with a <code>Subject</code>.
 */
public class SafeWordPrincipal implements Principal, java.io.Serializable {

    /**
     * @serial
     */
    private String name;

    /**
     * Create a SafeWordPrincipal with a SafeWord username.
     *
     * <p>
     *
     * @param name the SafeWord username for this user.
     *
     * @exception NullPointerException if the <code>name</code>
     *                                 is <code>null</code>.
     */
    public SafeWordPrincipal(String name) {
        if (name == null) {
	    throw new NullPointerException("illegal null input");
        }

	this.name = name;
    }

    /**
     * Return the SafeWord username for this 
     * <code>SafeWordPrincipal</code>.
     *
     * <p>
     *
     * @return the SafeWord username for this 
     *         <code>SafeWordPrincipal</code>
     */
    public String getName() {
	return name;
    }

    /**
     * Return a string representation of this 
     * <code>SafeWordPrincipal</code>.
     *
     * <p>
     *
     * @return a string representation of this
     *         <code>SafeWordPrincipal</code>.
     */
    public String toString() {
	return("SafeWordPrincipal: " + name);
    }

    /**
     * Compares the specified Object with this <code>SafeWordPrincipal
     * </code> for equality.  Returns true if the given object is also a
     * <code>SafeWordPrincipal</code> and the two SafeWordPrincipals
     * have the same username.
     *
     * <p>
     *
     * @param o Object to be compared for equality with this
     *          <code>SafeWordPrincipal</code>.
     *
     * @return true if the specified Object is equal equal to this
     *         <code>SafeWordPrincipal</code>.
     */
    public boolean equals(Object o) {
        if (o == null) {
	    return false;
        }

        if (this == o) {
            return true;
        }
 
        if (!(o instanceof SafeWordPrincipal)) {
            return false;
        }
        
        SafeWordPrincipal that = (SafeWordPrincipal)o;

        if (this.getName().equals(that.getName())) {
	    return true;
        }
        
	return false;
    }
 
    /**
     * Return a hash code for this <code>SafeWordPrincipal</code>.
     *
     * <p>
     *
     * @return a hash code for this <code>SafeWordPrincipal</code>.
     */
    public int hashCode() {
	return name.hashCode();
    }
    
}
