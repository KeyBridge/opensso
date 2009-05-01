/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
 * 
 * The contents of this file are subject to the terms
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
 * $Id: AuthnResponse.cs,v 1.1 2009-05-01 15:19:55 ggennaro Exp $
 */

using System;
using System.Collections;
using System.Globalization;
using System.Xml;
using System.Xml.XPath;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// SAMLv2 AuthnResponse object constructed from a response obtained from
    /// an Identity Provider for the hosted Service Provider.
    /// </summary>
    public class AuthnResponse
    {
        #region Members
        /// <summary>
        /// Namespace Manager for this authn response.
        /// </summary>
        private XmlNamespaceManager nsMgr;

        /// <summary>
        /// XML representation of the authn response.
        /// </summary>
        private XmlDocument xml;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the AuthnResponse class.
        /// </summary>
        /// <param name="samlResponse">Decoded SAMLv2 Response</param>
        public AuthnResponse(string samlResponse)
        {
            try
            {
                this.xml = new XmlDocument();
                this.xml.PreserveWhitespace = true;
                this.xml.LoadXml(samlResponse);
                this.nsMgr = new XmlNamespaceManager(this.xml.NameTable);
                this.nsMgr.AddNamespace("samlp", "urn:oasis:names:tc:SAML:2.0:protocol");
                this.nsMgr.AddNamespace("saml", "urn:oasis:names:tc:SAML:2.0:assertion");
                this.nsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");
            }
            catch (ArgumentNullException ane)
            {
                throw new Saml2Exception(Resources.AuthnResponseNullArgument, ane);
            }
            catch (XmlException xe)
            {
                throw new Saml2Exception(Resources.AuthnResponseXmlException, xe);
            }
        }
        #endregion

        #region Properties

        /// <summary>
        /// Gets the XML representation of the received authn response.
        /// </summary>
        public IXPathNavigable XmlDom
        {
            get
            {
                return this.xml;
            }
        }

        /// <summary>
        /// Gets the signature of the authn response as an XML element.
        /// </summary>
        public IXPathNavigable XmlSignature
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/ds:Signature";
                XmlNode root = this.xml.DocumentElement;
                XmlNode signatureElement = root.SelectSingleNode(xpath, this.nsMgr);
                return signatureElement;
            }
        }

        /// <summary>
        /// Gets the name of the issuer of the authn response.
        /// </summary>
        public string Issuer
        {
            get
            {
                string xpath = "/samlp:Response/saml:Issuer";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.InnerText.Trim();
            }
        }

        /// <summary>
        /// Gets the status code of the authn response within the status element.
        /// </summary>
        public string StatusCode
        {
            get
            {
                string xpath = "/samlp:Response/samlp:Status/samlp:StatusCode";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.Attributes["Value"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the X509 signature certificate of the authn response, null if
        /// none provided.
        /// </summary>
        public string SignatureCertificate
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                if (node == null)
                {
                    return null;
                }

                string value = node.InnerText.Trim(); // Regex.Replace(node.InnerText.Trim(), @"[\r\t]", "");
                return value;
            }
        }

        /// <summary>
        /// Gets the name ID of the subject within the authn response assertion.
        /// </summary>
        public string SubjectNameId
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Subject/saml:NameID";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return node.InnerText.Trim();
            }
        }

        /// <summary>
        /// Gets the extracted "NotBefore" condition from the authn response.
        /// </summary>
        public DateTime ConditionNotBefore
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Conditions";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return DateTime.Parse(node.Attributes["NotBefore"].Value.Trim(), CultureInfo.InvariantCulture);
            }
        }

        /// <summary>
        /// Gets the extracted "NotOnOrAfter" condition from the authn response.
        /// </summary>
        public DateTime ConditionNotOnOrAfter
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Conditions";
                XmlNode root = this.xml.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.nsMgr);
                return DateTime.Parse(node.Attributes["NotOnOrAfter"].Value.Trim(), CultureInfo.InvariantCulture);
            }
        }

        /// <summary>
        /// Gets the list containing string of entity ID's that are considered
        /// appropriate audiences for this authn response.
        /// </summary>
        public ArrayList ConditionAudiences
        {
            get
            {
                string xpath = "/samlp:Response/saml:Assertion/saml:Conditions/saml:AudienceRestriction/saml:Audience";
                XmlNode root = this.xml.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.nsMgr);
                IEnumerator nodes = (IEnumerator)nodeList.GetEnumerator();

                ArrayList audiences = new ArrayList();
                while (nodes.MoveNext())
                {
                    XmlNode node = (XmlNode)nodes.Current;
                    audiences.Add(node.InnerText.Trim());
                }

                return audiences;
            }
        }

        /// <summary>
        /// Gets the property containing the attributes provided in the SAML2
        /// assertion, if provided, otherwise an empty hashtable.
        /// </summary>
        public Hashtable Attributes
        {
            get
            {   
                string xpath = "/samlp:Response/saml:Assertion/saml:AttributeStatement/saml:Attribute";
                XmlNode root = this.xml.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.nsMgr);
                IEnumerator nodes = (IEnumerator)nodeList.GetEnumerator();

                Hashtable attributes = new Hashtable();
                while (nodes.MoveNext())
                {
                    XmlNode samlAttribute = (XmlNode)nodes.Current;
                    string name = samlAttribute.Attributes["Name"].Value.Trim();

                    XmlNode samlAttributeValue = samlAttribute.SelectSingleNode("descendant::saml:AttributeValue", this.nsMgr);
                    string value = samlAttributeValue.InnerText.Trim();

                    attributes.Add(name, value);
                }

                return attributes;
            }
        }
        #endregion

        #region Methods
        #endregion
    }
}