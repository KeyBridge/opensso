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
 * $Id: SecureSOAPMessage.java,v 1.2 2007-05-17 18:49:19 mallas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.wss.security.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Date;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPPart;

import com.sun.identity.wss.security.WSSConstants;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.SecurityMechanism;
import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.security.SecurityToken;
import com.sun.identity.wss.security.AssertionToken;
import com.sun.identity.wss.security.SecurityPrincipal;
import com.sun.identity.wss.security.BinarySecurityToken;
import com.sun.identity.wss.security.UserNameToken;
import com.sun.identity.wss.security.SAML2Token;
import com.sun.identity.wss.security.SAML2TokenUtils;

import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.xml.XMLUtils;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;



/**
 * This class <code>SecureSOAPMessage</code> constructs the secured 
 * <code>SOAPMessage</code> for the given security mechanism token.
 * @supported.all.api
 */
public class SecureSOAPMessage {

     private SOAPMessage soapMessage = null;
     private SecurityToken securityToken = null;
     private SecurityMechanism securityMechanism = null;
     private boolean create = false;
     private Element wsseHeader = null;
     private X509Certificate messageCertificate = null;
     private static Debug debug = WSSUtils.debug;
     private static ResourceBundle bundle = WSSUtils.bundle;

     /**
      * Constructor to create secure SOAP message. 
      *
      * @param soapMessage the SOAP message to be secured.
      *
      * @param create if true, creates a new secured SOAP message by adding
      *               security headers.
      *               if false, parses the secured SOAP message. 
      *
      * @exception SecurityException if failed in creating or parsing the
      *            new secured SOAP message. 
      */
     public SecureSOAPMessage(SOAPMessage soapMessage, boolean create) 
          throws SecurityException {
          
         this.soapMessage = soapMessage;
         this.create = create;
         if(!create) {
            parseSOAPMessage(soapMessage);
         } else {
            addNameSpaces();
            addSecurityHeader();
         }
     }

     /**
      * Returns the secured SOAP message.
      *
      * @return the secured SOAP message.
      */
     public SOAPMessage getSOAPMessage() {
         return soapMessage;
     }

