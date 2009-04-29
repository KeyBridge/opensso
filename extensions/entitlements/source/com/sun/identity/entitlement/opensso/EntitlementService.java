/**
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
 * $Id: EntitlementService.java,v 1.6 2009-04-29 18:14:16 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.interfaces.IPolicyConfig;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author dennis
 */
public class EntitlementService implements IPolicyConfig {
    public static final String SERVICE_NAME = "openssoEntitlement";

    private static final String ATTR_NAME_SUBJECT_ATTR_NAMES =
        "subjectAttributeNames";
    private static final String CONFIG_APPLICATIONS = "registeredApplications";
    private static final String CONFIG_APPLICATION = "application";
    private static final String CONFIG_APPLICATIONTYPE = "applicationType";
    private static final String CONFIG_ACTIONS = "actions";
    private static final String CONFIG_RESOURCES = "resources";
    private static final String CONFIG_CONDITIONS = "conditions";
    private static final String CONFIG_ENTITLEMENT_COMBINER =
        "entitlementCombiner";
    private static final String CONFIG_SEARCH_INDEX_IMPL = "searchIndexImpl";
    private static final String CONFIG_SAVE_INDEX_IMPL = "saveIndexImpl";
    private static final String CONFIG_RESOURCE_COMP_IMPL = "resourceComparator";
    private static final String CONFIG_APPLICATION_TYPES = "applicationTypes";

    public EntitlementService() {
    }

    public String getAttributeValue(String attrName) {
        Set<String> values = getAttributeValues(attrName);
        return ((values != null) && !values.isEmpty()) ?
            values.iterator().next() : null;
    }

