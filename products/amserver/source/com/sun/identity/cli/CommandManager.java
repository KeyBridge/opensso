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
 * $Id: CommandManager.java,v 1.7 2007-04-02 06:07:54 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.cli;


import com.iplanet.services.util.Crypt;
import com.sun.identity.security.AdminTokenAction;
import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.debug.Debug;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This is the "engine" that drives the CLI. This is a singleton class.
 */
public class CommandManager {
    private final static String RESOURCE_BUNDLE_NAME = "cliBase";
    public static ResourceBundle resourceBundle;
    private static Debug debugger = Debug.getInstance("amCLI");
        
    private ResourceBundle rbMessages;
    private Map environment;
    private String commandName;
    private IOutput outputWriter;
    private List definitionObjects;
    private List requestQueue = new Vector();
    private boolean bContinue;

    static {
        resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_NAME);
    }
    
    /**
     * Entry point to the engine.
     */
    public static void main(String[] argv) {
        getIsInstallTime();
        Crypt.checkCaller();
        new CommandManager(argv);
    }

    /**
     * Constructs a command line manager.
     *
     * @param env Map of option name to values.
     */
    public CommandManager(Map env)
        throws CLIException {
        init(env);
    }

    /**
     * Constructs a command line manager.
     *
     * @param argv Options from the command shell.
     */
    public CommandManager(String[] argv) {
        try {
            init(argv);
            requestQueue.add(new CLIRequest(null, argv));
            serviceRequestQueue();
        } catch (CLIException e) {
            Debugger.error(this, "CommandManager.<init>", e);
            String remainReq = null;
            if (!requestQueue.isEmpty()) {
                String[] arg = {Integer.toString(requestQueue.size())};
                remainReq = MessageFormat.format(rbMessages.getString(
                    "remaining-unprocessed-requests"), (Object[])arg);
            }
            if (outputWriter != null) {
                outputWriter.printlnError(e.getMessage());
                if (remainReq != null) {
                    outputWriter.printlnError(remainReq);
                }
            } else {
                System.out.println(e.getMessage());
                if (remainReq != null) {
                    System.out.println(remainReq);
                }
            }

            printUsageOnException(e);
            System.exit(e.getExitCode());
        }
    }

    private void printUsageOnException(CLIException e) {
        int exitCode = e.getExitCode();

        try {
            if (exitCode == ExitCodes.INCORRECT_OPTION) {
                String scmd = e.getSubcommandName();
                if (scmd != null) {
                    SubCommand cmd = getSubCommand(scmd);

                    if (cmd != null) {
                        UsageFormatter.getInstance().format(this, cmd);
                    }
                }
            }
        } catch (CLIException ex) {
            debugger.error("CommandManager.printUsageOnException", ex);
        }
    }


    private void init(Map env)
        throws CLIException {
        Locale locale = (Locale)env.get(CLIConstants.ARGUMENT_LOCALE);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        environment.put(CLIConstants.ARGUMENT_LOCALE, locale);

        try {
            rbMessages = ResourceBundle.getBundle(
                RESOURCE_BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            outputWriter.printlnError(e.getMessage());
            System.exit(ExitCodes.MISSING_RESOURCE_BUNDLE);
        }

        String defintionFiles = (String)env.get(
            CLIConstants.SYS_PROPERTY_DEFINITION_FILES);
        setupDefinitions(defintionFiles);

        commandName = (String)env.get(CLIConstants.SYS_PROPERTY_COMMAND_NAME);
        if ((commandName == null) || (commandName.length() == 0)) {
            throw new CLIException(rbMessages.getString(
                "exception-message-missing-command-name"),
                ExitCodes.MISSING_COMMAND_NAME);
        }

        outputWriter = (IOutput)env.get(
            CLIConstants.SYS_PROPERTY_OUTPUT_WRITER);
        if (outputWriter == null) {
            throw new CLIException("output writer is not defined.",
                ExitCodes.OUTPUT_WRITER_CLASS_CANNOT_INSTANTIATE);
        }

        environment = new HashMap();
        if (env.get(CLIConstants.ARGUMENT_DEBUG) != null) {
            environment.put(CLIConstants.ARGUMENT_DEBUG, Boolean.TRUE);
        }

        if (env.get(CLIConstants.ARGUMENT_VERBOSE) != null) {
            environment.put(CLIConstants.ARGUMENT_VERBOSE, Boolean.TRUE);
        }

        String webEnabledURL = (String)env.get(CLIConstants.WEB_ENABLED_URL);
        if (webEnabledURL != null) {
            environment.put(CLIConstants.WEB_ENABLED_URL, webEnabledURL);
        }
    }

    private void init(String[] argv)
        throws CLIException
    {
        environment = new HashMap();
        getLocale(argv);

        String defintionFiles = System.getProperty(
            CLIConstants.SYS_PROPERTY_DEFINITION_FILES);
        setupDefinitions(defintionFiles);

        commandName = System.getProperty(
            CLIConstants.SYS_PROPERTY_COMMAND_NAME);
        if ((commandName == null) || (commandName.length() == 0)) {
            throw new CLIException(rbMessages.getString(
                "exception-message-missing-command-name"),
                ExitCodes.MISSING_COMMAND_NAME);
        }

        String outputWriterClassName = System.getProperty(
            CLIConstants.SYS_PROPERTY_OUTPUT_WRITER);
        getOutputWriter(outputWriterClassName);

        if (getFlag(argv, CLIConstants.ARGUMENT_DEBUG,
            CLIConstants.SHORT_ARGUMENT_DEBUG)
        ) {
            environment.put(CLIConstants.ARGUMENT_DEBUG, Boolean.TRUE);
        }

        if (getFlag(argv, CLIConstants.ARGUMENT_VERBOSE,
            CLIConstants.SHORT_ARGUMENT_VERBOSE)
        ) {
            environment.put(CLIConstants.ARGUMENT_VERBOSE, Boolean.TRUE);
        }

        if (getFlag(argv, CLIConstants.ARGUMENT_NOLOG,
            CLIConstants.SHORT_ARGUMENT_NOLOG)
        ) {
            environment.put(CLIConstants.ARGUMENT_NOLOG, Boolean.TRUE);
        }
    }

    private void setupDefinitions(String defintionFiles)
        throws CLIException
    {
        if ((defintionFiles == null) || (defintionFiles.length() == 0)) {
            throw new CLIException(rbMessages.getString(
                "exception-message-missing-definition-class"),
                ExitCodes.MISSING_DEFINITION_FILES);
        }
        initDefinitions(defintionFiles);
    }

    private void initDefinitions(String definitionClassNames)
        throws CLIException
    {
        if (isVerbose()) {
            outputWriter.printlnMessage(rbMessages.getString(
                "verbose-reading-definition-files"));
        }
        definitionObjects = new ArrayList();
        StringTokenizer st = new StringTokenizer(definitionClassNames, ",");
        while (st.hasMoreTokens()) {
            String className = st.nextToken();
            getDefinitionObject(className);
        }
    }

    private void getDefinitionObject(String className)
        throws CLIException
    {
        try {
            Class clazz = Class.forName(className);
            IDefinition defClass = (IDefinition)clazz.newInstance();
            definitionObjects.add(defClass);
        } catch (ClassNotFoundException e) {
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-definition-class-not-found"), param);
            throw new CLIException(message, ExitCodes.MISSING_DEFINITION_CLASS);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-illegal-access-definition-class"),
                    param);
            throw new CLIException(message,
                ExitCodes.ILLEGEL_ACCESS_DEFINITION_CLASS);
        } catch (InstantiationException e) {
            e.printStackTrace();
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-instantiation-definition-class"), param);
            throw new CLIException(message,
                ExitCodes.INSTANTIATION_DEFINITION_CLASS);
        } catch (ClassCastException e) {
            Object[] param = {className};
            String message = MessageFormat.format(
                rbMessages.getString(
                    "exception-message-class-cast-definition-class"), param);
            throw new CLIException(message,
                ExitCodes.CLASS_CAST_DEFINITION_CLASS);
        }
    }

    /**
     * Returns resource bundle.
     *
     * @return resource bundle.
     */
    public ResourceBundle getResourceBundle() {
        return rbMessages;
    }

    /**
     * Returns commandline interface name.
     *
     * @return commandline interface name.
     */
    public String getCommandName() {
        return commandName;
    }

    /**
     * Returns a list of definition objects. Since this class is just
     * a engine, it requires definition objects to dictate the behavior
     * of the CLI.
     *
     * @return a list of definition objects.
     */
    public List getDefinitionObjects() {
        return definitionObjects;
    }

    /**
     * Returns <code>true</code> is log is turned off.
     *
     * @return <code>true</code> is log is turned off.
     */
    public boolean isLogOff() {
        return (environment.get(CLIConstants.ARGUMENT_NOLOG) != null);
    }

    /**
     * Returns locale object.
     *
     * @return locale object.
     */
    public Locale getLocale() {
        return (Locale)environment.get(CLIConstants.ARGUMENT_LOCALE);
    }

    /**
     * Returns true of the CLI has verbose set.
     *
     * @return true of the CLI has verbose set.
     */
    public boolean isVerbose() {
        return (environment.get(CLIConstants.ARGUMENT_VERBOSE) != null);
    }

    /**
     * Returns true of the CLI has debug turned on.
     *
     * @return true of the CLI has debug turned on.
     */
    public boolean isDebugOn() {
        return (environment.get(CLIConstants.ARGUMENT_DEBUG) != null);
    }

    /**
     * Returns debugger.
     *
     * @return debugger.
     */
    static Debug getDebugger() {
        return debugger;
    }

    /**
     * Returns output writer.
     *
     * @return output writer.
     */
    public IOutput getOutputWriter() {
        return outputWriter;
    }

    /**
     * Returns the sub command of a given name.
     *
     * @param name Name of Sub Command.
     * @return the sub command.
     */
    public SubCommand getSubCommand(String name) {
        SubCommand subcmd = null;
        for (Iterator i = definitionObjects.iterator();
            i.hasNext() && (subcmd == null);
        ) {
            IDefinition def = (IDefinition)i.next();
            subcmd = def.getSubCommand(name);
        }
        return subcmd;
    }

    private void getOutputWriter(String className)   
        throws CLIException
    {
        try {
            if ((className == null) || (className.length() == 0)) {
                outputWriter = (IOutput)OutputWriter.class.newInstance();
            } else {
                outputWriter = (IOutput)Class.forName(className).newInstance();
            }
        } catch (Exception e) {
            String[] param = {className};
            String msg = "Cannot construct output writer {0}.";
            // cannot localize yet - have not gotten resource bundle
            throw new CLIException(MessageFormat.format(msg, (Object[])param),
                ExitCodes.OUTPUT_WRITER_CLASS_CANNOT_INSTANTIATE);
        }
    }

    /**
     * Services the request queue.
     *
     * @throws CLIException if request cannot be processed.
     */
    public void serviceRequestQueue()
        throws CLIException {
        if (isVerbose()) {
            outputWriter.printlnMessage(
                rbMessages.getString("verbose-processing-request"));
        }

        while (!requestQueue.isEmpty()) {
            CLIRequest req = (CLIRequest)requestQueue.remove(0);
            try {
                req.process(this);
            } catch (CLIException e) {
                if (bContinue) {
                    outputWriter.printlnError(e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    private static boolean getFlag(
        String[] argv,
        String longName,
        String shortName
    ) throws CLIException
    {
        boolean flag = false;
        for (int i = 0; (i < argv.length) && !flag; i++) {
            String s = argv[i];
            if (s.equals(CLIConstants.PREFIX_ARGUMENT_LONG + longName) ||
                s.equals(CLIConstants.PREFIX_ARGUMENT_SHORT + shortName)
            ) {
                flag = true;
                int nextIdx = i+1;
                if (nextIdx < argv.length) {
                    String str = argv[nextIdx];
                    if (!str.startsWith(CLIConstants.PREFIX_ARGUMENT_SHORT)){
                        throw new CLIException(
                            "Incorrect " + longName + " option.",
                            ExitCodes.INCORRECT_OPTION);
                    }
                }
            }
        }
        return flag;
    }

    private void getLocale(String[] argv)
        throws CLIException
    {
        Locale locale = null;
        for (int i = 0; (i < argv.length) && (locale == null); i++) {
            String s = argv[i];
            if (s.equals(
                    CLIConstants.PREFIX_ARGUMENT_LONG +
                    CLIConstants.ARGUMENT_LOCALE) ||
                s.equals(
                    CLIConstants.PREFIX_ARGUMENT_SHORT +
                    CLIConstants.SHORT_ARGUMENT_LOCALE)
            ) {
                int nextIdx = i+1;
                if (nextIdx >= argv.length) {
                    throw new CLIException("Incorrect locale option.",
                        ExitCodes.INCORRECT_OPTION);
                } else {
                    String strLocale = argv[nextIdx];
                    if (strLocale.startsWith(
                        CLIConstants.PREFIX_ARGUMENT_SHORT)
                    ) {
                        throw new CLIException("Incorrect locale option.",
                            ExitCodes.INCORRECT_OPTION);
                    } else {
                        locale = getLocale(strLocale);
                    }
                }
            }
        }

        if (locale == null) {
            locale = Locale.getDefault();
        }

        environment.put(CLIConstants.ARGUMENT_LOCALE, locale);

        try {
            rbMessages = ResourceBundle.getBundle(
                RESOURCE_BUNDLE_NAME, locale);
        } catch (MissingResourceException e) {
            outputWriter.printlnError(e.getMessage());
            System.exit(ExitCodes.MISSING_RESOURCE_BUNDLE);
        }
    }

    private static Locale getLocale(String strLocale) {
        StringTokenizer st = new StringTokenizer(strLocale, "_");
        String lang = (st.hasMoreTokens()) ? st.nextToken() : "";
        String country = (st.hasMoreTokens()) ? st.nextToken() : "";
        String variant = (st.hasMoreTokens()) ? st.nextToken() : "";
        return new Locale(lang, country, variant);
    }

    private static void getIsInstallTime() {
        String strInstallTime = System.getProperty("installTime");

        if ((strInstallTime != null) && (strInstallTime.trim().length() > 0)) {
            if (strInstallTime.trim().toLowerCase().equals("true")) {
                /*
                 * Set the property to inform AdminTokenAction that
                 * "amadmin" CLI is executing the program
                 */
                SystemProperties.initializeProperties(
                    AdminTokenAction.AMADMIN_MODE, "true");
            }
        }
    }

    /**
     * Sets/Resets the continue flag. Queue of requests shall be processed 
     * in the event that one or more requests are errornous if this flag is
     * set. On the other hand, queue of requests shall be terminated at the
     * first encountered errorous request if this flag is reset.
     *
     * @param bContinue Continue status flag.
     */
    public void setContinueFlag(boolean bContinue) {
        this.bContinue = bContinue;
    }

    /**
     * Adds request to request queue.
     *
     * @param request CLI Request object to be added to the queue.
     */
    public void addToRequestQueue(CLIRequest request) {
        requestQueue.add(request);
    }

    /**
     * Returns Web enabled URL.
     *
     * @return Web enabled URL.
     */
    public String getWebEnabledURL() {
        return (String)environment.get(CLIConstants.WEB_ENABLED_URL);
    }


    /**
     * Returns <code>true</code> if command manager is created from JSP.
     *
     * @returns <code>true</code> if command manager is created from JSP.
     */
    public boolean webEnabled() {
        String url = getWebEnabledURL();
        return (url != null) && (url.length() > 0);
    }
}
