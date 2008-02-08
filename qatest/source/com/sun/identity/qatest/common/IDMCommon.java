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
 * $Id: IDMCommon.java,v 1.7 2008-02-08 08:28:52 kanduls Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.qatest.idm.IDMConstants;
import com.sun.identity.sm.OrganizationConfigManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * This class contains helper methods related to identity objects
 */
public class IDMCommon extends TestCommon {
    
    /**
     * Empty Constructor
     */
    public IDMCommon() {
        super("IDMCommon");
    }
    
    public IDMCommon(String componentName) {
        super(componentName);
    }
    
    /**
     * Creates a new Identity
     */
    public AMIdentity createIdentity(
            SSOToken ssoToken,
            String parentRealm,
            IdType idType,
            String entityName,
            Map values)
    throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        AMIdentity amid = repo.createIdentity(idType, entityName, values);
        assert (amid.getName().equals(entityName));
        return amid;
    }
    
    /**
     * Gets realm identity
     */
    public AMIdentity getRealmIdentity(
            SSOToken ssoToken,
            String parentRealm)
    throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        return (repo.getRealmIdentity());
    }
    
    /**
     * Returns the value of specified attribute for an Identity
     */
    public Set getIdentityAttribute(
            SSOToken ssoToken,
            String serviceName,
            String attributeName)
    throws Exception {
        AMIdentity amid = new AMIdentity(ssoToken);
        Map attrValues = amid.getServiceAttributes(serviceName);
        log(logLevel, "getIdentityAttribute", "Attributes List" + attrValues);
        return (Set)attrValues.get(attributeName);
    }
    
    /**
     * Modifies specified identity attributes
     */
    public void modifyIdentity(AMIdentity amid, Map values)
    throws Exception {
        amid.setAttributes(values);
        amid.store();
    }
    
    /**
     * Modifies specified identity attributes for a realm
     */
    public void modifyRealmIdentity(SSOToken token, String realm, Map values)
    throws Exception {
        AMIdentity amid = getRealmIdentity(token, realm);
        amid.setAttributes(values);
        amid.store();
    }
    
    /**
     * Deletes an identity based on specified ssotoken, realm, id type and
     * entity name
     */
    public void deleteIdentity(
            SSOToken ssoToken,
            String parentRealm,
            IdType idType,
            String entityName)
    throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        repo.deleteIdentities(getAMIdentity(
                ssoToken, entityName, idType, parentRealm));
    }

    /**
     * Deletes multiple identities based on specified ssotoken, realm, id type, 
     * and a list of entity names
     */
    public void deleteIdentity(
            SSOToken ssoToken,
            String parentRealm,
            List<IdType> idType,
            List entityName)
    throws Exception {
        AMIdentityRepository repo = new AMIdentityRepository(
                ssoToken, parentRealm);
        Iterator iterNameSet = entityName.iterator();
        Iterator iterTypeSet = idType.iterator();
        Set amid = new HashSet<AMIdentity>();
        while (iterNameSet.hasNext()) {
            amid.add(getFirstAMIdentity(ssoToken, (String)iterNameSet.next(), 
                    (IdType)iterTypeSet.next(), parentRealm));
        }
        repo.deleteIdentities(amid);
    }
    
    /**
     * Returns AMIdentity based in ssotoken, id name , id type and  sepcified
     * realm
     */
    public Set<AMIdentity> getAMIdentity(
            SSOToken ssoToken,
            String name,
            IdType idType,
            String realm)
    throws Exception {
        Set<AMIdentity> set = new HashSet<AMIdentity>();
        set.add(new AMIdentity(ssoToken, name, idType, realm, null));
        return set;
    }
    
    /**
     * Returns First AMIdentity based in ssotoken, id name , id type and
     * sepcified realm
     */
    public AMIdentity getFirstAMIdentity(
            SSOToken ssoToken,
            String name,
            IdType idType,
            String realm)
    throws Exception {
        Set<AMIdentity> set = getAMIdentity(ssoToken, name, idType, realm);
        AMIdentity amid = null;
        for (Iterator itr = set.iterator(); itr.hasNext();) {
            amid = (AMIdentity)itr.next();
        }
        return (amid);
    }
    
    /**
     * Creates a dummy user in the specified realm.
     * (a) The user name (sn and cn) are equal to  sn or cn plus the supplied
     *     suffix string
     * (b) The user password is concatanation of entityName and suffix
     * (c) User status is set to Active
     */
    public AMIdentity createDummyUser(
            SSOToken ssoToken,
            String parentRealm,
            String entityName,
            String suffix)
    throws Exception {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        putSetIntoMap("sn", map, "sn" + suffix);
        putSetIntoMap("cn", map, "cn" + suffix);
        putSetIntoMap("userpassword", map, entityName + suffix);
        putSetIntoMap("inetuserstatus", map, "Active");
        return createIdentity(ssoToken, parentRealm, IdType.USER, entityName +
                suffix, map);
    }
    
    /**
     * Returns the parent realm
     */
    public String getParentRealm(String realmName)
    throws Exception {
        if (realmName.lastIndexOf("/") == -1) {
            throw new RuntimeException("Incorrect Realm, " + realmName);
        }
        StringTokenizer tokens = new StringTokenizer(realmName, "/");
        int noRealms = tokens.countTokens();
        String parentRealm = realm;
        if (noRealms == 1) {
            return parentRealm;
        } else {
            for (int i = 1; i < noRealms; i++ ){
                parentRealm = tokens.nextToken();
            }
        }
        log(logLevel, "getParentRealm", "Parent realm for" + realmName + 
                " is " + parentRealm );
        return parentRealm;
    }
    
    /**
     * Creates a new realm under the specified realm
     */
    public void createSubRealm(SSOToken ssoToken, String realm)
    throws Exception {
        if ((realm != null) && !realm.equals("/")) {
            String parentRealm = getParentRealm(realm);
            createSubRealm(ssoToken, parentRealm);
            OrganizationConfigManager orgMgr = new OrganizationConfigManager(
                    ssoToken, parentRealm);
            int idx = realm.lastIndexOf("/");
            orgMgr.createSubOrganization(realm.substring(idx+1), null);
            assert (orgMgr.getSubOrganizationNames().contains(realm));
        }
    }

    /**
     * Deletes a realm
     */
    public void deleteRealm(SSOToken ssoToken, String realm)
    throws Exception {
        if ((realm != null) && !realm.equals("/")) {
            String parentRealm = getParentRealm(realm);
            OrganizationConfigManager orgMgr = new
                    OrganizationConfigManager(ssoToken, parentRealm);
            int idx = realm.lastIndexOf("/");
            orgMgr.deleteSubOrganization(realm.substring(idx+1), true);
            deleteRealm(ssoToken, parentRealm);
        }
    }
    
    /**
     * Adds a User Identity to a Group or a Role Identity on a root realm
     */
    public void addUserMember(SSOToken ssotoken, String userName,
            String memberName, IdType memberType)
    throws Exception {
        addUserMember(ssotoken, userName, memberName, memberType, realm);
    }
    
    /**
     * Adds a User Identity to a Group or a Role Identity
     */
    public void addUserMember(SSOToken ssotoken, String userName,
            String memberName, IdType memberType, String tRealm)
    throws Exception {
        Set setUser = getAMIdentity(ssotoken, userName, IdType.USER, tRealm);
        Set setMember = getAMIdentity(ssotoken, memberName, memberType, tRealm);
        AMIdentity amidUser = null;
        AMIdentity amidMember = null;
        Iterator itr;
        for (itr = setUser.iterator(); itr.hasNext();) {
            amidUser = (AMIdentity)itr.next();
        }
        for (itr = setMember.iterator(); itr.hasNext();) {
            amidMember = (AMIdentity)itr.next();
        }
        amidMember.addMember(amidUser);
    }
    
    /**
     * Remove a User Identity from a Group or a Role Identity
     * @param ssotoken SSO token
     * @param userName user name to be removed
     * @param memberName member name
     * @param memberType member type
     * @param tRealm realm name
     */
    public void removeUserMember(SSOToken ssotoken, String userName,
            String memberName, IdType memberType, String tRealm)
    throws Exception {
        Set setUser = getAMIdentity(ssotoken, userName, IdType.USER, tRealm);
        Set setMember = getAMIdentity(ssotoken, memberName, memberType, tRealm);
        AMIdentity amidUser = null;
        AMIdentity amidMember = null;
        Iterator itr;
        for (itr = setUser.iterator(); itr.hasNext();) {
            amidUser = (AMIdentity)itr.next();
        }
        for (itr = setMember.iterator(); itr.hasNext();) {
            amidMember = (AMIdentity)itr.next();
        }
        amidMember.removeMember(amidUser);
    }
    
    /**
     * Retrieve a list of members
     * @param ssotoken SSO token
     * @param idName identity name that retrieve a list of members
     * @param idType identity type
     * @param memberType member type
     * @param tRealm realm name
     */
    public Set<AMIdentity> getMembers(SSOToken ssotoken, String idName, 
            IdType idType, IdType memberType, String tRealm)
    throws Exception {
        Set setId = getAMIdentity(ssotoken, idName, idType, tRealm);
        AMIdentity amid = null;
        Iterator itr;
        for (itr = setId.iterator(); itr.hasNext();) {
            amid = (AMIdentity)itr.next();
        }
        return amid.getMembers(memberType);
    }
    
    /**
     * This method searches and retrieves a list of realm
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @parm realm under which search has to be perfomed
     * @return a set of realm name
     */
    public Set searchRealms(SSOToken ssotoken, String pattern, String realm)
    throws Exception  {
        entering("searchRealms", null);
        Set realmNames = searchIdentities(ssotoken, pattern, IdType.REALM, 
                realm);
        exiting("searchRealms");
        return realmNames;
    }
    
     /**
     * This method searches and retrieves a list of realm
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @return a set of realm name
     */
    public Set searchRealms(SSOToken ssotoken, String pattern)
    throws Exception  {
        entering("searchRealms", null);
        Set realmNames = searchRealms(ssotoken, pattern, realm);
        exiting("searchRealms");
        return realmNames;
    }
    
     /**
     * This method searches and retrieves a list of identity
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @param type identity type - user, role, filtered role, group, agent
     * @return a set of identity name
     */
    public Set searchIdentities(SSOToken ssotoken, String pattern, IdType type)
    throws Exception  {
        return searchIdentities(ssotoken, pattern, type, realm);
    }
    
    /**
     * This method searches and retrieves a list of identity
     * @param ssotoken SSO token object
     * @param pattern realm name or pattern
     * @param type identity type - user, role, filtered role, group, agent
     * @param realmName - realm name
     * @return a set of identity name
     */
    public Set searchIdentities(SSOToken ssotoken, String pattern, IdType type,
            String realmName)
    throws Exception  {
        entering("searchIdentities", null);
        AMIdentityRepository repo = new AMIdentityRepository(
                ssotoken, realmName);
        IdSearchControl searchControl = new IdSearchControl();
        IdSearchResults results = repo.searchIdentities(type, pattern,
                searchControl);
        log(Level.FINE, "searchIdentities", "Searching for " + type.getName() +
                " " + pattern + "... under realm "+realmName);
        Set idNames = results.getSearchResults();
        if ((idNames != null) && (!idNames.isEmpty())) {
            Iterator iter = idNames.iterator();
            AMIdentity amIdentity;
            while (iter.hasNext()) {
                amIdentity = (AMIdentity) iter.next();
                log(Level.FINEST, "searchIdentities", "Id name: " +
                        amIdentity.getName() +
                        " & UID = "  + amIdentity.getUniversalId());
            }
        } else {
            log(Level.FINE, "searchIdentities",
                    "Could not find identity name " + pattern);
        }
        exiting("searchIdentities");
        return idNames;
    }
    
    /**
     * This method retrieves the configuration key and values by the prefix 
     * string and store them in a map.
     * @param prefixName key prefix string
     * @param cfgFileName properties config file name
     * @return map of configuration
     */
    public Map getDataFromCfgFile(String prefixName, String cfgFileName)
    throws Exception {
        entering("getDataFromCfgFile", null);
        Map cfgMapTemp = new HashMap();
        Map cfgMapNew = new HashMap();
        cfgMapTemp = getMapFromResourceBundle(cfgFileName);
        Set keys = cfgMapTemp.keySet();
        Iterator keyIter = keys.iterator();
        String key;
        String value;
        while (keyIter.hasNext()) {
            key = keyIter.next().toString();
            value = cfgMapTemp.get(key).toString();
            if (key.substring(0, prefixName.length()).equals(prefixName))
                cfgMapNew.put(key, value);
        }
        log(Level.FINEST, "getDataFromCfgFile", cfgMapNew.toString());
        if (cfgMapNew.isEmpty()) {
            log(Level.SEVERE, "getDataFromCfgFile",
                    "Config data map is empty");
            assert false;
        }
        exiting("getDataFromCfgFile");
        return cfgMapNew;
    }
    
    /**
     * This method checks and compare IDM exception error message and error 
     * code with expected error message and code. 
     * @param IdRepoException idm exception
     * @param eMessage expected error message
     * @param eCode expected error code
     * @return true if match
     */
    public boolean checkIDMExpectedErrorMessageCode(IdRepoException e, 
            String eMessage, String eCode)
    throws Exception {
        entering("checkIDMExpectedErrorMessageCode", null);
        boolean isMatch = false;
        String errorCode = e.getErrorCode();
        String errorMessage = e.getMessage();
        log(Level.FINEST, "checkExpectedMessageErrorCode", "Error message: " +
                e.getMessage() + " error code: " + e.getErrorCode());
        log(Level.FINEST, "checkExpectedMessageErrorCode", 
                "Expected message: " + eMessage + " expected error code: " +
                eCode);
        if (errorCode.equals(eCode) && errorMessage.indexOf(eMessage) >= 0) {
            log(Level.FINE, "checkExpectedMessageErrorCode",
                    "Error code and message match");
            isMatch = true;
        }  
        exiting("checkIDMExpectedErrorMessageCode");
        return isMatch;
    }
    
    /**
     * This method checks the support identity type in current deployment.
     * @param ssotoken SSO token
     * @param realmName realm name
     * @param idtype identity type
     * @return true if identity type to be checked is supported identity type
     */
    public boolean isIdTypeSupported(SSOToken ssotoken, String realmName, 
            String idtype)
    throws Exception {
        entering("isIdTypeSupported", null);
        boolean supportsIDType = false;
        AMIdentityRepository idrepo = 
                new AMIdentityRepository(ssotoken, realmName);
        Set types = idrepo.getSupportedIdTypes();
        log(Level.FINEST, "isIdTypeSupported", "Support id type is " + 
                types.toString());
        Iterator iter = types.iterator();
        IdType type;
        while (iter.hasNext()) {
            type =(IdType)iter.next();
            if (type.getName().equalsIgnoreCase(idtype)) {
                supportsIDType = true;
                break;
            }
        }
        exiting("isIdTypeSupported");
        return supportsIDType;
    }
    
    /**
     * This method addes an user member to an identity with identity name, type,
     * and member name.
     * @param idName identity name
     * @param idType identity type
     * @param memberName user member name
     * @return true if member is added to an identity successfully
     */
    public boolean addMembers(String idName, String idType, String memberName,
            SSOToken ssoToken,
            String realmName)
    throws Exception {
        entering("addMembers", null);
        boolean opSuccess = false;
        log(Level.FINE, "addMembers", "Adding a user member name " +
                memberName + " to " + idType + " " + idName + "...");
        addUserMember(ssoToken, memberName, idName, getIdType(idType),
                realmName);
        AMIdentity memid = getFirstAMIdentity(ssoToken, memberName,
                IdType.USER, realmName);
        AMIdentity id = getFirstAMIdentity(ssoToken, idName, getIdType(idType),
                realmName);
        opSuccess = memid.isMember(id);
        if (opSuccess) {
            log(Level.FINE, "addMembers", "User member " + memberName +
                    " is added to " + idType + " " + idName + " successfully");
        } else {
            log(Level.FINE, "addMembers", "Failed to add member");
        }
        exiting("addMembers");
        return opSuccess;
    }
       
    /**
     * This method removes an user member from an identity with identity name,
     * type, and member name.
     * @param idName identity name
     * @param idType identity type
     * @param memberName user member name
     * @return true if member is removed from an identity successfully
     */
    public boolean removeMembers(String idName, String idType,
            String memberName,
            SSOToken ssoToken,
            String realmName)
    throws Exception {
        entering("removeMembers", null);
        boolean opSuccess = false;
        log(Level.FINE, "removeMembers", "Removing a user member name " +
                memberName + " from " + idType + " " + idName + "...");
        removeUserMember(ssoToken, memberName, idName, getIdType(idType),
                realmName);
        AMIdentity memid = getFirstAMIdentity(ssoToken, memberName,
                IdType.USER, realmName);
        AMIdentity id = getFirstAMIdentity(ssoToken, idName, getIdType(idType), 
                realmName);
        opSuccess = (!memid.isMember(id)) ? true : false;
        if (opSuccess) {
            log(Level.FINE, "removeMembers", "User member " + memberName +
                    " is removed from " + idType + " " + idName + 
                    " successfully");
        } else {
            log(Level.FINE, "removeMembers", "Failed to remove member");
        }
        exiting("removeMembers");
        return opSuccess;
    }
    
    /**
     * This method creates a identity with given name and type and verifies
     * that user exists.
     * @param idName    identity name
     * @param idType    identity type - user, group, role, filtered role, agent
     * @param userAttr  identity attributes. If null, default attributes is used
     * @param ssoToken  admin SSOtoken for creating identity
     * @param realmName realm name in which identity has be created.
     * @return true if the identity created successfully.
     */
    public boolean createID(String idName, String idType, String userAttr,
            SSOToken ssoToken,
            String realmName)
    throws Exception {
        entering("createID", null);
        boolean opSuccess = false;
        log(Level.FINE, "createID", "Creating identity " + idType +
                " name " + idName + "...");
        Map userAttrMap;
        if (userAttr == null) {
            userAttrMap = setDefaultIdAttributes(idType, idName);
        } else {
            userAttrMap = setIDAttributes(userAttr);
        }
        log(Level.FINEST, "createID", "realm = " + realmName
                + " type = " + getIdType(idType).getName() +
                " attributes = " + userAttrMap.toString());
        createIdentity(ssoToken, realmName, getIdType(idType), idName,
                userAttrMap);
        opSuccess = (doesIdentityExists(idName, idType, ssoToken,
                realmName)) ? true : false;
        if (opSuccess) {
            log(Level.FINE, "createID", idType + " " + idName +
                    " is created successfully.");
        } else {
            log(Level.FINE, "createID", "Failed to create " + idType +
                    " " + idName);
        }
        exiting("createID");
        return (opSuccess);
    }
    
    /**
     * This method creates a map of identity attributes
     */
    public Map setIDAttributes(String idAttrList)
    throws Exception {
        log(Level.FINEST, "setIDAttributes", "Attributes string " + idAttrList);
        Map tempAttrMap = new HashMap();
        Map idAttrMap = getAttributeMap(idAttrList,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        Set keys = idAttrMap.keySet();
        Iterator keyIter = keys.iterator();
        String key;
        String value;
        Set idAttrSet;
        while (keyIter.hasNext()) {
            key = (String)keyIter.next();
            value = (String)idAttrMap.get(key);
            putSetIntoMap(key, tempAttrMap, value);
        }
        return tempAttrMap;
    }
    
    /**
     * This method create a map with default identity attributes.  This map
     * is used to create an identity
     */
    public Map setDefaultIdAttributes(String siaType, String idName)
    throws Exception {
        Map<String, Set<String>> tempMap = new HashMap<String, Set<String>>();
        log(Level.FINEST, "setDefaultIdAttributes", "for " + idName);
        if (siaType.equals("user")) {
            putSetIntoMap("sn", tempMap, idName);
            putSetIntoMap("cn", tempMap, idName);
            putSetIntoMap("givenname", tempMap, idName);
            putSetIntoMap("userpassword", tempMap, idName);
            putSetIntoMap("inetuserstatus", tempMap, "Active");
        } else if (siaType.equals("agent")) {
            putSetIntoMap("userpassword", tempMap, idName);
            putSetIntoMap("sunIdentityServerDeviceStatus", tempMap, "Active");
        } else if (siaType.equals("filteredrole")) {
            putSetIntoMap("cn", tempMap, idName);
            putSetIntoMap("nsRoleFilter", tempMap,
                    "(objectclass=inetorgperson)");
        } else if (siaType.equals("role") || siaType.equals("group")) {
            putSetIntoMap("description", tempMap, siaType + " description");
        } else {
            log(Level.SEVERE, "setIdAttributes", "Invalid identity type " +
                    siaType);
            assert false;
        }
        return tempMap;
    }
    
    /**
     * This method checks if an identity exists.  It accepts identity name and
     * type from the arguments.
     * @param idName identity name
     * @param idType identity type - user, agent, role, filtered role, group
     * @return true if identity exists
     */
    public boolean doesIdentityExists(String idName, String idType,
            SSOToken ssoToken,
            String realmName)
    throws Exception {
        return doesIdentityExists(idName, getIdType(idType), ssoToken,
                realmName);
    }
    
    /**
     * This method checks if an identity exists.  It accepts identity name and
     * type from the arguments.
     * @param idName identity name
     * @param idType Idtype of identity type
     * @param ssoToken admin SSOToken
     * @parm realmName realm Name in which identity present.
     * @return true if identity exists
     */
    public boolean doesIdentityExists(String idName, IdType idType,
            SSOToken ssoToken,
            String realmName)
    throws Exception {
        entering("doesIdentityExists", null);
        boolean idFound = false;
        Set idRes = searchIdentities(ssoToken, idName, idType,
                realmName);
        Iterator iter = idRes.iterator();
        AMIdentity amIdentity;
        while (iter.hasNext()) {
            amIdentity = (AMIdentity) iter.next();
            if (amIdentity.getName().equals(idName)) {
                idFound = true;
                break;
            }
            log(Level.FINEST, "searchIdentities", "Search result - name: " +
                    amIdentity.getName());
        }
        exiting("doesIdentityExists");
        return (idFound);
    }
    
    /**
     * This method deletes one or multiple identities with identity name and
     * type
     * @param idName identity name
     * @param idType identity type - user, agent, role, filtered role, group
     * @param ssoToken admin SSOToken
     * @param realmName realm Name from which identity has to be deleted.
     * @return true if identity is deleted successfully
     */
    public boolean deleteID(String idName, String idType, SSOToken ssoToken,
            String realmName)
    throws Exception {
        entering("deleteID", null);
        boolean opSuccess = false;
        if (idType == null) {
            log(Level.FINE, "deleteID", "Failed to delete idType cannot be null" 
                    + idType + " " + idName);
            return false;
        }
        List idTypeList = getAttributeList(idType,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        List idNameList = getAttributeList(idName,
                IDMConstants.IDM_KEY_SEPARATE_CHARACTER);
        log(Level.FINEST, "deleteID", idNameList.toString());
        log(Level.FINE, "deleteID", "Deleting identity " + idType +
                " name " + idName + "...");
        Iterator iterName = idNameList.iterator();
        Iterator iterType = idTypeList.iterator();
        List newidTypeList = new ArrayList();
        while (iterName.hasNext()) {
            if (iterType.hasNext()) {
                newidTypeList.add(getIdType((String)iterType.next()));
            } else {
                newidTypeList.add(getIdType(idType));
            }
            iterName.next();
        }
        deleteIdentity(ssoToken, realmName, newidTypeList, idNameList);
        Iterator iterN = idNameList.iterator();
        Iterator iterT = newidTypeList.iterator();
        while (iterN.hasNext()) {
            if (doesIdentityExists((String)iterN.next(),
                    (IdType)iterT.next(), ssoToken, realmName)) {
                opSuccess = false;
                break;
            } else{
                opSuccess = true;
            }
        }
        if (opSuccess) {
            log(Level.FINE, "deleteID", idType + " " + idName +
                    " is deleted successfully.");
        } else {
            log(Level.FINE, "deleteID", "Failed to delete " + idType +
                    " " + idName);
        }
        exiting("deleteID");
        return opSuccess;
    }
    
    /**
     * This method return type IdType of identity type
     */
    public IdType getIdType(String gidtType)
    throws Exception {
        if (gidtType.equals("user")) {
            return IdType.USER;
        } else if (gidtType.equals("role")) {
            return IdType.ROLE;
        } else if (gidtType.equals("filteredrole")) {
            return IdType.FILTEREDROLE;
        } else if (gidtType.equals("agent")) {
            return IdType.AGENT;
        } else if (gidtType.equals("group")) {
            return IdType.GROUP;
        } else {
            log(Level.SEVERE, "getIdType", "Invalid id type " + gidtType);
            assert false;
            return null;
        }
    }
}
