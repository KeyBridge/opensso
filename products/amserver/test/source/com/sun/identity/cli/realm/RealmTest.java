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
 * $Id: RealmTest.java,v 1.2 2006-10-09 17:57:36 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli.realm;

import com.iplanet.sso.SSOException;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIRequest;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.DevNullOutputWriter;
import com.sun.identity.cli.IArgument;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.test.common.CollectionUtils;
import com.sun.identity.test.common.TestBase;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * This is to test the realm related sub commands.
 */
public class RealmTest extends TestBase{
    private CommandManager cmdManager;
    private static DevNullOutputWriter outputWriter = new DevNullOutputWriter();

    /**
     * Creates a new instance of <code>CLIFrameworkTest</code>
     */
    public RealmTest() {
        super("CLI");
    }
    
    /**
     * Create the CLIManager.
     */
    @BeforeTest(groups = {"cli-realm"})
    public void suiteSetup()
        throws CLIException {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(CLIConstants.SYS_PROPERTY_COMMAND_NAME, "testclifw");
        env.put(CLIConstants.SYS_PROPERTY_DEFINITION_FILES,
            "com.sun.identity.cli.AccessManager");
        env.put(CLIConstants.SYS_PROPERTY_OUTPUT_WRITER, outputWriter);
        cmdManager = new CommandManager(env);
    }
    
