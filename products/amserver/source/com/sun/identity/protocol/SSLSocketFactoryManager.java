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
 * $Id: SSLSocketFactoryManager.java,v 1.1 2007-10-25 05:51:03 beomsuk Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.protocol;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


import com.sun.identity.security.keystore.AMX509KeyManager;
import com.sun.identity.security.keystore.AMX509TrustManager;
import com.sun.identity.security.SecurityDebug;

/**
 * Generate SSLContext can be used to create an SSL socket connection 
 * to ssl enabled server.
 * It is using the JSSE package. 
 */

public class SSLSocketFactoryManager {
    static private String keyStore = null;
    static private KeyManager[] amKeyMgr = null;
    static private TrustManager[] amTrustMgr = null;
    static private SSLContext ctx = null;
	
    static {
        keyStore = System.getProperty("javax.net.ssl.keyStore", null);

    	try {
    	    if (keyStore != null) {
        	amKeyMgr = new KeyManager[] { new AMX509KeyManager() };
    	    }
    	    
    	    amTrustMgr = new TrustManager[] { new AMX509TrustManager() };
	
    	    ctx = SSLContext.getInstance("SSL");
    	    ctx.init(amKeyMgr, amTrustMgr, null);
	} catch (Exception e) {
	    SecurityDebug.debug.error(
                "Exception in SSLSocketFactoryManager.init()" + e.toString());
	}
    }

    static public SSLSocketFactory getSocketFactory() {
	SSLSocketFactory sf = null;
	if (ctx != null) {
            sf = ctx.getSocketFactory();
	}
        return sf;
    }
}
