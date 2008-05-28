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
 * $Id: SAMLv2SPServicesViewBean.java,v 1.2 2008-05-28 22:02:09 babysunil Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPropertySheet; 
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.SAMLv2AuthContexts;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.List;

public class SAMLv2SPServicesViewBean extends SAMLv2Base {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/SAMLv2SPServices.jsp";

    public SAMLv2SPServicesViewBean() {
        super("SAMLv2SPServices");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
   
  public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
        ps.init();
        SAMLv2Model model = (SAMLv2Model)getModel();
        ps.setAttributeValues(getStandardValues(), model);
        
        //only metalias from ext
        try {
            setDisplayFieldValue(model.SP_META_ALIAS,
            model.getMetaalias(realm, entityName, EntityModel.SERVICE_PROVIDER));
        } catch (AMConsoleException e){                
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            } 
    }
         
    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                    getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertySAMLv2SPServicesHosted.xml"));
        } else {
            psModel = new AMPropertySheetModel(
                    getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertySAMLv2SPServicesRemote.xml"));
        }
        psModel.clear();
    }
    
  public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        try {
            
            SAMLv2Model model = (SAMLv2Model)getModel();
            AMPropertySheet ps =
                    (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            //retrieve the standard metadata values from the property sheet
            Map spStdValues = ps.getAttributeValues(
                    model.getStandardServiceProviderAttributes(
                    realm, entityName), false, model);
            
            //save the standard metadata values for the Idp
            model.setSPStdAttributeValues(realm, 
                entityName, spStdValues, null, location);
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "samlv2.sp.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        forwardTo();
    }
  
  private Map getStandardValues() {
        Map map = new HashMap();
        try {
            
            //gets standard metadata values
            SAMLv2Model model = (SAMLv2Model)getModel();
            map = model.getStandardServiceProviderAttributes(
                    realm, entityName);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return map;
    }
   
}
