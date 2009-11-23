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
 * $Id: WscCreateProfileNameSummary.java,v 1.3 2009-11-23 19:54:06 ggennaro Exp $
 */

package com.sun.identity.admin.model;

import java.util.regex.Matcher;

import com.sun.identity.admin.Resources;

public class WscCreateProfileNameSummary extends WscCreateWizardSummary {

    public WscCreateProfileNameSummary(WscCreateWizardBean wizardBean) {
        super(wizardBean);
    }

    @Override
    public String getLabel() {
        Resources r = new Resources();
        String label = r.getString(this, "label");
        return label;
    }

    @Override
    public String getValue() {
        WscProfileBean wsc = getWscCreateWizardBean().getWscProfileBean();
        Resources r = new Resources();
        String value = r.getString(this, "valueFormat");

        value = value.replaceAll("\\{0\\}", 
                    Matcher.quoteReplacement(wsc.getProfileName()));
        value = value.replaceAll("\\{1\\}", 
                    Matcher.quoteReplacement(wsc.getEndPoint()));

        return value;
    }

    @Override
    public String getTemplate() {
        return null;
    }

    @Override
    public String getIcon() {
        return "../image/edit.png";
    }

    @Override
    public boolean isExpandable() {
        return false;
    }

    @Override
    public int getGotoStep() {
        return WscCreateWizardStep.WSC_PROFILE.toInt();
    }

}
