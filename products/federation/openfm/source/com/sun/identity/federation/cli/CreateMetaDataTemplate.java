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
 * $Id: CreateMetaDataTemplate.java,v 1.8 2007-04-19 18:28:54 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.federation.cli;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaSecurityUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;

/**
 * Create Meta Data Template.
 */
public class CreateMetaDataTemplate extends AuthenticatedCommand {
    
    private String entityID;
    private String metadata;
    private String extendedData;
    private String idpAlias;
    private String spAlias;
    private String pdpAlias;
    private String pepAlias;
    private String idpSCertAlias;
    private String idpECertAlias;
    private String pdpSCertAlias;
    private String pdpECertAlias;
    private String spSCertAlias;
    private String spECertAlias;
    private String pepSCertAlias;
    private String pepECertAlias;
    private String protocol;
    private String host;
    private String port;
    private String deploymentURI;
    private String realm = "/";
    private boolean isWebBased;
    
    /**
     * Creates Meta Data Template.
     *
     * @param rc Request Context.
     * @throws CLIException if unable to process this request.
     */
    public void handleRequest(RequestContext rc)
    throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        getOptions();
        validateOptions();
        normalizeOptions();
        
        String spec = FederationManager.getIDFFSubCommandSpecification(rc);
        if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
            handleSAML2Request(rc);
        } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
            handleIDFFRequest(rc);
        } else {
            throw new CLIException(
                    getResourceString("unsupported-specification"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
    }
    
    private void handleSAML2Request(RequestContext rc)
    throws CLIException {
        if (!isWebBased || (extendedData != null)) {
            buildConfigTemplate();
        }

        if (!isWebBased || (metadata != null)) {
            buildDescriptorTemplate();
        }
    }
    
    private void handleIDFFRequest(RequestContext rc)
    throws CLIException {
        if (!isWebBased || (extendedData != null)) {
            buildIDFFConfigTemplate();
        }
        if (!isWebBased || (metadata != null)) {
            buildIDFFDescriptorTemplate();
        }
    }
    
    private void getOptions() {
        entityID = getStringOptionValue(FedCLIConstants.ARGUMENT_ENTITY_ID);
        idpAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_IDENTITY_PROVIDER);
        spAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_SERVICE_PROVIDER);
        pdpAlias = getStringOptionValue(FedCLIConstants.ARGUMENT_PDP);
        pepAlias = getStringOptionValue(FedCLIConstants.ARGUMENT_PEP);
        
        metadata = getStringOptionValue(FedCLIConstants.ARGUMENT_METADATA);
        extendedData = getStringOptionValue(
            FedCLIConstants.ARGUMENT_EXTENDED_DATA);
        
        idpSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_IDP_S_CERT_ALIAS);
        idpECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_IDP_E_CERT_ALIAS);
        
        spSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_SP_S_CERT_ALIAS);
        spECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_SP_E_CERT_ALIAS);
        
        pdpSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PDP_S_CERT_ALIAS);
        pdpECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PDP_E_CERT_ALIAS);

        pepSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PEP_S_CERT_ALIAS);
        pepECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_PEP_E_CERT_ALIAS);

        protocol = SystemPropertiesManager.get(Constants.AM_SERVER_PROTOCOL);
        host = SystemPropertiesManager.get(Constants.AM_SERVER_HOST);
        port = SystemPropertiesManager.get(Constants.AM_SERVER_PORT);
        deploymentURI = SystemPropertiesManager.get(
            Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);
        String webURL = getCommandManager().getWebEnabledURL();
        isWebBased = (webURL != null) && (webURL.trim().length() > 0);
    }
    
    private void normalizeOptions() {
        if ((idpAlias != null) && !idpAlias.startsWith("/")) {
            idpAlias = "/" + idpAlias;
        }
        if ((spAlias != null) && !spAlias.startsWith("/")) {
            spAlias = "/" + spAlias;
        }
        if ((pdpAlias != null) && !pdpAlias.startsWith("/")) {
            pdpAlias = "/" + pdpAlias;
        }
        if ((pepAlias != null) && !pepAlias.startsWith("/")) {
            pepAlias = "/" + pepAlias;
        }
        if (entityID == null) {
            entityID = host + realm;
        }
        if (idpSCertAlias == null) {
            idpSCertAlias = "";
        }
        if (idpECertAlias == null) {
            idpECertAlias = "";
        }
        if (spSCertAlias == null) {
            spSCertAlias = "";
        }
        if (spECertAlias == null) {
            spECertAlias = "";
        }
        if (pdpSCertAlias == null) {
            pdpSCertAlias = "";
        }
        if (pdpECertAlias == null) {
            pdpECertAlias = "";
        }
        if (pepSCertAlias == null) {
            pepSCertAlias = "";
        }
        if (pepECertAlias == null) {
            pepECertAlias = "";
        }
    }
    
    private void validateOptions()
    throws CLIException {
        if ((idpAlias == null) && (spAlias == null) && (pdpAlias == null) && 
            (pepAlias == null)
        ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-role-null"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((idpAlias == null) &&
            ((idpSCertAlias != null) || (idpECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-idp-null-with-cert-alias"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((spAlias == null) &&
            ((spSCertAlias != null) || (spECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-sp-null-with-cert-alias"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((pdpAlias == null) &&
            ((pdpSCertAlias != null) || (pdpECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-pdp-null-with-cert-alias"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((pepAlias == null) &&
            ((pepSCertAlias != null) || (pepECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-pep-null-with-cert-alias"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if (protocol == null) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-protocol-not-found"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        if (host == null) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-host-not-found"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        if (port == null) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-port-not-found"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        if (deploymentURI == null) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-deploymentURI-not-found"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void buildDescriptorTemplate()
    throws CLIException {
        String url =  protocol + "://" + host + ":" + port + deploymentURI;
        
        Writer pw = null;
        try {
            if (!isWebBased && (metadata != null) && (metadata.length() > 0)) {
                pw = new PrintWriter(new FileWriter(metadata));
            } else {
                pw = new StringWriter();
            }
            pw.write(
                    "<EntityDescriptor\n" +
                    "    xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n" +
                    "    entityID=\"" + entityID + "\">\n");
            
            if (idpAlias != null) {
                addIdentityProviderTemplate(pw, url);
            }
            if (spAlias != null) {
                addServiceProviderTemplate(pw, url);
            }
            if (pdpAlias != null) {
                addPDPTemplate(pw, url);
            }
            if (pepAlias != null) {
                addPEPTemplate(pw, url);
            }
            pw.write("</EntityDescriptor>\n");
            
            if (!isWebBased) {
                Object[] objs = { metadata, realm };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-descriptor-template"), objs));
            }
        } catch (SAML2MetaException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            Object[] objs = { metadata };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }
    
    private void addIdentityProviderTemplate(Writer pw, String url)
        throws IOException, SAML2MetaException {
        String maStr = buildMetaAliasInURI(idpAlias);
        
        pw.write(
                "    <IDPSSODescriptor\n" +
                "        WantAuthnRequestsSigned=\"false\"\n" +
                "        protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n"
                );
        
        String idpSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
                idpSCertAlias);
        if (idpSX509Cert != null) {
            pw.write(
                    "        <KeyDescriptor use=\"signing\">\n" +
                    "            <KeyInfo xmlns=\"" + SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                    "                <X509Data>\n" +
                    "                    <X509Certificate>\n" + idpSX509Cert +
                    "                    </X509Certificate>\n" +
                    "                </X509Data>\n" +
                    "            </KeyInfo>\n" +
                    "        </KeyDescriptor>\n");
        }
        
        String idpEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
                idpECertAlias);
        if (idpEX509Cert != null) {
            pw.write(
                    "        <KeyDescriptor use=\"encryption\">\n" +
                    "            <KeyInfo xmlns=\"" + SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                    "                <X509Data>\n" +
                    "                    <X509Certificate>\n" + idpEX509Cert +
                    "                    </X509Certificate>\n" +
                    "                </X509Data>\n" +
                    "            </KeyInfo>\n" +
                    "            <EncryptionMethod Algorithm=" +
                    "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                    "                <KeySize xmlns=\"" + SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                    "128</KeySize>\n" +
                    "            </EncryptionMethod>\n" +
                    "        </KeyDescriptor>\n");
        }
        
        pw.write(
                "        <ArtifactResolutionService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
                "            Location=\"" + url + "/ArtifactResolver" + maStr + "\"\n" +
                "            index=\"0\"\n" +
                "            isDefault=\"1\"/>\n" +
                
                "        <SingleLogoutService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
                "            Location=\"" + url + "/IDPSloRedirect" + maStr + "\"\n" +
                "            ResponseLocation=\"" + url + "/IDPSloRedirect" + maStr + "\"/>\n" +
                "        <SingleLogoutService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
                "            Location=\"" + url + "/IDPSloSoap" + maStr + "\"/>\n" +
                "        <ManageNameIDService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
                "            Location=\"" + url + "/IDPMniRedirect" + maStr + "\"\n" +
                "            ResponseLocation=\"" + url + "/IDPMniRedirect" + maStr + "\"/>\n" +
                "        <ManageNameIDService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
                "            Location=\"" + url + "/IDPMniSoap" + maStr + "\"/>\n" +
                "        <NameIDFormat>\n" +
                "            urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\n" +
                "        </NameIDFormat>\n" +
                "        <NameIDFormat>\n" +
                "            urn:oasis:names:tc:SAML:2.0:nameid-format:transient\n" +
                "        </NameIDFormat>\n" +
                "        <SingleSignOnService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
                "            Location=\"" + url + "/SSORedirect" + maStr + "\"/>\n" +
                "        <SingleSignOnService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
                "            Location=\"" + url + "/SSORedirect" + maStr + "\"/>\n" +
                "    </IDPSSODescriptor>\n"
                );
    }
    
    private void addServiceProviderTemplate(Writer pw, String url)
    throws IOException, SAML2MetaException {
        String maStr = buildMetaAliasInURI(spAlias);
        pw.write(
                "    <SPSSODescriptor\n" +
                "        AuthnRequestsSigned=\"false\"\n" +
                "        WantAssertionsSigned=\"false\"\n" +
                "        protocolSupportEnumeration=\n" +
                "            \"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        
        String spSX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
                spSCertAlias);
        String spEX509Cert = SAML2MetaSecurityUtils.buildX509Certificate(
                spECertAlias);
        
        if (spSX509Cert != null) {
            pw.write(
                    "        <KeyDescriptor use=\"signing\">\n" +
                    "            <KeyInfo xmlns=\"" + SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                    "                <X509Data>\n" +
                    "                    <X509Certificate>\n" + spSX509Cert +
                    "                    </X509Certificate>\n" +
                    "                </X509Data>\n" +
                    "            </KeyInfo>\n" +
                    "        </KeyDescriptor>\n");
        }
        
        if (spEX509Cert != null) {
            pw.write(
                    "        <KeyDescriptor use=\"encryption\">\n" +
                    "            <KeyInfo xmlns=\"" + SAML2MetaSecurityUtils.NS_XMLSIG + "\">\n" +
                    "                <X509Data>\n" +
                    "                    <X509Certificate>\n" + spEX509Cert +
                    "                    </X509Certificate>\n" +
                    "                </X509Data>\n" +
                    "            </KeyInfo>\n" +
                    "            <EncryptionMethod Algorithm=" +
                    "\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\">\n" +
                    "                <KeySize xmlns=\"" + SAML2MetaSecurityUtils.NS_XMLENC +"\">" +
                    "128</KeySize>\n" +
                    "            </EncryptionMethod>\n" +
                    "        </KeyDescriptor>\n");
        }
        
        pw.write(
                "        <SingleLogoutService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
                "            Location=\"" + url + "/SPSloRedirect" + maStr + "\"\n" +
                "            ResponseLocation=\"" + url + "/SPSloRedirect" + maStr + "\"/>\n" +
                "        <SingleLogoutService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
                "            Location=\"" + url + "/SPSloSoap" + maStr + "\"/>\n" +
                "        <ManageNameIDService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\"\n" +
                "            Location=\"" + url + "/SPMniRedirect" + maStr + "\"\n" +
                "            ResponseLocation=\"" + url + "/SPMniRedirect" + maStr + "\"/>\n" +
                "        <ManageNameIDService\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"\n" +
                "            Location=\"" + url + "/SPMniSoap" + maStr + "\"\n" +
                "            ResponseLocation=\"" + url + "/SPMniSoap" + maStr + "\"/>\n" +
                "        <NameIDFormat>\n" +
                "            urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\n" +
                "        </NameIDFormat>\n" +
                "        <NameIDFormat>\n" +
                "            urn:oasis:names:tc:SAML:2.0:nameid-format:transient\n" +
                "        </NameIDFormat>\n" +
                "        <AssertionConsumerService\n" +
                "            isDefault=\"true\"\n" +
                "            index=\"0\"\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact\"\n" +
                "            Location=\"" + url + "/Consumer" + maStr + "\"/>\n" +
                "        <AssertionConsumerService\n" +
                "            index=\"1\"\n" +
                "            Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n" +
                "            Location=\"" + url + "/Consumer" + maStr + "\"/>\n" +
                "    </SPSSODescriptor>\n"
                );
    }
    
    private void addPDPTemplate(Writer pw, String url)
        throws IOException, SAML2MetaException {
        String maStr = buildMetaAliasInURI(pdpAlias);
        pw.write(
            "    <XACMLPDPDescriptor " + 
            "protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n" +
            "         <XACMLAuthzService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:SOAP\"" +
            " Location=\"" + url + "/saml2query" + maStr + "\"/>\n" +
            "    </XACMLPDPDescriptor>\n");
    }
    
    private void addPEPTemplate(Writer pw, String url)
        throws IOException, SAML2MetaException {
        pw.write("    <XACMLAuthzDecisionQueryDescriptor " +
            "wantAssertionSigned=\"true\" " +
            "protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"/>\n");
    }
    
    private static String buildMetaAliasInURI(String alias) {
        return "/" + SAML2MetaManager.NAME_META_ALIAS_IN_URI + alias;
    }
    
    private void buildConfigTemplate()
        throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (extendedData != null) &&
                (extendedData.length() > 0)
            ) {
                pw = new PrintWriter(new FileWriter(extendedData));
            } else {
                pw = new StringWriter();
            }
            
            pw.write(
                    "<EntityConfig xmlns=\"urn:sun:fm:SAML:2.0:entityconfig\"\n" +
                    "    xmlns:fm=\"urn:sun:fm:SAML:2.0:entityconfig\"\n" +
                    "    hosted=\"1\"\n" +
                    "    entityID=\"" + entityID + "\">\n\n");
            
            if (idpAlias != null) {
                buildIDPConfigTemplate(pw);
            }
            if (spAlias != null) {
                buildSPConfigTemplate(pw);
            }
            if (pdpAlias != null) {
                buildPDPConfigTemplate(pw);
            }
            if (pepAlias != null) {
                buildPEPConfigTemplate(pw);
            }
            
            pw.write("</EntityConfig>\n");
            
            if (!isWebBased) {
                Object[] objs = {extendedData, realm};
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-configuration-template"),
                    objs));
            }
        } catch (IOException ex) {
            Object[] objs = { extendedData };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }
    
    private void buildIDPConfigTemplate(Writer pw)
        throws IOException {
        pw.write(
                "    <IDPSSOConfig metaAlias=\"" + idpAlias + "\">\n" +
                "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS + "\">\n" +
                "            <Value>" + idpSCertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
                "            <Value>" + idpECertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ENABLED + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ATTRIBUTE + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" +
                SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE + "\">\n" +
                "            <Value>600</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.IDP_AUTHNCONTEXT_MAPPER_CLASS +
                "\">\n" +
                "            <Value>com.sun.identity.saml2.plugins.DefaultIDPAuthnContextMapper"
                + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.IDP_AUTHNCONTEXT_CLASSREF_MAPPING+
                "\">\n" +
                "            <Value>" + SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.IDP_ACCOUNT_MAPPER +"\">\n" +
                "            <Value>com.sun.identity.saml2.plugins.DefaultIDPAccountMapper" +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.IDP_ATTRIBUTE_MAPPER +"\">\n" +
                "            <Value>com.sun.identity.saml2.plugins.DefaultIDPAttributeMapper" +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.ATTRIBUTE_MAP +"\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_NAMEID_ENCRYPTED+"\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_ARTIFACT_RESOLVE_SIGNED +
                "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_LOGOUT_REQUEST_SIGNED +
                "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_LOGOUT_RESPONSE_SIGNED +
                "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_MNI_REQUEST_SIGNED +
                "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_MNI_RESPONSE_SIGNED +
                "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
                "        </Attribute>\n" +
                "    </IDPSSOConfig>\n"
                );
    }
    
    private void buildSPConfigTemplate(Writer pw)
        throws IOException {
        pw.write(
                "    <SPSSOConfig metaAlias=\"" + spAlias + "\">\n" +
                "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS + "\">\n" +
                "            <Value>" + spSCertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
                "            <Value>" + spECertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ENABLED + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.AUTO_FED_ATTRIBUTE + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.TRANSIENT_FED_USER + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.SP_ACCOUNT_MAPPER + "\">\n" +
                "            <Value>com.sun.identity.saml2.plugins.DefaultSPAccountMapper" +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.SP_ATTRIBUTE_MAPPER + "\">\n" +
                "            <Value>com.sun.identity.saml2.plugins.DefaultSPAttributeMapper" +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.SP_AUTHCONTEXT_MAPPER + "\">\n" +
                "            <Value>" + SAML2Constants.DEFAULT_SP_AUTHCONTEXT_MAPPER +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\""+ SAML2Constants.SP_AUTH_CONTEXT_CLASS_REF_ATTR+
                "\">\n" +
                "            <Value>" + SAML2Constants.SP_AUTHCONTEXT_CLASSREF_VALUE +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE +
                "\">\n" +
                "           <Value>" + SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE_VALUE +
                "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.ATTRIBUTE_MAP + "\">\n" +
                "           <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.AUTH_MODULE_NAME + "\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.LOCAL_AUTH_URL + "\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.INTERMEDIATE_URL + "\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.DEFAULT_RELAY_STATE + "\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.ASSERTION_TIME_SKEW+"\">\n" +
                "           <Value>300</Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_ATTRIBUTE_ENCRYPTED+"\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_ASSERTION_ENCRYPTED+"\">"
                + "\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_NAMEID_ENCRYPTED+"\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED +
                "\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_LOGOUT_REQUEST_SIGNED +
                "\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_LOGOUT_RESPONSE_SIGNED +
                "\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_MNI_REQUEST_SIGNED+"\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + SAML2Constants.WANT_MNI_RESPONSE_SIGNED+"\">\n" +
                "           <Value></Value>\n" +
                "       </Attribute>\n" +
                "       <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
                "       </Attribute>\n" +
                "    </SPSSOConfig>\n");
    }
    
    private void buildPDPConfigTemplate(Writer pw)
        throws IOException {
        pw.write(
                "    <XACMLPDPConfig metaAlias=\"" + pdpAlias + "\">\n" +
                "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS + "\">\n" +
                "            <Value>" + pdpSCertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
                "            <Value>" + pdpECertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON +  "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER +  "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD +  "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_XACML_AUTHZ_DECISION_QUERY_SIGNED +  "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_XACML_AUTHZ_DECISION_RESPONSED_SIGNED +  "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
                "        </Attribute>\n" +
                "   </XACMLPDPConfig>\n");
    }
    
    private void buildPEPConfigTemplate(Writer pw)
        throws IOException {
        pw.write(
                "   <XACMLAuthzDecisionQueryConfig metaAlias=\"" + pepAlias + "\">\n" +
                "        <Attribute name=\"" + SAML2Constants.SIGNING_CERT_ALIAS + "\">\n" +
                "            <Value>" + pepSCertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.ENCRYPTION_CERT_ALIAS + "\">\n" +
                "            <Value>" + pepECertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_ON +  "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_USER +  "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.BASIC_AUTH_PASSWD +  "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_XACML_AUTHZ_DECISION_QUERY_SIGNED +  "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + SAML2Constants.WANT_XACML_AUTHZ_DECISION_RESPONSED_SIGNED +  "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
                "        </Attribute>\n" +            
                "  </XACMLAuthzDecisionQueryConfig>\n");
    }
    
    private void buildIDFFConfigTemplate()
    throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (extendedData != null) && 
                (extendedData.length() > 0)
            ) {
                pw = new PrintWriter(new FileWriter(extendedData));
            } else {
                pw = new StringWriter();
            }
            pw.write(
                    "<EntityConfig xmlns=\"urn:sun:fm:ID-FF:entityconfig\"\n" +
                    "    hosted=\"1\"\n" +
                    "    entityID=\"" + entityID + "\">\n\n");
            
            if (idpAlias != null) {
                buildIDFFIDPConfigTemplate(pw);
            }
            if (spAlias != null) {
                buildIDFFSPConfigTemplate(pw);
            }

            pw.write("</EntityConfig>\n");

            if (!isWebBased) {
                Object[] objs = {extendedData, realm};
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-configuration-template"),
                    objs));
            }
        } catch (IOException ex) {
            Object[] objs = { extendedData };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                    ((StringWriter)pw).toString());
            }
        }
    }
    
    private void buildIDFFIDPConfigTemplate(Writer pw)
    throws IOException {
        pw.write(
                "    <IDPDescriptorConfig metaAlias=\"" + idpAlias + "\">\n" +
                "        <Attribute name=\"" + IFSConstants.PROVIDER_STATUS + "\">\n" +
                "            <Value>active</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.PROVIDER_DESCRIPTION + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.SIGNING_CERT_ALIAS + "\">\n" +
                "            <Value>" + idpSCertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENCRYPTION_CERT_ALIAS + "\">\n" +
                "            <Value>" + idpECertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENABLE_NAMEID_ENCRYPTION + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.GENERATE_BOOTSTRAPPING + "\">\n" +
                "            <Value>true</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.REALM_NAME + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.RESPONDS_WITH + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.FS_USER_PROVIDER_CLASS + "\">\n" +
                "            <Value>com.sun.identity.federation.accountmgmt.DefaultFSUserProvider</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.NAMEID_IMPL_CLASS + "\">\n" +
                "            <Value>com.sun.identity.federation.services.util.FSNameIdentifierImpl</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.AUTH_TYPE + "\">\n" +
                "            <Value>local</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.REGISTRATION_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.TERMINATION_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.LOGOUT_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.FEDERATION_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.DOFEDERATE_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.LISTOFCOTS_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ERROR_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.SSO_FAILURE_REDIRECT_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.PROVIDER_HOME_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ASSERTION_INTERVAL + "\">\n" +
                "            <Value>60</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.CLEANUP_INTERVAL + "\">\n" +
                "            <Value>180</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ARTIFACT_TIMEOUT + "\">\n" +
                "            <Value>120</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ASSERTION_LIMIT + "\">\n" +
                "            <Value>0</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ASSERTION_ISSUER + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ATTRIBUTE_PLUGIN + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.IDP_ATTRIBUTE_MAP + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.DEFAULT_AUTHNCONTEXT + "\">\n" +
                "            <Value>" + IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.IDP_AUTHNCONTEXT_MAPPING + "\">\n" +
                "            <Value>context=" + IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD + "|key=Module|value=DataStore|level=1</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENABLE_AUTO_FEDERATION + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.AUTO_FEDERATION_ATTRIBUTE + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ATTRIBUTE_MAPPER_CLASS + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "       <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
                "       </Attribute>\n" +
                "    </IDPDescriptorConfig>\n"
                );
    }
    
    private void buildIDFFSPConfigTemplate(Writer pw)
    throws IOException {
        pw.write(
                "    <SPDescriptorConfig metaAlias=\"" + spAlias + "\">\n" +
                "        <Attribute name=\"" + IFSConstants.PROVIDER_STATUS + "\">\n" +
                "            <Value>active</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.PROVIDER_DESCRIPTION + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.SIGNING_CERT_ALIAS + "\">\n" +
                "            <Value>" + spSCertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENCRYPTION_CERT_ALIAS + "\">\n" +
                "            <Value>" + spECertAlias + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENABLE_IDP_PROXY + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.IDP_PROXY_LIST + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.IDP_PROXY_COUNT + "\">\n" +
                "            <Value>-1</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.USE_INTRODUCTION_FOR_IDP_PROXY + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENABLE_AFFILIATION + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENABLE_NAMEID_ENCRYPTION + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.REALM_NAME + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.SUPPORTED_SSO_PROFILE + "\">\n" +
                "            <Value>http://projectliberty.org/profiles/brws-art</Value>\n" +
                "            <Value>http://projectliberty.org/profiles/brws-post</Value>\n" +
                "            <Value>http://projectliberty.org/profiles/wml-post</Value>\n" +
                "            <Value>http://projectliberty.org/profiles/lecp</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.NAMEID_POLICY + "\">\n" +
                "            <Value>federated</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.FORCE_AUTHN + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.IS_PASSIVE + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.RESPONDS_WITH + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.FS_USER_PROVIDER_CLASS + "\">\n" +
                "            <Value>com.sun.identity.federation.accountmgmt.DefaultFSUserProvider</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.NAMEID_IMPL_CLASS + "\">\n" +
                "            <Value>com.sun.identity.federation.services.util.FSNameIdentifierImpl</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.AUTH_TYPE + "\">\n" +
                "            <Value>remote</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.REGISTRATION_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.TERMINATION_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.LOGOUT_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.FEDERATION_DONE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.DOFEDERATE_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.LISTOFCOTS_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ERROR_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.SSO_FAILURE_REDIRECT_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.PROVIDER_HOME_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.DEFAULT_AUTHNCONTEXT + "\">\n" +
                "            <Value>" + IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD + "</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.SP_AUTHNCONTEXT_MAPPING + "\">\n" +
                "            <Value>context=" + IFSConstants.DEFAULT_AUTHNCONTEXT_PASSWORD + "|level=1</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ENABLE_AUTO_FEDERATION + "\">\n" +
                "            <Value>false</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.AUTO_FEDERATION_ATTRIBUTE + "\">\n"+
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ATTRIBUTE_MAPPER_CLASS + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.SP_ATTRIBUTE_MAP + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.FEDERATION_SP_ADAPTER + "\">\n" +
                "            <Value>com.sun.identity.federation.plugins.FSDefaultSPAdapter</Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.FEDERATION_SP_ADAPTER_ENV + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + COTConstants.COT_LIST + "\">\n" +
                "        </Attribute>\n" +
                "    </SPDescriptorConfig>\n");
    }
    
    private void buildIDFFDescriptorTemplate()
    throws CLIException {
        String url =  protocol + "://" + host + ":" + port + deploymentURI;
        
        Writer pw = null;
        try {
            if (!isWebBased && (metadata != null) && (metadata.length() > 0)) {
                pw = new PrintWriter(new FileWriter(metadata));
            } else {
                pw = new StringWriter();
            }
            pw.write(
                    "<EntityDescriptor\n" +
                    "    xmlns=\"urn:liberty:metadata:2003-08\"\n" +
                    "    providerID=\"" + entityID + "\">\n");
            
            if (idpAlias != null) {
                addIDFFIdentityProviderTemplate(pw, url);
            }
            if (spAlias != null) {
                addIDFFServiceProviderTemplate(pw, url);
            }
            pw.write("</EntityDescriptor>\n");
            
            if (!isWebBased) {
                Object[] objs = { metadata, realm };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-descriptor-template"), objs));
            }
        } catch (IDFFMetaException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            Object[] objs = { metadata };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if ((pw != null) && (pw instanceof PrintWriter)) {
                ((PrintWriter)pw).close();
            } else {
                this.getOutputWriter().printlnMessage(
                        ((StringWriter)pw).toString());
            }
        }
    }
    
    
    private void addIDFFIdentityProviderTemplate(Writer pw, String url)
    throws IOException, IDFFMetaException {
        String maStr = buildMetaAliasInURI(idpAlias);
        
        pw.write(
                "    <IDPDescriptor\n" +
                "        protocolSupportEnumeration=" +
                "\"urn:liberty:iff:2003-08 urn:liberty:iff:2002-12\">\n"
                );
        
        String idpSX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
                idpSCertAlias);
        if (idpSX509Cert != null) {
            pw.write(
                    "        <KeyDescriptor use=\"signing\">\n" +
                    "            <KeyInfo xmlns=\"" + IDFFMetaSecurityUtils.NS_XMLSIG + "\">\n" +
                    "                <X509Data>\n" +
                    "                    <X509Certificate>\n" + idpSX509Cert +
                    "                    </X509Certificate>\n" +
                    "                </X509Data>\n" +
                    "            </KeyInfo>\n" +
                    "        </KeyDescriptor>\n");
        }
            
        String idpEX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
            idpECertAlias);
        if (idpEX509Cert != null) {
            pw.write(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <EncryptionMethod>http://www.w3.org/2001/04/xmlenc#aes128-cbc</EncryptionMethod>\n" +
                "            <KeySize>128</KeySize>\n" +
                "            <KeyInfo xmlns=\"" + IDFFMetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + idpEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        pw.write(
                "        <SoapEndpoint>" + url + "/SOAPReceiver" + maStr + "</SoapEndpoint>\n" +
                "        <SingleLogoutServiceURL>" + url + "/ProcessLogout" + maStr
                + "</SingleLogoutServiceURL>\n" +
                "        <SingleLogoutServiceReturnURL>" + url + "/ReturnLogout" + maStr
                + "</SingleLogoutServiceReturnURL>\n" +
                "        <FederationTerminationServiceURL>" + url + "/ProcessTermination"
                + maStr + "</FederationTerminationServiceURL>\n" +
                "        <FederationTerminationServiceReturnURL>" + url + "/ReturnTermination"
                + maStr + "</FederationTerminationServiceReturnURL>\n" +
                "        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-sp-http</FederationTerminationNotificationProtocolProfile>\n" +
                "        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-sp-soap</FederationTerminationNotificationProtocolProfile>\n" +
                "        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-sp-http</SingleLogoutProtocolProfile>\n" +
                "        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-sp-soap</SingleLogoutProtocolProfile>\n" +
                "        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-sp-http</RegisterNameIdentifierProtocolProfile>\n" +
                "        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-sp-soap</RegisterNameIdentifierProtocolProfile>\n" +
                "        <RegisterNameIdentifierServiceURL>" + url + "/ProcessRegistration"
                + maStr + "</RegisterNameIdentifierServiceURL>\n" +
                "        <RegisterNameIdentifierServiceReturnURL>" + url + "/ReturnRegistration"
                + maStr + "</RegisterNameIdentifierServiceReturnURL>\n" +
                "        <SingleSignOnServiceURL>" + url + "/SingleSignOnService" + maStr
                + "</SingleSignOnServiceURL>\n" +
                "        <SingleSignOnProtocolProfile>http://projectliberty.org/profiles/brws-art</SingleSignOnProtocolProfile>\n" +
                "        <SingleSignOnProtocolProfile>http://projectliberty.org/profiles/brws-post</SingleSignOnProtocolProfile>\n" +
                "        <SingleSignOnProtocolProfile>http://projectliberty.org/profiles/lecp</SingleSignOnProtocolProfile>\n" +
                "    </IDPDescriptor>\n"
                );
    }
    
    private void addIDFFServiceProviderTemplate(Writer pw, String url)
    throws IOException, IDFFMetaException {
        String maStr = buildMetaAliasInURI(spAlias);
        pw.write(
                "    <SPDescriptor\n" +
                "        protocolSupportEnumeration=\n" +
                "            \"urn:liberty:iff:2003-08 urn:liberty:iff:2002-12\">\n");
        
        String spSX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
            spSCertAlias);
        if (spSX509Cert != null) {
            pw.write(
                "        <KeyDescriptor use=\"signing\">\n" +
                "            <KeyInfo xmlns=\"" + IDFFMetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + spSX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
            
        String spEX509Cert = IDFFMetaSecurityUtils.buildX509Certificate(
            spECertAlias);
        if (spEX509Cert != null) {
            pw.write(
                "        <KeyDescriptor use=\"encryption\">\n" +
                "            <EncryptionMethod>http://www.w3.org/2001/04/xmlenc#aes128-cbc</EncryptionMethod>\n" +
                "            <KeySize>128</KeySize>\n" +
                "            <KeyInfo xmlns=\"" + IDFFMetaSecurityUtils.NS_XMLSIG + "\">\n" +
                "                <X509Data>\n" +
                "                    <X509Certificate>\n" + spEX509Cert +
                "                    </X509Certificate>\n" +
                "                </X509Data>\n" +
                "            </KeyInfo>\n" +
                "        </KeyDescriptor>\n");
        }
        pw.write(
                "        <SoapEndpoint>" + url + "/SOAPReceiver" + maStr + "</SoapEndpoint>\n" +
                "        <SingleLogoutServiceURL>" + url + "/ProcessLogout" + maStr
                + "</SingleLogoutServiceURL>\n" +
                "        <SingleLogoutServiceReturnURL>" + url + "/ReturnLogout" + maStr
                + "</SingleLogoutServiceReturnURL>\n" +
                "        <FederationTerminationServiceURL>" + url + "/ProcessTermination"
                + maStr + "</FederationTerminationServiceURL>\n" +
                "        <FederationTerminationServiceReturnURL>" + url + "/ReturnTermination"
                + maStr + "</FederationTerminationServiceReturnURL>\n" +
                "        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-idp-http</FederationTerminationNotificationProtocolProfile>\n" +
                "        <FederationTerminationNotificationProtocolProfile>http://projectliberty.org/profiles/fedterm-idp-soap</FederationTerminationNotificationProtocolProfile>\n" +
                "        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-idp-http</SingleLogoutProtocolProfile>\n" +
                "        <SingleLogoutProtocolProfile>http://projectliberty.org/profiles/slo-idp-soap</SingleLogoutProtocolProfile>\n" +
                "        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-idp-http</RegisterNameIdentifierProtocolProfile>\n" +
                "        <RegisterNameIdentifierProtocolProfile>http://projectliberty.org/profiles/rni-idp-soap</RegisterNameIdentifierProtocolProfile>\n" +
                "        <RegisterNameIdentifierServiceURL>" + url + "/ProcessRegistration"
                + maStr + "</RegisterNameIdentifierServiceURL>\n" +
                "        <RegisterNameIdentifierServiceReturnURL>" + url + "/ReturnRegistration"
                + maStr + "</RegisterNameIdentifierServiceReturnURL>\n" +
                "        <AssertionConsumerServiceURL id=\"1\" isDefault=\"true\">" + url
                + "/AssertionConsumerService" + maStr
                + "</AssertionConsumerServiceURL>\n" +
                "        <AuthnRequestsSigned>false</AuthnRequestsSigned>\n" +
                "    </SPDescriptor>\n"
                );
    }
}
