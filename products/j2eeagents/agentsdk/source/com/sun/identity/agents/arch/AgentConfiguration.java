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
 * $Id: AgentConfiguration.java,v 1.13 2008-01-12 01:19:40 huacui Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.agents.arch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import com.iplanet.services.comm.client.AlreadyRegisteredException;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.naming.URLNotFoundException;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IApplicationSSOTokenProvider;
import com.sun.identity.agents.util.AgentRemoteConfigUtils;
import com.sun.identity.common.DebugPropertiesObserver;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * <p>
 * Provides access to the configuration as set in the system. Underneath the
 * covers, the <code>AgentConfiguration</code> checks to see if the agent has
 * been deployed on a system where Access Manager or one of its components such
 * has FM has already been deployed. If so, the configuration file used would
 * be the applicable Access Manager configuration such as AMConfig.properties.
 * </p><p>
 * If the agent has been deployed on a system that does not have Access Manager
 * or any of its components such as FM, it uses the configuration file called
 * AMAgent.properties which it expects to find in the system classpath. Any 
 * properties necessary for the Access Manager's client SDK that is bundled 
 * with the agent are set by this class as system properties which in turn are
 * picked up by the client SDK classes where necessary. 
 * </p><p>
 * Configuration updates are available if the configuration setting 
 * <code>com.sun.identity.agents.j2ee.config.load.interval</code> has been set
 * to a non-zero positive value indicating the number of seconds after which
 * the system will poll to identify configuration changes. If this value is set
 * to zero, the configuration reloads will be disabled. Every time a 
 * configuration reload occurs, the value of the configuration setting that 
 * governs the reload interval is also recalculated, thereby making it possible
 * to dynamically disable reloads by setting the value to zero when reloads are
 * active. Note that in the event of a reload, only the configuration keys that
 * are designated for Agent operation are reloaded. These keys begin with the
 * <code>com.sun.identity.agents.config</code> prefix. The keys that do
 * not match this criteria are kept unchanged and reflect the values that were
 * present during system initialization. Certain keys which meet this crieteria
 * would still not be reloaded due to potential security concerns associated
 * with the swapping of the associated values.
 * </p><p>
 * General access methods in this class are package protected and thus cannot 
 * be directly invoked by classes that are not within the same package. It is
 * expected and required that all configuration access be done via a designated 
 * <code>Manager</code> of the subsystem which in turn acts as an intermediate
 * caching point for configuration values. Public methods are available for
 * configuration settings that are considered core settings.
 * </p>
 * 
 * @see com.sun.identity.agents.arch.Manager
 * @see com.sun.identity.agents.arch.IConfigurationListener
 */
