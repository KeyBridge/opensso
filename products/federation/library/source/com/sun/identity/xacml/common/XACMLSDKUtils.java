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
 * $Id: XACMLSDKUtils.java,v 1.1 2007-08-29 23:39:28 dillidorai Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */


package com.sun.identity.xacml.common;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Decision;
import java.security.SecureRandom;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import org.w3c.dom.Element;

/**
 * The <code>XACMLSDKUtils</code> contains utility methods for XACML 2.0
 * implementation.
 *
 * @supported.all.api
 */
public class XACMLSDKUtils extends SAML2SDKUtils {
    //
    // This utility class will be run on client side as well,
    // so DO NOT add any static block which will not run on client side.
    //

    // The deugging instance
    public static Debug debug = Debug.getInstance("libXACML");
    
    /**
     * Defines mapping between interface and implementation class,
     * the properties are read from AMConfig.properties in following format:
     * com.sun.identity.xacml.sdk.mapping.<interface>=<implementation_class>
     * e.g.
     * com.sun.identity.xacml.sdk.mapping.Assertion=com.xxx.xacml.RequestImpL
     */
    private static Map classMapping = new HashMap();

    /**
     * Constants class
     */
    private static XACMLConstants xc;

    /**
     * List of Interfaces in context package which could have 
     * customized implementation
     */
    private static String[] interfaceNames = {
        xc.REQUEST, xc.SUBJECT, xc.RESOURCE, xc.ACTION, xc.ATTRIBUTE, 
        xc.ATTRIBUTE_VALUE, xc.RESOURCE_CONTENT, xc.XACMLAUTHZDECISIONQUERY };

    static {
        // initialize class mapper
        int len = interfaceNames.length;
        for (int i = 0; i < len; i++) {
            String iName = interfaceNames[i];
            try {
                String implClass = SystemPropertiesManager.get(
                    XACMLConstants.SDK_CLASS_MAPPING + iName);
                if (implClass != null && implClass.trim().length() != 0) {
                    // try it out
                    if (debug.messageEnabled()) {
                        debug.message("XACMLSDKUtils.init: mapper for " + iName
                            + "=" + implClass);
                    }
                    classMapping.put(iName, Class.forName(implClass.trim()));
                }
            } catch (ClassNotFoundException cnfe) {
                debug.error("XACMLSDKUtils.init: " + iName, cnfe);
            } 
        }
    }
    
    /**
     * Protected contstructor.
     */
    protected XACMLSDKUtils() {}
   

    
    public static Attribute createAttribute(List values, URI attributeId, 
            URI dataType, String issuer) throws XACMLException
    {
         ContextFactory factory = ContextFactory.getInstance();
         Attribute attr = null;
         attr = factory.getInstance().createAttribute();
         attr.setAttributeID(attributeId);
         attr.setDataType(dataType);
         attr.setAttributeValues(values);;
         attr.setIssuer(issuer);
         return attr;
    }

    public static boolean isValidDecision(Decision decision) {
        boolean valid = false;
        if (decision != null) {
            String value = decision.getValue();
            valid = isValidDecision(value);
        }
        return valid;
    }

    public static boolean isValidDecision(String value) {
        boolean valid = false;
        if (value != null) {
            if (value.equals(XACMLConstants.PERMIT) 
                    || value.equals(XACMLConstants.DENY)
                    || value.equals(XACMLConstants.INDETERMINATE)
                    || value.equals(XACMLConstants.NOT_APPLICABLE)) {
                valid = true;
            }
        }
        return valid;
    }

    public static boolean isValidStatusMessage(String value) {
        boolean valid = true;
        //FIXME: add validation
        return valid;
    }

    public static boolean isValidStatusCode(String value) {
        boolean valid = true;
        //FIXME: add validation
        return valid;
    }

    public static boolean isValidMinorStatusCode(String value) {
        boolean valid = true;
        //FIXME: add validation
        return valid;
    }

    public static String quote(String s) {
        String val = null;
        if (s == null) {
            val = "\"\"";
        } else {
            val = "\"" + s + "\"";
        }
        return val;
    }

}
