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
 * $Id: SCSAML2SOAPBindingRequestHandlerListDupViewBean.java,v 1.1 2008-01-15 01:33:19 asyhuang Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.identity.console.service.model.SCSAML2SOAPBindingModelImpl;
import java.util.Map;

public class SCSAML2SOAPBindingRequestHandlerListDupViewBean
    extends SCSAML2SOAPBindingRequestHandlerListAddViewBean {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCSAML2SOAPBindingRequestHandlerListDup.jsp";
    private int dupIndex = -1;
    
    public SCSAML2SOAPBindingRequestHandlerListDupViewBean() {
        super("SCSAML2SOAPBindingRequestHandlerListDup", DEFAULT_DISPLAY_URL);
    }
    
    protected String getPageTitleText() {
        return "soapBinding.service.requestHandlerList.duplicate.page.title";
    }
    
    void setDupIndex(int index) {
        dupIndex = index;
    }
    
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException 
    {
        super.beginDisplay(event);
        
        if (dupIndex != -1) {
            Map mapAttrs = (Map)getPageSessionAttribute(
                SCSAML2SOAPBindingViewBean.PROPERTY_ATTRIBUTE);
            OrderedSet set = (OrderedSet)mapAttrs.get(
                SCSAML2SOAPBindingModelImpl.ATTRIBUTE_NAME_REQUEST_HANDLER_LIST);
            setValues((String)set.get(dupIndex));
        }
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.webservices.soapbinding.requesthandlerlist.dup";
    }
    
    protected boolean startPageTrail() {
        return false;
    }
}
