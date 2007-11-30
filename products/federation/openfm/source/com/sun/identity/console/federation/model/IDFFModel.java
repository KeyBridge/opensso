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
 * $Id: IDFFModel.java,v 1.1 2007-11-30 01:11:20 asyhuang Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.liberty.ws.meta.jaxb.IDPDescriptorType;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;
import java.util.Set;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;

public interface IDFFModel
    extends EntityModel 
{
    
    public static final String ATTR_PROVIDER_ALIAS = "tfAlias";
    public static final String ATTR_PROVIDER_TYPE = "tfProviderType";
    
    public static final String ATTR_SERVER_NAME_IDENTIFIER_MAPPING =
        "elistServerNameIdentifierMapping";
    
    // standard meta data
    public static final String ATTR_XMLNS = "xmlns";
    public static final String ATTR_PROVIDER_ID = "providerID";
    public static final String ATTR_PROTOCOL_SUPPORT_ENUMERATION =
        "txtProtocolSupportEnum";
    public static final String ATTR_SOAP_END_POINT =
        "tfSOAPEndpointURL";
    public static final String ATTR_SINGLE_SIGN_ON_SERVICE_URL =
        "tfSingleSignOnServiceURL";
    public static final String ATTR_SINGLE_LOGOUT_SERVICE_URL =
        "tfSingleLogoutServiceURL";
    public static final String ATTR_SINGLE_LOGOUT_SERVICE_RETURN_URL =
        "tfSingleLogoutReturnURL";
    public static final String ATTR_FEDERATION_TERMINATION_SERVICES_URL =
        "tfFederationTerminationServiceURL";
    public static final String ATTR_FEDERATION_TERMINATION_SERVICE_RETURN_URL =
        "tfFederationTerminationReturnURL";
    public static final String ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_URL =
        "tfNameRegistrationServiceURL";
    public static final
        String ATTR_REGISTRATION_NAME_IDENTIFIER_SERVICE_RETURN_URL =
        "tfNameRegistrationReturnURL";
    
    // communication profiles
    public static final String
        ATTR_FEDERATION_TERMINATION_NOTIFICATION_PROTOCOL_PROFILE =
        "singleChoiceFederationTerminationProfile";
    public static final String ATTR_SINGLE_LOGOUT_PROTOCOL_PROFILE =
        "singleChoiceSingleLogoutProfile";
    public static final
        String ATTR_REGISTRATION_NAME_IDENTIFIER_PROFILE_PROFILE =
        "singleChoiceNameRegistrationProfile";
    public static final String ATTR_SINGLE_SIGN_ON_PROTOCOL_PROFILE =
        "singleChoiceFederationProfile";
    
    //KeyDescriptor property.
    public static final String ATTR_SIGNING_KEY_ALIAS =
        "tfSigningKeyAlias";
    public static final String ATTR_ENCRYPTION_KEY_ALIAS =
        "tfEncryptionKeyAlias";
    public static final String ATTR_EMCRYPTION_KEY_SIZE =
        "tfEncryptionKeySize";
    public static final String ATTR_ENCRYPTION_METHOD =
        "singleChoiceEncryptionMethod";
    public static final String ATTR_ENABLE_NAME_IDENTIFIER_ENCRYPTION =
        "cbEnableNameIdentifierEncryption";
    
    // SP standard meta Assertion Consumer Service URL property.
    public static final String ATTR_ASSERTION_CUSTOMER_SERVICE_URIID =
        "tfAssertionConsumerServiceURLID";
    public static final String ATTR_ASSERTION_CUSTOMER_SERVICE_URL =
        "tfAssertionConsumerServiceURL";
    public static final String ATTR_ASSERTION_CUSTOMER_SERVICE_URL_AS_DEFAULT =
        "cbAssertionConsumerServiceURLasDefault";
    public static final String ATTR_AUTHN_REQUESTS_SIGNED =
        "cbAuthnRequestsSigned";
    
    // BOTH idp AND SP extended metadata
    public static final String ATTR_DO_FEDERATION_PAGE_URL =
        "doFederatePageURL";
    public static final String ATTR_ATTRIBUTE_MAPPER_CLASS =
        "attributeMapperClass";
    public static final String ATTR_ENABLE_AUTO_FEDERATION =
        "enableAutoFederation";
    public static final String ATTR_REGISTERATION_DONE_URL =
        "registrationDoneURL";
    public static final String ATTR_COT_LIST =
        "cotlist";
    public static final String ATTR_RESPONSD_WITH =
        "responsdWith";
    public static final String ATTR_ENABLE_NAME_ID_ENCRYPTION =
        "enableNameIDEncryption";
    public static final String ATTR_SSO_FAILURE_REDIRECT_URL =
        "ssoFailureRedirectURL";
    public static final String ATTR_LIST_OF_COTS_PAGE_URL =
        "listOfCOTsPageURL";
    public static final String ATTR_DEFAULT_AUTHN_CONTEXT =
        "defaultAuthnContext";
    public static final String ATTR_SIGNING_CERT_ALIAS =
        "signingCertAlias";
    public static final String ATTR_REALM_NAME =
        "realmName";
    public static final String ATTR_USER_PROVIDER_CLASS =
        "userProviderClass";
    public static final String ATTR_NAME_ID_IMPLEMENETATION_CLASS =
        "nameIDImplementationClass";
    public static final String ATTR_FEDERATION_DONE_URL =
        "federationDoneURL";
    public static final String ATTR_AUTH_TYPE =
        "authType";
    public static final String ATTR_ENCRYPTION_CERT_ALIAS =
        "encryptionCertAlias";
    public static final String ATTR_TERMINATION_DONE_URL =
        "terminationDoneURL";
    public static final String ATTR_AUTO_FEDERATION_ATTRIBUTE =
        "autoFederationAttribute";
    public static final String ATTR_ERROR_PAGE_URL =
        "errorPageURL";
    public static final String ATTR_PROVIDER_STATUS =
        "providerStatus";
    public static final String ATTR_PROVIDER_DESCRIPTION =
        "providerDescription";
    public static final String ATTR_LOGOUT_DONE_URL =
        "logoutDoneURL";
    public static final String ATTR_PROVIDER_HOME_PAGE_URL =
        "providerHomePageURL";
    
    // IDP extend meta attribute ONLY IDP
    
    // idp
    public static final String ATTR_ASSERTION_LIMIT =
        "assertionLimit";
    public static final String ATTR_ATTRIBUTE_PLUG_IN =
        "attributePlugin";
    public static final String ATTR_IDP_ATTRIBUTE_MAP =
        "idpAttributeMap";
    public static final String ATTR_ASSERTION_ISSUER =
        "assertionIssuer";
    public static final String ATTR_CLEANUP_INTERVAL =
        "cleanupInterval";
    public static final String ATTR_IDP_AUTHN_CONTEXT_MAPPING =
        "idpAuthnContextMapping";
    public static final String ATTR_GERNERATE_BOOT_STRAPPING =
        "generateBootstrapping";
    public static final String ATTR_ARTIFACT_TIMEOUT =
        "artifactTimeout";
    public static final String ATTR_ASSERTION_INTERVAL =
        "assertionInterval";
    
    
    // SP extend meta attribute.. ONLY SP
    
    public static final String ATTR_IS_PASSIVE =
        "isPassive";
    public static final String ATTR_SP_ATTRIBUTE_MAP =
        "spAttributeMap";
    public static final String ATTR_SP_AUTHN_CONTEXT_MAPPING =
        "spAuthnContextMapping";
    public static final String ATTR_IDP_PROXY_LIST =
        "idpProxyList";
    public static final String ATTR_ENABLE_IDP_PROXY =
        "enableIDPProxy";
    public static final String ATTR_NAME_ID_POLICY =
        "nameIDPolicy";
    public static final String ATTR_FEDERATION_SP_ADAPTER_ENV =
        "federationSPAdapterEnv";
    public static final String ATTR_ENABLE_AFFILIATION =
        "enableAffiliation";
    public static final String ATTR_FORCE_AUTHN =
        "forceAuthn";
    public static final String ATTR_IDP_PROXY_COUNT =
        "idpProxyCount";
    public static final String ATTR_FEDERATION_SP_ADAPTER =
        "federationSPAdapter";
    public static final String ATTR_USE_INTRODUCTION_FOR_IDP_PROXY =
        "useIntroductionForIDPProxy";
    public static final String ATTR_SUPPORTED_SSO_PROFILE =
        "supportedSSOProfile";
    
    /**
     * General Page attributes
     */
    /* Attribute Name for Entity Descriptor Description. */
    String ATTR_DESCRIPTION = "tfDescription";
    
    /* Attribute Name for Entity Descriptor Valid Until.  */
    String ATTR_VALID_UNTIL = "tfValidUntil";
    
    /* Attribute Name for Entity Descriptor Cache Duration.  */
    String ATTR_CACHE_DURATION = "tfCacheDuration";
    
    
    /**
     * Affiliate
     */
    
    /* Attribute name of affiliate ID. */
    public static final String ATTR_AFFILIATE_ID =
        "tfAffiliateID";
    
    /* Attribute name of affiliate Owner ID. */
    public static final String ATTR_AFFILIATE_OWNER_ID =
        "tfAffiliateOwnerID";
    
    /* Attribute name of affiliate's Valid Until. */
    public static final String ATTR_AFFILIATE_VALID_UNTIL =
        "tfAffiliateValidUntil";
    
    /* Attribute name of affiliate's Cache Duration.  */
    public static final String ATTR_AFFILIATE_CACHE_DURATION =
        "tfAffiliateCacheDuration";
    
    /* Attribute name of Signing Key's Key Alias.   */
    public static final String ATTR_AFFILIATE_SIGNING_KEY_ALIAS =
        "tfSigningKeyAlias";
    
    /* Attribute name of Encryption Key's Key Alias.  */
    public static final String ATTR_AFFILIATE_ENCRYPTION_KEY_ALIAS =
        "tfEncryptionKeyAlias";
    
    /* Attribute name of Encryption Key's Key Size. */
    public static final String ATTR_AFFILIATE_ENCRYPTION_KEY_SIZE =
        "tfEncryptionKeySize";
    
    /* Attribute name of Encryption Key's Key Method.  */
    public static final String ATTR_AFFILIATE_ENCRYPTION_KEY_METHOD =
        "tfEncryptionMethod";
    
    /* Attribute name of Affiliate Members. */
    public static final String ATTR_AFFILIATE_MEMBERS =
        "arlistAffiliateMembers";
    
    
    /**
     * Returns provider-affiliate common attribute values.
     *
     * @param realm the realm in which the entity resides.
     * @param entityName Name of Entity Descriptor.
     * @return provider-affiliate common attribute values.
     * @throws AMConsoleException if attribute values cannot be obtained.
     */
    public Map getCommonAttributeValues(String realm, String entityName)
        throws AMConsoleException;
    
    /**
     * Modifies entity descriptor profile.
     *
     * @param realm the realm in which the entity resides.
     * @param entityName Name of entity descriptor.
     * @param map Map of attribute type to a Map of attribute name to values.
     * @throws AMConsoleException if profile cannot be modified.
     */
    public void modifyEntityProfile(String realm, String entityName, Map map)
        throws AMConsoleException;
    
    /**
     * Returns Map values of IDP Descriptor.
     *
     * @param realm Realm of Entity.
     * @param entityName name of Entity Descriptor.
     */
    public Map getEntityIDPDescriptor(String realm, String entityName)
        throws AMConsoleException;
    
    /**
     * Returns Map values of SP Descriptor.
     *
     * @param entityName name of Entity Descriptor.
     * @param realm Realm of Entity.
     */
    public Map getEntitySPDescriptor(String entityName, String realm)
        throws AMConsoleException;
    
    /**
     * Returns attributes values of provider.
     *
     * @param entityName Name of Entity Descriptor.
     * @param realm Realm of Entity
     * @param location Location of provider such as Hosted or Remote.
     * @return attributes values of provider.
     */
    public Map getIDPEntityConfig(
        String entityName,
        String realm,
        String location
    ) throws AMConsoleException ;
    
    /**
     * Returns attributes values of provider.
     *
     * @param realm Realm of Entity
     * @param entityName Name of Entity Descriptor.
     * @param location Location of provider such as Hosted or Remote.
     * @return attributes values of provider.
     */
    public Map getSPEntityConfig(
        String realm,
        String entityName,
        String location
    ) throws AMConsoleException ;
    
    /**
     * updateEntitySPDescriptor
     * Modifies a service provider's standard metadata.
     *
     * @param entityName Name of Entity Descriptor.
     * @param realm Realm of Entity
     * @param attrValues Map of attribute name to set of values.
     * @throws AMConsoleException if provider cannot be modified.
     */
    public void updateEntitySPDescriptor(
        String realm,
        String entityName,
        Map attrValues
    ) throws AMConsoleException ;
    
    /**
     * updateEntityIDPDescriptor
     * Modifies a identity provider's standard metadata.
     *
     * @param entityName Name of Entity Descriptor.
     * @param realm Realm of Entity
     * @param attrValues Map of attribute name to set of values.
     * @throws AMConsoleException if provider cannot be modified.
     */
    public void updateEntityIDPDescriptor(
        String realm,
        String entityName,
        Map attrValues
    ) throws AMConsoleException ;
    
    /**
     * updateIDPEntityConfig
     * Modifies a provider's extended metadata.
     *
     * @param entityName Name of Entity Descriptor.
     * @param realm Realm of Entity
     * @param role Role of provider. (SP or IDP)
     * @param attrValues Map of attribute name to set of values.
     * @throws AMConsoleException if provider cannot be modified.
     */
    public void updateIDPEntityConfig(
        String realm,
        String entityName,
        Map attrValues
    ) throws AMConsoleException;
    
    /**
     * updateSPEntityConfig
     * Modifies a provider's extended metadata.
     *
     * @param entityName Name of Entity Descriptor.
     * @param realm Realm of Entity
     * @param attrValues Map of attribute name to set of values.
     * @throws AMConsoleException if provider cannot be modified.
     */
    public void updateSPEntityConfig(
        String realm,
        String entityName,
        Map attrValues
    ) throws AMConsoleException;

    /**
     * createEntityConfig
     * create a provider's extended metadata.
     *
     * @param entityName Name of Entity Descriptor.
     * @param realm Realm of Entity
     * @param role Role of provider. (SP or IDP)
     * @param location if the entity is remote or hosted.
     * @throws AMConsoleException if provider cannot be modified.
     */
    public void createEntityConfig(
        String realm,
        String entityName,
        String role,
        String location
    ) throws AMConsoleException;
    
    /**
     * Returns true if entity descriptor is an affiliate.
     *
     * @param realm Realm of Entity
     * @param entityName Name of entity descriptor.
     * @return true if entity descriptor is an affiliate.
     */
    public boolean isAffiliate(String realm, String entityName)
        throws AMConsoleException;
    
    /**
     * Returns affiliate profile attribute values.
     *
     * @param realm the realm in which the entity resides.
     * @param entityName Name of Entity Descriptor.
     * @return affiliate profile attribute values.
     * @throws AMConsoleException if attribute values cannot be obtained.
     */
    public Map getAffiliateProfileAttributeValues(String realm, String entityName)
        throws AMConsoleException;
    
    /**
     * Modifies affiliate profile.
     *
     * @param realm the realm in which the entity resides.
     * @param entityName Name of entity descriptor.
     * @param values Map of attribute name/value pairs.
     * @param members Set of Affiliate memebers
     * @throws AMConsoleException if profile cannot be modified.
     */
    public void updateAffiliateProfile(
        String realm,
        String entityName,
        Map values,
        Set members
    ) throws AMConsoleException;
    
    /*
     * Returns a Set of all the idff entities
     *
     * @param realm the realm in which the entity resides.
     * @throws AMConsoleException if value cannot be obtained.
     */
    public  Set getAllEntityDescriptorNames(String realm)
        throws AMConsoleException;
    
     /*
      * Returns a Set of all the affiliate entity name
      *
      * @param realm the realm in which the entity resides.
      * @throws AMConsoleException if value cannot be obtained.
      */
    public  Set getAllAffiliateEntityDescriptorNames(String realm)
        throws AMConsoleException ;
    
    /*
     * Returns a Set of all the affiliate members
     *
     * @param realm the realm in which the entity resides.
     * @param entityName Name of Entity Descriptor.
     * @throws AMConsoleException if values cannot be obtained.
     */
    public Set getAllAffiliateMembers(String realm, String entityName)
        throws AMConsoleException ;
}
