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
 * $Id: FederationManagerCLI.java,v 1.14 2008-07-30 22:28:18 srivenigan Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common.cli;

import com.sun.identity.qatest.cli.CLIConstants;
import com.sun.identity.qatest.cli.CommandMessages;
import com.sun.identity.qatest.cli.FederationManagerCLIConstants;
import com.sun.identity.qatest.cli.GlobalConstants;
import com.sun.identity.qatest.common.TestCommon;
import com.sun.identity.qatest.common.TestConstants;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Level;

/**
 * <code>AccessMangerCLI</code> is a utility class which allows the user
 * to invoke the famadm CLI to perform operations which correspond to supported
 * sub-commands of famadm (e.g. create-realm, delete-realm, list-realms,
 * create-identity, delete-identity, list-identities, etc.).
 */
public class FederationManagerCLI extends CLIUtility 
        implements CLIConstants, FederationManagerCLIConstants, 
        GlobalConstants, CLIExitCodes {
    private boolean useDebugOption;
    private boolean useVerboseOption;
    private boolean useLongOptions;
    private long commandTimeout;
    private static int SUBCOMMAND_VALUE_INDEX = 1;
    private static int ADMIN_ID_ARG_INDEX = 2;
    private static int ADMIN_ID_VALUE_INDEX = 3;
    private static int PASSWORD_ARG_INDEX = 4;
    private static int PASSWORD_VALUE_INDEX = 5;
    private static int DEFAULT_COMMAND_TIMEOUT = 20;
    
    /**
     * Create a new instance of <code>FederationManagerCLI</code>
     * @param useDebug - a flag indicating whether to add the debug option
     * @param useVerbose - a flag indicating whether to add the verbose option
     * @param useLongOpts - a flag indicating whether long options 
     * (e.g. --realm) should be used
     */
    public FederationManagerCLI(boolean useDebug, boolean useVerbose, 
            boolean useLongOpts)
    throws Exception {
        super(new StringBuffer(cliPath).
                append(System.getProperty("file.separator")).append(uri).
                append(System.getProperty("file.separator")).append("bin").
                append(System.getProperty("file.separator")).append("famadm").
                toString());    
        useLongOptions = useLongOpts;
        try {
            addAdminUserArgs();
            addPasswordArgs();
            useDebugOption = useDebug;
            useVerboseOption = useVerbose;
            commandTimeout = (new Long(timeout).longValue()) * 1000;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;         
        }        
    }
    
    /**
     * Get the absolute path to the famadm CLI.
     *
     * @return - the absolute path to famadm.
     */
    public String getCliPath() {
        return(new StringBuffer(cliPath).
                append(System.getProperty("file.separator")).append(uri).
                append(System.getProperty("file.separator")).append("bin").
                append(System.getProperty("file.separator")).append("famadm").
                toString());
    }
     
    /**
     * Sets the "--adminid" and admin user ID arguments in the argument list.
     */
    private void addAdminUserArgs() {
        String adminArg;
        if (useLongOptions) {
            adminArg = PREFIX_ARGUMENT_LONG + ARGUMENT_ADMIN_ID;
        } else {
            adminArg = PREFIX_ARGUMENT_SHORT + SHORT_ARGUMENT_ADMIN_ID;
        }
        setArgument(ADMIN_ID_ARG_INDEX, adminArg);
        setArgument(ADMIN_ID_VALUE_INDEX, adminUser);
    }
    
    /**
     * Sets "--passwordfile" and the password file path in the argument list.
     */
    private void addPasswordArgs() {
        String passwordArg;
        
        if (useLongOptions) {
            passwordArg = PREFIX_ARGUMENT_LONG + ARGUMENT_PASSWORD_FILE;
        } else {
            passwordArg = PREFIX_ARGUMENT_SHORT + SHORT_ARGUMENT_PASSWORD_FILE;
        }
        
        setArgument(PASSWORD_ARG_INDEX, passwordArg);
        setArgument(PASSWORD_VALUE_INDEX, passwdFile);
    }
    
    /**
     * Adds the "--debug" argument to the argument list.
     */
    private void addDebugArg() {
        String debugArg;
        if (useLongOptions) {
            debugArg = PREFIX_ARGUMENT_LONG + DEBUG_ARGUMENT;
        } else {
            debugArg = PREFIX_ARGUMENT_SHORT + SHORT_DEBUG_ARGUMENT;
        }
        addArgument(debugArg);
    }
    
    /**
     * Adds the "--verbose" argument to the arugment list.
     */
    private void addVerboseArg() {
        String verboseArg;
        if (useLongOptions) {
            verboseArg = PREFIX_ARGUMENT_LONG + VERBOSE_ARGUMENT;
        } else {
            verboseArg = PREFIX_ARGUMENT_SHORT + SHORT_VERBOSE_ARGUMENT;
        }
        addArgument(verboseArg);
    }
    
    /**
     * Adds the "--locale" arugment and the locale value to the argument list.
     */
    private void addLocaleArgs() {
        String localeArg;
        if (useLongOptions) {
            localeArg = PREFIX_ARGUMENT_LONG + LOCALE_ARGUMENT;
        } else {
            localeArg = PREFIX_ARGUMENT_SHORT + SHORT_LOCALE_ARGUMENT;
        }
        addArgument(localeArg);
        addArgument(localeValue);
    }
    
    /**
     * Adds the global arguments (--debug, --verbose, --locale) to the argument
     * list if they are specified.
     */
    private void addGlobalOptions() {
        if (useDebugOption) {
            addDebugArg();
        }
        if (useVerboseOption) {
            addVerboseArg();
        }
        if (localeValue != null) {
            addLocaleArgs();
        }
    }
    
    /**
     * Adds the "--realm" argument and realm value to the argument list
     * @param realm - the realm value to add to the argument list
     */
    private void addRealmArguments(String realm) {
        String realmArg;
        if (useLongOptions) {
            realmArg = PREFIX_ARGUMENT_LONG + REALM_ARGUMENT;
        } else {
            realmArg = PREFIX_ARGUMENT_SHORT + SHORT_REALM_ARGUMENT;
        }
        addArgument(realmArg);
        addArgument(realm);
    }
    
    /**
     * Adds the "--recursive" argument to the argument list.
     */
    private void addRecursiveArgument() {
        String recursiveArg;
        if (useLongOptions) {
            recursiveArg = PREFIX_ARGUMENT_LONG + RECURSIVE_ARGUMENT;
        } else {
            recursiveArg = PREFIX_ARGUMENT_SHORT + SHORT_RECURSIVE_ARGUMENT;
        }
        addArgument(recursiveArg);
    }
    
    /**
     * Adds the "--filter" argument to the argument list.
     */
    private void addFilterArguments(String filter) {
        String filterArg;
        if (useLongOptions) {
            filterArg = PREFIX_ARGUMENT_LONG + FILTER_ARGUMENT;
        } else {
            filterArg = PREFIX_ARGUMENT_SHORT + SHORT_FILTER_ARGUMENT;
        }
        addArgument(filterArg);
        addArgument(filter);
    }
    
    /**
     * Adds the "--idname" and identity name arguments to the argument list.
     */
    private void addIdnameArguments(String name) {
        String idnameArg;
        if (useLongOptions) {
            idnameArg = PREFIX_ARGUMENT_LONG + ID_NAME_ARGUMENT;
        } else {
            idnameArg = PREFIX_ARGUMENT_SHORT + SHORT_ID_NAME_ARGUMENT;
        }
        addArgument(idnameArg);
        addArgument(name);
    }
    
    /**
     * Adds the "--idtype" and identity type arguments to the argument list.
     */
    private void addIdtypeArguments(String type) {
        String idtypeArg;
        if (useLongOptions) {
            idtypeArg = PREFIX_ARGUMENT_LONG + ID_TYPE_ARGUMENT;
        } else {
            idtypeArg = PREFIX_ARGUMENT_SHORT + SHORT_ID_TYPE_ARGUMENT;
        }
        addArgument(idtypeArg);
        addArgument(type);
    }
    
    /**
     * Adds the "--memberidname" and member identity name arguments to the 
     * argument list.
     */
    private void addMemberIdnameArguments(String name) {
        String idnameArg;
        if (useLongOptions) {
            idnameArg = PREFIX_ARGUMENT_LONG + MEMBER_ID_NAME_ARGUMENT;
        } else {
            idnameArg = PREFIX_ARGUMENT_SHORT + SHORT_MEMBER_ID_NAME_ARGUMENT;
        }
        addArgument(idnameArg);
        addArgument(name);        
    }
    
    /**
     * Adds the "--memberidtype" and member identity type arguments to the 
     * argument list.
     */
    private void addMemberIdtypeArguments(String type) {
        String idtypeArg;
        if (useLongOptions) {
            idtypeArg = PREFIX_ARGUMENT_LONG + MEMBER_ID_TYPE_ARGUMENT;
        } else {
            idtypeArg = PREFIX_ARGUMENT_SHORT + SHORT_MEMBER_ID_TYPE_ARGUMENT;
        }
        addArgument(idtypeArg);
        addArgument(type);        
    }
    
    /**
     * Adds the "--membershipidtype" and membership identity type arguments to 
     * the argument list.
     *
     * @param type - the identity type for the member
     */
    private void addMembershipIdtypeArguments(String type) {
        String idtypeArg;
        if (useLongOptions) {
            idtypeArg = PREFIX_ARGUMENT_LONG + MEMBERSHIP_ID_TYPE_ARGUMENT;
        } else {
            idtypeArg = PREFIX_ARGUMENT_SHORT + 
                    SHORT_MEMBERSHIP_ID_TYPE_ARGUMENT;
        }
        addArgument(idtypeArg);
        addArgument(type);        
    }  
    
    /**
     * Adds the "--servicename" and the servicename arguments to the argument 
     * list.
     *
     * @param name - the name of the service
     */
    private void addServiceNameArguments(String name) {
        String serviceNameArg;
        if (useLongOptions) {
            serviceNameArg = PREFIX_ARGUMENT_LONG + SERVICENAME_ARGUMENT;
        } else {
            serviceNameArg = PREFIX_ARGUMENT_SHORT + SHORT_SERVICENAME_ARGUMENT;
        }
        addArgument(serviceNameArg);
        addArgument(name);
    }
    
    /**  
     * Add the "--attributename" and the attribute name arguments to the 
     * argument list.
     *
     * @param name - the attribute name
     */
    private void addAttributeNameArguments(String name) {
        String attrNameArg;
        if (useLongOptions) {
            attrNameArg = PREFIX_ARGUMENT_LONG + ATTRIBUTE_NAME_ARGUMENT;
        } else {
            attrNameArg = PREFIX_ARGUMENT_SHORT + SHORT_ATTRIBUTE_NAMES_ARGUMENT;
        }
        addArgument(attrNameArg);
        addArgument(name);
    }
    
    /**
     * Add the "--authtype" and the authentication type arguments to the 
     * argument list.
     *
     * @param type - the authentication type
     */
    private void addAuthtypeArguments(String type) {
        String authTypeArg;
        if (useLongOptions) {
            authTypeArg = PREFIX_ARGUMENT_LONG + AUTHTYPE_ARGUMENT;
        } else {
            authTypeArg = PREFIX_ARGUMENT_SHORT + SHORT_AUTHTYPE_ARGUMENT;
        }
        addArgument(authTypeArg);
        addArgument(type);
    }
    
    
    /**  
     * Add the "--attributenames" and the attribute name arguments to the 
     * argument list.
     *
     * @param names - a semi-colon delimited list of attribute names
     */
    private void addAttributeNamesArguments(String names) {
        String attrNameArg;
        if (useLongOptions) {
            attrNameArg = PREFIX_ARGUMENT_LONG + ATTRIBUTE_NAMES_ARGUMENT;
        } else {
            attrNameArg = PREFIX_ARGUMENT_SHORT + SHORT_ATTRIBUTE_NAMES_ARGUMENT;
        }
        addArgument(attrNameArg);
        String[] nameList = names.split(";");
        for (int i=0; i < nameList.length; i++) {
            addArgument(nameList[i]);
        }
    }
 
    /**
     * Add the "--name" and the auth instance name arguments to the argument
     * list.
     *
     * @param name - the auth instance name
     */
    private void addNameArguments(String name) {
        String nameArg;
        if (useLongOptions) {
            nameArg = PREFIX_ARGUMENT_LONG + NAME_ARGUMENT;
        } else {
            nameArg = PREFIX_ARGUMENT_SHORT + SHORT_NAMES_ARGUMENT;
        }
        addArgument(nameArg);
        addArgument(name);
    }    
    
    /**
     * Add the "--names" and the auth instance name arguments to the argument
     * list.
     *
     * @param names - a semi-colon delimited list of auth instance names
     */
    private void addNamesArguments(String names) {
        String namesArg;
        if (useLongOptions) {
            namesArg = PREFIX_ARGUMENT_LONG + NAMES_ARGUMENT;
        } else {
            namesArg = PREFIX_ARGUMENT_SHORT + SHORT_NAMES_ARGUMENT;
        }
        addArgument(namesArg);
        String[] nameList = names.split(";");
        for (String name: nameList) {
            addArgument(name);
        }
    }
    
    /**
     * Add the "--append" argument to the argument list.
     */
    private void addAppendArgument() {
        String appendArg;
        if (useLongOptions) {
            appendArg = PREFIX_ARGUMENT_LONG + APPEND_ARGUMENT;
        } else {
            appendArg = PREFIX_ARGUMENT_SHORT + SHORT_APPEND_ARGUMENT;
        }
        addArgument(appendArg);           
    }
    
    /**
     * Add the  --revisionnumber and revision number arguments to the 
     * argument list.
     */
   private void addRevisionNumberArgument(String revisionNumber) {
       String revArg;
       if(useLongOptions) {
           revArg = PREFIX_ARGUMENT_LONG + REVISION_NO_ARGUMENT;
       } else {
           revArg = PREFIX_ARGUMENT_SHORT + SHORT_REVISION_NO_ARGUMENT;
       }
       addArgument(revArg);
       addArgument(revisionNumber);
   }
    
    /**
     * Create a new realm.
     * 
     * @param realmToCreate - the name of the realm to be created
     * @return the exit status of the "create-realm" command
     */
    public int createRealm(String realmToCreate) 
    throws Exception {
        setSubcommand(CREATE_REALM_SUBCOMMAND);
        addRealmArguments(realmToCreate);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Get revision number.
     * 
     * @param serviceName - the name of the service.
     */
    public int getRevisionNumber(String serviceName)
    throws Exception {
        setSubcommand(GET_REVISION_NUMBER_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Set revision number.
     * 
     * @param serviceName - the name of the service.
     */
    public int setRevisionNumber(String serviceName, String revisionNumber)
    throws Exception {
        setSubcommand(SET_REVISION_NUMBER_SUBCOMMAND);
        addServiceNameArguments(serviceName);
        addRevisionNumberArgument(revisionNumber);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Create multiple realms from a list of realms.
     *
     * @param realmList - a list of realms to create separated by semi-colons.
     * @return a boolean indicating whether all the realms are created 
     * successfully.
     */
    public boolean createRealms(String realmList)
    throws Exception {
        boolean allRealmsCreated = true;
        
        if (realmList != null) {
            if (realmList.length() > 0) {
                String [] realms = realmList.split(";");
                for (int i=0; i < realms.length; i++) {
                    log(Level.FINE, "createRealms", "Creating realm " + 
                            realms[i]);
                    int exitStatus = createRealm(realms[i]);
                    logCommand("createRealms");
                    resetArgList();
                    if (exitStatus != SUCCESS_STATUS) {
                        allRealmsCreated = false;
                        log(Level.SEVERE, "createRealms", "The realm " + 
                                realms[i] + " failed to be created.");
                    }
                }
            } else {
                allRealmsCreated = false;
                log(Level.SEVERE, "createRealms", 
                        "The list of realms is empty.");
            }
        } else {
            allRealmsCreated = false;
            log(Level.SEVERE, "createRealms", "The list of realms is null.");
        }
        return (allRealmsCreated);
    }
       
    /**
     * Delete a realm.
     *
     * @param realmToDelete - the name of the realm to be deleted
     * @param recursiveDelete - a flag indicating whether the realms beneath 
     * realmToDelete should be recursively deleted as well
     * @return the exit status of the "delete-realm" command
     */
    public int deleteRealm(String realmToDelete, boolean recursiveDelete) 
    throws Exception {
        setSubcommand(DELETE_REALM_SUBCOMMAND);
        addRealmArguments(realmToDelete);
        if (recursiveDelete) {
            addRecursiveArgument();
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout)); 
    }
    
    /**
     * Delete a realm without using recursion.
     * @param realmToDelete - the name of the realm to be deleted.
     * @return the exit status of the "delete-realm" command
     */
    public int deleteRealm(String realmToDelete) 
    throws Exception {
        return (deleteRealm(realmToDelete, false));
    }
    
    /**
     * List the realms which exist under a realm.
     *
     * @param startRealm - the realm from which to start the search
     * @param filter - a string containing a filter which will be used to 
     * restrict the realms that are returned (e.g. "*realms")
     * @param recursiveSearch - a boolean which should be set to "false" to 
     * perform a single level search or "true" to perform a recursive search
     * @return the exit status of the "list-realms" command
     */
    public int listRealms(String startRealm, String filter, 
            boolean recursiveSearch) 
    throws Exception {
        setSubcommand(LIST_REALMS_SUBCOMMAND);
        addRealmArguments(startRealm);        
        if (filter != null) {
            addFilterArguments(filter);
        }
        if (recursiveSearch) {
            addRecursiveArgument();
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout)); 
    }
    
    /**
     * Perform a listing of realms with no filter
     * @param startRealm - the realm from which to start the search
     * @param recursiveSearch - a boolean which should be set to "false" to 
     * perform a single level search or "true" to perform a recursive search
     * @return the exit status of the "list-realms" command
     */
    public int listRealms (String startRealm, boolean recursiveSearch) 
    throws Exception {
        return listRealms(startRealm, null, recursiveSearch);
    }
    
    /**
     * Perform a non-recursive listing of realms with no filter
     * @param startRealm - the realm from which to start the search
     * @return the exit status of the "list-realms" command
     */
    public int listRealms (String startRealm) 
    throws Exception {
        return listRealms(startRealm, null, false);
    }
    
    /**
     * Create an identity in a realm
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @param attributeValues - a string containing the attribute values for the 
     * identity to be created
     * @param useAttributeValues - a boolean flag indicating whether the 
     * "--attributevalues" option should be used 
     * @param useDatafile - a boolean flag indicating whether the attribute 
     * values should be written to a file and passed to the CLI using the 
     * "--datafile <file-path>" arguments
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type, 
            List attributeValues, boolean useAttributeValues, 
            boolean useDatafile) 
    throws Exception {
        setSubcommand(CREATE_IDENTITY_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnameArguments(name);
        addIdtypeArguments(type);
        
        if (attributeValues != null) {
            if (useAttributeValues) {
                addAttributevaluesArguments(attributeValues);
            }
            if (useDatafile) {
                addDatafileArguments(attributeValues, "attrValues", ".txt");
            }
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }       
    
    /**
     * Create an identity in a realm
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @param attributeValues - a string containing the attribute values for the 
     * identity to be created
     * @param useAttributeValues - a boolean flag indicating whether the 
     * "--attributevalues" option should be used 
     * @param useDatafile - a boolean flag indicating whether the attribute 
     * values should be written to a file and passed to the CLI using the 
     * "--datafile <file-path>" arguments
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type, 
            String attributeValues, boolean useAttributeValues, 
            boolean useDatafile) 
    throws Exception {
        setSubcommand(CREATE_IDENTITY_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnameArguments(name);
        addIdtypeArguments(type);
        
        if (attributeValues != null) {
            ArrayList attributeList = new ArrayList();
            
            if (useAttributeValues) {
                addAttributevaluesArguments(attributeValues);
            } 
            if (useDatafile) {
                addDatafileArguments(attributeValues, "attrValues", ".txt");
            }
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Create an identity in a realm using the "--attribute-values" argument
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values for the identity to be created
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type, 
            String attributeValues)
    throws Exception {
        return (createIdentity(realm, name, type, attributeValues, true, 
                false));
    }
    
    /**
     * Create an identity in a realm using the "--attribute-values" argument
     * @param realm - the realm in which to create the identity
     * @param name - the name of the identity to be created
     * @param type - the type of identity to be created (e.g. "User", "Role", 
     * and "Group")
     * @return the exit status of the "create-identity" command
     */
    public int createIdentity(String realm, String name, String type)
    throws Exception {
        String emptyString = null;
        return (createIdentity(realm, name, type, emptyString, false, false));
    }
    
    /**
     * Create multiple identities from a list of identities.
     *
     * @param idList - a list of identities to create separated by semi-colons.
     * @return a boolean indicating whether all the realms are created 
     * successfully.
     */
    public boolean createIdentities(String idList)
    throws Exception {
        boolean allIdsCreated = true;
        String setupID = null;
        
        if (idList != null) {
            if (idList.length() > 0) {
                String [] ids = idList.split("\\|");
                for (int i=0; i < ids.length; i++) {
                    log(Level.FINE, "createIdentities", "Creating id " + 
                            ids[i]);
                    String [] idArgs = ids[i].split("\\,");
                    if (idArgs.length >= 3) {
                        String idRealm = idArgs[0];
                        String idName = idArgs[1];
                        String idType = idArgs[2];
                        log(Level.FINEST, "createIdentities", "Realm for id: " +
                                idRealm);
                        log(Level.FINEST, "createIdentities", "Name for id: " + 
                                idName);
                        log(Level.FINEST, "createIdentities", "Type for id: " + 
                                idType);                            
                        String idAttributes = null;
                        int exitStatus = -1;
                        if (idArgs.length > 3) {
                            idAttributes = idArgs[3];
                            exitStatus = createIdentity(idRealm, idName, idType,
                                    idAttributes);
                        } else {                                
                            exitStatus = createIdentity(idRealm, idName, 
                                    idType);
                        }
                        logCommand("createIdentities");
                        resetArgList();
                        if (exitStatus != SUCCESS_STATUS) {
                            allIdsCreated = false;
                            log(Level.SEVERE, "createIdentities", 
                                    "The creation of " + idName + 
                                    " failed with exit status " + exitStatus + 
                                    ".");
                        }
                    } else {
                        allIdsCreated = false;
                        log(Level.SEVERE, "createIdentities", 
                                "The identity " + ids[i] + 
                                " must have a realm, an identity name, and an " 
                                + "identity type");
                    }
                }
            } else {
                allIdsCreated = false;
                log(Level.SEVERE, "createIdentities", 
                        "The identity list is empty.");
            }
        } else {
            allIdsCreated = false;
            log(Level.SEVERE, "createIdentities", "The identity list is null.");
        }
        return(allIdsCreated);
    }
    
    /**
     * Delete one or more identities in a realm
     * @param realm - the realm from which the identies should be deleted
     * @param names - one or more identity names to be deleted
     * @param type - the type of the identity (identities) to be deleted
     * @return the exit status of the "delete-identities" command
     */
    public int deleteIdentities(String realm, String names, String type)
    throws Exception {
        setSubcommand(DELETE_IDENTITIES_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnamesArguments(names);
        addIdtypeArguments(type);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * List the identities in a particular realm
     * @param realm - the realm in which to start the search for identities
     * @param filter - the filter to apply in the search for identities
     * @param idtype - the type of identities (e.g. "User", "Group", "Role") for
     * which the search sould be performed
     * @return the exit status of the "list-identities" command
     */
    public int listIdentities(String realm, String filter, String idtype)
    throws Exception {
        setSubcommand(LIST_IDENTITIES_SUBCOMMAND);
        addRealmArguments(realm);
        addFilterArguments(filter);
        addIdtypeArguments(idtype);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Get the attributes of an identity.
     *
     * @param realm - the realm in which the identity exists.
     * @param idName - the name of the identity for which the attributes should 
     * be retrieved.
     * @param idType - the type of the identity for which the attributes should 
     * be retrieved.
     * @param attributeNames - the name or names of the attributes that should 
     * be retrieved.
     * @return the exit status of the "get-identity" command.
     */
    public int getIdentity(String realm, String idName, String idType, 
            String attributeNames)
    throws Exception {
        setSubcommand(GET_IDENTITY_SUBCOMMAND);
        addRealmArguments(realm);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        if (attributeNames.length() > 0) {
            addAttributeNamesArguments(attributeNames);
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
            
    /**
     * Add an identity as a member of another identity.
     * @param realm - the realm in which the member identity should be added.
     * @param memberName - the name of the identity which should be added as a
     * member.
     * @param memberType - the type of the identity which should be added as a 
     * member.
     * @param idName - the name of the identity in which the member should be 
     * added.
     * @param idType - the type of the identity in which the member should be 
     * added.
     * @return the exit status of the "add-member" command
     */
    public int addMember(String realm, String memberName, String memberType, 
            String idName, String idType)
    throws Exception {
        setSubcommand(ADD_MEMBER_SUBCOMMAND);
        addRealmArguments(realm);
        addMemberIdnameArguments(memberName);
        addMemberIdtypeArguments(memberType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Remove an identity as a member of another identity.
     * @param realm - the realm in which the member identity should be removed.
     * @param memberName - the name of the identity which should be removed as a
     * member.
     * @param memberType - the type of the identity which should be removed as a 
     * member.
     * @param idName - the name of the identity in which the member should be 
     * removed.
     * @param idType - the type of the identity in which the member should be 
     * removed.
     * @return the exit status of the "remove-member" command
     */
    public int removeMember(String realm, String memberName, String memberType, 
            String idName, String idType)
    throws Exception {
        setSubcommand(REMOVE_MEMBER_SUBCOMMAND);
        addRealmArguments(realm);
        addMemberIdnameArguments(memberName);
        addMemberIdtypeArguments(memberType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));       
    }
    
    /**
     * Display the member identities of an identity.
     * @param realm - the realm in which the identity exists.
     * @param membershipType - the identity type of the member which should be
     * displayed.
     * @param idName - the name of the identity for which the members should be 
     * displayed.
     * @param idType - the type of the identity for which the members should be 
     * displayed.
     * @return the exit status of the "show-members" command.
     */
    public int showMembers(String realm, String membershipType, String idName, 
            String idType)
    throws Exception {
        setSubcommand(SHOW_MEMBERS_SUBCOMMAND);
        addRealmArguments(realm);
        addMembershipIdtypeArguments(membershipType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));            
    } 
    
    /**
     * Display the identities of which an identity is a member.
     * @param realm - the realm in which the identity exists.
     * @param membershipType - the type of membership which should be displayed.
     * @param idName - the identity name for which the memberships should be 
     * displayed.
     * @param idType - the identity type for which the memberships should be 
     * displayed.
     * @return the exit status of the "show-memberships" command.
     */
    public int showMemberships(String realm, String membershipType, 
            String idName, String idType)
    throws Exception {
        setSubcommand(SHOW_MEMBERSHIPS_SUBCOMMAND);
        addRealmArguments(realm);
        addMembershipIdtypeArguments(membershipType);
        addIdnameArguments(idName);
        addIdtypeArguments(idType);
        addGlobalOptions();
        return (executeCommand(commandTimeout));             
    }
    
    /**
     * Add an attribute to a realm.
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values for the identity to be created.
     * @param useDatafile - a boolean indicating whether a datafile should be 
     * used.  If true, a datafile will be created and the "--datafile" argument
     * will be used.  If false, the "--attributevalues" argument and a list of 
     * attribute name/value pairs will be used.
     * @return the exit status of the "add-realm-attributes" command.
     */
    public int addRealmAttributes(String realm, String serviceName, 
            String attributeValues, boolean useDatafile)
    throws Exception {
        setSubcommand(SET_REALM_ATTRIBUTES_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName);
        addAppendArgument();
        
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
    
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Delete an attribute from a realm.
     *
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @param attributeName - the name of the attribute to be deleted.
     * @return the exit status of the "delete-realm-attribute" command.
     */
    public int deleteRealmAttribute(String realm, String serviceName, 
            String attributeName)
    throws Exception {
        setSubcommand(DELETE_REALM_ATTRIBUTE_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName);
        addAttributeNameArguments(attributeName);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Set an attribute in a realm.
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values for the identity to be created.
     * @param useDatafile - a boolean indicating whether a datafile should be 
     * used.  If true, a datafile will be created and the "--datafile" argument
     * will be used.  If false, the "--attributevalues" argument and a list of 
     * attribute name/value pairs will be used.
     * @return the exit status of the "set-realm-attributes" command.
     */
    public int setRealmAttributes(String realm, String serviceName, 
            String attributeValues, boolean useDatafile)
    throws Exception {
        setSubcommand(SET_REALM_ATTRIBUTES_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName); 
        
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
    
        addGlobalOptions();
        return (executeCommand(commandTimeout));        
    }
    
    /**
     * Retrive the attributes of a realm.
     *
     * @param realm - the realm in which the identity exists.
     * @param serviceName - the name of the service in which to add the 
     * attribute.
     * @return the exit status of the "get-realm" command.
     */
    public int getRealm(String realm, String serviceName) 
    throws Exception {
        setSubcommand(GET_REALM_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(serviceName);
        
        addGlobalOptions();
        return (executeCommand(commandTimeout));         
    }
    
    /**
     * Create an authentication instance.
     * 
     * @param realm - the realm in which the authentication instance should be 
     * created.
     * @param name - the name of the authentication instance to be created.
     * @param type - the type of the authentication instance to be created.
     * @return the exit status of the "create-auth-instance" command.
     */
    public int createAuthInstance(String realm, String name, String type)
    throws Exception {
        setSubcommand(CREATE_AUTH_INSTANCE_SUBCOMMAND);
        addRealmArguments(realm);
        addNameArguments(name);
        addAuthtypeArguments(type);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Create multiple auth instances from a list of instances.
     *
     * @param instanceList - a list of instances to create separated by pipes 
     * ('|') in the following format 
     * (<realm1>,<auth-instance-name1>,<auth-instance-type1>|
     * <realm2>,<auth-instance-name2>,<auth-instance-type2>)
     * @return a boolean indicating whether all the instances are created 
     * successfully.
     */
    public boolean createAuthInstances(String instanceList)
    throws Exception {
        boolean allInstancesCreated = true;
        
        if (instanceList != null) {
            if (instanceList.length() > 0) {
                String [] instances = instanceList.split("\\|");
                for (String instance: instances) {
                    String[] instanceInfo = instance.split(",");
                    if (instanceInfo.length == 3) {
                        String realm = instanceInfo[0];
                        String instanceName = instanceInfo[1];
                        String authType = instanceInfo[2];
                        log(Level.FINE, "createAuthInstances", "Creating auth " + 
                                "instance " + instance);
                        int exitStatus = createAuthInstance(realm, instanceName,
                                authType);
                        logCommand("createAuthInstances");
                        resetArgList();
                        if (exitStatus != SUCCESS_STATUS) {
                            allInstancesCreated = false;
                            log(Level.SEVERE, "createAuthInstances", 
                                    "The instance " + instance + 
                                    " failed to be created.");
                        }
                    } else {
                        allInstancesCreated = false;
                        log(Level.SEVERE, "createAuthInstances", 
                                "The instance to be created must contain a " + 
                                "realm, an instance name, and an instance " +
                                "type.");
                    }
                }
            } else {
                allInstancesCreated = false;
                log(Level.SEVERE, "createAuthInstances", 
                        "The list of instances is empty.");
            }
        } else {
            allInstancesCreated = false;
            log(Level.SEVERE, "createAuthInstances", 
                    "The list of instances is null.");
        }
        return (allInstancesCreated);
    }
    
    /**
     * Delete one or more authentication instances.
     *
     * @param realm - the realm in which the authentication instance should be
     * deleted.
     * @param names - the name(s) of the authentication instance(s) to be
     * deleted.
     * @return the exit status of the "delete-auth-instance" command.
     */
    public int deleteAuthInstances(String realm, String names)
    throws Exception {
        setSubcommand(DELETE_AUTH_INSTANCES_SUBCOMMAND);
        addRealmArguments(realm);
        addNamesArguments(names);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Get the attribute values for an authentication instance.
     * 
     * @param realm - the realm in which the authentication instance exists.
     * @param name - the name of the authentication instance for which the 
     * instance should be retrieved.
     * @return the exit status of the "get-auth-instance" command.
     */
    public int getAuthInstance(String realm, String name) 
    throws Exception {
        setSubcommand(GET_AUTH_INSTANCE_SUBCOMMAND);
        addRealmArguments(realm);
        addNameArguments(name);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * List the authentication instances for a realm.
     *
     * @param realm - the realm in which the authentication instances should be
     * displayed.
     * @return the exit status of the "list-auth-instances" command.
     */
    public int listAuthInstances(String realm)
    throws Exception {
        setSubcommand(LIST_AUTH_INSTANCES_SUBCOMMAND);
        addRealmArguments(realm);
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Update attribute values in an authentication instance.
     *
     * @param realm - the realm which contains the authentication instance to be
     * updated.
     * @param name - the name of the authentication instance which should be 
     * updated.
     * @param attributevalues - a semi-colon (';') delimited list of attribute
     * name / value pairs.
     * @param useDatafile - a boolean value indicating whether the datafile 
     * option should be used.
     * @return the exit status of the "update-auth-instance" command.
     */
    public int updateAuthInstance(String realm, String name, 
            String attributeValues, boolean useDatafile)
    throws Exception {
        setSubcommand(UPDATE_AUTH_INSTANCE_SUBCOMMAND);
        addRealmArguments(realm);
        addNameArguments(name);
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
    
        addGlobalOptions();
        return(executeCommand(commandTimeout));
    }
    
    /**
     * Set attribute values in multiple auth instances.
     *
     * @param instanceList - a list of instances to create separated by pipes 
     * ('|') in the following format 
     * (<realm1>,<auth-instance-name1>,<attribute-name1>=<attribute-value1>;
     * <attribute-name2>=<attribute-value2>|
     * <realm2>,<auth-instance-name2>,<attribute-name1>=<attribute-value1>;
     * <attribute-name2>=<attribute-value2>)
     * @return a boolean indicating whether all the instances are updated 
     * successfully.
     */
    public boolean updateAuthInstances(String instanceList)
    throws Exception {
        boolean allInstancesUpdated = true;
        
        if (instanceList != null) {
            if (instanceList.length() > 0) {
                String [] instances = instanceList.split("\\|");
                for (String instance: instances) {
                    String[] instanceInfo = instance.split(",", 3);
                    if (instanceInfo.length == 3) {
                        String realm = instanceInfo[0];
                        String instanceName = instanceInfo[1];
                        String attributeValues = instanceInfo[2];
                        log(Level.FINE, "updateAuthInstances", "Creating auth " + 
                                "instance " + instance);
                        int exitStatus = updateAuthInstance(realm, instanceName,
                                attributeValues, false);
                        logCommand("updateAuthInstances");
                        resetArgList();
                        if (exitStatus != SUCCESS_STATUS) {
                            allInstancesUpdated = false;
                            log(Level.SEVERE, "updateAuthInstances", 
                                    "The instance " + instance + 
                                    " failed to be updated.");
                        }
                    } else {
                        allInstancesUpdated = false;
                        log(Level.SEVERE, "updateAuthInstances", 
                                "The instance to be created must contain a " + 
                                "realm, an instance name, and an instance " +
                                "type.");
                    }
                }
            } else {
                allInstancesUpdated = false;
                log(Level.SEVERE, "updateAuthInstnces", 
                        "The list of instances is empty.");
            }
        } else {
            allInstancesUpdated = false;
            log(Level.SEVERE, "updateAuthInstances", 
                    "The list of instances is null.");
        }
        return (allInstancesUpdated);
    }
    
    
    /**
     * Set service attribute values in a realm.
     *
     * @param realm - the realm in which the attribute should be set.
     * @param serviceName - the name of the service in which to set the 
     * attribute.
     * @param attributeValues - a semi-colon delimited string containing the 
     * attribute values to be set in the service.
     * @param useDatafile - a boolean indicating whether a datafile should be 
     * used.  If true, a datafile will be created and the "--datafile" argument
     * will be used.  If false, the "--attributevalues" argument and a list of 
     * attribute name/value pairs will be used.
     * @return the exit status of the "set-service-attributes" command.
     */
    public int setServiceAttributes(String realm, String service, 
            String attributeValues, boolean useDatafile) 
    throws Exception {
        setSubcommand(SET_SERVICE_ATTRIBUTES_SUBCOMMAND);
        addRealmArguments(realm);
        addServiceNameArguments(service);
        if (!useDatafile) {
            addAttributevaluesArguments(attributeValues);
        } else {
            addDatafileArguments(attributeValues, "attr", ".txt");
        }
        addGlobalOptions();
        return(executeCommand(commandTimeout));
    }
    
    /**
     * Execute the command specified by the subcommand and teh argument list
     * @param subcommand - the CLI subcommand to be executed
     * @param argList - a String containing a list of arguments separated by 
     * semicolons (';').
     * @return the exit status of the CLI command
     */
    public int executeCommand(String subcommand, String argList)
    throws Exception {
        setSubcommand(subcommand);
        String [] args = argList.split(";");
        for (String arg: args) {
            addArgument(arg);
        }
        addGlobalOptions();
        return (executeCommand(commandTimeout));
    }
    
    /**
     * Iterate through a list containing attribute values and add the 
     * "--attributevalues" argument and a list of one attribute name/value pairs
     * to the argument list
     * @param valueList - a List object containing one or more attribute 
     * name/value pairs.
     */
    private void addAttributevaluesArguments(List valueList) 
    throws Exception {
       String attributesArg;
       if (useLongOptions) {
           attributesArg = PREFIX_ARGUMENT_LONG + ATTRIBUTE_VALUES_ARGUMENT;
       } else {
           attributesArg = PREFIX_ARGUMENT_SHORT + 
                   SHORT_ATTRIBUTE_VALUES_ARGUMENT;
       }
       addArgument(attributesArg);
       
       Iterator i = valueList.iterator();
       while (i.hasNext()) {
           addArgument((String)i.next());
       }
    }    
    
    
    /**
     * Parse a string containing attribute values and add the 
     * "--attributevalues" and a list of one attribute name/value pairs to the
     * argument list
     * 
     */
    private void addAttributevaluesArguments(String values) 
    throws Exception {
       StringTokenizer tokenizer = new StringTokenizer(values, ";");        
       ArrayList attList = new ArrayList(tokenizer.countTokens());
       
       while (tokenizer.hasMoreTokens()) {
           attList.add(tokenizer.nextToken());
       }
       addAttributevaluesArguments(attList);
    }

    /**
     * Create a datafile and add the "--datafile" and file path arguments to the
     * argument list.
     * @param valueList - a list containing attribute name value pairs separated 
     * by semi-colons (';')
     * @param filePrefix - a string containing a prefix for the datafile that 
     * will be created
     * @param fileSuffix - a string containing a suffix for the datafile that 
     * will be created
     */
    private void addDatafileArguments(List valueList, String filePrefix,
            String fileSuffix)
    throws Exception {
        StringBuffer valueBuffer = new StringBuffer();
        Iterator i = valueList.iterator();
        while (i.hasNext()) {
            valueBuffer.append((String)i.next());
            if (i.hasNext()) {
                valueBuffer.append(";");
            }
        }
        String values = valueBuffer.toString();
        addDatafileArguments(values, filePrefix, fileSuffix);
    }    
    
    /**
     * Create a datafile and add the "--datafile" and file path arguments to the
     * argument list.
     * @param values - a string containing attribute name value pairs separated 
     * by semi-colons (';')
     * @param filePrefix - a string containing a prefix for the datafile that 
     * will be created
     * @param fileSuffix - a string containing a suffix for the datafile that 
     * will be created
     */
    private void addDatafileArguments(String values, String filePrefix,
            String fileSuffix)
    throws Exception {
        Map attributeMap = parseStringToMap(values.replaceAll("\"",""));
        ResourceBundle rb_amconfig = 
                ResourceBundle.getBundle(TestConstants.TEST_PROPERTY_AMCONFIG);
        String attFileDir = getBaseDir() + fileseparator + 
                rb_amconfig.getString(TestConstants.KEY_ATT_SERVER_NAME) + 
                fileseparator + "built" + fileseparator + "classes" + 
                fileseparator;
        String attFile = attFileDir + filePrefix + 
                (new Integer(new Random().nextInt())).toString() + fileSuffix;
        String[] valueArray = values.split(";");
        StringBuffer buff = new StringBuffer();
        for (String value: valueArray) {
            buff.append(value + newline);
        }
        buff.append(newline);
        BufferedWriter out = new BufferedWriter(new FileWriter(attFile));
        out.write(buff.toString());
        out.close();
        String dataFileArg;
        if (useLongOptions) {
            dataFileArg = PREFIX_ARGUMENT_LONG + DATA_FILE_ARGUMENT;
        } else {
            dataFileArg = PREFIX_ARGUMENT_SHORT + SHORT_DATA_FILE_ARGUMENT;
        }
        addArgument(dataFileArg);
        addArgument(attFile);
    }
    
    /**
     * Add the "--idnames" argument and value to the argument list
     */
    private void addIdnamesArguments(String names) {
        String idnamesArg;
        if (useLongOptions) {
            idnamesArg = PREFIX_ARGUMENT_LONG + ID_NAMES_ARGUMENT;
        } else {
            idnamesArg = PREFIX_ARGUMENT_SHORT + SHORT_ID_NAME_ARGUMENT;
        }

        addArgument(idnamesArg);
        
        StringTokenizer tokenizer = new StringTokenizer(names);
        while (tokenizer.hasMoreTokens()) {
            addArgument(tokenizer.nextToken());
        }
    }
       
    /**
     * Sets the sub-command in the second argument of the argument list
     * @param command - the sub-command value to be stored
     */
    private void setSubcommand(String command) { 
        setArgument(SUBCOMMAND_VALUE_INDEX, command);
    }
    
    /**
     * Sets the user ID of the user that will execute the CLI.
     * @param  user - the user ID of the CLI
     */
    private void setAdminUser(String user) { adminUser = user; }
    
    /**
     * Sets the password for the admin user that will execute the CLI
     * @param passwd - the value of the admin user's password
     */
    private void setAdminPassword(String passwd) { adminPassword = passwd; }
    
    /**
     * Sets the member variable passwdFile to the name of the file containing
     * the CLI user's password for use with the "--passwordfile" argument.
     * @param fileName - the file containing the CLI user's password
     */
    private void setPasswordFile(String fileName) { passwdFile = fileName; }
    
    /**
     * Clear all arguments following the value of the admin user's password
     * or the password file.  Removes all sub-command specific arguments.
     */
    public void resetArgList() {
        clearArguments(PASSWORD_VALUE_INDEX);
    }
    
    /**
     * Check to see if a realm exists using the "famadm list-realms" command
     * @param realmsToFind - the realm or realms to find in the output of 
     * "famadm list-realms".  Multiple realms should be separated by semi-colons
     * (';').
     * @return a boolean value of true if the realm(s) is(are) found and false 
     * if one or more realms is not found.
     */
    public boolean findRealms(String startRealm, String filter, 
            boolean recursiveSearch, String realmsToFind)
    throws Exception {
        boolean realmsFound = true;
        
        if ((realmsToFind != null) && (realmsToFind.length() > 0)) {
            if (listRealms(startRealm, filter, recursiveSearch) == 
                    SUCCESS_STATUS) {                    
                StringTokenizer tokenizer = new StringTokenizer(realmsToFind, 
                        ";");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (token != null) {
                        if (token.length() > 1) {
                            String searchRealm = token.substring(1);
                            if (!findStringInOutput(searchRealm)) {
                                log(logLevel, "findRealms", "Realm " + 
                                        searchRealm + " was not found.");
                                realmsFound = false;
                            } else {
                                log(logLevel, "findRealms", "Realm " + 
                                  searchRealm + " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findRealms", "Realm " + token + 
                                    " should be longer than 1 character.");
                            realmsFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findRealms", "Realm " + token + 
                                " in realmsToFind is null.");
                        realmsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findRealms", 
                        "famadm list-realms command failed");
                realmsFound = false;
            }
            logCommand("findRealms");
        } else {
            log(Level.SEVERE, "findRealms", "realmsToFind is null or empty");
            realmsFound = false;
        }
        return realmsFound;
    }
    
    /**
     * Check to see if a realm exists using the "famadm list-realms" command
     * @param realmsToFind - the realm or realms to find in the output of 
     * "famadm list-realms".  Multiple realms should be separated by semi-colons
     * (';').
     * @return a boolean value of true if the realm(s) is(are) found and false 
     * if one or more realms is not found.
     */
    public boolean findRealms(String realmsToFind)
    throws Exception {
        return(findRealms(TestCommon.realm, "*", true, realmsToFind));
    }  
    
    /**
     * Check to see if an identity exists using the "famadm list-identities" 
     * command.
     * @param startRealm - the realm in which to find identities
     * @param filter - the filter that will be applied in the search
     * @param type - the type of identities (User, Group, Role) for which the 
     * search will be performed
     * @param idsToFind - the identity or identities to find in the output of 
     * "famadm list-identities".  Multiple identities should be separated by 
     * a space (' ').
     * @return a boolean value of true if the identity(ies) is(are) found and 
     * false if one or more identities is not found.
     */
    public boolean findIdentities(String startRealm, String filter, String type,
            String idsToFind)
    throws Exception {
        boolean idsFound = true;
        
        if ((idsToFind != null) && (idsToFind.length() > 0)) {
            if (listIdentities(startRealm, filter, type) == SUCCESS_STATUS) {
                String [] ids = idsToFind.split(";");
                for (int i=0; i < ids.length; i++) {
                    String token = ids[i];
                    String rootDN = "";
                    if (token != null) {
                        if (startRealm.equals(TestCommon.realm)) {
                            rootDN = TestCommon.basedn; 
                        } else {
                            String [] realms = startRealm.split("/");
                            StringBuffer buffer = new StringBuffer();
                            for (int j = realms.length-1; j >= 0; j--) {
                                if (realms[j].length() > 0) {
                                    buffer.append("o=" + realms[j] + ",");
                                }
                            }
                            buffer.append("ou=services,").
                                    append(TestCommon.basedn);
                            rootDN = buffer.toString();
                        }
                        if (token.length() > 0) {
                            String idString = token + " (id=" + token + ",ou=" + 
                                    type.toLowerCase() + "," + rootDN + ")";
                            if (!findStringInOutput(idString)) {
                                log(logLevel, "findIdentities", "String \'" + 
                                        idString + "\' was not found.");
                                idsFound = false;
                            } else {
                                log(logLevel, "findIdentities", type + 
                                        " identity " + token + " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findIdentities", 
                                    "The identity to find is empty.");
                            idsFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findIdentities", 
                                "Identity in idsToFind is null.");
                        idsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findIdentities", 
                        "famadm list-identities command failed");
                idsFound = false;
            }
            logCommand("findIdentities");
        } else {
            log(Level.SEVERE, "findIdentities", "idsToFind is null or empty");
            idsFound = false;
        }
        return idsFound;
    }

   /**
     * Check to see if an identity exists using the "famadm show-members" 
     * command.
     * @param startRealm - the realm in which to find identities
     * @param filter - the filter that will be applied in the search
     * @param type - the type of identities (User, Group, Role) for which the 
     * search will be performed
     * @param membersToFind - the member identity or identities to find in the 
     * output of "famadm show-members".  Multiple identities should be separated 
     * by a semicolon (';').
     * @return a boolean value of true if the member(s) is(are) found and 
     * false if one or more members is not found.
     */
    public boolean findMembers(String startRealm, String idName, String idType,
            String memberType, String membersToFind)
    throws Exception {
        boolean membersFound = true;
        
        if ((membersToFind != null) && (membersToFind.length() > 0)) {
            if (showMembers(startRealm, memberType, idName, idType) == 
                    SUCCESS_STATUS) {
                String [] members = membersToFind.split(";");
                for (int i=0; i < members.length; i++) {
                    String rootDN = "";
                    if (members[i] != null) {
                        if (startRealm.equals(TestCommon.realm)) {
                            rootDN = TestCommon.basedn; 
                        } else {
                            String [] realms = startRealm.split("/");
                            StringBuffer realmBuffer = new StringBuffer();
                            for (int j = realms.length-1; j >= 0; j--) {
                                if (realms[j].length() > 0) {
                                    realmBuffer.append("o=" + realms[j] + ",");
                                }
                            }
                            realmBuffer.append("ou=services,").
                                    append(TestCommon.basedn);
                            rootDN = realmBuffer.toString();
                        }
                        if (members[i].length() > 0) {
                            StringBuffer idBuffer = 
                                    new StringBuffer(members[i]);
                            idBuffer.append(" (id=").append(members[i]).
                                    append(",ou=").
                                    append(memberType.toLowerCase()).
                                    append(",").append(rootDN).append(")");
                            if (!findStringInOutput(idBuffer.toString())) {
                                log(Level.FINEST, "findMember", 
                                        "String \'" + idBuffer.toString() + 
                                        "\' was not found.");
                                membersFound = false;
                            } else {
                                log(Level.FINEST, "findMembers", memberType + 
                                        " identity " + members[i] + 
                                        " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findMembers", 
                                    "The member to find is empty.");
                            membersFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findMembers", 
                                "Identity in membersToFind is null.");
                        membersFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findMembers", 
                        "famadm show-members command failed");
                membersFound = false;
            }
            logCommand("findMembers");
        } else {
            log(Level.SEVERE, "findMembers", "membersToFind is null or empty");
            membersFound = false;
        }
        return membersFound;
    }    

    /**
     * Check to see if a member exists using the "famadm show-memberships" 
     * command.
     * @param startRealm - the realm in which to find identities
     * @param filter - the filter that will be applied in the search
     * @param type - the type of identities (User, Group, Role) for which the 
     * search will be performed
     * @param membersToFind - the member identity or identities to find in the 
     * output of "famadm show-memberships".  Multiple memberships should be 
     * separated by a semicolon (';').
     * @return a boolean value of true if the membership(s) is(are) found and 
     * false if one or more memberships is not found.
     */
    public boolean findMemberships(String startRealm, String idName,
            String idType, String membershipType, String membershipsToFind)
    throws Exception {
        boolean membershipsFound = true;

        if ((membershipsToFind != null) && (membershipsToFind.length() > 0)) {
            if (showMemberships(startRealm, membershipType, idName, idType) == 
                    SUCCESS_STATUS) {
                String [] memberships = membershipsToFind.split(";");
                for (int i=0; i < memberships.length; i++) {
                    String rootDN = "";
                    if (memberships[i] != null) {
                        if (startRealm.equals(TestCommon.realm)) {
                            rootDN = TestCommon.basedn; 
                        } else {
                            String [] realms = startRealm.split("/");
                            StringBuffer realmBuffer = new StringBuffer();
                            for (int j = realms.length-1; j >= 0; j--) {
                                if (realms[j].length() > 0) {
                                    realmBuffer.append("o=" + realms[j] + ",");
                                }
                            }
                            realmBuffer.append("ou=services,").
                                    append(TestCommon.basedn);
                            rootDN = realmBuffer.toString();
                        }
                        if (memberships[i].length() > 0) {
                            StringBuffer idBuffer = 
                                    new StringBuffer(memberships[i]);
                            idBuffer.append(" (id=").append(memberships[i]).
                                    append(",ou=").
                                    append(membershipType.toLowerCase()).
                                    append(",").append(rootDN).append(")");
                            if (!findStringInOutput(idBuffer.toString())) {
                                log(Level.FINEST, "findMemberships", 
                                        "String \'" + idBuffer.toString() + 
                                        "\' was not found.");
                                membershipsFound = false;
                            } else {
                                log(Level.FINEST, "findMemberships", 
                                        membershipType + " identity " + 
                                        memberships[i] + " was found.");
                            }
                        } else {
                            log(Level.SEVERE, "findMemberships", 
                                    "The membership to find is empty.");
                            membershipsFound = false;
                        }
                    } else {
                        log(Level.SEVERE, "findMemberships", 
                                "Identity in membersToFind is null.");
                        membershipsFound = false;
                    }
                }
            } else {
                log(Level.SEVERE, "findMemberships", 
                        "famadm show-memberships command failed");
                membershipsFound = false;
            }
            logCommand("findMemberships");
        } else {
            log(Level.SEVERE, "findMemberships", 
                    "membersToFind is null or empty");
            membershipsFound = false;
        }
        return membershipsFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm for which the attributes should be retrieved.
     * @param serviceName - the name of service for which the attributes should 
     * be retrieved.
     * @param attributeValues - a semi-colon delimited list of attribute 
     * name/value pairs.
     * @return a boolean flag indicating whether all of the attribute name/value
     * pairs are found in the output of the "famadm get-realm" command.
     */
    public boolean findRealmAttributes(String realm, String serviceName, 
            String attributeValues)
    throws Exception {
        boolean attributesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            if (serviceName != null && !serviceName.equals("")) {
                if (attributeValues != null && !attributeValues.equals("")) {
                    commandStatus = getRealm(realm, serviceName);
                    if (commandStatus == SUCCESS_STATUS) {
                        attributesFound = findStringsInOutput(attributeValues, 
                                ";");
                    } else {
                        log(Level.SEVERE, "findRealmAttributes", 
                                "The famadm get-realm command returned " + 
                                commandStatus + " as an exit status");
                        attributesFound = false;
                    }
                } else {
                    log(Level.SEVERE, "findRealmAttributes", 
                            "The attribute value list is not valid");
                    attributesFound = false;
                }
            } else {
                log(Level.SEVERE, "findRealmAttributes", 
                        "The service name is not valid");
                attributesFound = false;
            }
        } else {
            log(Level.SEVERE, "findRealmAttributes", 
                    "The realm name is not valid");
            attributesFound = false;
        }
        return attributesFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm in which the identity exists.
     * @param idName - the name of the identity for which the attributes should 
     * be retrieved.
     * @param idType - the type of the identity for which the attributes should 
     * be retrieved.
     * @param attributeNames - the name or names of the attributes that should 
     * be retrieved.
     * @param attributeValues - the attribute name/value pair or pairs which 
     * should be found.
     * @return a boolean flag indicating whether all of the attribute name/value
     * pairs are found in the output of the "famadm get-identity" command.
     */
    public boolean findIdentityAttributes(String realm, String idName, 
            String idType, String attributeNames, String attributeValues)
    throws Exception {
        boolean attributesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            if (idName != null && !idName.equals("")) {
                if (idType != null && !idType.equals("")) {
                    commandStatus = getIdentity(realm, idName, idType, 
                            attributeNames);
                    if (commandStatus == SUCCESS_STATUS) {
                        attributesFound = findStringsInOutput(attributeValues, 
                                ";");
                    } else {
                        log(Level.SEVERE, "findIdentityAttributes", 
                                "The famadm get-identity command returned " + 
                                commandStatus + " as an exit status");
                        attributesFound = false;
                    }
                } else {
                    log(Level.SEVERE, "findIdentityAttributes", 
                            "The attribute value list is not valid");
                    attributesFound = false;
                }
            } else {
                log(Level.SEVERE, "findIdentityAttributes", 
                        "The service name is not valid");
                attributesFound = false;
            }
        } else {
            log(Level.SEVERE, "findIdentityAttributes", 
                    "The realm name is not valid");
            attributesFound = false;
        }
        return attributesFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm in which the auth instances should be listed.
     * @param instanceName - a pipe ('|') separated list of auth instances in 
     * the following format: 
     * <auth-instance-name1>,<auth-instance-type1>|<auth-instance-name2>,
     * <auth-instance-type2>
     * @return a boolean flag indicating whether all of the auth instances were
     * found in the output of the "famadm list-auth-instances command.
     */
    public boolean findAuthInstances(String realm, String instanceNames)
    throws Exception {
        boolean instancesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            commandStatus = listAuthInstances(realm);
            logCommand("findAuthInstances");
            if (commandStatus == SUCCESS_STATUS) {
                StringBuffer instanceList = new StringBuffer();
                String[] instances = instanceNames.split("\\|");
                for (int i=0; i < instances.length; i++) {
                    String[] instanceInfo = instances[i].split(",");
                    if (instanceInfo.length == 2) {
                        String name = instanceInfo[0];
                        String type = instanceInfo[1];
                        instanceList.append(name).append(", [type=").
                                append(type).append("]");
                        if (i < instances.length - 1) {
                            instanceList.append(";");
                        }
                    } else {
                        log(Level.SEVERE, "findAuthInstances", 
                                "The instance list must contain a name and " +
                                "type for each instance");
                        instancesFound = false;
                    }
                }
                if (instancesFound) {
                    log(Level.FINE, "findAuthInstances", "Searching for the " + 
                            "following instances: " + instanceList);
                    instancesFound = 
                            findStringsInOutput(instanceList.toString(), ";");
                }
            } else {
                log(Level.SEVERE, "findAuthInstances", 
                        "The famadm list-auth-instances command returned " + 
                        commandStatus + " as an exit status");
                instancesFound = false;
            }
        } else {
            log(Level.SEVERE, "findIdentityAttributes", 
                    "The realm name is not valid");
            instancesFound = false;
        }
        return instancesFound;
    }
    
    /**
     * Search through the attributes of a realm to verify that certain list of
     * attributes name/value pairs are present.
     *
     * @param realm - the realm in which the identity exists.
     * @param instanceName - the name of the authentication instance for which 
     * the attributes should be retrieved.
     * @param attributeValues - the attribute name/value pair or pairs which 
     * should be found.
     * @return a boolean flag indicating whether all of the attribute name/value
     * pairs are found in the output of the "famadm get-auth-instance" command.
     */
    public boolean findAuthInstanceAttributes(String realm, 
            String instanceName, String attributeValues)
    throws Exception {
        boolean attributesFound = true;
        int commandStatus = -1;
        
        if (realm != null && !realm.equals("")) {
            if (instanceName != null && !instanceName.equals("")) {
                if (attributeValues != null && !attributeValues.equals("")) {
                    commandStatus = getAuthInstance(realm, instanceName);
                    if (commandStatus == SUCCESS_STATUS) {
                        attributesFound = findStringsInOutput(attributeValues, 
                                ";");
                    } else {
                        log(Level.SEVERE, "findAuthInstanceAttributes", 
                                "The famadm get-auth-instance command returned "
                                + commandStatus + " as an exit status");
                        attributesFound = false;
                    }
                } else {
                    log(Level.SEVERE, "findAuthInstanceAttributes", 
                            "The attribute value list is not valid");
                    attributesFound = false;
                }
            } else {
                log(Level.SEVERE, "findAuthInstanceAttributes", 
                        "The instance name is not valid");
                attributesFound = false;
            }
        } else {
            log(Level.SEVERE, "findAuthInstanceAttributes", 
                    "The realm name is not valid");
            attributesFound = false;
        }
        return attributesFound;
    }    
}
