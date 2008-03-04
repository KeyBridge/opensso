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
 * $Id: CreateMetaDataTemplate.java,v 1.29 2008-03-04 23:42:16 hengming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.federation.cli;

import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cot.COTConstants;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaSecurityUtils;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.meta.SAML2MetaSecurityUtils;
import com.sun.identity.workflow.CreateSAML2HostedProviderTemplate;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.ClaimType;
import com.sun.identity.wsfederation.jaxb.wsfederation.DisplayNameType;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.SingleSignOutNotificationEndpointElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerEndpointElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenIssuerNameElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenSigningKeyInfoElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenType;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenTypesOfferedElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.UriNamedClaimTypesOfferedElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.security.cert.CertificateEncodingException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.SecurityTokenReference;
import org.w3._2000._09.xmldsig_.X509Data;
import org.w3._2000._09.xmldsig_.X509DataType.X509Certificate;
import org.w3._2005._08.addressing.AttributedURIType;

/**
 * Create Meta Data Template.
 */
public class CreateMetaDataTemplate extends AuthenticatedCommand {
    
    private String entityID;
    private String metadata;
    private String extendedData;
    private String idpAlias;
    private String spAlias;
    private String attraAlias;
    private String attrqAlias;
    private String authnaAlias;
    private String pdpAlias;
    private String pepAlias;
    private String idpSCertAlias;
    private String idpECertAlias;
    private String attraSCertAlias;
    private String attraECertAlias;
    private String authnaSCertAlias;
    private String authnaECertAlias;
    private String pdpSCertAlias;
    private String pdpECertAlias;
    private String spSCertAlias;
    private String spECertAlias;
    private String attrqSCertAlias;
    private String attrqECertAlias;
    private String affiAlias;
    private List   affiMembers;
    private String affiSCertAlias;
    private String affiECertAlias;
    private String pepSCertAlias;
    private String pepECertAlias;
    private String protocol;
    private String host;
    private String port;
    private String deploymentURI;
    private String realm;
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
        getOptions(rc);
        validateOptions();
        normalizeOptions();
        
