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
 * $Id: CLIRequest.java,v 1.5 2007-04-09 23:34:20 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * This class encapsulates the CLI request information. Upon creation of this
 * object, it is added a queue where requests are processed one after another.
 */
public class CLIRequest {
    private CLIRequest parent;
    private String[] argv;
    private SSOToken ssoToken;

    /**
     * Constructs a CLI request object.
     *
     * @param parent Parent request object. This can be null if the request is
     *        the root request.
     * @param argv Options for the request.
     * @param ssoToken Single Sign On token of the administrator.
     */
    public CLIRequest(CLIRequest parent, String[] argv, SSOToken ssoToken) {
        this.parent = parent;
        this.argv = argv;
        this.ssoToken = ssoToken;
    }

    /**
     * Constructs a CLI request object.
     *
     * @param parent Parent request object. This can be null if the request is the
     *        root request.
     * @param argv Options for the request.
     */
    public CLIRequest(CLIRequest parent, String[] argv) {
        this.parent = parent;
        this.argv = argv;
    }
    
    /**
     * Returns the single sign on token.
     *
     * @return the single sign on token.
     */
    public SSOToken getSSOToken() {
        return ssoToken;
    }


    /**
     * Returns options for the request.
     *
     * @return options for the request.
     */
    public String[] getOptions() {
        return argv;
    }

    /**
     * Returns parent request object.
     *
     * @return parent request object.
     */
    public CLIRequest getParent() {
        return parent;
    }

    /**
     * Processes the request.
     *
     * @param mgr Command Manager instance.
     * @param ssoToken Single Sign On Token of the user.
     * @throws CLIException if the request cannot be serviced.
     */
    public void process(CommandManager mgr, SSOToken ssoToken)
        throws CLIException {
        if (argv.length == 0) {
            UsageFormatter.getInstance().format(mgr);
        } else if (argv.length == 1) {
            process(mgr, argv[0], ssoToken);
        } else {
            process(mgr, argv, ssoToken);
        }
    }
    
    /**
     * Processes the request.
     *
     * @param mgr Command Manager instance.
     * @throws CLIException if the request cannot be serviced.
     */
    public void process(CommandManager mgr)
        throws CLIException {
        if (argv.length == 0) {
            UsageFormatter.getInstance().format(mgr);
        } else if (argv.length == 1) {
            process(mgr, argv[0], null);
        } else {
            process(mgr, argv, null);
        }
    }
    
    private void process(CommandManager mgr, String arg, SSOToken ssoToken)
        throws CLIException {
        String commandName = mgr.getCommandName();
        ResourceBundle rb = mgr.getResourceBundle();
        if (matchOption(arg, CLIConstants.ARGUMENT_HELP,
                CLIConstants.SHORT_ARGUMENT_HELP)
        ) {
            UsageFormatter.getInstance().format(mgr);
        } else if (matchOption(arg, CLIConstants.ARGUMENT_VERSION,
                CLIConstants.SHORT_ARGUMENT_VERSION)
        ) {
            processVersion(mgr);
        } else if (arg.startsWith(CLIConstants.PREFIX_ARGUMENT_SHORT)) {
            Object[] param = {commandName + " " + arg};
            throw new CLIException(MessageFormat.format(
                rb.getString("error-message-incorrect-options"), param),
                    ExitCodes.INCORRECT_OPTION, arg);
        } else {
            SubCommand subcmd = mgr.getSubCommand(arg);
            if ((subcmd == null) || 
                (mgr.webEnabled() && !subcmd.webEnabled())
            ) {
                Object[] param = {commandName + " " + arg};
                throw new CLIException(MessageFormat.format(
                    rb.getString("error-message-unknown-subcommand"), param),
                        ExitCodes.INVALID_SUBCOMMAND, arg);
            } else {
                subcmd.execute(new RequestContext(this, mgr, subcmd));
            }
        }
    }

    private void process(CommandManager mgr, String[] argv, SSOToken ssoToken)
        throws CLIException {
        String commandName = mgr.getCommandName();
        ResourceBundle rb = mgr.getResourceBundle();
        String sumOfArgs = addAllArgs(argv);

        if (argv[0].startsWith(CLIConstants.PREFIX_ARGUMENT_SHORT) ||
            !argv[1].startsWith(CLIConstants.PREFIX_ARGUMENT_SHORT)
        ) {
            Object[] param = {commandName + " " + sumOfArgs};
            throw new CLIException(MessageFormat.format(
                rb.getString("error-message-incorrect-options"),
                param), ExitCodes.INCORRECT_OPTION);
        }

        String cmdName = argv[0];
        SubCommand subcmd = mgr.getSubCommand(cmdName);
        if ((subcmd == null) || 
            (mgr.webEnabled() && !subcmd.webEnabled())
        ) {
            Object[] param = {commandName + " " + sumOfArgs};
            throw new CLIException(MessageFormat.format(
                rb.getString("error-message-unknown-subcommand"),
                param), ExitCodes.INVALID_SUBCOMMAND);
        }

        if ((argv.length == 2) && (argv[1].equals(
            CLIConstants.PREFIX_ARGUMENT_SHORT +
            CLIConstants.SHORT_ARGUMENT_HELP) ||
            argv[1].equals(CLIConstants.PREFIX_ARGUMENT_LONG +
                CLIConstants.ARGUMENT_HELP))
        ) {
           UsageFormatter.getInstance().format(mgr, subcmd);
        } else {
            subcmd.execute(new RequestContext(this, mgr, subcmd));
        }
    }

    private String addAllArgs(String[] argv) {
        StringBuffer buff = new StringBuffer();
        buff.append(argv[0]);
        for (int i = 1; i < argv.length; i++) {
            buff.append(" ").append(argv[i]);
        }
        return buff.toString();
    }

    private boolean matchOption(
        String arg,
        String longOption,
        String shortOption
    ) {
        return arg.equals(CLIConstants.PREFIX_ARGUMENT_LONG + longOption) ||
            arg.equals(CLIConstants.PREFIX_ARGUMENT_SHORT + shortOption);
    }

    private void processVersion(CommandManager mgr) {
        IOutput outputWriter = mgr.getOutputWriter();
        outputWriter.printlnMessage("");
        outputWriter.printlnMessage("");
        outputWriter.printlnMessage(
            SystemProperties.get(Constants.AM_VERSION));
        outputWriter.printlnMessage("");
    }

}
