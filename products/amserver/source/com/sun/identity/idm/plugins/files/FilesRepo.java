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
 * $Id: FilesRepo.java,v 1.6 2006-06-24 00:09:10 arviranga Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.idm.plugins.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import com.iplanet.am.sdk.AMEvent;
import com.iplanet.am.util.Debug;
import com.iplanet.services.util.Hash;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoBundle;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdRepoListener;
import com.sun.identity.idm.IdRepoUnsupportedOpException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.jaxrpc.SOAPClient;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.sm.SchemaType;

/*******************************************************************************
 * FilesRepo stores identity information in flat files using java.io.File
 * classes. The directory structure is organized as follows: The root directory
 * is specified by the system property "com.sun.identity.idm.files.rootDir".
 * Under the root directory are sub-directories for each identity type (i.e.,
 * users, roles, agenets, etc). In these sub-directories an identity is stored
 * as a propertries file.
 ******************************************************************************/

public class FilesRepo extends IdRepo {
    // Class name
    public static final String NAME = 
        "com.sun.identity.idm.plugins.files.FilesRepo";

    // Debug
    public static final Debug debug = Debug.getInstance("amIdRepoFiles");

    // SMS Configurations
    // Root directory
    public static final String DIRECTORY = "sunFilesIdRepoDirectory";

    // Password attribute
    public static final String PASSWORD = "sunFilesPasswordAttr";

    // Status attribute
    public static final String STATUS = "sunFilesStatusAttr";

    // Attributes to be hashed
    public static final String HASH = "sunFilesHashAttrs";

    // Attributes to be encrypted
    public static final String ENCRYPT = "sunFilesEncryptAttrs";

    // Objectclass attribute
    public static final String OC = "objectclass";

    // Supported operations
    private static Map supportedOps = new HashMap();

    // Cache of Identity objects
    private static Map identityCache = new HashMap();

    // Default directory to store identity data
    private String directory = "/var/opt/SUNWam/idm/flatfiles";

    // Password attribute
    private String passwordAttribute = "userPassword";

    // Status attribute
    private String statusAttribute = "inetUserStatus";

    // Role membership attribute
    private String roleMembershipAttribute = "nsRoleDN";

    // Group members attribute
    private String groupMembersAttribute = "memberOfGroup";

    // Attributes to be hashed
    private Set hashAttributes = new CaseInsensitiveHashSet();

    // Attributes to be encrypted
    private Set encryptAttributes = new CaseInsensitiveHashSet();

    // IdRepo listner to send notifications
    IdRepoListener repoListener;

    // Initialization exception
    IdRepoException initializationException;

    // Initialize supported types and operations
    static {
        loadSupportedOps();
    }