    public Set<String> getAttributeValues(String attrName) {
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            ServiceSchemaManager smgr = new ServiceSchemaManager(
                SERVICE_NAME, adminToken);
            AttributeSchema as = smgr.getGlobalSchema().getAttributeSchema(
                attrName);
            return as.getDefaultValues();
        } catch (SMSException ex) {
            //TOFIX;
        } catch (SSOException ex) {
            //TOFIX;
        }
        return Collections.EMPTY_SET;
    }

    public Set<ApplicationType> getApplicationTypes() {
        Set<ApplicationType> results = new HashSet<ApplicationType>();
        try {
            ServiceConfig conf = getApplicationTypeCollectionConfig();
            Set<String> names = conf.getSubConfigNames();
            for (String name : names) {
                ServiceConfig appType = conf.getSubConfig(name);
                Map<String, Set<String>> data = appType.getAttributes();
                results.add(createApplicationType(name, data));
            }
        } catch (SMSException ex) {
            // TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
        return results;
    }

    private ServiceConfig getApplicationTypeCollectionConfig()
        throws SMSException, SSOException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ServiceConfigManager mgr = new ServiceConfigManager(
            SERVICE_NAME, adminToken);
        ServiceConfig globalConfig = mgr.getGlobalConfig(null);
        if (globalConfig != null) {
            return globalConfig.getSubConfig(CONFIG_APPLICATION_TYPES);
        }
        return null;
    }

    private Set<String> getActionSet(Map<String, Boolean> actions) {
        Set<String> set = new HashSet<String>();
        if (actions != null) {
            for (String k : actions.keySet()) {
                set.add(k + "=" + Boolean.toString(actions.get(k)));
            }
        }
        return set;
    }

    private Map<String, Boolean> getActions(Map<String, Set<String>> data) {
        Map<String, Boolean> results = new HashMap<String, Boolean>();
        Set<String> actions = data.get(CONFIG_ACTIONS);
        for (String a : actions) {
            int index = a.indexOf('=');
            String name = a;
            Boolean defaultVal = Boolean.TRUE;

            if (index != -1) {
                name = a.substring(0, index);
                defaultVal = Boolean.parseBoolean(a.substring(index+1));
            }
            results.put(name, defaultVal);
        }
        return results;
    }

    private String getAttribute(
        Map<String, Set<String>> data,
        String attributeName) {
        Set<String> set = data.get(attributeName);
        return ((set != null) && !set.isEmpty()) ? set.iterator().next() : null;
    }

    private Set<String> getSet(String str) {
        Set<String> set = new HashSet<String>();
        if (str != null) {
            set.add(str);
        }
        return set;
    }


    public Set<Application> getApplications(String realm) {
        Set<Application> results = new HashSet<Application>();
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            ServiceConfigManager mgr = new ServiceConfigManager(
                SERVICE_NAME, adminToken);
            ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
            if (orgConfig != null) {
                ServiceConfig conf = orgConfig.getSubConfig(
                    CONFIG_APPLICATIONS);
                Set<String> names = conf.getSubConfigNames();

                for (String name : names) {
                    ServiceConfig applConf = conf.getSubConfig(name);
                    Map<String, Set<String>> data = applConf.getAttributes();
                    Application app = createApplication(name, data);
                    results.add(app);
                }
            }
        } catch (SMSException ex) {
            // TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
        return results;
    }

    private static Class getEntitlementCombiner(String className) {
        if (className == null) {
            return null;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            //TOFIX debug error
        }
        return com.sun.identity.entitlement.DenyOverride.class;
    }

    public void addSubjectAttributeNames(
        String realm,
        String applicationName,
        Set<String> names
    ) {
        if ((names == null) || names.isEmpty()) {
            return;
        }
        
        try {
            ServiceConfig applConf = getApplicationSubConfig(realm,
                applicationName);
            if (applConf != null) {
                Set<String> orig = (Set<String>)
                    applConf.getAttributes().get(ATTR_NAME_SUBJECT_ATTR_NAMES);
                if ((orig == null) || orig.isEmpty()) {
                    orig = new HashSet<String>();
                }
                orig.addAll(names);
                Map<String, Set<String>> map = new
                    HashMap<String, Set<String>>();
                map.put(ATTR_NAME_SUBJECT_ATTR_NAMES, orig);
                applConf.setAttributes(map);
            }
        } catch (SMSException ex) {
            //TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
    }

    /**
     * Adds a new action.
     *
     * @param realm Realm name.
     * @param appName application name.
     * @param name Action name.
     * @param defVal Default value.
     */
    public void addApplicationAction(
        String realm,
        String appName,
        String name,
        Boolean defVal) {
        try {
            ServiceConfig applConf = getApplicationSubConfig(realm, appName);

            if (applConf != null) {
                Map<String, Set<String>> data =
                    applConf.getAttributes();
                Map<String, Set<String>> result =
                    addAction(data, name, defVal);
                if (result != null) {
                    applConf.setAttributes(result);
                }
            }
        } catch (SMSException ex) {
            //TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
    }

    private ServiceConfig getApplicationSubConfig(String realm, String appName)
        throws SMSException, SSOException {
        ServiceConfig applConf = null;
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME,
            adminToken);
        ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
        if (orgConfig != null) {
            ServiceConfig conf = orgConfig.getSubConfig(
                CONFIG_APPLICATIONS);
            if (conf != null) {
                applConf = conf.getSubConfig(appName);
            }
        }
        return applConf;
    }

    private Map<String, Set<String>> addAction(
        Map<String, Set<String>> data,
        String name,
        Boolean defVal
    ) {
        Map<String, Set<String>> results = null;

        Map<String, Boolean> actionMap = getActions(data);
        if (!actionMap.keySet().contains(name)) {
            Set<String> actions = data.get(CONFIG_ACTIONS);
            Set<String> cloned = new HashSet<String>();
            cloned.addAll(actions);
            cloned.add(name + "=" + defVal.toString());
            results = new HashMap<String, Set<String>>();
            results.put(CONFIG_ACTIONS, cloned);
        } else {
            //TOFIX
        }

        return results;
    }

    public void removeApplication(String realm, String name) {
        try {
            ServiceConfig conf = getApplicationCollectionConfig(realm);
            if (conf != null) {
                conf.removeSubConfig(name);
            }
        } catch (SMSException ex) {
            //TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
    }

    public void removeApplicationType(String name) {
        try {
            ServiceConfig conf = getApplicationTypeCollectionConfig();
            if (conf != null) {
                conf.removeSubConfig(name);
            }
        } catch (SMSException ex) {
            //TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
    }

    private ServiceConfig getApplicationCollectionConfig(String realm)
        throws SMSException, SSOException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ServiceConfigManager mgr = new ServiceConfigManager(SERVICE_NAME,
            adminToken);
        ServiceConfig orgConfig = mgr.getOrganizationConfig(realm, null);
        if (orgConfig != null) {
            return orgConfig.getSubConfig(CONFIG_APPLICATIONS);
        }
        return null;
    }

    public void storeApplication(String realm, Application appl)
        throws EntitlementException {
        try {
            ServiceConfig orgConfig = getApplicationCollectionConfig(realm);
            if (orgConfig != null) {
                ServiceConfig appConfig = 
                    orgConfig.getSubConfig(appl.getName());
                if (appConfig == null) {
                    orgConfig.addSubConfig(appl.getName(),
                        CONFIG_APPLICATION, 0, getApplicationData(appl));
                } else {
                    appConfig.setAttributes(getApplicationData(appl));
                }
            }
        } catch (SMSException ex) {
            //TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
    }

    public void storeApplicationType(ApplicationType applicationType)
        throws EntitlementException {
        try {
            ServiceConfig conf = getApplicationTypeCollectionConfig();
            if (conf != null) {
                ServiceConfig sc = conf.getSubConfig(applicationType.getName());
                if (sc == null) {
                    conf.addSubConfig(applicationType.getName(),
                        CONFIG_APPLICATIONS, 0,
                        getApplicationTypeData(applicationType));
                } else {
                    sc.setAttributes(getApplicationTypeData(applicationType));
                }
            }
        } catch (SMSException ex) {
            //TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
    }

    private Map<String, Set<String>> getApplicationTypeData(
        ApplicationType applType) {
        Map<String, Set<String>> data = new HashMap<String, Set<String>>();
        data.put(CONFIG_ACTIONS, getActionSet(applType.getActions()));

        ISaveIndex sIndex = applType.getSaveIndex();
        String saveIndexClassName = (sIndex != null) ?
            sIndex.getClass().getName() : null;
        data.put(CONFIG_SAVE_INDEX_IMPL, (saveIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(saveIndexClassName));

        ISearchIndex searchIndex = applType.getSearchIndex();
        String searchIndexClassName = (searchIndex != null) ?
            searchIndex.getClass().getName() : null;
        data.put(CONFIG_SEARCH_INDEX_IMPL, (searchIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(searchIndexClassName));

        ResourceName recComp = applType.getResourceComparator();
        String resCompClassName = (recComp != null) ?
            recComp.getClass().getName() : null;
        data.put(CONFIG_RESOURCE_COMP_IMPL, (resCompClassName == null) ?
            Collections.EMPTY_SET : getSet(resCompClassName));

        return data;
    }

    private Map<String, Set<String>> getApplicationData(Application app) {
        Map<String, Set<String>> data = new HashMap<String, Set<String>>();
        data.put(CONFIG_APPLICATIONTYPE, 
            getSet(app.getApplicationType().getName()));
        data.put(CONFIG_ACTIONS, getActionSet(app.getActions()));

        Set<String> resources = app.getResources();
        data.put(CONFIG_RESOURCES, (resources == null) ? Collections.EMPTY_SET :
            resources);
        data.put(CONFIG_ENTITLEMENT_COMBINER,
            getSet(app.getEntitlementCombiner().getClass().getName()));
        Set<String> conditions = app.getConditions();
        data.put(CONFIG_CONDITIONS, (conditions == null) ?
            Collections.EMPTY_SET : conditions);

        ISaveIndex sIndex = app.getSaveIndex();
        String saveIndexClassName = (sIndex != null) ? 
            sIndex.getClass().getName() : null;
        data.put(CONFIG_SAVE_INDEX_IMPL, (saveIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(saveIndexClassName));

        ISearchIndex searchIndex = app.getSearchIndex();
        String searchIndexClassName = (searchIndex != null) ?
            searchIndex.getClass().getName() : null;
        data.put(CONFIG_SEARCH_INDEX_IMPL, (searchIndexClassName == null) ?
            Collections.EMPTY_SET : getSet(searchIndexClassName));

        ResourceName recComp = app.getResourceComparator();
        String resCompClassName = (recComp != null) ? 
            recComp.getClass().getName() : null;
        data.put(CONFIG_RESOURCE_COMP_IMPL, (resCompClassName == null) ?
            Collections.EMPTY_SET : getSet(resCompClassName));

        Set<String> sbjAttributes = app.getAttributeNames();
        data.put(ATTR_NAME_SUBJECT_ATTR_NAMES, (sbjAttributes == null) ?
            Collections.EMPTY_SET : sbjAttributes);
        return data;
    }

    private ApplicationType createApplicationType(
        String name,
        Map<String, Set<String>> data
    ) {
        Map<String, Boolean> actions = getActions(data);
        String saveIndexImpl = getAttribute(data,
            CONFIG_SAVE_INDEX_IMPL);
        ISaveIndex saveIndex = ApplicationTypeManager.getSaveIndex(
            saveIndexImpl);
        String searchIndexImpl = getAttribute(data,
            CONFIG_SEARCH_INDEX_IMPL);
        ISearchIndex searchIndex =
            ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        String resourceComp = getAttribute(data,
            CONFIG_RESOURCE_COMP_IMPL);
        ResourceName resComp =
            ApplicationTypeManager.getResourceComparator(resourceComp);

        return new ApplicationType(name, actions, searchIndex, saveIndex,
            resComp);
    }
    
    private Application createApplication(
        String name,
        Map<String, Set<String>> data
    ) {
        String applicationType = getAttribute(data,
            CONFIG_APPLICATIONTYPE);
        ApplicationType appType = ApplicationTypeManager.getAppplicationType(
            applicationType);
        Application app = new Application(name, appType);

        Map<String, Boolean> actions = getActions(data);
        if (actions != null) {
            app.setActions(actions);
        }

        Set<String> resources = data.get(CONFIG_RESOURCES);
        if (resources != null) {
            app.setResources(resources);
        }

        String entitlementCombiner = getAttribute(data,
            CONFIG_ENTITLEMENT_COMBINER);
        Class combiner = getEntitlementCombiner(
            entitlementCombiner);
        app.setEntitlementCombiner(combiner);

        Set<String> conditionClassNames = data.get(
            CONFIG_CONDITIONS);
        if (conditionClassNames != null) {
            app.setConditions(conditionClassNames);
        }

        String saveIndexImpl = getAttribute(data,
            CONFIG_SAVE_INDEX_IMPL);
        ISaveIndex saveIndex = ApplicationTypeManager.getSaveIndex(
            saveIndexImpl);
        if (saveIndex != null) {
            app.setSaveIndex(saveIndex);
        }

        String searchIndexImpl = getAttribute(data,
            CONFIG_SEARCH_INDEX_IMPL);
        ISearchIndex searchIndex =
            ApplicationTypeManager.getSearchIndex(searchIndexImpl);
        if (searchIndex != null) {
            app.setSearchIndex(searchIndex);
        }

        String resourceComp = getAttribute(data,
            CONFIG_RESOURCE_COMP_IMPL);
        ResourceName resComp =
            ApplicationTypeManager.getResourceComparator(resourceComp);
        if (resComp != null) {
            app.setResourceComparator(resComp);
        }

        Set<String> attributeNames = data.get(
            ATTR_NAME_SUBJECT_ATTR_NAMES);
        if (attributeNames != null) {
            app.setAttributeNames(attributeNames);
        }

        return app;
    }

    public Set<String> getSubjectAttributeNames(
        String realm,
        String application) {
        try {
            ServiceConfig applConfig = getApplicationSubConfig(realm,
                application);
            if (applConfig != null) {
                Application app = createApplication(realm,
                    applConfig.getAttributes());
                return app.getAttributeNames();
            }
        } catch (SMSException ex) {
            //TOFIX
        } catch (SSOException ex) {
            //TOFIX
        }
        return Collections.EMPTY_SET;
    }
}
