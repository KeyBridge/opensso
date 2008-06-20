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
 * $Id: SAML2TokenUtils.java,v 1.3 2008-06-20 20:42:36 mallas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.wss.security;

import org.w3c.dom.Element;
import java.util.List;
import java.security.cert.X509Certificate;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.NameID;
import java.security.Principal;

/**
 * This class provides utility classes for the SAML2 token support
 */
public class SAML2TokenUtils {
    
    /** Creates a new instance of SAML2TokenUtils */
    public SAML2TokenUtils() {
    }
    
    /**
     * Returns an <code>X509Certificate</code> from the security token if it 
     * is of type SAML2 token holder of key.      
     */
    public static X509Certificate getCertificate(SecurityToken securityToken)
             throws SecurityException {
        
        SAML2Token saml2Token = (SAML2Token)securityToken;
        if(saml2Token.isSenderVouches()) {
           return null;
        }
        Assertion assertion = saml2Token.getAssertion();
        Element keyInfo = getKeyInfo(assertion);
        return WSSUtils.getCertificate(keyInfo);
    }
    
    /**
     * Returns the <code>KeyInfo</code> element from the assertion.
     */
    public static Element getKeyInfo(Assertion assertion) {
        try {
            Subject subj = assertion.getSubject();
            List list = subj.getSubjectConfirmation();
            
            SubjectConfirmation subjConfirmation =
                           (SubjectConfirmation)list.get(0);
            List content =  subjConfirmation.getSubjectConfirmationData().
                    getContent();
            if(content == null || content.isEmpty()) {
               WSSUtils.debug.error("SAMLTokenUtils.getKeyInfo: " +
                       "KeyInfo not found");
               return null;
            }
            
            return (Element)content.get(0);

        } catch (Exception e) {
            WSSUtils.debug.error("SAML2TokenUtils.getKeyInfo Exception: ", e);
        }
        return null;        
        
    }
    
    /**
     * Validates Assertion and sets the principal into the container Subject.
     */
    public static boolean validateAssertion(Assertion assertion, 
            javax.security.auth.Subject subject) 
               throws SecurityException {

        if((assertion.getConditions() != null) &&
                  !(assertion.getConditions().checkDateValidity(
                    System.currentTimeMillis())) ) {
           if(WSSUtils.debug.messageEnabled()) {
              WSSUtils.debug.message("SAML2TokenUtils.validateAssertionToken::"
                      + " assertion time is not valid");
           }
           return false;
        }

        Subject sub = assertion.getSubject();
        if(sub == null) {
           if(WSSUtils.debug.messageEnabled()) {
              WSSUtils.debug.message("SAML2TokenUtils.validateAssertio:: " +
              "Assertion does not have subject");
           }
           return false;
        }

        NameID ni = sub.getNameID();
        if(ni == null) {
           return false;
        }

        Principal principal = new SecurityPrincipal(ni.getValue()); 
        subject.getPrincipals().add(principal);
        Element keyInfo = getKeyInfo(assertion);
        if(keyInfo != null) {
           X509Certificate cert = WSSUtils.getCertificate(keyInfo);
           subject.getPublicCredentials().add(cert);
        }
        WSSUtils.setRoles(subject, ni.getValue());
        return true;
    }
}
