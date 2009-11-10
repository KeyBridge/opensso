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
 * $Id: RemoveApplicationPrivilegeResources.java,v 1.1 2009-11-10 19:01:04 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public class RemoveApplicationPrivilegeResources extends ApplicationPrivilegeBase {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throw CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String name = getStringOptionValue(PARAM_NAME);
        String[] params = {realm, name};
        Map<String, Set<String>> mapAppToResources =
            getApplicationResourcesMap(rc, realm);

        Subject userSubject = SubjectUtils.createSubject(
            getAdminSSOToken());
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance(realm, userSubject);
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_UPDATE_APPLICATION_PRIVILEGE", params);
        try {
            ApplicationPrivilege appPrivilege = apm.getPrivilege(name);
            Map<String, Set<String>> origAppToResources =
                getApplicationToResources(appPrivilege);
            removeFromMap(origAppToResources, mapAppToResources);

            if (origAppToResources.isEmpty()) {
                throw new CLIException(getResourceString(
                    "remove-application-privilege-resources-emptied-resources"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            appPrivilege.setApplicationResources(origAppToResources);
            apm.replacePrivilege(appPrivilege);

            Object[] msgParam = {name};
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("update-application-privilege-succeeded"),
                msgParam));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_UPDATE_APPLICATION_PRIVILEGE", params);
        } catch (EntitlementException ex) {
            String[] paramExs = {realm, name, ex.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_UPDATE_APPLICATION_PRIVILEGE", paramExs);
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
