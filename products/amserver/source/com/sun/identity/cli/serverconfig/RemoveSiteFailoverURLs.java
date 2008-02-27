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
 * $Id: RemoveSiteFailoverURLs.java,v 1.2 2008-02-27 05:46:01 veiming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli.serverconfig;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.SiteConfiguration;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

/**
 * Remove secondary URLs from a site.
 */
public class RemoveSiteFailoverURLs extends AuthenticatedCommand {
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
        IOutput outputWriter = getOutputWriter();

        String siteName = getStringOptionValue(IArgument.SITE_NAME);
        List secondaryURLs = (List)rc.getOption(IArgument.SECONDARY_URLS);
        String[] params = {siteName};
        
        try {
            if (SiteConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }
        
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_REMOVE_SITE_FAILOVER_URLS", params);
            if (SiteConfiguration.isSiteExist(adminSSOToken, siteName)) {
                SiteConfiguration.removeSiteSecondaryURLs(
                    adminSSOToken, siteName, secondaryURLs);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("remove-site-secondary-urls-succeeded"),
                    (Object[])params));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("remove-site-secondary-urls-no-exists"),
                    (Object[])params));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_REMOVE_SITE_FAILOVER_URLS", params);
        } catch (SSOException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("RemoveSiteFailoverURLs.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SITE_FAILOVER_URLS", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {siteName, e.getMessage()};
            debugError("RemoveSiteFailoverURLs.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SITE_FAILOVER_URLS", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
