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
 * $Id: LDAPStoreWizardPage.java,v 1.3 2008-01-15 19:58:59 jefberpe Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.config.wizard;

import com.sun.identity.config.pojos.LDAPStore;
import com.sun.identity.config.util.AjaxPage;
import net.sf.click.control.ActionLink;

/**
 * @author Les Hazlewood
 */
public class LDAPStoreWizardPage extends AjaxPage {

    public LDAPStore store = null;

    public ActionLink clearLink = new ActionLink( "clearStore", this, "clearStore" );
    public ActionLink checkNameLink = new ActionLink( "checkName", this, "checkName" );
    public ActionLink checkHostLink = new ActionLink( "checkServer", this, "checkServer" );
    public ActionLink checkBaseDNLink = new ActionLink( "checkBaseDN", this, "checkBaseDN" );
    public ActionLink checkLoginIdLink = new ActionLink( "checkLoginId", this, "checkLoginId" );

    private String type = "config";
    private String typeTitle = "Configuration";
    private String storeSessionName = "customConfigStore";
    private int pageNum = 3;

    public LDAPStoreWizardPage() {
    }

    public String getType() {
        return type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public String getTypeTitle() {
        return typeTitle;
    }

    public void setTypeTitle( String typeTitle ) {
        this.typeTitle = typeTitle;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum( int pageNum ) {
        this.pageNum = pageNum;
    }

    public String getStoreSessionName() {
        return storeSessionName;
    }

    public void setStoreSessionName( String storeSessionName ) {
        this.storeSessionName = storeSessionName;
    }

    public void onInit() {
        super.onInit();
        addModel( "type", getType() );
        addModel( "typeTitle", getTypeTitle() );
        addModel( "pageNum", Integer.valueOf( getPageNum() ) );
        store = getConfig();
        addModel( "usingCustomStore", Boolean.valueOf( store != null ) );

        store = ensureConfig();
        addModel( "store", store );
    }

    public boolean clearStore() {
        getContext().removeSessionAttribute( getStoreSessionName() );
        setPath( null );
        return false;
    }

    protected LDAPStore getConfig() {
        return (LDAPStore)getContext().getSessionAttribute( getStoreSessionName() );
    }

    protected LDAPStore ensureConfig() {
        LDAPStore store = getConfig();
        if ( store == null ) {
            store = new LDAPStore();
        }
        return store;
    }

    protected void save( LDAPStore config ) {
        getContext().setSessionAttribute( getStoreSessionName(), config );
    }

    public boolean checkName() {
        String storeName = toString( "name" );
        if ( storeName != null ) {
            LDAPStore config = ensureConfig();
            config.setName( storeName );
            save( config );
            writeToResponse( "true" );
        } else {
            writeToResponse( "Please specify a name." );
        }
        //ajax response - rendered directly - prevent click from rendering a velocity template:
        setPath( null );
        return false;
    }

    public boolean checkServer() {
        String host = toString( "host" );
        int port = toInt( "port" );
        boolean portSecure = toBoolean( "securePort" );

        if ( host == null ) {
            writeToResponse( "Please specify a host." );
        } else if ( port > 65535 ) {
            writeToResponse( "Please use a port less than or equal to 65535" );
        } else {
            try {
                LDAPStore store = ensureConfig();
                store.setHostName( host );
                store.setHostPort( port );
                store.setHostPortSecure( portSecure );

                getConfigurator().testHost( store );
                save( store );
                writeToResponse( "true" );
            } catch ( Exception e ) {
                writeToResponse( e.getMessage() );
            }
        }

        setPath( null );
        return false;
    }

    public boolean checkBaseDN() {
        String baseDN = toString( "baseDN" );
        if ( baseDN == null ) {
            writeToResponse( "Please specify a Base DN." );
        } else {
            LDAPStore store = ensureConfig();
            store.setBaseDN( baseDN );
            try {
                getConfigurator().testBaseDN( store );
                save( store );
                writeToResponse( "true" );
            } catch ( Exception e ) {
                writeToResponse( e.getMessage() );
            }
        }

        setPath( null );
        return false;
    }

    public boolean checkLoginId() {
        String loginId = toString( "loginId" );
        String password = toString( "password" );
        if ( loginId == null ) {
            writeToResponse( "Please specify a login ID" );
        } else if ( password == null ) {
            writeToResponse( "Please specify a password." );
        } else {
            LDAPStore store = ensureConfig();
            store.setUsername( loginId );
            store.setPassword( password );

            try {
                getConfigurator().testLoginId( store );
                save( store );
                writeToResponse( "true" );
            } catch ( Exception e ) {
                writeToResponse( e.getMessage() );
            }
        }
        setPath( null );
        return false;
    }
}
