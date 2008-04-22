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
 * $Id: DeleteAgentGroups.java,v 1.2 2008-04-22 00:23:14 veiming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli.agentconfig;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command deletes agent groups.
 */
public class DeleteAgentGroups extends AuthenticatedCommand {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throw CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        SSOToken adminSSOToken = getAdminSSOToken();
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        List names = (List)rc.getOption(IArgument.AGENT_GROUP_NAMES);

        String displayableNames = tokenize(names);
        String[] params = {realm, displayableNames};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_DELETE_AGENT_GROUPS", params);

        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            Set setDelete = new HashSet();

            for (Iterator i = names.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                AMIdentity amid = new AMIdentity(
                    adminSSOToken, name, IdType.AGENTGROUP, realm, null); 
                setDelete.add(amid);
            }

            amir.deleteIdentities(setDelete);
            IOutput outputWriter = getOutputWriter();
            outputWriter.printlnMessage(getResourceString(
                "delete-agent-group-succeeded"));

            for (Iterator i = names.iterator(); i.hasNext(); ) {
                outputWriter.printlnMessage("    " + (String)i.next());
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_DELETE_AGENT_GROUPS", params);
        } catch (IdRepoException e) {
            String[] args = {realm, displayableNames, e.getMessage()};
            debugError("DeleteAgentGroups.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_AGENT_GROUPS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, displayableNames, e.getMessage()};
            debugError("DeleteAgentGroups.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_AGENT_GROUPS", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