public class AgentConfiguration implements 
        IAgentConfigurationConstants, 
        IConfigurationKeyConstants, 
        IClientConfigurationKeyConstants,
        IConfigurationDefaultValueConstants
{
    
   /**
    * A constant value used to identify the default web application context
    * for configuration settings that are application specific.
    */
    public static final String DEFAULT_WEB_APPLICATION_NAME = "DefaultWebApp";
    private static final String ATTRIBUTE_SERVICE_NAME = "idsvcs-rest";
    private static final String AGENT_CONFIG_CENTRALIZED = "centralized";
    private static final String AGENT_CONFIG_LOCAL = "local";
    public static final String ROOT_REALM_NAME = "/";
    
    
   /**
    * Returns a header name that contains the client IP address. If no header
    * name is specified in the configuration file, this method will return
    * <code>null</code>.
    * 
    * @return a header name that contains client IP address or <code>null</code>
    * if no header name is specified.
    */
    public static String getClientIPAddressHeader() {
        return _clientIPAddressHeader;
    }
    
   /**
    * Returns a header name that contains the client hostname. If no header
    * name is specified in the configuration file, this method will return
    * <code>null</code>.
    * 
    * @return a header name that contains client hostname or <code>null</code>
    * if no header name is specified.
    */
    public static String getClientHostNameHeader() {
        return _clientHostNameHeader;
    }
    
   /**
    * Returns the name of the organization that can be used for authenticating
    * the Agent services. This value represents the organization name or the
    * realm name to which the Agent profile belongs.
    * 
    * @return the organization or realm name to which the Agent profile belongs.
    */
    public static String getOrganizationName() {
        return ROOT_REALM_NAME;
    }    
    
   /**
    * Returns a boolean indicating if the notifications for Policy changes
    * have been set as enabled.
    * @return <code>true</code> if notifications for Policy are enabled, 
    * <code>false</code> otherwise.
    */
    public static boolean isPolicyNotificationEnabled() {
        return _policyNotificationEnabledFlag;
    }
    
   /**
    * Returns the URL that will be used by the Server to send policy 
    * notifications.
    * @return the policy notification URL.
    */
    public static String getPolicyNotificationURL() {
        return _policyNotificationURL;
    }
    
   /**
    * Returns the URL that will be used by the Server to send all 
    * notifications to agents. This will include policy, session, and agent 
    * configuration change notifications.
    * @return the client notification URL.
    */
    public static String getClientNotificationURL() {
        return _clientNotificationURL;
    }
    
   /**
    * Returns a boolean indicating if the notificatiosn for Sessino changes have
    * been set as enabled.
    * 
    * @return <code>true</code> if notifications for Session are enabled, 
    * <code>false</code> otherwise.
    */
    public static boolean isSessionNotificationEnabled() {
        return _sessionNotificationEnabledFlag;
    }
    
   /**
    * Returns the URL that will be used by the Server to send session
    * notifications.
    * 
    * @return the session notification URL.
    */
    public static String getSessionNotificationURL() {
        return _sessionNotificationURL;
    }
    
   /**
    * Returns the name of the Access Manager Session property that is used by
    * the Agent runtime to identify the user-id of the current user.
    * 
    * @return the Session property name that identifies the user-id of the 
    * current user.
    */
    public static String getUserIdPropertyName() {
        return _userIdPropertyName;
    }
    
   /**
    * A method that ensures that any class that directly depends
    * upon Client SDK can first initialize the <code>AgentConfiguration</code>.
    * Failing to initialize the <code>AgentConfiguration</code> can result
    * in the malfunction of the Client SDK due to configuration dependancies.
    */
    public static void initialize() {
        // No processing requried
    }
    
   /**
    * Returns the name of the cookie or URI parameter that holds the users
    * SSO token.
    * 
    * @return the SSO token cookie or parameter name.
    */
    public static synchronized String getSSOTokenName() {
        return _ssoTokenCookieName;
    }
     
   /**
    * Returns the <code>ServiceResolver</code> instance associated with the
    * Agent runtime.
    * @return the configured <code>ServiceResolver</code>.
    */
    public static ServiceResolver getServiceResolver() {
        return _serviceResolver;
    }
    
   /**
    * Returns the <code>UserMapingMode</code> configured in the system. This
    * setting is not hot-swappable and is initialized during system 
    * initialization and never changed thereafter.
    * 
    * @return the configured <code>UserMappingMode</code>.
    */
    public static UserMappingMode getUserMappingMode() {
        return _userMappingMode;
    }
    
   /**
    * Returns the <code>AuditLogMode</code> configured in the system. This
    * setting is not hot-swappable and is initialized during system
    * initialization and never changed thereafeter.
    * 
    * @return the configured <code>AuditLogMode</code>.
    */
    public static AuditLogMode getAuditLogMode() {
        return _auditLogMode;
    }
     
   /**
    * Returns the user attribute value configured in the system. This setting
    * is not hot-swappable and is initialized during system initialization and
    * never changed thereafter.
    * 
    * @return the configured user attribute value.
    */
    public static String getUserAttributeName() {
        return _userAttributeName;
    }  
    
   /**
    * Returns <code>true</code> if the runtime is configured to use the user's
    * <code>DN</code> instead of the regular <code>userid</code> for 
    * identification purposes. This setting is not hot-swappable and is 
    * initialized during system initialization and never changed thereafter.
    * 
    * @return <code>true</code> if the system is configured to use the user's
    * <code>DN</code> for identification purposes, <code>false</code> otherwise.
    */
    public static boolean isUserPrincipalEnabled() {
        return _userPrincipalEnabled;
    }    
    
   /**
    * Allows other parts of the subsystem to register for configuration
    * change events by registering the specified 
    * <code>IConfigurationListener</code>.
    * 
    * @param listener the <code>IConfigurationListener</code> to be registered.
    */
    public static void addConfigurationListener(
            IConfigurationListener listener) {

        if(isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Adding listener for : "
                       + listener.getName());
        }

        Vector configurationListeners = getModuleConfigurationListeners();

        synchronized(configurationListeners) {
            configurationListeners.add(listener);
        }
    }
    
    /**
     * Returns the application user name to be used to identify the Agent
     * runtime.
     * 
     * @return the application user name.
     */
     public static String getApplicationUser() {
         return _applicationUser;
     }
     
    /**
     * Returns the application password to be used to identify the Agent
     * runtime.
     * 
     * @return the application password.
     */
     public static String getApplicationPassword() {
         return _applicationPassword;
     }    
     
    /**
     * Returns the anonymous user name.
     * 
     * @return the user name for anonymous user.
     */
     public static String getAnonymousUserName() {
         return _anonymousUserName;
     }
    
   /**
    * Would be called by agent configuration notification handler when the 
    * agent housekeeping app receives configuration update notifications from
    * the fam server
    */
    public static void updatePropertiesUponNotification() {       
        if(isAgentConfigurationRemote()) {
            hotSwapAgentConfiguration();
            if (isLogMessageEnabled()) {
                logMessage(
                    "AgentConfiguration.updatePropertiesUponNotification():" +
                    " updating configuration from a notification while" +
                    " in centralized mode.");
            }      
        } else {
            if (isLogMessageEnabled()) {
                logMessage(
                    "AgentConfiguration.updatePropertiesUponNotification():" +
                    " caller trying to update configuration from a" +
                    " notification while in local mode." +
                    " Should only be called when in centralized mode.");
            }     
        }
    }
    
   /**
    * Returns the configuration value corresponding to the specified 
    * <code>id</code> or the supplied <code>defaultValue</code> if not 
    * present.
    * 
    * @param id the configuration key to be looked up.
    * @param defaultValue the default value to be used in case no configuration
    * is specified for the given <code>id</code>.
    * 
    * @return the associated configuration value with the specified 
    * <code>id</code> or the <code>defaultValue</code> if no value is specified
    * in the configuration.
    */
    static String getProperty(String id, String defaultValue) {
        String value = getProperty(id);
        if (value == null) {
            value = defaultValue;
        }
        
        return value;
    }
    
   /**
    * Returns the configuration value corresponding to the specified 
    * <code>id</code> or <code>null</code> if no value is present.
    * 
    * @param id the configuration key to be looked up.
    * 
    * @return the associated configuration value with the specified
    * <code>id</code> or <code>null</code> if no value is specified in the
    * configuration.
    */
    static String getProperty(String id) {
        String result = null;
        String value = System.getProperty(id);
        if (value == null) {
            value = getPropertyInternal(id);
        }
        
        if (value != null && value.trim().length() > 0) {
            result = value.trim();
        }
        
        return result;
    }
    
   /**
    * Returns a <code>Properties</code> instance that holds all the available
    * configuration as available in the system.
    * 
    * @return the configuration as a <code>Properties</code> instance.
    */
    static Properties getAll() {
        Properties result = getAllInternal();
        Iterator it = System.getProperties().keySet().iterator();
        while (it.hasNext()) {
            String nextKey = (String) it.next();
            result.put(nextKey, System.getProperty(nextKey));
        }
        return result;
    }
        
    private synchronized static String getPropertyInternal(String id) {
        return getProperties().getProperty(id);
    }
    
    private synchronized static Properties getAllInternal() {
        Properties properties = new Properties();
        properties.putAll(getProperties());
        return properties;
    }    
    
    private static synchronized void setConfigurationFilePath() {
        if (!isInitialized()) {
            String result = null;
            URL resUrl = ClassLoader.getSystemResource(CONFIG_FILE_NAME);
            if(resUrl == null) {
                ClassLoader cl = 
                    Thread.currentThread().getContextClassLoader();
                if(cl != null) {
                    resUrl = cl.getResource(CONFIG_FILE_NAME);
                }
            }
            
            if (resUrl == null) {
                throw new RuntimeException(
                    "Failed to get configuration file:" + CONFIG_FILE_NAME);
            }
            result = resUrl.getPath();                     
            if (result == null) {
                throw new RuntimeException(
                    "Failed to get configuration file:" + CONFIG_FILE_NAME);
            }
            setConfigFilePath(result);
            int index = result.lastIndexOf(CONFIG_FILE_NAME);
            if (index < 0) {
                throw new RuntimeException(
                "Failed to find the agent bootstrap file:" + CONFIG_FILE_NAME);
            }
            String pathDir = result.substring(0, index);
            setLocalConfigFilePath(pathDir + LOCAL_CONFIG_FILE_NAME);
        }
    }
    
    /**
     * load from AMAgent.properties for start up properties
     **/
    private static Properties getPropertiesFromConfigFile() 
    throws Exception {
        Properties result = new Properties();
        BufferedInputStream instream = null;
        try {
            instream = new BufferedInputStream(
                    new FileInputStream(getConfigFilePath()));
            result.load(instream);
        } catch (Exception ex) {
            throw ex;
        } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (Exception ex) {
                        // No handling required
                    }
                }
        }
        
        return result;
    }
    
    private static Properties getPropertiesFromRemote(Vector urls) 
        throws AgentException {
        Properties result = new Properties();
        String tokenId = getAppSSOToken().getTokenID().toString();
        result = AgentRemoteConfigUtils.getAgentProperties(urls, tokenId);
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Centralized agent properties =" 
                    + result);
        }
        return result; 
    }
           
    private static Properties getPropertiesFromLocal() 
        throws AgentException {
        Properties result = new Properties();
        BufferedInputStream instream = null;
        try {
            instream = new BufferedInputStream(
                    new FileInputStream(getLocalConfigFilePath()));
            result.load(instream);
        } catch (Exception ex) {
            throw new AgentException(ex);
        } finally {
                if (instream != null) {
                    try {
                        instream.close();
                    } catch (Exception ex) {
                        // No handling required
                    }
                }
        }
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Local config properties =" + result);
        }
        return result;
    }
    
    private static void setAppSSOToken() throws AgentException {
        CommonFactory cf = new CommonFactory(BaseModule.getModule());
        IApplicationSSOTokenProvider provider =  
            cf.newApplicationSSOTokenProvider();
        
        _appSSOToken = provider.getApplicationSSOToken();
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: appSSOToken =" + _appSSOToken);
        }
    }
    
    private static SSOToken getAppSSOToken() throws AgentException {
        if (_appSSOToken != null) {
            try {
               // check if token is still valid with the session server.
               // This refreshSession call throws a SSOException if the token
               // is not valid any more.
               SSOTokenManager.getInstance().refreshSession(_appSSOToken);
            } catch (SSOException se) {
                if (isLogMessageEnabled()) {
                    logMessage("AgentConfiguration.getAppSSOToken: " +
                       "The app SSO token is invalid, indicating opensso " +
                       "server may have restarted, so need to " + 
                       "reauthenticate to get a new app SSO token");
                }
                setAppSSOToken();
            }
        } else {
            setAppSSOToken();
        }
        return _appSSOToken;
    }
    
    private static Vector getAttributeServiceURLs() throws AgentException {
        if (_attributeServiceURLs == null) {
            try {
                _attributeServiceURLs = WebtopNaming.getServiceAllURLs(
                                          ATTRIBUTE_SERVICE_NAME);
                if (isLogMessageEnabled()) {
                    logMessage("AgentConfiguration: attribute service urls"
                                + _attributeServiceURLs);
                }    
            } catch (URLNotFoundException ue) {
                throw new AgentException(ue);
            }
        }
        return _attributeServiceURLs;
    }
    /**
     * Collect all configuration info. Store all config properties, including 
     * AMAgent.properties bootstrap small set of props and also agent config
     * props (from fam server or if local config file 
     * AMAgentConfiguration.properties) and store ALL the properties in a 
     * class field for later use, plus set a few fields on this class for some
     * props that are used throughout agent code and accessed from this class.
     * Also, for any clientsdk properties, push them into the JVM system 
     * properties so they can be accessed by clientsdk.
     * All non-system properties start with AGENT_CONFIG_PREFIX and this is how
     * we distinguish between agent properties and cleintsdk properties.
     * Note, a few clientsdk props (like notification url and notification
     * enable flags) are ALSO stored by this class in fields since
     * they are also use throughout agent code as well as by cleintsdk.
     */   
    private static synchronized void bootStrapClientConfiguration() {
        if (!isInitialized()) {
            HashMap sysPropertyMap = null;
            setConfigurationFilePath();
            try {
                sysPropertyMap = new HashMap();
                Properties properties = getProperties();
                properties.clear();
                properties.putAll(getPropertiesFromConfigFile());
               
                //debug level can optionally be set in AMAgent.properties
                //but by default is not set, so we provide default if no value
                //This debug level(either default or prop in AmAgent.properties)
                //file is only used for bootup time logging.
                //Real runtime debug level value is later retrieved with rest of 
                //agent config from fam server
                String initialDebugLevel = properties.getProperty(
                    Constants.SERVICES_DEBUG_LEVEL);
                if ((initialDebugLevel == null) || 
                    (initialDebugLevel.trim().length() == 0)) {
                    properties.setProperty(
                        Constants.SERVICES_DEBUG_LEVEL, Debug.STR_MESSAGE);
                }

                //push the bootstrap properties to JVM system properties
                Iterator iter = properties.keySet().iterator();
                while (iter.hasNext()) {
                    String nextKey = (String) iter.next(); 
                    if (!nextKey.startsWith(AGENT_CONFIG_PREFIX)) {
                        String nextValue = 
                                getProperties().getProperty(nextKey);
                        System.setProperty(nextKey, nextValue);
                        sysPropertyMap.put(nextKey, nextValue);
                    }
                }
                
                // instantiate the instance of DebugPropertiesObserver
                debugObserver = DebugPropertiesObserver.getInstance();
                setDebug(Debug.getInstance(IBaseModuleConstants.BASE_RESOURCE));
                setServiceResolver();
                setApplicationUser();
                setApplicationPassword();
                
                setAppSSOToken();

                Vector attrServiceURLs = getAttributeServiceURLs();
                //if fam 8.0 server
                if (attrServiceURLs != null) {
                    Properties propsFromFAMserver = 
                            getPropertiesFromRemote(attrServiceURLs);
                    String agentConfigLocation = 
                     propsFromFAMserver.getProperty(CONFIG_REPOSITORY_LOCATION);
                    
                    //if agent profile on fam server is 2.2 style(null or 
                    //blank value)-maybe to help agent upgrade use case)
                    //OR agent profile is 3.0 style(common case) with local
                    //config flag set
                    if ((agentConfigLocation == null)
                         || (agentConfigLocation.trim().equals(""))
                         || (agentConfigLocation.equalsIgnoreCase(
                            AGENT_CONFIG_LOCAL))) {  
                            properties.putAll(getPropertiesFromLocal());
                    } else if (agentConfigLocation.equalsIgnoreCase(
                            AGENT_CONFIG_CENTRALIZED)) {
                        markAgentConfigurationRemote();
                        properties.putAll(propsFromFAMserver);
                    } else {
                        throw new AgentException("Invalid agent config"
                             + "location: does not specify local or centralized");
                    }
                } else {    //else if Access Manager 7.1/7.0 server
                        // Need to read the rest of agent config from its local
                        // configuration file
                        properties.putAll(getPropertiesFromLocal());
                }

                Iterator it = properties.keySet().iterator();
                while (it.hasNext()) {
                    String nextKey = (String) it.next();
                    if (!nextKey.startsWith(AGENT_CONFIG_PREFIX)) {
                        String nextValue = 
                               getProperties().getProperty(nextKey);
                        System.setProperty(nextKey, nextValue);
                        //save in sysPropertyMap for upcoming log messages
                        sysPropertyMap.put(nextKey, nextValue);
                    }
                }

                String modIntervalString = getProperty(CONFIG_LOAD_INTERVAL);
                try {
                    long modInterval = Long.parseLong(modIntervalString);
                    setModInterval(modInterval * 1000L);
                } catch (NumberFormatException nfex) {
                    System.err.println(
                          "AgentConfiguration: Exception while reading "
                           + "new mod interval: \"" + modIntervalString + "\"");
                }
                markCurrent();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                throw new RuntimeException("Failed to load configuration: "
                        + ex.getMessage());
            }
        
            // Initilaize the Debug Engine
            //setDebug(Debug.getInstance(IBaseModuleConstants.BASE_RESOURCE));
            if (isLogMessageEnabled()) {
                if (sysPropertyMap != null) {
                    logMessage(
                        "AgentConfiguration: The following properties "
                        + "were added to system: " + sysPropertyMap);
                } else {
                    logMessage(
                        "AgentConfiguration: No properties were added "
                        + " to system.");
                }
                logMessage("AgentConfiguration: Mod Interval is set to: "
                        + getModInterval() + " ms.");
            }
        
            //Start the Configuration Monitor if necessary
            if (getModInterval() > 0L) {
                Thread monitorThread = new Thread(
                        new ConfigurationMonitor(), "AgentConfigMonitor");
                monitorThread.setDaemon(true);
                monitorThread.setPriority(Thread.MIN_PRIORITY);
                monitorThread.start();
            }
        }
    }
   
   /**
    * Registers the agent config notification handler with PLLClient. The 
    * handler is registered once and exists for continuous hot swaps if an 
    * agent is configured to enable agent configuration updates from the fam
    * server. The handler is used by notification filter task handler when 
    * the filter receives agent configuration XML notifications. This method
    * only needs to be called once when the agent boots up and initializes.
    */
    private static void registerAgentNotificationHandler () {       
        AgentConfigNotificationHandler handler =
                new AgentConfigNotificationHandler();
        try {
            PLLClient.addNotificationHandler(
                    AgentConfigNotificationHandler.AGENT_CONFIG_SERVICE, 
                    handler);   
            if (isLogMessageEnabled()) {
                logMessage(
                    "AgentConfiguration.registerAgentNotificationHandler():" +
                    " registered handler for accepting agent configuration" +
                    " notifications while in centralized mode.");
                } 
        } catch (AlreadyRegisteredException arex) {
            //should only be one handler per VM since static & global
            //so probably will never happen
            if(isLogWarningEnabled()){
              logWarning("AgentConfiguration.registerAgentNotificationHandler" +
                    " Tried to register the AgentConfigNotificationHandler" +
                    " with PLL Client but PLL client already has it" +
                    " registered." , arex );
            }         
        }
    }
       
    private static synchronized void setServiceResolver() {
        if (!isInitialized()) {
            String serviceResolverClassName =
                    getProperty(CONFIG_SERVICE_RESOLVER);        
                try {
                    if (isLogMessageEnabled()) {
                        logMessage(
                            "AgentConfiguration: service resolver set to: "
                            + serviceResolverClassName);
                    }
                    _serviceResolver = (ServiceResolver) Class.forName(
                    serviceResolverClassName).newInstance();
                    
                    if (isLogMessageEnabled()) {
                        logMessage(
                               "AgentConfiguration: service resolver reports "
                               + "EJBContext available: " 
                               + _serviceResolver.isEJBContextAvailable());
                    }
                } catch (Exception ex) {
                    logError(
                        "AgentConfiguration: Failed to set Service Resolver: "
                        + serviceResolverClassName, ex);
                    throw new RuntimeException(
                        "Failed to set Service Resolver: "
                        + serviceResolverClassName + ": "
                        + ex.getMessage());
                }
        }
    }
    
    private static synchronized void setUserMappingMode() {
        UserMappingMode mode = UserMappingMode.get(
        getProperty(CONFIG_USER_MAPPING_MODE));
        
        if (mode == null) {
            throw new RuntimeException("Unknown User Mapping Mode: "
                    + getProperty(CONFIG_USER_MAPPING_MODE));
        }
        _userMappingMode = mode;
        
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserMappingMode: User Mapping"
                    + " mode set to: "
                    + _userMappingMode);
        }
    }
    
    private static synchronized void setAuditLogMode() {
        AuditLogMode mode = AuditLogMode.get(
                getProperty(CONFIG_AUDIT_LOG_MODE));
        
        if (mode == null) {
            throw new RuntimeException("Unknown Audit Log Mode: "
                    + getProperty(CONFIG_AUDIT_LOG_MODE));
        }
        _auditLogMode = mode;
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setAuditLogMode: Audit Log mode"
                    + " set to: "
                    + _auditLogMode);
        }
    }
    
    private static synchronized void setAnonymousUserName() {
        if (!isInitialized()) {
            String name = getProperty(CONFIG_ANONYMOUS_USER_NAME);
                if (name == null || name.trim().length() == 0) {
                    name = DEFAULT_ANONYMOUS_USER_NAME;
                    if (isLogWarningEnabled()) {
                        logWarning(
                            "AgentConfiguration: No name specified for user "
                            +"anonymous. Using default: " + name);
                    }
                }
        
                _anonymousUserName = name;
                if (isLogMessageEnabled()) {
                    logMessage(
                        "AgentConfiguration: Anonymous user name set to: "
                        + _anonymousUserName);
                }
        }
    }
    
    private static synchronized void setUserAttributeName() {
        String userAttributeName = getProperty(CONFIG_USER_ATTRIBUTE_NAME);
        if (userAttributeName == null ||
                userAttributeName.trim().length() == 0) {
            userAttributeName = DEFAULT_USER_ATTRIBUTE_NAME;
            logError("AgentConfiguation.setUserAttributeName: Unable to load"
                    + " user attribute name. Using default value: " 
                    + userAttributeName);
        }
        _userAttributeName = userAttributeName;
        
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserAttributeName: User"
                    + " attribute name set to: "
                    + _userAttributeName);
        }
    }
    
    private static synchronized void setUserPrincipalEnabledFlag() {
        String userPrinsipalFlagString = getProperty(CONFIG_USER_PRINCIPAL);
        if (userPrinsipalFlagString == null ||
                userPrinsipalFlagString.trim().length() == 0) {
            userPrinsipalFlagString = DEFAULT_USE_DN;
        }
        
        _userPrincipalEnabled = Boolean.valueOf(
                userPrinsipalFlagString).booleanValue();
        
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserPrincipalEnabledFlag: use-DN"
                    + " User Principal Enabled Flag is set to: " 
                    + _userPrincipalEnabled);
        }
    }
    
    private static synchronized void setSSOTokenName() {
        if (!isInitialized()) {
            _ssoTokenCookieName = getProperty(SDKPROP_SSO_COOKIE_NAME);
            if (_ssoTokenCookieName == null || 
                    _ssoTokenCookieName.trim().length() == 0) {
                throw new RuntimeException("Invalid SSO Cookie name set");
            }
        
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: SSO Token name set to: "
                        + _ssoTokenCookieName);
            }
        }
    }
    
    private static synchronized void setApplicationUser() {
        if (!isInitialized()) {
            _applicationUser = getProperty(SDKPROP_APP_USERNAME);
            if (_applicationUser == null || _applicationUser.trim().length()==0)
            {
                throw new RuntimeException(
                        "Invalid application user specified");
            }
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Application User: "
                        + _applicationUser);
            }
        }
    }
    
    private static synchronized void setApplicationPassword() {
            
        if (!isInitialized()) {
            try {
                    _crypt = ServiceFactory.getCryptProvider();
                if(_crypt != null) {
                    String encodedPass = getProperty(SDKPROP_APP_PASSWORD);
                    _applicationPassword = 
                        _crypt.decrypt(encodedPass);
                }
            } catch (Exception ex) {
                logError("AgentConfiguration: Unable to create new instance of "
                    + "Crypt class with exception ", ex);
            }

            if (_applicationPassword == null || 
                _applicationPassword.trim().length() == 0) {
                throw new RuntimeException(
                        "Invalid application password specified");
            }
        }
    }
    
    private static synchronized void setUserIdPropertyName() {
        String propertyName = getProperty(CONFIG_USER_ID_PROPERTY);
        if (propertyName == null || propertyName.trim().length() == 0) {
            propertyName = DEFAULT_USER_ID_PROPERTY;
            if (isLogWarningEnabled()) {
                logWarning("AgentConfiguration.setUserIdPropertyName: No value"
                     + " specified for user id property name. Using default: " 
                     + propertyName);
            }
        }
        _userIdPropertyName = propertyName;       
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setUserIdPropertyName: User id"
                    + " property name set to: "
                    + _userIdPropertyName);
            }
    }
    
    //this property is a hot swappable ClientSDK property
    private static synchronized void setSessionNotificationURL() {
        //if (!isInitialized()) {
            String url = getProperty(SDKPROP_SESSION_NOTIFICATION_URL);
            if (url != null && url.trim().length() > 0) {
                _sessionNotificationURL = url;
            } else {
                if (isLogWarningEnabled()) {
                    logWarning(
                         "AgentConfiguration: No session notification URL set");
                }
            }
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Session notification URL: "
                        + _sessionNotificationURL);
            }     
    }
    
    //this property is a hot swappable ClientSDK property
    private static synchronized void setSessionNotificationEnabledFlag() {
        //if (!isInitialized()) {
            _sessionNotificationEnabledFlag = true;
            boolean pollingEnabled = false;
            String flag = getProperty(SDKPROP_SESSION_POLLING_ENABLE);
            if (flag != null && flag.trim().length() > 0) {
                pollingEnabled = Boolean.valueOf(flag).booleanValue();
            }
            
            _sessionNotificationEnabledFlag = !pollingEnabled;
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Session notification enable: " 
                        + _sessionNotificationEnabledFlag);
            }        
    }
    
    //this property is a hot swappable ClientSDK property
    private static synchronized void setPolicyNotificationURL() {
        //if (!isInitialized()) {
            String url = getProperty(SDKPROP_POLICY_NOTIFICATION_URL);
            if (url != null && url.trim().length() > 0) {
                _policyNotificationURL = url;
            } else {
                if (isLogWarningEnabled()) {
                    logWarning(
                         "AgentConfiguration: No policy notification URL set");
                }
            }
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Policy notification URL: "
                        + _policyNotificationURL);
            }
    }
    
    //this property is a hot swappable ClientSDK property
    private static synchronized void setClientNotificationURL() {
        //if (!isInitialized()) {
            String url = getProperty(SDKPROP_CLIENT_NOTIFICATION_URL);
            if (url != null && url.trim().length() > 0) {
                _clientNotificationURL = url;
            } else {
                if (isLogWarningEnabled()) {
                    logWarning(
                         "AgentConfiguration: No client notification URL set");
                }
            }
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Client notification URL: "
                        + _clientNotificationURL);
            }
    }
    
    //this property is a hot swappable ClientSDK property
    private static synchronized void setPolicyNotificationEnabledFlag() {
        //if (!isInitialized()) {
            boolean enable = false;
            String flag = getProperty(SDKPROP_POLICY_NOTIFICATION_ENABLE);
            if (flag != null && flag.trim().length() > 0) {
                enable = Boolean.valueOf(flag).booleanValue();
            }
            
            _policyNotificationEnabledFlag = enable;
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Policy notification enable: " 
                        + _policyNotificationEnabledFlag);
            }
    }
    
    private static synchronized void setClientIPAddressHeader() {
            _clientIPAddressHeader = getProperty(CONFIG_CLIENT_IP_HEADER);
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration.setClientIPAddressHeader:"
                        + " Client IP Address Header: "
                        + _clientIPAddressHeader);
            }
    }
    
    private static synchronized void setClientHostNameHeader() {
        _clientHostNameHeader = getProperty(CONFIG_CLIENT_HOSTNAME_HEADER);
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration.setClientHostNameHeader: Client"
                    + " Hostname Header: "
                    + _clientHostNameHeader);
            }
    }
    
    private static synchronized void initializeConfiguration() {
        if (!isInitialized()) {
            //read in all properties, save all props & values in map to use 
            //later and push some to JVM system for clientsdk
            bootStrapClientConfiguration();  
            registerAgentNotificationHandler();
            
            //now set some class fields with property values
            setAnonymousUserName();           
            setHotSwappableConfigProps();          
            //set some fields as some clientsdk props also used by agent code
            setSSOTokenName();   
            setHotSwappableClientSDKProps();                      
            markInitialized(); 
        }
    }
    
    /**
     * Some properties managed by this class are hotswappable and should be
     * set on initialization and reload of configuration property values
     */
    private static void setHotSwappableConfigProps() {
        setAuditLogMode();
        setUserMappingMode();
        setUserAttributeName();
        setUserPrincipalEnabledFlag();
        setUserIdPropertyName();
        setClientIPAddressHeader();
        setClientHostNameHeader();
    }
    
    /**
     * some of clientSDK props are hot-swappable and are used by clientSDK thru
     * JVM system properties are are ALSO used by agent code and hence we store
     * their current values in some fields.
     */
    private static void setHotSwappableClientSDKProps() {
        setSessionNotificationURL();
        setSessionNotificationEnabledFlag();
        setPolicyNotificationURL();
        setClientNotificationURL();
        setPolicyNotificationEnabledFlag();
    }
    
    
    private static void logMessage(String msg) {
        getDebug().message(msg);
    }
    
    private static void logMessage(String msg, Throwable th) {
        getDebug().message(msg, th);
    }
    
    private static void logWarning(String msg) {
        getDebug().warning(msg);
    }
    
    private static void logWarning(String msg, Throwable th) {
        getDebug().warning(msg, th);
    }
    
    private static void logError(String msg) {
        getDebug().error(msg);
    }
    
    private static void logError(String msg, Throwable th) {
        getDebug().error(msg, th);
    }
    
    private static boolean isLogWarningEnabled() {
        return getDebug().warningEnabled();
    }
    
    private static boolean isLogMessageEnabled() {
        return getDebug().messageEnabled();
    }    
   
    private static void updatePropertiesUponPolling() {
        if (needToRefresh()) {
            if (!isAgentConfigurationRemote()) {
                File configFile = new File(getLocalConfigFilePath());
                if (!configFile.exists()) {
                    configFile = new File(getConfigFilePath());
                }
                if(getLastLoadTime() > configFile.lastModified()) {
                    markCurrent();
                    return; 
                } 
            }
            hotSwapAgentConfiguration();
        }
    }
    
    private static void hotSwapAgentConfiguration() {
        if (loadProperties()) {
            notifyModuleConfigurationListeners();
            // notify possible debug level change
            debugObserver.notifyChanges();
        }
    }
    
    private static void notifyModuleConfigurationListeners() {

        Vector configurationListeners = getModuleConfigurationListeners();
        if (isLogMessageEnabled()) {
            logMessage("AgentConfiguration: Notifying all listeners");
        }

        synchronized(configurationListeners) {
            for(int i = 0; i < configurationListeners.size(); i++) {
                IConfigurationListener nextListener =
                    (IConfigurationListener) configurationListeners.get(i);

                nextListener.configurationChanged();

                if(isLogMessageEnabled()) {
                    logMessage("AgentConfiguration: Notified listener for "
                               + nextListener.getName());
                }
            }
        }
    }    

    private synchronized static boolean loadProperties() {
        boolean result = false;
        try {
            Properties properties = new Properties();
            properties.clear();
            properties.putAll(getPropertiesFromConfigFile());
            
            if (!isAgentConfigurationRemote()) {                        
                properties.putAll(getPropertiesFromLocal());
            } else {
                properties.putAll(getPropertiesFromRemote(
                    getAttributeServiceURLs()));
            }

            String modIntervalString = properties.getProperty(
                    CONFIG_LOAD_INTERVAL);
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: interval=" + modIntervalString);
            }
            if (modIntervalString != null && 
                    modIntervalString.trim().length()>0) 
            {
                modIntervalString = modIntervalString.trim();
            } else {
                logWarning("AgentConfiguration: No mod interval setting found");
                modIntervalString = "0";
            }
            long modInterval = 0L;
            try {
                modInterval = Long.parseLong(modIntervalString);
            } catch (NumberFormatException nfex) {
                logWarning("AgentConfiguration: Exception while reading "
                        + "new mod interval: \"" + modIntervalString + "\"");
            }
            setModInterval(modInterval*1000L);
            getProperties().clear();
            getProperties().putAll(properties);
            markCurrent();

            Iterator it = properties.keySet().iterator();
            while (it.hasNext()) {
                String nextKey = (String) it.next();
                if (!nextKey.startsWith(AGENT_CONFIG_PREFIX)) {
                    String nextValue = getProperties().getProperty(nextKey);
                    System.setProperty(nextKey, nextValue);
                }
            }
            //set local copies of config property values stored by this class
            setHotSwappableConfigProps();
            //set local copies of some clientsdk property values we store
            setHotSwappableClientSDKProps();
            
            result = true;            
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: loaded new configuration.");
            }          
        } catch (Exception ex) {
            result = false;
            logError("AgentConfiguration: Exception during reload:", ex);
            logError("AgentConfiguration: Setting reload interval to 0");
            setModInterval(0L);
        }
        return result;
    }

    private static boolean needToRefresh() {
        return((System.currentTimeMillis() - getLastLoadTime())
               >= getModInterval());
    }
    
    
    private static class ConfigurationMonitor implements Runnable {

        public ConfigurationMonitor() {

            if(isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Monitor initialized");
            }
        }

        public void run() {

            if(isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Monitor started");
            }

            while(getModInterval() > 0L) {
                updatePropertiesUponPolling();

                try {
                    Thread.sleep(getModInterval());
                } catch(InterruptedException ex) {
                    if(isLogMessageEnabled()) {
                        logMessage("AgentConfiguration: Monitor interrupted",
                                   ex);
                    }
                }
            }

            if(isLogMessageEnabled()) {
                logMessage("AgentConfiguration: Monitor is exiting");
            }
        }
    }    
    
    private static boolean isInitialized() {
        return _initialized;
    }
    
    private static void markInitialized() {
        if (!isInitialized()) {
            _initialized = true;
            if (isLogMessageEnabled()) {
                logMessage("AgentConfiguration: initialized.");
            }
        }
    }
    
    private static boolean isAgentConfigurationRemote() {
        return _isAgentConfigurationRemote;
    }
   
    private static void markAgentConfigurationRemote() {
        _isAgentConfigurationRemote = true; 
    }
   
    private static String getConfigFilePath() {
        return _configFilePath;
    }
    
    private static void setConfigFilePath(String configFilePath) {
        _configFilePath = configFilePath;
    }
    
    private static String getLocalConfigFilePath() {
        return _localConfigFilePath;
    }
    
    private static void setLocalConfigFilePath(String localConfigFilePath) {
        _localConfigFilePath = localConfigFilePath;
    }
    
    private static Properties getProperties() {
        return _properties;
    }
    
    private static void setDebug(Debug debug) {
        _debug = debug;
    }
    
    private static Debug getDebug() {
        return _debug;
    }  
    
    private static void setModInterval(long modInterval) {
        _modInterval = modInterval;
    }
    
    private static long getModInterval() {
        return _modInterval;
    }
    
    private static long getLastLoadTime() {
        return _lastLoadTime;
    }
    
    private static void markCurrent() {
        _lastLoadTime = System.currentTimeMillis();
    }    
    
    private static Vector getModuleConfigurationListeners() {
        return _moduleConfigListeners;
    }
    
    private static boolean _isAgentConfigurationRemote = false;
    private static boolean _initialized;
    private static String _configFilePath;
    private static String _localConfigFilePath;
    private static Properties _properties = new Properties();
    private static Debug _debug;
    private static long _modInterval = 0L;
    private static long _lastLoadTime = 0L;
    private static Vector _moduleConfigListeners = new Vector();
    private static ServiceResolver _serviceResolver;
    private static UserMappingMode _userMappingMode = 
        UserMappingMode.MODE_USER_ID;
    private static String _userAttributeName;
    private static boolean _userPrincipalEnabled;
    private static String _ssoTokenCookieName;
    private static String _applicationUser;
    private static String _applicationPassword;
    private static String _userIdPropertyName;
    private static AuditLogMode _auditLogMode = AuditLogMode.MODE_BOTH;
    private static String _anonymousUserName;
    private static String _sessionNotificationURL;
    private static String _policyNotificationURL;
    private static String _clientNotificationURL;
    private static boolean _policyNotificationEnabledFlag;
    private static boolean _sessionNotificationEnabledFlag;
    private static String _clientIPAddressHeader;
    private static String _clientHostNameHeader;
    private static ICrypt _crypt;
    private static SSOToken _appSSOToken = null;
    private static Vector _attributeServiceURLs = null;
    private static DebugPropertiesObserver debugObserver; 
    
    static {
        initializeConfiguration();
    }
}