        String spec = FederationManager.getIDFFSubCommandSpecification(rc);
        if (spec.equals(FederationManager.DEFAULT_SPECIFICATION)) {
            handleSAML2Request(rc);
        } else if (spec.equals(FedCLIConstants.IDFF_SPECIFICATION)) {
            handleIDFFRequest(rc);
        } else if (spec.equals(FedCLIConstants.WSFED_SPECIFICATION)) {
            handleWSFedRequest(rc);
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
    
    private void handleWSFedRequest(RequestContext rc)
    throws CLIException {
        String url =  protocol + "://" + host + ":" + port + deploymentURI;

        if (!isWebBased || (extendedData != null)) {
            buildWSFedConfigTemplate(url);
        }

        if (!isWebBased || (metadata != null)) {
            buildWSFedDescriptorTemplate(url);
        }
    }
    
    private void getOptions(RequestContext rc) {
        entityID = getStringOptionValue(FedCLIConstants.ARGUMENT_ENTITY_ID);
        idpAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_IDENTITY_PROVIDER);
        spAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_SERVICE_PROVIDER);
        attraAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRIBUTE_AUTHORITY);
        attrqAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRIBUTE_QUERY_PROVIDER);
        authnaAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AUTHN_AUTHORITY);
        pdpAlias = getStringOptionValue(FedCLIConstants.ARGUMENT_PDP);
        pepAlias = getStringOptionValue(FedCLIConstants.ARGUMENT_PEP);
        affiAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AFFILIATION);
        affiMembers = (List)rc.getOption(
            FedCLIConstants.ARGUMENT_AFFI_MEMBERS);
        
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
        
        attraSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRA_S_CERT_ALIAS);
        attraECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRA_E_CERT_ALIAS);
        
        attrqSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRQ_S_CERT_ALIAS);
        attrqECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_ATTRQ_E_CERT_ALIAS);

        authnaSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AUTHNA_S_CERT_ALIAS);
        authnaECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AUTHNA_E_CERT_ALIAS);

        affiSCertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AFFI_S_CERT_ALIAS);
        affiECertAlias = getStringOptionValue(
            FedCLIConstants.ARGUMENT_AFFI_E_CERT_ALIAS);

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
        if ((attraAlias != null) && !attraAlias.startsWith("/")) {
            attraAlias = "/" + attraAlias;
        }
        if ((attrqAlias != null) && !attrqAlias.startsWith("/")) {
            attrqAlias = "/" + attrqAlias;
        }
        if ((authnaAlias != null) && !authnaAlias.startsWith("/")) {
            authnaAlias = "/" + authnaAlias;
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
        if (attraSCertAlias == null) {
            attraSCertAlias = "";
        }
        if (attraECertAlias == null) {
            attraECertAlias = "";
        }
        if (attrqSCertAlias == null) {
            attrqSCertAlias = "";
        }
        if (attrqECertAlias == null) {
            attrqECertAlias = "";
        }
        if (authnaSCertAlias == null) {
            authnaSCertAlias = "";
        }
        if (authnaECertAlias == null) {
            authnaECertAlias = "";
        }
        if (affiSCertAlias == null) {
            affiSCertAlias = "";
        }
        if (affiECertAlias == null) {
            affiECertAlias = "";
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
            (pepAlias == null) && (attraAlias == null) &&
            (attrqAlias == null) && (authnaAlias == null) &&
            (affiAlias == null)) {

            throw new CLIException(getResourceString(
                "create-meta-template-exception-role-null"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((affiAlias != null) && ((idpAlias != null) ||
            (spAlias != null) || (pdpAlias != null) || (pepAlias != null) ||
            (attraAlias != null) || (attrqAlias != null) ||
            (authnaAlias != null))) {

            throw new CLIException(getResourceString(
                "create-meta-template-exception-affi-conflict"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((affiAlias != null) &&
            ((affiMembers == null) || affiMembers.isEmpty())) {

            throw new CLIException(getResourceString(
                "create-meta-template-exception-affi-members-empty"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((affiAlias == null) &&
            ((affiSCertAlias != null) || (affiECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                    "create-meta-template-exception-affi-null-with-cert-alias"),
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
        
        if ((attraAlias == null) &&
            ((attraSCertAlias != null) || (attraECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-attra-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        if ((attrqAlias == null) &&
            ((attrqSCertAlias != null) || (attrqECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-attrq-null-with-cert-alias"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        if ((authnaAlias == null) &&
            ((authnaSCertAlias != null) || (authnaECertAlias != null))
            ) {
            throw new CLIException(getResourceString(
                "create-meta-template-exception-authna-null-with-cert-alias"),
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
        Writer pw = null;
        try {
            if (!isWebBased && (metadata != null) && (metadata.length() > 0)) {
                pw = new PrintWriter(new FileWriter(metadata));
            } else {
                pw = new StringWriter();
            }

            String xml =
                CreateSAML2HostedProviderTemplate.buildMetaDataTemplate(
                    entityID, getWorkflowParamMap());
            pw.write(xml);
            
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

            String xml =
                CreateSAML2HostedProviderTemplate.createExtendedDataTemplate(
                entityID, getWorkflowParamMap());
            pw.write(xml);
            
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
                realm = IDFFMetaUtils.getRealmByMetaAlias(idpAlias);
                buildIDFFIDPConfigTemplate(pw);
            }
            if (spAlias != null) {
                realm = IDFFMetaUtils.getRealmByMetaAlias(spAlias);
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
                "        <Attribute name=\"" + IFSConstants.LISTOFCOTS_PAGE_URL + "\">\n" +
                "            <Value></Value>\n" +
                "        </Attribute>\n" +
                "        <Attribute name=\"" + IFSConstants.ERROR_PAGE_URL + "\">\n" +
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
                realm = IDFFMetaUtils.getRealmByMetaAlias(idpAlias);
                addIDFFIdentityProviderTemplate(pw, url);
            }
            if (spAlias != null) {
                realm = IDFFMetaUtils.getRealmByMetaAlias(spAlias);
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
    
    private void buildWSFedDescriptorTemplate(String url)
    throws CLIException {
        Writer pw = null;
        try {
            if (!isWebBased && (metadata != null) && (metadata.length() > 0)) {
                pw = new PrintWriter(new FileWriter(metadata));
            } else {
                pw = new StringWriter();
            }
            
            JAXBContext jc = WSFederationMetaUtils.getMetaJAXBContext();
            com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory 
                objFactory = new 
                com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory();
            
            FederationElement fed = objFactory.createFederationElement();
            fed.setFederationID(entityID);

            if (idpAlias != null) {
                addWSFedIdentityProviderTemplate(objFactory, fed, url);
            }
            if (spAlias != null) {
                addWSFedServiceProviderTemplate(objFactory, fed, url);
            }

            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(fed, pw);
            
            if (!isWebBased) {
                Object[] objs = { metadata, realm };
                getOutputWriter().printlnMessage(MessageFormat.format(
                    getResourceString(
                    "create-meta-template-created-descriptor-template"), objs));
            }
        } catch (IOException e) {
            Object[] objs = { metadata };
            throw new CLIException(MessageFormat.format(
                    getResourceString("cannot-write-to-file"), objs),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (JAXBException e) {
            throw new CLIException(e.getMessage(),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (CertificateEncodingException e) {
            throw new CLIException(e.getMessage(),
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

    private void addWSFedIdentityProviderTemplate(
        com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory 
        objFactory, FederationElement fed, String url)
        throws JAXBException, CertificateEncodingException {
        String maStr = buildMetaAliasInURI(idpAlias);
        
        if (idpSCertAlias.length() > 0) {
            org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.ObjectFactory 
                secextObjFactory = new 
                org.oasis_open.docs.wss._2004._01.oasis_200401_wss_wssecurity_secext_1_0.ObjectFactory();
            org.w3._2000._09.xmldsig_.ObjectFactory dsObjectFactory = new 
                org.w3._2000._09.xmldsig_.ObjectFactory();

            TokenSigningKeyInfoElement tski = 
                objFactory.createTokenSigningKeyInfoElement();
            SecurityTokenReference str = 
                secextObjFactory.createSecurityTokenReference();
            X509Data x509Data = dsObjectFactory.createX509Data();
            X509Certificate x509Cert = 
                dsObjectFactory.createX509DataTypeX509Certificate();
            x509Cert.setValue(
                KeyUtil.getKeyProviderInstance().getX509Certificate(idpSCertAlias).getEncoded());
            x509Data.getX509IssuerSerialOrX509SKIOrX509SubjectName().add(x509Cert);
            str.getAny().add(x509Data);
            tski.setSecurityTokenReference(str);
            fed.getAny().add(tski);
        }
        
        TokenIssuerNameElement tin = objFactory.createTokenIssuerNameElement();
        tin.setValue(entityID);
        fed.getAny().add(tin);
        
        TokenIssuerEndpointElement tie = 
            objFactory.createTokenIssuerEndpointElement();
        org.w3._2005._08.addressing.ObjectFactory addrObjFactory = 
            new org.w3._2005._08.addressing.ObjectFactory();
        AttributedURIType auri = addrObjFactory.createAttributedURIType();
        auri.setValue(url + "/WSFederationServlet" + maStr);
        tie.setAddress(auri);        
        fed.getAny().add(tie);
        
        TokenTypesOfferedElement tto = 
            objFactory.createTokenTypesOfferedElement();
        TokenType tt = objFactory.createTokenType();
        tt.setUri(WSFederationConstants.URN_OASIS_NAMES_TC_SAML_11);
        tto.getTokenType().add(tt);
        fed.getAny().add(tto);
        
        UriNamedClaimTypesOfferedElement uncto = 
            objFactory.createUriNamedClaimTypesOfferedElement();
        ClaimType ct = objFactory.createClaimType();
        ct.setUri(WSFederationConstants.NAMED_CLAIM_TYPES[
            WSFederationConstants.NAMED_CLAIM_UPN]);
        DisplayNameType dnt = objFactory.createDisplayNameType();
        dnt.setValue(WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
            WSFederationConstants.NAMED_CLAIM_UPN]);
        ct.setDisplayName(dnt);
        uncto.getClaimType().add(ct);
        fed.getAny().add(uncto);
    }

    private void addWSFedServiceProviderTemplate(
        com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory 
        objFactory, FederationElement fed, String url)
        throws JAXBException {
        String maStr = buildMetaAliasInURI(spAlias);
        
        TokenIssuerNameElement tin = objFactory.createTokenIssuerNameElement();
        tin.setValue(entityID);
        fed.getAny().add(tin);
        
        TokenIssuerEndpointElement tie = 
            objFactory.createTokenIssuerEndpointElement();
        org.w3._2005._08.addressing.ObjectFactory addrObjFactory = 
            new org.w3._2005._08.addressing.ObjectFactory();
        AttributedURIType auri = addrObjFactory.createAttributedURIType();
        auri.setValue(url + "/WSFederationServlet" + maStr);
        tie.setAddress(auri);        
        fed.getAny().add(tie);

        SingleSignOutNotificationEndpointElement ssne = 
            objFactory.createSingleSignOutNotificationEndpointElement();
        AttributedURIType ssneUri = addrObjFactory.createAttributedURIType();
        ssneUri.setValue(url + "/WSFederationServlet" + maStr);
        ssne.setAddress(auri);        
        fed.getAny().add(ssne);
    }

    private void buildWSFedConfigTemplate(String url)
    throws CLIException {
        JAXBContext jc = WSFederationMetaUtils.getMetaJAXBContext();
        com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory 
            objFactory = 
            new com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory();

        Writer pw = null;
        try {
            if (!isWebBased && (extendedData != null) &&
                (extendedData.length() > 0)
            ) {
                pw = new PrintWriter(new FileWriter(extendedData));
            } else {
                pw = new StringWriter();
            }

            FederationConfigElement fedConfig = 
                objFactory.createFederationConfigElement();

            fedConfig.setFederationID(entityID);
            fedConfig.setHosted(true);

            if (idpAlias != null) {
                buildWSFedIDPConfigTemplate(objFactory, fedConfig, url);
            }
            if (spAlias != null) {
                buildWSFedSPConfigTemplate(objFactory, fedConfig, url);
            }

            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.marshal(fedConfig, pw);

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
        } catch (JAXBException e) {
            throw new CLIException(e.getMessage(),
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

    private void buildWSFedIDPConfigTemplate(
        com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory 
        objFactory, FederationConfigElement fedConfig, String url)
        throws JAXBException
    {
        String[][] configDefaults = { 
            { WSFederationConstants.DISPLAY_NAME, idpAlias },
            { WSFederationConstants.UPN_DOMAIN, "" },
            { SAML2Constants.SIGNING_CERT_ALIAS, idpSCertAlias },
            { SAML2Constants.ASSERTION_NOTBEFORE_SKEW_ATTRIBUTE, "600" },
            { SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE, "600" },
            { SAML2Constants.IDP_AUTHNCONTEXT_MAPPER_CLASS, 
                  "com.sun.identity.wsfederation.plugins.DefaultIDPAuthenticationMethodMapper" 
            },
            { SAML2Constants.IDP_ACCOUNT_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultIDPAccountMapper" },
            { SAML2Constants.IDP_ATTRIBUTE_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultIDPAttributeMapper" },
            { SAML2Constants.ATTRIBUTE_MAP, "" },
            { COTConstants.COT_LIST, null },
        };

        com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement 
            idpSSOConfig = objFactory.createIDPSSOConfigElement();

        idpSSOConfig.setMetaAlias(idpAlias);

        for ( int i = 0; i < configDefaults.length; i++ )
        {
            com.sun.identity.wsfederation.jaxb.entityconfig.AttributeElement 
                attribute = objFactory.createAttributeElement();
            attribute.setName(configDefaults[i][0]);
            if (configDefaults[i][1] != null) {
                attribute.getValue().add(configDefaults[i][1]);
            }

            idpSSOConfig.getAttribute().add(attribute);
        }
        
        fedConfig.getIDPSSOConfigOrSPSSOConfig().add(idpSSOConfig);
    }

    private void buildWSFedSPConfigTemplate(
        com.sun.identity.wsfederation.jaxb.entityconfig.ObjectFactory 
        objFactory, FederationConfigElement fedConfig, String url)
    throws JAXBException
    {
        String maStr = buildMetaAliasInURI(spAlias);

        String[][] configDefaults = { 
            { WSFederationConstants.DISPLAY_NAME, spAlias },
            { WSFederationConstants.ACCOUNT_REALM_SELECTION, "cookie" },
            { WSFederationConstants.ACCOUNT_REALM_COOKIE_NAME, 
                  "amWSFederationAccountRealm" },
            { WSFederationConstants.HOME_REALM_DISCOVERY_SERVICE, 
                  url + "/RealmSelection" + maStr },
            { SAML2Constants.SIGNING_CERT_ALIAS, 
                  ( idpSCertAlias.length() > 0 ) ? idpSCertAlias : "" },
            { SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE, "600" },
            { SAML2Constants.SP_ACCOUNT_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultADFSPartnerAccountMapper" },
            { SAML2Constants.SP_ATTRIBUTE_MAPPER, 
                  "com.sun.identity.wsfederation.plugins.DefaultSPAttributeMapper" },
            { SAML2Constants.SP_AUTHCONTEXT_MAPPER, 
                  SAML2Constants.DEFAULT_SP_AUTHCONTEXT_MAPPER },
            { SAML2Constants.SP_AUTH_CONTEXT_CLASS_REF_ATTR, 
                  SAML2Constants.SP_AUTHCONTEXT_CLASSREF_VALUE },
            { SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE, 
                  SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE_VALUE },
            { SAML2Constants.ATTRIBUTE_MAP, "" },
            { SAML2Constants.AUTH_MODULE_NAME, "" },
            { SAML2Constants.DEFAULT_RELAY_STATE, "" },
            { SAML2Constants.ASSERTION_TIME_SKEW, "300" },
            { SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED, "true" },
            { COTConstants.COT_LIST, null },
        };

        com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement 
            spSSOConfig = objFactory.createSPSSOConfigElement();

        spSSOConfig.setMetaAlias(spAlias);

        for ( int i = 0; i < configDefaults.length; i++ )
        {
            com.sun.identity.wsfederation.jaxb.entityconfig.AttributeElement 
                attribute = objFactory.createAttributeElement();
            attribute.setName(configDefaults[i][0]);
            if (configDefaults[i][1] != null) {
                attribute.getValue().add(configDefaults[i][1]);
            }

            spSSOConfig.getAttribute().add(attribute);
        }
        
        fedConfig.getIDPSSOConfigOrSPSSOConfig().add(spSSOConfig);
    }

    private Map getWorkflowParamMap() {
        Map map = new HashMap();
        map.put(CreateSAML2HostedProviderTemplate.P_IDP, idpAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_SP, spAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_ATTR_AUTHORITY, attraAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_ATTR_QUERY_PROVIDER,
            attrqAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_AUTHN_AUTHORITY, authnaAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_AFFILIATION, affiAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_AFFI_MEMBERS, affiMembers);
        map.put(CreateSAML2HostedProviderTemplate.P_PDP, pdpAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_PEP, pepAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_IDP_E_CERT, idpECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_IDP_S_CERT, idpSCertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_SP_E_CERT, spECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_SP_S_CERT, spSCertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_ATTR_AUTHORITY_E_CERT,
            attraECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_ATTR_AUTHORITY_S_CERT,
            attraSCertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_ATTR_QUERY_PROVIDER_E_CERT,
            attrqECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_ATTR_QUERY_PROVIDER_S_CERT,
            attrqSCertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_AUTHN_AUTHORITY_E_CERT,
            authnaECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_AUTHN_AUTHORITY_S_CERT,
            authnaSCertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_AFFI_E_CERT,
            affiECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_AFFI_S_CERT,
            affiSCertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_PDP_E_CERT, pdpECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_PDP_S_CERT, pdpSCertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_PEP_E_CERT, pepECertAlias);
        map.put(CreateSAML2HostedProviderTemplate.P_PEP_S_CERT, pepSCertAlias);
        return map;
    }
}
