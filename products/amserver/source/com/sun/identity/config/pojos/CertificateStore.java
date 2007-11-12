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
 * $Id: CertificateStore.java,v 1.3 2007-11-12 14:51:15 lhazlewood Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.config.pojos;

/**
 * @author Jeffrey Bermudez
 */
public class CertificateStore extends AuthenticationStore {

    private String userId;
    private boolean checkAgainstLDAP;
    private boolean checkAgainstCRL;
    private String searchAttribute;
    private boolean checkAgainstOSCP;
    private LDAPStore userStore = new LDAPStore();


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isCheckAgainstLDAP() {
        return checkAgainstLDAP;
    }

    public void setCheckAgainstLDAP(boolean checkAgainstLDAP) {
        this.checkAgainstLDAP = checkAgainstLDAP;
    }

    public boolean isCheckAgainstCRL() {
        return checkAgainstCRL;
    }

    public void setCheckAgainstCRL(boolean checkAgainstCRL) {
        this.checkAgainstCRL = checkAgainstCRL;
    }

    public String getSearchAttribute() {
        return searchAttribute;
    }

    public void setSearchAttribute(String searchAttribute) {
        this.searchAttribute = searchAttribute;
    }

    public boolean isCheckAgainstOSCP() {
        return checkAgainstOSCP;
    }

    public void setCheckAgainstOSCP(boolean checkAgainstOSCP) {
        this.checkAgainstOSCP = checkAgainstOSCP;
    }

    public LDAPStore getUserStore() {
        return userStore;
    }

    public void setUserStore(LDAPStore userStore) {
        this.userStore = userStore;
    }
    
}
