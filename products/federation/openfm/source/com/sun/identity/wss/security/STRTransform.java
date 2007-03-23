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
 * $Id: STRTransform.java,v 1.1 2007-03-23 00:02:01 mallas Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.wss.security;

import java.io.IOException;
import java.security.cert.X509Certificate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xml.internal.security.c14n.Canonicalizer;
import com.sun.org.apache.xml.internal.security.c14n.CanonicalizationException;
import com.sun.org.apache.xml.internal.security.c14n.
                                       InvalidCanonicalizerException;
import com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput;
import com.sun.org.apache.xml.internal.security.transforms.Transform;
import com.sun.org.apache.xml.internal.security.transforms.TransformSpi;
import com.sun.org.apache.xml.internal.security.transforms.
                                                TransformationException;
import com.sun.org.apache.xml.internal.security.keys.content.X509Data;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.debug.Debug;

/**
 * This class <code>STRTransform</code> extends from <code>TransformSpi</code>
 * and will be used to transform the <code>XMLSignatureInput</code> as 
 * required by the WS-Security specification.
 */
public class STRTransform extends TransformSpi {

    public static final String STR_TRANSFORM_URI = 
                "http://docs.oasis-open.org/wss/2004/01/" +
                "oasis-200401-wss-soap-message-security-1.0#STR-Transform";


    private static String XMLNS = "xmlns=";
    private static Debug debug = WSSUtils.debug;
    static {
       try {
           Transform.register(STR_TRANSFORM_URI, STRTransform.class.getName());
       } catch (Exception e) {
           debug.message("STRTransform.static already registered");
       }
    }

    /**
     * Returns the transformation engine URI.
     */
    protected String engineGetURI() {
        return STR_TRANSFORM_URI;
    }

    /**
     * Perform the XMLSignature transformation for the given input.
     */
    protected XMLSignatureInput enginePerformTransform(XMLSignatureInput input)
          throws IOException, CanonicalizationException, 
          InvalidCanonicalizerException, TransformationException {
  
        debug.message("STRTransform.enginePerformTransform:: Start");
        Document doc = this._transformObject.getDocument();
        Element str = null;
        if(input.isElement()) {
        } else {
           debug.error("STRTransform.enginePerformTransform:: Input is not " +
           " an element");
           throw new  CanonicalizationException("inputNotAnElement");
        }
        Element element = (Element)input.getSubNode();
        if(!WSSConstants.TAG_SECURITYTOKEN_REFERENCE.equals(
                     element.getLocalName())) {
           debug.error("STRTransform.enginePerformTransform:: input must be " +
           "security token reference");
           throw new IOException("invalidInputElement");
        }
        Element dereferencedToken = null;
        SecurityTokenReference ref = null; 
        try {
            ref = new SecurityTokenReference(element);
            dereferencedToken = dereferenceSTR(doc, ref);
        } catch (SecurityException se) {
            debug.error("STRTransform.enginePerformTransform:: error", se);
            throw new TransformationException("transformfailed");
        }
        String canonAlgo = getCanonicalizationAlgo();
        Canonicalizer canon = Canonicalizer.getInstance(canonAlgo);
        byte[] buf = canon.canonicalizeSubtree(dereferencedToken, "#default");
        StringBuffer bf = new StringBuffer(new String(buf));
        String bf1 = bf.toString();

        int lt = bf1.indexOf("<");
        int gt = bf1.indexOf(">");
        int idx = bf1.indexOf(XMLNS);
        if (idx < 0 || idx > gt) {
            idx = bf1.indexOf(" ");
            bf.insert(idx + 1, "xmlns=\"\" ");
            bf1 = bf.toString();
        }
        return new XMLSignatureInput(bf1.getBytes());
    }

    /**
     * Derefence the security token reference from the given document.
     */
    private Element dereferenceSTR(Document doc, SecurityTokenReference secRef)
         throws SecurityException {

        debug.message("STRTransform.deferenceSTR:: start");
        Element tokenElement = null;
        String refType = secRef.getReferenceType();

        if(SecurityTokenReference.DIRECT_REFERENCE.equals(refType)) {
           debug.message("STRTRansform.deferenceSTR:: Direct reference");
           tokenElement = secRef.getTokenElement(doc);

        } else if(SecurityTokenReference.X509DATA_REFERENCE.equals(refType)) {
           debug.message("STRTRansform.deferenceSTR:: X509 data reference");
           X509Data x509Data = secRef.getX509IssuerSerial();
           X509Certificate cert = 
                     AMTokenProvider.getX509Certificate(x509Data);
           tokenElement = createBinaryToken(doc, cert);

        } else if(SecurityTokenReference.KEYIDENTIFIER_REFERENCE.
                  equals(refType)) {
           debug.message("STRTRansform.deferenceSTR:: keyidentifier reference");
           KeyIdentifier keyIdentifier = secRef.getKeyIdentifier();
           if(WSSConstants.ASSERTION_VALUE_TYPE.equals(
                      keyIdentifier.getValueType())) {
              tokenElement = keyIdentifier.getTokenElement(doc);
           } else {
              X509Certificate cert = keyIdentifier.getX509Certificate();
              tokenElement = createBinaryToken(doc, cert);
           }
        }
        return tokenElement;
    }

    /**
     * Creates binary security token using the given x509 certificate.
     */
    private Element createBinaryToken(Document doc, 
         X509Certificate cert) throws SecurityException {

        BinarySecurityToken token =  new BinarySecurityToken(cert, 
            BinarySecurityToken.X509V3, BinarySecurityToken.BASE64BINARY);

        Element tokenE = token.toDocumentElement();
        doc.importNode(tokenE, true);
        return tokenE;
    }

    /**
     * Returns the canonicalization algorithm in transformation params.
     */
    private String getCanonicalizationAlgo() {
        String canonAlgo = null;
        if (this._transformObject.length(WSSConstants.WSSE_NS,
                    WSSConstants.TRANSFORMATION_PARAMETERS) == 1) {
            Node tmpE = XMLUtils.getChildNode(
                this._transformObject.getElement(), WSSConstants.WSSE_TAG + ":" 
                       + WSSConstants.TRANSFORMATION_PARAMETERS);
            Element canonElem = (Element) WSSUtils.getDirectChild(
                        tmpE, "CanonicalizationMethod", 
                        WSSConstants.XMLSIG_NAMESPACE_URI);
            canonAlgo = canonElem.getAttribute("Algorithm");
        }
        return canonAlgo;
    }
    
}
