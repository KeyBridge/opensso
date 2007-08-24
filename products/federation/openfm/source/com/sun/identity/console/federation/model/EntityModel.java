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
 * $Id: EntityModel.java,v 1.3 2007-08-24 18:17:11 asyhuang Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Set;
import java.util.Map;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/* - NEED NOT LOG - */

public interface EntityModel 
    extends AMModel 
{
    public static final String LOCATION = "location";
    public static final String ROLE = "role";
    public static final String PROTOCOL = "protocol";
    public static final String REALM = "realm";
    public static final String WSFED = "WSFed";
    public static final String SAMLV2 = "SAMLv2";
    public static final String IDFF = "IDFF";
        
    public Map getEntities() throws AMConsoleException;
    public void deleteEntities(Map entities) throws AMConsoleException;
    public void createEntity(Map data) throws AMConsoleException;
    public List getTabMenu(String protocol, String name, String realm);
    public boolean isAffiliate(String name) throws AMConsoleException;
}
