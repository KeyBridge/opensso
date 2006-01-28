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
 * $Id: AMX509KeyManager.java,v 1.1 2006-01-28 09:28:37 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.security.keystore;

import java.io.File;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ResourceBundle;
import java.util.Locale;

import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import com.iplanet.am.util.AMResourceBundleCache;
import com.iplanet.am.util.Debug;
import com.sun.identity.security.SecurityDebug;

/**
 * The <code>WSX509KeyManager</code> class implements JSSE X509KeyManager
 * interface. This implementation is the same as JSSE default implementation
 * exception it will supply user-specified client certificate alias when
 * client authentication is on.
 */
public class AMX509KeyManager implements X509KeyManager {

    static final String bundleName = "amSecurity";
    static ResourceBundle bundle = null;
    static AMResourceBundleCache amCache = AMResourceBundleCache.getInstance(); 
    public static Debug debug = SecurityDebug.debug;
    static private String keyStoreType = 
                     System.getProperty("javax.net.ssl.keyStoreType", "JKS");
    static private String keyStoreFile = 
                     System.getProperty("javax.net.ssl.keyStore", null);
    static private String keyStoreProvider = 
                     System.getProperty("javax.net.ssl.keyStoreProvider", null);
    static private String certAlias = null;
    private X509KeyManager sunX509KeyManager = null;
    private KeyStore keyStore = null;
    KeyStore.Builder builder = null;
    KeyStore.CallbackHandlerProtection callback = null;
    private KeyManagerFactory kmf = null;
    String passwdPrompt = null;
  
    // create sunX509KeyManager
    //
    // for example:
    //     Create/load a keystore
    //     Get instance of a "SunX509" KeyManagerFactory "kmf"
    //     init the KeyManagerFactory with the keystore
    public AMX509KeyManager() {
        // initialize KeyStore and get KeyManagerFactory 
        try {
            bundle = amCache.getResBundle(bundleName, Locale.getDefault());
            passwdPrompt = bundle.getString("KeyStorePrompt");
            callback = new KeyStore.CallbackHandlerProtection
                                       (new AMCallbackHandler(passwdPrompt));
            if (keyStoreType.equalsIgnoreCase("JKS") 
                             || keyStoreType.equalsIgnoreCase("PKCS12")) {
                builder = KeyStore.Builder.newInstance(keyStoreType, 
                             Security.getProvider(keyStoreProvider), 
                             new File(keyStoreFile), callback);
            } else if (keyStoreType.equalsIgnoreCase("PKCS11")) {
                builder = KeyStore.Builder.newInstance(keyStoreType, 
                             Security.getProvider(keyStoreProvider), callback);
            }
                    
            KeyStoreBuilderParameters param = 
                          new KeyStoreBuilderParameters(builder);
                    
            kmf = KeyManagerFactory.getInstance("NewSunX509", "SunJSSE");
            kmf.init(param);
        } catch (Exception e) {
            debug.error(e.toString());
        }

        sunX509KeyManager = (X509KeyManager) kmf.getKeyManagers()[0];
    }

    /**
     * This constructor takes a JSSE default implementation and a
     * user-specified client certificate alias.
     * @param defaultX509KeyManager a JSSE default implementation
     * @param client certificate alias
     */
    static public void setAlias(String alias)
    {
        certAlias = alias;
    }

   /** 
    * Choose an alias to authenticate the client side of a secure socket given
    * the public key type and the list of certificate issuer authorities
    * recognized by the peer (if any). If the certAlias specified in the
    * constructor is not null, it will be used.
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the alias name for the desired key
    */
    public String chooseClientAlias(String[] keyType, Principal[] issuers, 
           Socket sock) {
        if (certAlias != null && certAlias.length() > 0) {
            return certAlias;
        }

        return sunX509KeyManager.chooseClientAlias(keyType, issuers, sock);
    }

   /**
    * Choose an alias to authenticate the server side of a secure socket
    * given the public key type and the list of certificate issuer
    * authorities recognized by the peer (if any).
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the alias name for the desired key
    */
    public String chooseServerAlias(
        String keyType,
        Principal[] issuers,
        Socket sock) {
        return sunX509KeyManager.chooseServerAlias(keyType, issuers, sock);
    }

   /**
    * Get the matching aliases for authenticating the client side of a secure
    * socket given the public key type and the list of certificate issuer
    * authorities recognized by the peer (if any).
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the matching alias names
    */
    public String[] getClientAliases(String keyType, Principal[] issuers)
    {
        return sunX509KeyManager.getClientAliases(keyType, issuers);
    }

   /**
    * Get the matching aliases for authenticating the server side of a secure
    * socket given the public key type and the list of certificate issuer
    * authorities recognized by the peer (if any).
    * @param keyType the key algorithm type name
    * @param issuers the list of acceptable CA issuer subject names
    * @return the matching alias names
    */
    public String[]
    getServerAliases(String keyType, Principal[] issuers)
    {
        return sunX509KeyManager.getServerAliases(keyType, issuers);
    }

   /**
    * Returns the certificate chain associated with the given alias.
    * @param alias the alias name
    * @return the certificate chain (ordered with the user's certificate first
    *         and the root certificate authority last)
    */
    public X509Certificate[]  getCertificateChain(String alias)
    {
             X509Certificate[] certchain = null;
        try {
                   KeyStore keystore = builder.getKeyStore();
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) 
                keystore.getEntry(alias, builder.getProtectionParameter(alias));
            if (entry != null) {
                certchain = (X509Certificate[])entry.getCertificateChain();
            } else {
                certchain = sunX509KeyManager.getCertificateChain(alias);
            }
        } catch (Exception e) {
            debug.error("Error in getting certificate chain from keystore." +
                                                                 e.toString());
              }

        return certchain;
    }

   /**
    * Returns the private key associated with the given alias.
    * @return the private key associated with the given alias
    */
    public PrivateKey getPrivateKey(String alias)
    {
             PrivateKey pkey = null;
        try {
           KeyStore keystore = builder.getKeyStore();
           KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) 
           keystore.getEntry(alias, builder.getProtectionParameter(alias));
           if (entry != null) {
               pkey = entry.getPrivateKey();
           } else {
               pkey =  sunX509KeyManager.getPrivateKey(alias);
           }
        } catch (Exception e) {
           debug.error("Error in getting private key from keystore." + 
                                                                 e.toString());
              }

        return pkey;
    }
}
