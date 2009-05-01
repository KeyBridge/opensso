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
 * $Id: IdentityProvider.cs,v 1.1 2009-05-01 15:19:55 ggennaro Exp $
 */

using System.Collections;
using System.IO;
using System.Security.Cryptography.X509Certificates;
using System.Text;
using System.Xml;
using Sun.Identity.Properties;
using Sun.Identity.Saml2.Exceptions;

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Class representing all metadata for an Identity Provider.
    /// </summary>
    public class IdentityProvider
    {
        #region Members
        /// <summary>
        /// XML document representing the metadata for this Identity Provider.
        /// </summary>
        private XmlDocument metadata;

        /// <summary>
        /// Namespace Manager for the metadata.
        /// </summary>
        private XmlNamespaceManager metadataNsMgr;

        /// <summary>
        /// XML document representing the extended metadata for this Identity 
        /// Provider.
        /// </summary>
        private XmlDocument extendedMetadata;
        
        /// <summary>
        /// Namespace Manager for the extended metadata.
        /// </summary>
        private XmlNamespaceManager extendedMetadataNsMgr;
        
        /// <summary>
        /// Identity Provider's X509 certificate.
        /// </summary>
        private X509Certificate2 certificate;
        #endregion

        #region Constructors
        /// <summary>
        /// Initializes a new instance of the IdentityProvider class.
        /// </summary>
        /// <param name="metadataFileName">Name of file for metdata.</param>
        /// <param name="extendedMetadataFileName">Name of file for extended metadata.</param>
        public IdentityProvider(string metadataFileName, string extendedMetadataFileName)
        {
            try
            {
                this.metadata = new XmlDocument();
                this.metadata.Load(metadataFileName);
                this.metadataNsMgr = new XmlNamespaceManager(this.metadata.NameTable);
                this.metadataNsMgr.AddNamespace("md", "urn:oasis:names:tc:SAML:2.0:metadata");
                this.metadataNsMgr.AddNamespace("ds", "http://www.w3.org/2000/09/xmldsig#");

                this.extendedMetadata = new XmlDocument();
                this.extendedMetadata.Load(extendedMetadataFileName);
                this.extendedMetadataNsMgr = new XmlNamespaceManager(this.extendedMetadata.NameTable);
                this.extendedMetadataNsMgr.AddNamespace("mdx", "urn:sun:fm:SAML:2.0:entityconfig");

                // Load now since a) it doesn't change and b) its a 
                // performance dog on Win 2003 64-bit.
                byte[] byteArray = Encoding.UTF8.GetBytes(this.EncodedCertificate);
                this.certificate = new X509Certificate2(byteArray);
            }
            catch (DirectoryNotFoundException dnfe)
            {
                throw new IdentityProviderException(Resources.IdentityProviderDirNotFound, dnfe);
            }
            catch (FileNotFoundException fnfe)
            {
                throw new IdentityProviderException(Resources.IdentityProviderFileNotFound, fnfe);
            }
            catch (XmlException xe)
            {
                throw new IdentityProviderException(Resources.IdentityProviderXmlException, xe);
            }
        }
        #endregion

        #region Properties

        /// <summary>
        /// Gets the entity ID of this identity provider.
        /// </summary>
        public string EntityId
        {
            get
            {
                string xpath = "/md:EntityDescriptor";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                return node.Attributes["entityID"].Value.Trim();
            }
        }

        /// <summary>
        /// Gets the encoded X509 certifcate located within the identity
        /// provider's metadata.
        /// </summary>
        public string EncodedCertificate
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:IDPSSODescriptor/md:KeyDescriptor/ds:KeyInfo/ds:X509Data/ds:X509Certificate";
                XmlNode root = this.metadata.DocumentElement;
                XmlNode node = root.SelectSingleNode(xpath, this.metadataNsMgr);
                string value = node.InnerText.Trim(); // Regex.Replace(node.InnerText.Trim(), @"[\r\t]", "");
                return value;
            }
        }

        /// <summary>
        /// Gets the X509 certificate for this identity provider.
        /// </summary>
        public X509Certificate2 Certificate
        {
            get
            {
                return this.certificate;
            }
        }

        /// <summary>
        /// Gets the list of single sign on service locations, if present,
        /// otherwise an empty list.
        /// </summary>
        public XmlNodeList SingleSignOnServiceLocations
        {
            get
            {
                string xpath = "/md:EntityDescriptor/md:IDPSSODescriptor/md:SingleSignOnService";
                XmlNode root = this.metadata.DocumentElement;
                XmlNodeList nodeList = root.SelectNodes(xpath, this.metadataNsMgr);

                return nodeList;
            }
        }

        #endregion

        #region Methods
        #endregion
    }
}