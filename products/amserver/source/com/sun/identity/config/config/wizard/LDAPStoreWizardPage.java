package com.sun.identity.config.config.wizard;

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
        addModel( "type", getType() );
        addModel( "typeTitle", getTypeTitle() );
        addModel( "pageNum", new Integer( getPageNum() ) );

        store = getConfig();
        addModel( "usingCustomStore", Boolean.valueOf( store != null) );

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
        } else if ( port < 1 || port > 65535 ) {
            writeToResponse( "Please use a port from 1 to 65535" );
        } else {
            try {
                //TODO - test host/port/secure here via backend call
                //for now, just return true always:
                LDAPStore store = ensureConfig();
                store.setHostName( host );
                store.setHostPort( port );
                store.setHostPortSecure( portSecure );
                save( store );
                writeToResponse( "true" );
            } catch ( Exception e ) {
                //TODO - set appropriate error message via writeToResponse
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
            //TODO - validate base DN via backend call
            //for now, just return true to simulate working functionality
            LDAPStore store = ensureConfig();
            store.setBaseDN( baseDN );
            save( store );
            writeToResponse( "true" );
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
            //TODO - validate account can login via backend call
            //for now, just return true to simulate working login
            LDAPStore store = ensureConfig();
            store.setUsername( loginId );
            store.setPassword( password );
            save( store );
            writeToResponse( "true" );
        }
        setPath( null );
        return false;
    }
}