    public FilesRepo() {
        // do nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#initialize(java.util.Map)
     */
    public void initialize(Map configParams) {
        super.initialize(configParams);
        // Get the directory to store the identity information
        Set set = (Set) configParams.get(DIRECTORY);
        if (set != null && !set.isEmpty()) {
            directory = (String) set.iterator().next();
            try {
                initDir(directory);
            } catch (IdRepoException ide) {
                initializationException = ide;
                debug.error("FilesRepo: Init exception", ide);
            }
        }

        // Get password attribute
        set = (Set) configParams.get(PASSWORD);
        if (set != null && !set.isEmpty()) {
            passwordAttribute = (String) set.iterator().next();
        }
        // Get status attribute
        set = (Set) configParams.get(STATUS);
        if (set != null && !set.isEmpty()) {
            statusAttribute = (String) set.iterator().next();
        }
        // Get attributes to be hashed
        set = (Set) configParams.get(HASH);
        if (set != null && !set.isEmpty()) {
            hashAttributes.addAll(set);
        }
        // Get attributes to be encyrpted
        set = (Set) configParams.get(ENCRYPT);
        if (set != null && !set.isEmpty()) {
            encryptAttributes.addAll(set);
        }

        if (debug.messageEnabled()) {
            debug.message("FlatFiles: Root dir: " + directory
                    + "\n\tPassword Attr: " + passwordAttribute
                    + "\n\tStatus Attr: " + statusAttribute
                    + "\n\tAttrs Hashed: " + hashAttributes
                    + "\n\tAttrs Encyrpted: " + encryptAttributes);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#addListener(com.iplanet.sso.SSOToken,
     *      com.iplanet.am.sdk.IdRepoListener)
     */
    public int addListener(SSOToken token, IdRepoListener listener)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo addListener called");
        }
        repoListener = listener;
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#assignService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void assignService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType stype, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("Assign service called for: " + type.getName() + ":"
                    + name + "\n\t" + serviceName + "=" + attrMap
                    + "\n\tSchema=" + stype);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        if (type.equals(IdType.USER) || type.equals(IdType.ROLE)
                || type.equals(IdType.REALM)) {
            // Update the objectclass and set attributes
            Set set = new HashSet();
            set.add(OC);
            Map attrs = getAttributes(token, type, name, set);
            Set objectclasses = (Set) attrs.get(OC);
            CaseInsensitiveHashMap sAttrs = new CaseInsensitiveHashMap();
            sAttrs.putAll(attrMap);
            Set serviceOcs = (Set) sAttrs.get(OC);
            if (objectclasses != null && !objectclasses.isEmpty()
                    && serviceOcs != null) {
                // Update objectclasses
                serviceOcs.addAll(objectclasses);
            }
            setAttributes(token, type, name, attrMap, false);
        } else {
            Object args[] = { NAME, IdOperation.SERVICE.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#create(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map)
     */
    public String create(SSOToken token, IdType type, String name, Map attrMap)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }
        if (supportedOps.keySet().contains(type)) {
            // Check if identity exists
            File file = constructFile(directory, type, name);
            if (!file.exists()) {
                // Create the identity
                attrMap = processAttributes(attrMap, hashAttributes,
                        encryptAttributes);
                writeFile(file, attrMap);
                // %%% Send notification (must be via a different thread)
                if (repoListener != null) {
                    repoListener.objectChanged(name, AMEvent.OBJECT_ADDED,
                            repoListener.getConfigMap());
                }
            } else {
                // throw exception
                String args[] = { file.getAbsolutePath() };
                throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "310",
                        args));
            }
        } else {
            Object args[] = { NAME, IdOperation.SERVICE.getName(),
                    type.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return (name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#delete(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public void delete(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }
        File file = constructFile(directory, type, name);
        file.delete();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAssignedServices(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Map)
     */
    public Set getAssignedServices(SSOToken token, IdType type, String name,
            Map mapOfServicesAndOCs) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: GetAssignedService called: " + name
                    + "\n\tmapOfServicesAndOCs=" + mapOfServicesAndOCs);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        // Get objectclasses for the identity
        Set attrNames = new HashSet();
        attrNames.add(OC);
        Map ocs = getAttributes(token, type, name, attrNames);
        Set presentOcs = (Set) ocs.get(OC);
        if (presentOcs == null || presentOcs.isEmpty()) {
            return (Collections.EMPTY_SET);
        }

        // Check ocs against the mapOfServicesAndOCs
        Set answer = new HashSet();
        for (Iterator items = mapOfServicesAndOCs.keySet().iterator(); items
                .hasNext();) {
            String serviceName = (String) items.next();
            Set reqOcs = (Set) mapOfServicesAndOCs.get(serviceName);
            if (reqOcs != null && !reqOcs.isEmpty()
                    && presentOcs.containsAll(reqOcs)) {
                answer.add(serviceName);
            }
        }
        return (answer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Set)
     */
    public Map getAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: getAttributes called: " + name
                    + "\n\treturn attributes=" + attrNames);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        // Get all the attributes and return the subset
        Map answer = (attrNames == null) ? null : new HashMap();
        Map map = getAttributes(token, type, name);
        if (attrNames == null) {
            answer = map;
        } else {
            for (Iterator items = attrNames.iterator(); items.hasNext();) {
                Object key = items.next();
                Object value = map.get(key);
                if (value != null) {
                    answer.put(key, value);
                }
            }
        }
        return (answer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public Map getAttributes(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: get all attributes called: "
                    + type.getName() + "::" + name);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }
        File file = constructFile(directory, type, name);
        return (decodeAttributes(readFile(file), encryptAttributes));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set)
     */
    public Map getBinaryAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        Map stringAttributes = getAttributes(token, type, name, attrNames);
        Map binaryAttributes = new HashMap();
        for (Iterator items = stringAttributes.keySet().iterator(); items
                .hasNext();) {
            String attrName = (String) items.next();
            Set values = (Set) stringAttributes.get(attrName);
            byte[][] binValues = new byte[values.size()][];
            int i = 0;
            try {
                for (Iterator it = values.iterator(); it.hasNext(); i++) {
                    binValues[i] = ((String) it.next()).getBytes("UTF-8");
                }
            } catch (UnsupportedEncodingException e) {
                // Ignore the exception
            }
            binaryAttributes.put(attrName, binValues);
        }
        return (binaryAttributes);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#setBinaryAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Map, boolean)
     */
    public void setBinaryAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) throws IdRepoException, SSOException 
    {
        if (attributes == null) {
            return;
        }
        // Convert byte[][] attributes values to Set and save it
        Map attrs = new HashMap();
        for (Iterator items = attributes.keySet().iterator(); items.hasNext();) 
        {
            String attrName = (String) items.next();
            byte[][] attrBytes = (byte[][]) attributes.get(attrName);
            Set attrValues = new HashSet();
            int size = attrBytes.length;
            try {
                for (int i = 0; i < size; i++) {
                    attrValues.add(new String(attrBytes[i], "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                // Ignore the exception
            }
            attrs.put(attrName, attrValues);
        }
        setAttributes(token, type, name, attrs, isAdd);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getMembers(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String,
     *      com.sun.identity.idm.IdType)
     */
    public Set getMembers(SSOToken token, IdType type, String name,
            IdType membersType) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: getMembers called" + type + ": " + name
                    + ": " + membersType);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        // Memers can be returned for roles and groups
        if (!type.equals(IdType.ROLE) && !type.equals(IdType.GROUP)) {
            if (debug.messageEnabled()) {
                debug.message("FilesRepo:getMembers supported for "
                        + "roles and groups");
            }
            throw new IdRepoException(IdRepoBundle.getString("203"), "203");
        }

        // Set to maintain the members
        Set results = new HashSet();

        // Process group members
        if (type.equals(IdType.GROUP)) {
            // Read the group files and return the membership attribute
            File file = constructFile(directory, type, name);
            Map attrs = decodeAttributes(readFile(file), encryptAttributes);
            Set members = (Set) attrs.get(groupMembersAttribute);
            // Iterate through members and add to results only if "membersType"
            // matches
            if (members != null && !members.isEmpty()) {
                String mtype = membersType.getName();
                int mtypeLen = mtype.length();
                for (Iterator items = members.iterator(); items.hasNext();) {
                    String sname = (String) items.next();
                    if (sname.startsWith(mtype)) {
                        results.add(sname.substring(mtypeLen));
                    }
                }
            }
        } else if (type.equals(IdType.ROLE)) {
            // Get the list of all "membersType" and check if they belong
            // to the group
            Set returnAttrs = new HashSet();
            returnAttrs.add(roleMembershipAttribute);
            RepoSearchResults allUsers = search(token, membersType, "*", 0, 0,
                    returnAttrs, false, IdRepo.OR_MOD, null, false);
            Map userAttributes = null;
            if (allUsers != null
                    && (userAttributes = allUsers.getResultAttributes()) != 
                        null) {
                for (Iterator items = userAttributes.keySet().iterator(); items
                        .hasNext();) {
                    String sname = (String) items.next();
                    Map attrs = (Map) userAttributes.get(sname);
                    // Check if user belongs to the role
                    Set roles = (Set) attrs.get(roleMembershipAttribute);
                    if (roles != null && roles.contains(name)) {
                        results.add(sname);
                    }
                }
            }
        } else {
            // throw unsupported operation exception
            Object args[] = { NAME, IdOperation.READ.getName(), type.getName() 
                    };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return (results);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getMemberships(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, com.sun.identity.idm.IdType)
     */
    public Set getMemberships(SSOToken token, IdType type, String name,
            IdType membershipType) throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: getMemberships called" + type + ": "
                    + name + ": " + membershipType);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        // Memerships can be returned for users and agents
        if (!type.equals(IdType.USER) && !type.equals(IdType.AGENT)) {
            if (debug.messageEnabled()) {
                debug.message("FilesRepo:getMemberships supported for "
                        + "users and agents");
            }
            Object args[] = { NAME };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "206", args));
        }

        // Set to maintain the members
        Set results = new HashSet();
        if (membershipType.equals(IdType.ROLE)) {
            // Get the role attribute and return
            Set returnAttrs = new HashSet();
            returnAttrs.add(roleMembershipAttribute);
            Map attrs = getAttributes(token, type, name, returnAttrs);
            if (attrs != null) {
                Set roles = (Set) attrs.get(roleMembershipAttribute);
                if (roles != null) {
                    results = roles;
                }
            }
        } else if (membershipType.equals(IdType.GROUP)) {
            // Get the list of groups and search for memberships
            Set returnAttrs = new HashSet();
            returnAttrs.add(groupMembersAttribute);
            RepoSearchResults allGroups = search(token, membershipType, "*", 0,
                    0, returnAttrs, false, IdRepo.OR_MOD, null, false);
            Map groupAttrs = null;
            if (allGroups != null
                    && (groupAttrs = allGroups.getResultAttributes()) != null) {
                // Prefix name with IdType
                name = type.getName() + name;
                for (Iterator items = groupAttrs.keySet().iterator(); items
                        .hasNext();) {
                    String sname = (String) items.next();
                    Map attrs = (Map) groupAttrs.get(sname);
                    Set ids = (Set) attrs.get(groupMembersAttribute);
                    if (ids != null && ids.contains(name)) {
                        results.add(sname);
                    }
                }
            }
        } else {
            // throw unsupported operation exception
            Object args[] = { NAME, IdOperation.READ.getName(),
                    membershipType.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
        return (results);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getServiceAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.lang.String, java.util.Set)
     */
    public Map getServiceAttributes(SSOToken token, IdType type, String name,
            String serviceName, Set attrNames) throws IdRepoException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: get serviceAttributes called: " + name
                    + "\n\t" + serviceName + "=" + attrNames);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        if (!type.equals(IdType.USER) && !type.equals(IdType.ROLE)
                && !type.equals(IdType.REALM)) {
            // Unsupported Operation
            Object args[] = { NAME, IdOperation.SERVICE.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }

        // Get attributes from Identity Object
        Map results = getAttributes(token, type, name, attrNames);
        if (results == null) {
            results = new HashMap();
        }

        // For types role and realm, return the attributes
        if (!type.equals(IdType.USER)) {
            return (results);
        }

        // Get the roles for the identity and add the service attributes
        Set roles = getMemberships(token, type, name, IdType.ROLE);
        for (Iterator items = roles.iterator(); items.hasNext();) {
            String role = (String) items.next();
            Map roleAttrs = getAttributes(token, IdType.ROLE, role, attrNames);
            // Add the attributes to results
            for (Iterator ris = roleAttrs.keySet().iterator(); ris.hasNext();) {
                Object roleAttrName = ris.next();
                Set roleAttrValues = (Set) roleAttrs.get(roleAttrName);
                Set idAttrValues = (Set) results.get(roleAttrName);
                if (idAttrValues == null) {
                    results.put(roleAttrName, roleAttrValues);
                } else {
                    // Need to combine the values
                    idAttrValues.addAll(roleAttrValues);
                }
            }
        }

        // Get the service attributes for the realm and add it
        Map realmAttrs = getAttributes(token, IdType.REALM,
                "ContainerDefaultTemplateRole", attrNames);
        // Add the attributes to results
        for (Iterator ris = realmAttrs.keySet().iterator(); ris.hasNext();) {
            Object realmAttrName = ris.next();
            Set realmAttrValues = (Set) realmAttrs.get(realmAttrName);
            Set idAttrValues = (Set) results.get(realmAttrName);
            if (idAttrValues == null) {
                results.put(realmAttrName, realmAttrValues);
            } else {
                // Need to combine the values
                idAttrValues.addAll(realmAttrValues);
            }
        }
        return (results);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#isExists(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isExists(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }
        Map map = getAttributes(token, type, name);
        return (!map.isEmpty());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyMemberShip(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set, com.sun.identity.idm.IdType, int)
     */
    public void modifyMemberShip(SSOToken token, IdType type, String name,
            Set members, IdType membersType, int operation)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: modifyMemberShip called " + type
                    + "; name= " + name + "; members= " + members
                    + "; membersType= " + membersType + "; operation= "
                    + operation);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }
        if (members == null || members.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("FilesRepo.modifyMemberShip: "
                        + "Members set is empty");
            }
            throw new IdRepoException(IdRepoBundle.getString("201"), "201");
        }
        if (type.equals(IdType.USER) || type.equals(IdType.AGENT)) {
            if (debug.messageEnabled()) {
                debug.message("FilesRepo.modifyMembership: "
                        + "Membership to users and agents is not supported");
            }
            throw new IdRepoException(IdRepoBundle.getString("203"), "203");
        }
        if (!membersType.equals(IdType.USER)
                && !membersType.equals(IdType.AGENT)) {
            if (debug.messageEnabled()) {
                debug.message("FilesRepo.modifyMembership: A non user/agent " 
                        + "type cannot  be made a member of any identity"
                        + membersType.getName());
            }
            Object[] args = { NAME };
            throw new IdRepoException(IdRepoBundle.getString("206", args),
                    "206", args);
        }
        if (type.equals(IdType.GROUP)) {
            // add the identities to the user's group membership
            File file = constructFile(directory, type, name);
            Map attrs = decodeAttributes(readFile(file), encryptAttributes);
            Set gmembers = (Set) attrs.get(groupMembersAttribute);
            if (gmembers == null) {
                gmembers = new HashSet();
                attrs.put(groupMembersAttribute, gmembers);
            }
            for (Iterator items = members.iterator(); items.hasNext();) {
                String item = membersType.getName() + items.next().toString();
                if (operation == ADDMEMBER) {
                    gmembers.add(item);
                } else {
                    gmembers.remove(item);
                }
            }
            // Save the results
            setAttributes(token, type, name, attrs, false);
        } else if (type.equals(IdType.ROLE)) {
            // add to user/agents's role attribute
            Set returnAttrs = new HashSet();
            returnAttrs.add(roleMembershipAttribute);
            for (Iterator items = members.iterator(); items.hasNext();) {
                String sname = (String) items.next();
                // Get role attribute
                Map roles = getAttributes(token, membersType, sname,
                        returnAttrs);
                if (roles == null) {
                    roles = new HashMap();
                }
                Set sroles = (Set) roles.get(roleMembershipAttribute);
                if (sroles == null) {
                    sroles = new HashSet();
                    roles.put(roleMembershipAttribute, sroles);
                }
                if (operation == ADDMEMBER) {
                    sroles.add(name);
                } else {
                    sroles.remove(name);
                }
                // Save the attribute
                setAttributes(token, membersType, sname, roles, false);
            }
        } else {
            Object[] args = { NAME, type.getName() };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "209", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#modifyService(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.lang.String,
     *      com.sun.identity.sm.SchemaType, java.util.Map)
     */
    public void modifyService(SSOToken token, IdType type, String name,
            String serviceName, SchemaType sType, Map attrMap)
            throws IdRepoException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: Modify service called: " + name + "\n\t"
                    + serviceName + "=" + attrMap + "\n\tSchema=" + sType);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        if (type.equals(IdType.USER) || type.equals(IdType.ROLE)
                || type.equals(IdType.REALM)) {
            // Set the attributes
            assignService(token, type, name, serviceName, sType, attrMap);
        } else {
            Object args[] = { NAME, IdOperation.SERVICE.getName() };
            throw new IdRepoUnsupportedOpException(IdRepoBundle.BUNDLE_NAME,
                    "305", args);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#removeAttributes(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.util.Set)
     */
    public void removeAttributes(SSOToken token, IdType type, String name,
            Set attrNames) throws IdRepoException, SSOException {
        Map answer = getAttributes(token, type, name);
        for (Iterator items = attrNames.iterator(); items.hasNext();) {
            answer.remove(items.next());
        }
        File file = constructFile(directory, type, name);
        writeFile(file, processAttributes(answer, Collections.EMPTY_SET,
                encryptAttributes));
        // %%% Send notification (must be via a different thread)
        if (repoListener != null) {
            repoListener.objectChanged(name, AMEvent.OBJECT_CHANGED,
                    repoListener.getConfigMap());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#removeListener()
     */
    public void removeListener() {
        repoListener = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, int, int,
     *      java.util.Set, boolean, int, java.util.Map, boolean)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, int maxTime, int maxResults, Set returnAttrs,
            boolean returnAllAttrs, int filterOp, Map avPairs, 
            boolean recursive) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        // Directory to start the search
        File dir = new File(new File(directory), type.getName());
        String[] files = dir.list(new FileRepoFileFilter(pattern));
        if (files.length == 0) {
            return new RepoSearchResults(Collections.EMPTY_SET,
                    RepoSearchResults.SUCCESS, Collections.EMPTY_MAP, type);
        }

        // Check if attribute mapping has to be done
        Set results = new HashSet();
        if (avPairs != null && !avPairs.isEmpty()) {
            for (int i = 0; i < files.length; i++) {
                // Check if the attributes match
                Map allAttrs = getAttributes(token, type, files[i]);
                Set attrNames = new CaseInsensitiveHashSet();
                attrNames.addAll(allAttrs.keySet());
                boolean addResult = (filterOp == IdRepo.AND_MOD);
                for (Iterator items = avPairs.keySet().iterator(); items
                        .hasNext();) {
                    String attrName = (String) items.next();
                    Set attrValue = (Set) avPairs.get(attrName);
                    if (attrValue == null || attrValue.isEmpty()) {
                        // Check if the attribute is present
                        if (attrNames.contains(attrName)) {
                            if (filterOp == IdRepo.OR_MOD) {
                                addResult = true;
                                break;
                            }
                        } else if (filterOp == IdRepo.AND_MOD) {
                            addResult = false;
                            break;
                        }
                    } else {
                        // Check if the values are present
                        Set matchValues = (Set) allAttrs.get(attrName);
                        if (matchValues != null
                                && matchValues.contains(attrValue)) {
                            if (filterOp == IdRepo.OR_MOD) {
                                addResult = true;
                                break;
                            }
                        } else if (filterOp == IdRepo.AND_MOD) {
                            addResult = false;
                            break;
                        }
                    }
                }
                if (addResult) {
                    results.add(files[i]);
                }
            }
        } else {
            for (int i = 0; i < files.length; i++) {
                results.add(files[i]);
            }
        }

        // Build RepoSearchResults
        Map resultsWithAttrs = new HashMap();
        for (Iterator items = results.iterator(); items.hasNext();) {
            String item = (String) items.next();
            if (returnAllAttrs || returnAttrs == null) {
                resultsWithAttrs.put(item, getAttributes(token, type, item));
            } else if (returnAttrs.isEmpty()) {
                resultsWithAttrs.put(item, Collections.EMPTY_MAP);
            } else {
                resultsWithAttrs.put(item, getAttributes(token, type, item,
                        returnAttrs));
            }
        }
        return (new RepoSearchResults(results, RepoSearchResults.SUCCESS,
                resultsWithAttrs, type));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#search(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map,
     *      boolean, int, int, java.util.Set)
     */
    public RepoSearchResults search(SSOToken token, IdType type,
            String pattern, Map avPairs, boolean recursive, int maxResults,
            int maxTime, Set returnAttrs) throws IdRepoException, SSOException {
        return (search(token, type, pattern, maxTime, maxResults, returnAttrs,
                (returnAttrs == null), OR_MOD, avPairs, recursive));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#setAttributes(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String, java.util.Map,
     *      boolean)
     */
    public void setAttributes(SSOToken token, IdType type, String name,
            Map attributes, boolean isAdd) throws IdRepoException, SSOException 
    {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo:setAttributs for " + type + " " + name
                    + "\n\tAttributes=" + attributes.keySet() + " isAdd="
                    + isAdd);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        Map answer = getAttributes(token, type, name);
        if (answer == Collections.EMPTY_MAP) {
            answer = new HashMap();
        }

        // Hash the new attributes, if any
        attributes = processAttributes(attributes, hashAttributes,
                Collections.EMPTY_SET);
        if (!isAdd) {
            answer.putAll(attributes);
        } else {
            for (Iterator items = attributes.keySet().iterator(); items
                    .hasNext();) {
                Object key = items.next();
                Set value = (Set) answer.get(key);
                if (value != null) {
                    value.addAll((Set) attributes.get(key));
                } else {
                    value = (Set) attributes.get(key);
                }
                answer.put(key, value);
            }
        }
        File file = constructFile(directory, type, name);
        writeFile(file, processAttributes(answer, Collections.EMPTY_SET,
                encryptAttributes));
        // %%% Send notification (must be via a different thread)
        if (repoListener != null) {
            repoListener.objectChanged(name, AMEvent.OBJECT_CHANGED,
                    repoListener.getConfigMap());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#unassignService(
     *      com.iplanet.sso.SSOToken, com.sun.identity.idm.IdType,
     *      java.lang.String, java.lang.String, java.util.Map)
     */
    public void unassignService(SSOToken token, IdType type, String name,
            String serviceName, Map attrMap) throws IdRepoException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: Uasssign service called: " + name
                    + "\n\t" + serviceName + "=" + attrMap);
        }
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        // Need to selectively remove the objectclassess
        // First get the objectclasses
        Set set = new HashSet();
        set.add(OC);
        Map attrs = getAttributes(token, type, name, set);
        Set objectclasses = (Set) attrs.get(OC);
        CaseInsensitiveHashMap sAttrs = new CaseInsensitiveHashMap();
        sAttrs.putAll(attrMap);
        Set serviceOCs = (Set) sAttrs.remove(OC);
        if (objectclasses != null && !objectclasses.isEmpty()
                && serviceOCs != null) {
            objectclasses.removeAll(serviceOCs);
            setAttributes(token, type, name, attrs, false);
        }
        removeAttributes(token, type, name, sAttrs.keySet());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedOperations(
     *      com.sun.identity.idm.IdType)
     */
    public Set getSupportedOperations(IdType type) {
        return (Set) supportedOps.get(type);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#getSupportedTypes()
     */
    public Set getSupportedTypes() {
        return supportedOps.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.identity.idm.IdRepo#isActive(com.iplanet.sso.SSOToken,
     *      com.sun.identity.idm.IdType, java.lang.String)
     */
    public boolean isActive(SSOToken token, IdType type, String name)
            throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }
        Map attributes = getAttributes(token, type, name);
        if (attributes == null) {
            Object[] args = { NAME, name };
            throw new IdRepoException(IdRepoBundle.BUNDLE_NAME, "202", args);
        }
        Set activeVals = (Set) attributes.get(statusAttribute);
        if (activeVals == null || activeVals.isEmpty()) {
            return true;
        } else {
            Iterator it = activeVals.iterator();
            String active = (String) it.next();
            return (active.equalsIgnoreCase("active") ? true : false);
        }
    }

    private static void loadSupportedOps() {
        Set opSet = new HashSet();
        opSet.add(IdOperation.EDIT);
        opSet.add(IdOperation.READ);
        opSet.add(IdOperation.CREATE);
        opSet.add(IdOperation.DELETE);
        supportedOps.put(IdType.GROUP, Collections.unmodifiableSet(opSet));
        supportedOps.put(IdType.AGENT, Collections.unmodifiableSet(opSet));
        Set nopSet = new HashSet(opSet);
        nopSet.add(IdOperation.SERVICE);
        supportedOps.put(IdType.USER, Collections.unmodifiableSet(nopSet));
        supportedOps.put(IdType.ROLE, Collections.unmodifiableSet(nopSet));
        supportedOps.put(IdType.REALM, Collections.unmodifiableSet(nopSet));
        if (debug.messageEnabled()) {
            debug.message("FilesRepo: loadSupportedOps  called"
                    + "\n\tsupportedOps=" + supportedOps.toString());
        }
    }

    public String getFullyQualifiedName(SSOToken token, IdType type, 
            String name) throws IdRepoException, SSOException {
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }
        RepoSearchResults results = search(token, type, name, null, true, 0, 0,
                null);
        Set dns = results.getSearchResults();
        if (dns != null || dns.size() != 1) {
            String[] args = { NAME, name };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "220", args));
        }
        return ("files://FilesRepo/" + type.getName() + "/" + dns.iterator()
                .next().toString());
    }

    public boolean supportsAuthentication() {
        return (true);
    }

    public boolean authenticate(Callback[] credentials) throws IdRepoException,
            AuthLoginException {
        debug.message("FilesRepo:authenticate called");
        if (initializationException != null) {
            debug.error("FilesRepo: throwing initialization exception");
            throw (initializationException);
        }

        // Obtain user name and password from credentials and authenticate
        String username = null;
        String password = null;
        for (int i = 0; i < credentials.length; i++) {
            if (credentials[i] instanceof NameCallback) {
                username = ((NameCallback) credentials[i]).getName();
                if (debug.messageEnabled()) {
                    debug.message("FilesRepo:authenticate username: "
                            + username);
                }
            } else if (credentials[i] instanceof PasswordCallback) {
                char[] passwd = ((PasswordCallback) credentials[i])
                        .getPassword();
                if (passwd != null) {
                    password = new String(passwd);
                    debug.message("FilesRepo:authN passwd present");
                }
            }
        }
        if (username == null || password == null) {
            return (false);
        }

        // Get user's password attribute
        Map attrs = null;
        try {
            attrs = getAttributes(null, IdType.USER, username);
            if (attrs.isEmpty()) {
                // Try agent
                if (debug.messageEnabled()) {
                    debug.message("FilesRepo:authn user not found. "
                            + "Searching for agent");
                }
                attrs = getAttributes(null, IdType.AGENT, username);
            } else if (debug.messageEnabled()) {
                debug.message("FilesRepo:authenticate found user entry");
            }
        } catch (SSOException ssoe) {
            // Since SSOToken are not sent, this should not happen
        }
        if (attrs == null || attrs.isEmpty()
                || !attrs.containsKey(passwordAttribute)) {
            // Cound not find user or agent, return false
            if (debug.messageEnabled()) {
                debug.message("FilesRepo:authenticate not found user/agent");
            }
            return (false);
        }
        Set storedPasswords = (Set) attrs.get(passwordAttribute);
        if (storedPasswords == null || storedPasswords.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("FilesRepo:authenticate no stored password");
            }
            return (false);
        }
        String storedPassword = (String) storedPasswords.iterator().next();
        if (hashAttributes.contains(passwordAttribute)) {
            password = Hash.hash(password);
        }
        if (debug.messageEnabled()) {
            debug.message("FilesRepo:authenticate AuthN of " + username + "="
                    + password.equals(storedPassword));
        }
        return (password.equals(storedPassword));
    }

    // -----------------------------------------------
    // static methods to manage directory structure
    // -----------------------------------------------
    // Methods for cache management

    // Initialize, read and write methods
    static void initDir(String rootDir) throws IdRepoException {
        // Check if roor dir exists, if not create
        File root = new File(rootDir);
        if (!root.exists() && !root.mkdirs()) {
            // Unable to create the directory
            Object args[] = { root.getAbsolutePath() };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "309", args));
        } else if (!root.isDirectory()) {
            // Not a directory
            Object args[] = { root.getAbsolutePath() };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "308", args));
        }
        // Check sub-directories
        Set types = supportedOps.keySet();
        for (Iterator items = types.iterator(); items.hasNext();) {
            String subDir = ((IdType) items.next()).getName();
            File dir = new File(root, subDir);
            if (!dir.exists() && !dir.mkdir()) {
                // Unable to create the directory
                String args[] = { dir.getAbsolutePath() };
                throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "309",
                        args));
            } else if (!dir.isDirectory()) {
                // Not a directory
                String args[] = { dir.getAbsolutePath() };
                throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "308",
                        args));
            }
            if (subDir.equals(IdType.REALM.getName())) {
                // Create realm ContainerDefaultTemplateRole
                File role = new File(dir, "ContainerDefaultTemplateRole");
                if (!role.exists()) {
                    // Write an empyt map to the file
                    writeFile(role, Collections.EMPTY_MAP);
                }
            }
        }

    }

    static File constructFile(String rootDir, IdType type, String name) {
        // Construct file name
        File root = new File(rootDir);
        File subDir = new File(root, type.getName());
        return (new File(subDir, name));
    }

    static void writeFile(File file, Map values) throws IdRepoException {
        writeFile(file.getAbsolutePath(), values);
    }

    static void writeFile(String fileName, Map values) throws IdRepoException {
        // Convert Map to AttributeValuePairs and write to file
        PrintWriter pw = null;
        try {
            SOAPClient client = new SOAPClient();
            String encodedMap = client.encodeMap("result", values);
            pw = new PrintWriter(new FileOutputStream(fileName));
            pw.println(encodedMap);
            // update the cache
            identityCache.put(fileName.toLowerCase(), values);
        } catch (IOException ioe) {
            if (debug.messageEnabled()) {
                debug
                        .message("FilesRepo: error writing file: " + fileName,
                                ioe);
            }
            String[] args = { NAME, fileName };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "220", args));
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }

    static Map readFile(File file) throws IdRepoException {
        Map answer = readFile(file.getAbsolutePath());
        if (answer == null) {
            answer = Collections.EMPTY_MAP;
        }
        return (answer);
    }

    static Map readFile(String fileName) throws IdRepoException {
        Map answer = null;
        // Check in cache
        String cacheName = fileName.toLowerCase();
        if ((answer = (Map) identityCache.get(cacheName)) != null) {
            return (answer);
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            StringBuffer encodedMapBuffer = new StringBuffer(200);
            String line;
            while ((line = br.readLine()) != null) {
                encodedMapBuffer.append(line);
            }
            String encodedMap = encodedMapBuffer.toString();
            SOAPClient client = new SOAPClient();
            Map map = client.decodeMap(encodedMap);
            // Convert HashMap to CaseInsensitiveHashMap
            answer = new CaseInsensitiveHashMap();
            for (Iterator items = map.keySet().iterator(); items.hasNext();) {
                Object key = items.next();
                Set ovalue = (Set) map.get(key);
                Set nvalue = new CaseInsensitiveHashSet();
                nvalue.addAll(ovalue);
                answer.put(key, nvalue);
            }
            // Add to cache
            identityCache.put(cacheName, answer);
        } catch (IOException ioe) {
            if (debug.messageEnabled()) {
                debug
                        .message("FilesRepo: error reading file: " + fileName,
                                ioe);
            }
            String[] args = { NAME, fileName };
            throw (new IdRepoException(IdRepoBundle.BUNDLE_NAME, "220", args));
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    debug.error("FilesRepo: read error: " + fileName, e);
                }
            }
        }
        return (answer);
    }

    static void clearCache() {
        identityCache = new HashMap();
    }

    static Map processAttributes(Map attrs, Set hashAttrs, Set encAttrs) {
        // Convert to CaseInsensitiveHashMap
        Map answer = new CaseInsensitiveHashMap();
        for (Iterator items = attrs.keySet().iterator(); items.hasNext();) {
            Object key = items.next();
            Set ovalue = (Set) attrs.get(key);
            Set nvalue = new CaseInsensitiveHashSet();
            if (hashAttrs.contains(key)) {
                for (Iterator i = ovalue.iterator(); i.hasNext();) {
                    nvalue.add(Hash.hash((String) i.next()));
                }
            } else if (encAttrs.contains(key)) {
                try {
                    for (Iterator i = ovalue.iterator(); i.hasNext();) {
                        nvalue.add((String) AccessController
                                .doPrivileged(new EncodeAction((String) i
                                        .next())));
                    }
                } catch (Throwable e) {
                    // Printing the attribute value could be security issue
                    debug.error("FilesRepo: unable to encode", e);
                }
            } else {
                nvalue.addAll(ovalue);
            }
            answer.put(key, nvalue);
        }
        return (answer);
    }

    static Map decodeAttributes(Map attrs, Set encAttrs) {
        if (encAttrs.isEmpty()) {
            return (attrs);
        }
        // Decode the attributes
        for (Iterator items = encAttrs.iterator(); items.hasNext();) {
            Object key = items.next();
            Set ovalue = (Set) attrs.get(key);
            if (ovalue != null && !ovalue.isEmpty()) {
                Set nvalue = new CaseInsensitiveHashSet();
                for (Iterator i = ovalue.iterator(); i.hasNext();) {
                    try {
                        nvalue.add((String) AccessController
                                .doPrivileged(new DecodeAction((String) i
                                        .next())));
                    } catch (Throwable e) {
                        // Printing the attribute value could be security issue
                        debug.error("FilesRepo: unable to decode", e);
                    }
                }
                attrs.put(key, nvalue);
            }
        }
        return (attrs);
    }

    // File name filter inner class
    class FileRepoFileFilter implements FilenameFilter {
        // Pattern to match
        Pattern pattern;

        // Default constructor
        FileRepoFileFilter(String p) {
            if (p != null && p.length() != 0 && !p.equals("*")) {
                // Replace "*" with ".*"
                int idx = p.indexOf('*');
                while (idx != -1) {
                    p = p.substring(0, idx) + ".*" + p.substring(idx + 1);
                    idx = p.indexOf('*', idx + 2);
                }
                pattern = Pattern.compile(p.toLowerCase());
            }
        }

        public boolean accept(File dir, String name) {
            if (pattern == null) {
                return (true);
            } else {
                return (pattern.matcher(name.toLowerCase()).matches());
            }
        }
    }
}
