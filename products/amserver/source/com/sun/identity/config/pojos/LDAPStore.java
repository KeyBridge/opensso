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
 * $Id: LDAPStore.java,v 1.5 2008-02-21 22:35:44 jonnelson Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.identity.config.pojos;

import java.io.Serializable;

/**
 * @author Les Hazlewood
 */
public class LDAPStore implements Serializable {

    private String name;
    private String hostName;
    private int hostPort;
    private boolean hostPortSecure = false;
    private String baseDN;
    private String username;
    private String password;

    public LDAPStore(){}

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName( String hostName ) {
        this.hostName = hostName;
    }

    public int getHostPort() {
        return hostPort;
    }

    public void setHostPort( int hostPort ) {
        this.hostPort = hostPort;
    }

    public boolean isHostPortSecure() {
        return hostPortSecure;
    }

    public void setHostPortSecure( boolean hostPortSecure ) {
        this.hostPortSecure = hostPortSecure;
    }

    public String getBaseDN() {
        return baseDN;
    }

    public void setBaseDN( String baseDN ) {
        this.baseDN = baseDN;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername( String username ) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }
    
}
