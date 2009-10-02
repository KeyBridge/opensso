/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
 * $Id: WspCreateWizardBean.java,v 1.2 2009-10-02 16:10:55 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.io.Serializable;

public class WspCreateWizardBean
        extends WssWizardBean
        implements Serializable
{
    
    private RealmSummary realmSummary;
    
    public WspCreateWizardBean() {
        super();
        initialize();
    }

    @Override
    public void reset() {
        super.reset();
        initialize();
    }

    private void initialize() {
        this.setRealmSummary(new RealmSummary());
    }

    // Getters / Setters -------------------------------------------------------

    public void setRealmSummary(RealmSummary realmSummary) {
        this.realmSummary = realmSummary;
    }

    public RealmSummary getRealmSummary() {
        return realmSummary;
    }
}