    @Parameters ({"realm"})
    @BeforeTest(groups = {"cli-realm"})
    public void createRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("createRealm", param);
        String[] args = {
            "create-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        String parentRealm = RealmUtils.getParentRealm(realm);
        String realmName = RealmUtils.getChildRealm(realm);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), parentRealm);
        Set names = ocm.getSubOrganizationNames(realmName, true);
        assert (names.size() == 1);
        String name = (String)names.iterator().next();
        assert name.equals(realmName);
        exiting("createRealm");
    }

    @Parameters ({"parent-realm"})
    @Test(groups = {"cli-realm"})
    public void listRealms(String parentRealm)
        throws CLIException, SMSException {
        String[] param = {parentRealm};
        entering("listRealms", param);
        String[] args = {
            "list-realms",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.PATTERN,
            "*",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            parentRealm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("listRealms");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-realm"})
    public void getAssignableServicesInRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("getAssignableServicesInRealm", param);
        String[] args = {
            "list-realm-assignable-services",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAssignableServicesInRealm");
    }
 
    @Parameters ({"realm"})
    @Test(groups = {"cli-realm"})
    public void getAssignedServicesInRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("getAssignedServicesInRealm", param);
        String[] args = {
            "show-realm-services",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getAssignedServicesInRealm");
    }
    
    @Parameters ({"realm", "service-name", "attribute-value"})
    @Test(groups = {"cli-realm"})
    public void assignedServiceToRealm(
        String realm,
        String serviceName,
        String attributeValue
    ) throws CLIException, IdRepoException, SSOException {
        String[] param = {realm, serviceName, attributeValue};
        entering("assignedServiceToRealm", param);
        String[] args = {
            "add-service-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            attributeValue
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        AMIdentityRepository amir = new AMIdentityRepository(
            getAdminSSOToken(), realm);
        AMIdentity ai = amir.getRealmIdentity();
        ai.getServiceAttributes(serviceName);
        exiting("assignedServiceToRealm");
    }

    @Parameters ({"realm"})
    @Test(groups = {"cli-realm"})
    public void getRealmAttributeValues(String realm)
        throws CLIException {
        String[] param = {realm};
        entering("getRealmAttributeValues", param);
        String[] args = {
            "get-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "sunIdentityRepositoryService"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getRealmAttributeValues");
    }
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-realm"},
        dependsOnMethods = {"removeRealmAttribute"})
    public void setRealmAttributeValues(String realm)
        throws CLIException, SMSException, SSOException {
        String[] param = {realm};
        entering("setRealmAttributeValues", param);
        String[] args = {
            "set-realm-attributes",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "sunIdentityRepositoryService",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            "sunOrganizationStatus=Active"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), realm);
        Map values = ocm.getAttributes("sunIdentityRepositoryService");
        Set attrValues = (Set)values.get("sunOrganizationStatus");
        assert attrValues.contains("Active");
        exiting("setRealmAttributeValues");
    }  
    
    @Parameters ({"realm"})
    @Test(groups = {"cli-realm"})
    public void removeRealmAttribute(String realm)
        throws CLIException, SMSException, SSOException {
        String[] param = {realm};
        entering("removeRealmAttribute", param);
        String[] args = {
            "delete-realm-attribute",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            "sunIdentityRepositoryService",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_NAME,
            "sunOrganizationStatus"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), realm);
        Map values = ocm.getAttributes("sunIdentityRepositoryService");
        Set attrValues = (Set)values.get("sunOrganizationStatus");
        assert (attrValues == null);
        exiting("removeRealmAttribute");
    }  
    
    @Test(groups = {"cli-realm"})
    public void createPolicy()
        throws CLIException, PolicyException, SSOException {
        entering("createPolicy", null);
        String[] args = {
            "create-policies",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.XML_FILE,
            "mock/cli/createpolicy.xml"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        PolicyManager pm = new PolicyManager(getAdminSSOToken(), "/");
        Policy p = pm.getPolicy("clipolicy");
        assert (p != null);
        exiting("createPolicy");        
    }
    
    @Test(groups = {"cli-realm"}, dependsOnMethods = {"createPolicy"})
    public void getPolicy()
        throws CLIException, PolicyException, SSOException {
        entering("getPolicy", null);
        String[] args = {
            "list-policies",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getPolicy");        
    }

    @Test(groups = {"cli-realm"}, dependsOnMethods = {"getPolicy"},
        expectedExceptions = {PolicyException.class})
    public void deletePolicy()
        throws CLIException, PolicyException, SSOException {
        entering("deletePolicy", null);
        String[] args = {
            "delete-policies",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            "/",
            CLIConstants.PREFIX_ARGUMENT_LONG + 
                RealmDeletePolicy.ARGUMENT_POLICY_NAMES,
            "clipolicy"
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        
        PolicyManager pm = new PolicyManager(getAdminSSOToken(), "/");
        Policy p = pm.getPolicy("clipolicy");
        assert (p == null);
        exiting("deletePolicy");        
    }
    
    @Parameters ({"realm", "service-name"})
    @Test(groups = {"cli-realm", "services"}, 
        dependsOnMethods = {"assignedServiceToRealm"})
    public void getServiceAttribute(String realm, String serviceName)
        throws CLIException {
        String[] param = {realm};
        entering("getServiceAttribute", param);
        String[] args = {
            "show-realm-service-attributes",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        exiting("getServiceAttribute");
    }

    
    @Parameters ({"realm", "service-name", "modify-attribute-value"})
    @Test(groups = {"cli-realm", "services"}, 
        dependsOnMethods = {"assignedServiceToRealm"})
    public void setServiceAttribute(
        String realm,
        String serviceName,
        String attributeValue
    ) throws CLIException, IdRepoException, SSOException {        
        String[] param = {realm};
        entering("setServiceAttribute", param);
        String[] args = {
            "set-service-attribute",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.ATTRIBUTE_VALUES,
            attributeValue
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        AMIdentityRepository amir = new AMIdentityRepository(
            getAdminSSOToken(), realm);
        AMIdentity ai = amir.getRealmIdentity();
        Map map = ai.getServiceAttributes(serviceName);
        Map<String, Set<String>> orig = 
            CollectionUtils.parseStringToMap(attributeValue);
        
        String key = orig.keySet().iterator().next();
        String value = orig.get(key).iterator().next();
        
        Set resultSet = (Set)map.get(key);
        String result = (String)resultSet.iterator().next();
        
        assert (result.equals(value));
        exiting("setServiceAttribute");
    }
    
    @Parameters ({"realm", "service-name", "attribute-value"})
    @Test(groups = {"cli-realm"}, dependsOnGroups = {"services"})
    public void unassignServiceFromRealm(
        String realm,
        String serviceName,
        String attributeValue
    ) throws CLIException, IdRepoException, SSOException {
        String[] param = {realm};
        entering("unassignServiceFromRealm", param);
        String[] args = {
            "remove-service-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm,
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.SERVICE_NAME,
            serviceName
        };

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();
        AMIdentityRepository amir = new AMIdentityRepository(
            getAdminSSOToken(), realm);
        AMIdentity ai = amir.getRealmIdentity();
        Map map = ai.getServiceAttributes(serviceName);
        Map orig = CollectionUtils.parseStringToMap(attributeValue);
        assert !map.equals(orig);
        exiting("unassignServiceFromRealm");
    }
    
    @Parameters ({"realm"})
    @AfterTest(groups = {"cli-realm"})
    @Test(expectedExceptions = {SMSException.class})
    public void deleteRealm(String realm)
        throws CLIException, SMSException {
        String[] param = {realm};
        entering("deleteRealm", param);
        String[] args = {
            "delete-realm",
            CLIConstants.PREFIX_ARGUMENT_LONG + IArgument.REALM_NAME,
            realm};

        CLIRequest req = new CLIRequest(null, args, getAdminSSOToken());
        cmdManager.addToRequestQueue(req);
        cmdManager.serviceRequestQueue();

        String parentRealm = RealmUtils.getParentRealm(realm);
        String realmName = RealmUtils.getChildRealm(realm);
        OrganizationConfigManager ocm = new OrganizationConfigManager(
            getAdminSSOToken(), realm);
        exiting("deleteRealm");
    }
}
