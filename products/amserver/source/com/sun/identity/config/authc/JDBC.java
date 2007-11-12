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
 * $Id: JDBC.java,v 1.2 2007-11-12 14:51:10 lhazlewood Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.config.authc;

import com.sun.identity.config.pojos.JDBCStore;
import com.sun.identity.config.util.AjaxPage;

/**
 * @author Jeffrey Bermudez
 */
public class JDBC extends AjaxPage {

    public JDBCStore jdbcStore;


    public void onInit() {
        jdbcStore = new JDBCStore();
    }

    public void onPost() {
        jdbcStore.getRealm().setName(toString("realmName"));
        jdbcStore.setConnectionType(toString("connectionType"));
        jdbcStore.setStoreDetails(toString("storeDetails"));
        jdbcStore.setJdbcURL(toString("jdbcURL"));
        jdbcStore.setJndiName(toString("jndiName"));
        jdbcStore.setUsername(toString("username"));
        jdbcStore.setPassword(toString("password"));
        jdbcStore.setPasswordField(toString("passwordField"));
        jdbcStore.setRetrievalStatement(toString("retrievalStatement"));
        jdbcStore.setTransformationClass(toString("transformationClass"));

        save(jdbcStore);
    }

    protected void save(JDBCStore jdbcStore) {
        getConfigurator().addAuthenticationStore(jdbcStore);
    }

}