     /**
      * Parses the secured SOAP message.
      * @param soapMessage the secured SOAP message which needs to be parsed.
      *
      * @exception SecurityException if there is any failure in parsing.
      */
     private void parseSOAPMessage(SOAPMessage soapMessage) 
                throws SecurityException {
         try {
             SOAPHeader header = 
                   soapMessage.getSOAPPart().getEnvelope().getHeader();   
             if(header == null) {
                debug.error("SecureSOAPMessage.parseSOAPMessage: " +
                     "No SOAP header found.");
                throw new SecurityException(
                    bundle.getString("securityHeaderNotFound"));
             }
             NodeList headerChildNodes = header.getChildNodes();
             if((headerChildNodes == null) || 
                        (headerChildNodes.getLength() == 0)) {
                debug.error("SecureSOAPMessage.parseSOAPMessage: " +
                     "No security header found.");
                throw new SecurityException(
                    bundle.getString("securityHeaderNotFound"));
             }
             for(int i=0; i < headerChildNodes.getLength(); i++) {

                 Node currentNode = headerChildNodes.item(i);
                 if(currentNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                 }
                 if((WSSConstants.WSSE_SECURITY_LNAME.equals(
                       currentNode.getLocalName())) &&
                    (WSSConstants.WSSE_NS.equals(
                       currentNode.getNamespaceURI()))) {
                    parseSecurityHeader(currentNode);
                 }
             }
             if(securityToken == null) {
                debug.error("SecureSOAPMessage.parseSOAPMessage: " +
                     "security token is null");
                throw new SecurityException(
                    bundle.getString("securityHeaderNotFound"));
             }
         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.parseSOAPMessage: SOAP" +
             "Exception in parsing the headers.", se);
             throw new SecurityException(se.getMessage());
         }
     }

     /**
      * Parses for the security header.
      * @param node security header node.
      *
      * @exception SecurityException if there is any error occured.
      */
     private void parseSecurityHeader(Node node) throws SecurityException {

         NodeList securityHeaders = node.getChildNodes();
         for(int i=0; i < securityHeaders.getLength(); i++) {
             Node currentNode =  securityHeaders.item(i);
             if(currentNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
             }
             String localName =  currentNode.getLocalName();
             String nameSpace = currentNode.getNamespaceURI();

             if( (SAMLConstants.TAG_ASSERTION.equals(localName)) &&
                 (SAMLConstants.assertionSAMLNameSpaceURI.equals(nameSpace)) ) {

                if(debug.messageEnabled()) {
                   debug.message("SecureSOAPMessage.parseSecurityHeader:: " +
                   "Assertion token found in the security header.");
                }
                try {
                    securityToken = new AssertionToken((Element)currentNode);
                    AssertionToken assertionToken = 
                               (AssertionToken)securityToken;
                    if(assertionToken.isSenderVouches()) {
                       securityMechanism = 
                                  SecurityMechanism.WSS_NULL_SAML_SV;
                    } else {
                       securityMechanism = 
                                  SecurityMechanism.WSS_NULL_SAML_HK;
                    }
                    messageCertificate = 
                           WSSUtils.getCertificate(assertionToken);
                } catch (SAMLException se) {
                    debug.error("SecureSOAPMessage.parseSecurity" +
                    "Header: unable to parse the token", se);
                    throw new SecurityException(se.getMessage());
                }
                
             } else if( (SAMLConstants.TAG_ASSERTION.equals(localName)) &&
                 (SAML2Constants.ASSERTION_NAMESPACE_URI.equals(nameSpace)) ) {

                if(debug.messageEnabled()) {
                   debug.message("SecureSOAPMessage.parseSecurityHeader:: " +
                   "SAML2 token found in the security header.");
                }
                try {
                    securityToken = new SAML2Token((Element)currentNode);
                    SAML2Token saml2Token = 
                               (SAML2Token)securityToken;
                    if(saml2Token.isSenderVouches()) {
                       securityMechanism = 
                                  SecurityMechanism.WSS_NULL_SAML2_SV;
                    } else {
                       securityMechanism = 
                                  SecurityMechanism.WSS_NULL_SAML2_HK;
                    }
                    messageCertificate = 
                           SAML2TokenUtils.getCertificate(saml2Token);
                } catch (SAML2Exception se) {
                    debug.error("SecureSOAPMessage.parseSecurity" +
                    "Header: unable to parse the token", se);
                    throw new SecurityException(se.getMessage());
                }

             } else if( (WSSConstants.TAG_BINARY_SECURITY_TOKEN.
                         equals(localName)) && 
                        (WSSConstants.WSSE_NS.equals(nameSpace)) ) {

                 if(debug.messageEnabled()) {
                    debug.message("SecureSOAPMessage.parseSecurityHeader:: " +
                    "binary token found in the security header.");
                 }
                 securityToken = new BinarySecurityToken((Element)currentNode);
                 securityMechanism = SecurityMechanism.WSS_NULL_X509_TOKEN;
                 messageCertificate = WSSUtils.getCertificate(securityToken);

             } else if( (WSSConstants.TAG_USERNAME_TOKEN.equals(localName)) &&
                        (WSSConstants.WSSE_NS.equals(nameSpace)) ) {

                 if(debug.messageEnabled()) {
                    debug.message("SecureSOAPMessage.parseSecurityHeader:: " +
                    "username token found in the security header.");
                 }
                 securityToken = new UserNameToken((Element)currentNode);
                 securityMechanism = SecurityMechanism.WSS_NULL_USERNAME_TOKEN;

             }
          }
     }

     /**
      * Returns the security mechanism of the secure soap message.
      *
      * @return SecurityMechanism the security mechanism of the secure
      *         <code>SOAPMessage</code>.
      */
     public SecurityMechanism getSecurityMechanism() {
         return securityMechanism;    
     }

     /**
      * Sets the security mechanism for securing the soap message.
      *
      * @param securityMechanism the security mechanism that will be used
      *        to secure the soap message.
      */
     public void setSecurityMechanism(SecurityMechanism securityMechanism) {
         this.securityMechanism = securityMechanism;
     }

     /**
      * Sets the security token for securing the soap message.
      *
      * @param token the security token that is used to secure the soap message.
      *
      * @exception SecurityException if the security token can not be added
      *       to the security header. 
      */
     public void setSecurityToken(SecurityToken token) 
                  throws SecurityException {

         if(wsseHeader == null) {
            debug.error("SecureSOAPMessage.setSecurityToken:: WSSE security" +
            " Header is not found in the Secure SOAP Message.");
            throw new SecurityException(
                 bundle.getString("securityHeaderNotFound"));
         }
         this.securityToken = token;
         Element tokenE = token.toDocumentElement();
         Node tokenNode = soapMessage.getSOAPPart().importNode(tokenE, true);
         WSSUtils.prependChildElement(wsseHeader, (Element)tokenNode, true, 
                (Document)soapMessage.getSOAPPart());
     }

     /**
      * Returns the security token associated with this secure soap message.
      *
      * @return SecurityToken the security token for this secure soap message.
      */
     public SecurityToken getSecurityToken() {
         return securityToken;
     }

     /**
      * Adds the WSSE related name spaces to the SOAP Envelope.
      */
     private void addNameSpaces() throws SecurityException {
         try {
             SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
             envelope.setAttributeNS(WSSConstants.NS_XML,
                       WSSConstants.TAG_XML_WSU,
                       WSSConstants.WSU_NS);

             SOAPBody body = soapMessage.getSOAPPart().getEnvelope().getBody();
             body.setAttributeNS(WSSConstants.NS_XML,
                       WSSConstants.TAG_XML_WSU,
                       WSSConstants.WSU_NS);
             body.setAttribute(WSSConstants.WSU_ID, SAMLUtils.generateID());

         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.addNameSpaces:: Could not add " + 
             "Name spaces. ", se);
             throw new SecurityException(
                   bundle.getString("nameSpaceAdditionfailure"));
         }
     }

     /**
      * Adds the security header to the SOAP Envelope.
      */
     private void addSecurityHeader() throws SecurityException {

         if(debug.messageEnabled()) {
            debug.message("SecureSOAPMessage.addSecurityHeader:: preparing the"+
            " security header");
         }
         try {
             SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
             SOAPHeader header = envelope.getHeader(); 
             if(header == null) {
                header = soapMessage.getSOAPPart().getEnvelope().addHeader();
             }
             SOAPPart soapPart = soapMessage.getSOAPPart();
             wsseHeader = soapMessage.getSOAPPart().createElementNS(
                          WSSConstants.WSSE_NS,
                          WSSConstants.WSSE_TAG + ":" +
                          WSSConstants.WSSE_SECURITY_LNAME);
             wsseHeader.setAttributeNS(
                          WSSConstants.NS_XML,
                          WSSConstants.TAG_XML_WSSE,
                          WSSConstants.WSSE_NS);
             wsseHeader.setAttributeNS(WSSConstants.NS_XML,
                          WSSConstants.TAG_XML_WSU,
                          WSSConstants.WSU_NS);
             wsseHeader.setAttributeNS(
                     WSSConstants.NS_XML,
                     WSSConstants.TAG_XML_WSSE11,
                     WSSConstants.WSSE11_NS);

             String envPrefix = envelope.getPrefix();
             if(envPrefix != null) {
                wsseHeader.setAttribute(envPrefix + ":" +
                          WSSConstants.MUST_UNDERSTAND, "1");
             }

             //Add time stamp
             Element timeStamp = soapMessage.getSOAPPart().createElementNS(
                          WSSConstants.WSU_NS, 
                          WSSConstants.WSU_TAG + ":" +
                          WSSConstants.TIME_STAMP);
             wsseHeader.appendChild(timeStamp);
             Element created = soapMessage.getSOAPPart().createElementNS(
                          WSSConstants.WSU_NS,
                          WSSConstants.WSU_TAG + ":" + WSSConstants.CREATED);

             Date createTime = new Date();
             Date expireTime = new Date(); 
             expireTime.setTime(createTime.getTime() + 
                          WSSConstants.INTERVAL * 1000);

             created.appendChild(soapPart.createTextNode(
                          DateUtils.toUTCDateFormat(createTime))); 
             timeStamp.appendChild(created);
 
             Element expires = soapMessage.getSOAPPart().createElementNS(
                          WSSConstants.WSU_NS,
                          WSSConstants.WSU_TAG + ":" + WSSConstants.EXPIRES);
             expires.appendChild(soapPart.createTextNode(
                          DateUtils.toUTCDateFormat(expireTime))); 
             timeStamp.appendChild(expires);
             header.appendChild(wsseHeader);

         } catch (SOAPException se) {
             debug.error("SecureSOAPMessage.addSecurityHeader:: SOAPException"+
             " while adding the security header.", se);
             throw new SecurityException(
                    bundle.getString("addSecurityHeaderFailed"));
         }
     }

    /**
     * Signs the <code>SOAPMessage</code>  for the given security profile.
     *
     * @param certAlias the certificate alias
     *
     * @exception SecurityException if there is any failure in signing.
     */
     public void sign(String certAlias) throws SecurityException {

         Document doc = null;
         try {
             ByteArrayOutputStream bop = new ByteArrayOutputStream();
             soapMessage.writeTo(bop);
             ByteArrayInputStream bin =
                     new ByteArrayInputStream(bop.toByteArray());
             doc = XMLUtils.toDOMDocument(bin, debug);

         } catch (Exception se) {
             debug.error("SecureSOAPMessage.sign:: can not convert" + 
               "to an XMLDocument", se);
             throw new SecurityException(
                   bundle.getString("cannotConvertToDocument"));
         }
         String tokenType = securityToken.getTokenType();

         if(SecurityToken.WSS_SAML_TOKEN.equals(tokenType) ||
                 SecurityToken.WSS_SAML2_TOKEN.equals(tokenType)) {
            signWithAssertion(doc, certAlias);
         } else if(SecurityToken.WSS_X509_TOKEN.equals(tokenType)) {
            signWithBinaryToken(doc, certAlias);
         } else {
            debug.error("SecureSOAPMessage.sign:: Invalid token type for" +
            " XML signing.");
         }
     }

     /**
      * Signs the SOAP Message with SAML Assertion.
      */
     private void signWithAssertion(Document doc, String certAlias)
              throws SecurityException {
         
         XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
         KeyProvider keyProvider = sigManager.getKeyProvider();
         Certificate cert = null;
         String uri =  securityMechanism.getURI(); 

         if( (SecurityMechanism.WSS_NULL_SAML_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML_HK_URI.equals(uri)) ) {
             cert = WSSUtils.getCertificate(securityToken);
             
         } else if( (SecurityMechanism.WSS_NULL_SAML2_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML2_HK_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML2_HK_URI.equals(uri)) ) {
             cert = SAML2TokenUtils.getCertificate(securityToken);
             
         } else if( (SecurityMechanism.WSS_NULL_SAML_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_NULL_SAML2_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_TLS_SAML2_SV_URI.equals(uri)) ||
             (SecurityMechanism.WSS_CLIENT_TLS_SAML2_SV_URI.equals(uri)) ) {
             cert =  keyProvider.getX509Certificate(certAlias);
             
         } else {
             debug.error("SecureSOAPMessage.signWithSAMLAssertion:: " +
              "Unknown security mechanism");
             throw new SecurityException(
                   bundle.getString("unknownSecurityMechanism"));
         }
 
         Element sigElement = null;
         try {
             String assertionID = null;
             if(securityToken instanceof AssertionToken) {
                AssertionToken assertionToken = (AssertionToken)securityToken;
                assertionID = assertionToken.getAssertion().getAssertionID();
             } else if (securityToken instanceof SAML2Token) {
                SAML2Token saml2Token = (SAML2Token)securityToken;
                assertionID = saml2Token.getAssertion().getID();
             }
                          
             sigElement = sigManager.signWithSAMLToken(doc,
                   cert, assertionID, "", getSigningIds());

         } catch (XMLSignatureException se) {
             debug.error("SecureSOAPMessage.signWithAssertion:: " +
                "signing failed", se);
             throw new SecurityException(
                   bundle.getString("unabletoSign"));
         } catch (Exception ex) {
             debug.error("SecureSOAPMessage.signWithAssertion:: " +
                "signing failed", ex);
             throw new SecurityException(
                   bundle.getString("unabletoSign"));
         }
         wsseHeader.appendChild(
                 soapMessage.getSOAPPart().importNode(sigElement, true));
         soapMessage =  WSSUtils.toSOAPMessage(sigElement.getOwnerDocument());
     }


     /**
      * Signs the document with binary security token.
      */
     private void signWithBinaryToken(Document doc, String certAlias) 
           throws SecurityException {

         Certificate cert = null;
         Element sigElement = null;
         XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
         KeyProvider keyProvider = sigManager.getKeyProvider();
         try {
             cert =  keyProvider.getX509Certificate(certAlias);
             sigElement = sigManager.signWithBinarySecurityToken(
                doc, cert, "", getSigningIds());
         } catch (XMLSignatureException se) {
            debug.error("SecureSOAPMessage.signWithAssertion:: Signature " +
            "Exception.", se);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         } catch (Exception ex) {
            debug.error("SecureSOAPMessage.signWithAssertion:: " +
                "signing failed", ex);
            throw new SecurityException(
                   bundle.getString("unabletoSign"));
         }
         wsseHeader.appendChild(
                 soapMessage.getSOAPPart().importNode(sigElement, true));
         soapMessage =  WSSUtils.toSOAPMessage(sigElement.getOwnerDocument());

     }

     /**
      * Returns the list of signing ids.
      */
     private List getSigningIds() throws Exception {
        List ids = new ArrayList();
        SOAPBody body = soapMessage.getSOAPPart().getEnvelope().getBody();
        String id  = body.getAttribute(WSSConstants.WSU_ID); 
        ids.add(id);
        return ids;
     }

     /**
      * Verifies the signature of the SOAP message.
      * @return true if the signature verification is successful.
      * @exception SecurityException if there is any failure in validation. 
      */
     public boolean verifySignature() throws SecurityException {

        try {
            Document doc = toDocument();
            XMLSignatureManager sigManager = WSSUtils.getXMLSignatureManager();
            String certAlias = null;
            if(messageCertificate != null) {
               certAlias = sigManager.getKeyProvider().
                           getCertificateAlias(messageCertificate);
            }
            return sigManager.verifyWSSSignature(doc, certAlias); 
        } catch (SAMLException se) {
            debug.error("SecureSOAPMessage.verify:: Signature validation " +
                   "failed", se);
            throw new SecurityException(
                bundle.getString("signatureValidationFailed"));
        }
     }

     /**
      * Converts the SOAP Message into an XML document.
      */
     private Document toDocument() throws SecurityException {
        try {
            ByteArrayOutputStream bop = new ByteArrayOutputStream();
            soapMessage.writeTo(bop);
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bop.toByteArray());
            return XMLUtils.toDOMDocument(bin, WSSUtils.debug);
        } catch (Exception ex) {
            debug.error("SecureSOAPMessage.toDocument: Could not" +
            " Convert the SOAP Message to XML document.", ex); 
            throw new SecurityException(ex.getMessage());
        }
     }

     /**
      * Returns the <code>X509Certificate</code> that is used to secure
      * the <code>SOAPMessage</code>.
      *
      * @return the X509 certificate. 
      */
     public X509Certificate getMessageCertificate() {
         return messageCertificate;
     }
}
