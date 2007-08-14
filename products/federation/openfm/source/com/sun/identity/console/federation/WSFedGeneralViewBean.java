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
 * "Portions Copyrighted [year] [name of copyright owner]
 *
 * $Id: WSFedGeneralViewBean.java,v 1.4 2007-08-14 21:54:34 babysunil Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.model.WSFedPropertiesModel;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.Iterator;

public class WSFedGeneralViewBean extends WSFedGeneralBase {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/WSFedGeneral.jsp";
    
    public WSFedGeneralViewBean() {
        super("WSFedGeneral");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        
        //setting the Name fields
        setDisplayFieldValue(WSFedPropertiesModel.TF_REALM, realm);
        setDisplayFieldValue(WSFedPropertiesModel.TF_NAME, entityName);
        setDisplayFieldValue(WSFedPropertiesModel.TF_DISPNAME, entityName);
        
        try {
            WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
            FederationElement fedElement =
                    model.getEntityDesc(realm, entityName);
            
            //setting the Token Issuer Name
            setDisplayFieldValue(WSFedPropertiesModel.TFTOKENISSUER_NAME,
                    model.getTokenName(fedElement));
            
            //setting the Token Issuer End Point
            setDisplayFieldValue(WSFedPropertiesModel.TFTOKENISSUER_ENDPT,
                    model.getTokenEndpoint(fedElement));
        } catch (AMConsoleException e) {
            debug.error("WSFedGeneralViewBean.beginDisplay", e);
        }
    }
    
    protected void createPropertyModel() {
        psModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertyWSFedGeneralView.xml"));
        psModel.clear();
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        
        // get the entity name, realm, and location...
        retrieveCommonProperties();
        try {
            WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
            AMPropertySheet ps =
                (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            //TBD- save of 'Display Name' and 'Description' to be added later 
            //when backend api available
            
            //genValues has the values displayed in the General property page
            Map genValues =
                ps.getAttributeValues(model.getGenDataMap(), false, model);
            
            //save for the general attributes
            model.setGenAttributeValues(realm, entityName, genValues);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "wsfed.general.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }
}
