/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SPFedSession.java,v 1.4 2008-06-25 05:47:55 qcheng Exp $
 *
 */


package com.sun.identity.saml2.profile;

import com.sun.identity.saml2.common.NameIDInfo;
import java.util.List; 
import java.util.ArrayList; 
import java.util.Iterator;
import com.sun.identity.saml2.common.SAML2Utils;

/**
 * This class provides the memory store for
 * SAML request and response information on Service Provider side.
 *
 */

public class SPFedSession {
    /**
     * <code>SessionIndex</code> from IDP.
     */
    public String idpSessionIndex = null;

    /**
     * SP side session ID.
     */
    public String spTokenID = null;

    /**
     * <code>NameIDInfo</code> for the session.
     */
    public NameIDInfo info = null;
    
    /**
     * Constructs new <code>SPFedSession</code> object
     *
     * @param idpSessionIndex sessionIndex of Identity Provider
     * @param sessionID session id of Service Provider
     * @param info NameIDInfo object.
     */
    public SPFedSession(String idpSessionIndex,
                        String sessionID,
                        NameIDInfo info)
    {
        this.idpSessionIndex = idpSessionIndex;
        this.spTokenID = sessionID;
        this.info = info;
    }
}
