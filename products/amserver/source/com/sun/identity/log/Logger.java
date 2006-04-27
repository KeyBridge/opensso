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
 * $Id: Logger.java,v 1.2 2006-04-27 07:53:30 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ReaderWriterLock;
import com.sun.identity.log.spi.Authorizer;
import com.sun.identity.log.spi.Debug;

/**
 * Sun Java System Access Manager extension to the jdk1.4 Logger
 * This extension gives some functionality required by
 * Sun Java System Access Manager secure logger.
 * For <code>JDK1.4</code> Logger please refer to
 * <pre>
 * http://java.sun.com/j2se/1.4.1/docs/api/java/util/logging/Logger.html
 * </pre>
 * @supported.all.api
 */
public class Logger extends java.util.logging.Logger {
    
    private String currentFileName = new String();
    private static LogManager lm;
    private String logName;
    protected static boolean resolveHostName;

    /**
     * Lock to prevent parallel writing and reading at the same time.
     */
    public static ReaderWriterLock rwLock = new ReaderWriterLock();
    
    static {
        lm = (com.sun.identity.log.LogManager) LogManagerUtil.getLogManager();
        try {
            lm.readConfiguration();
        } catch (Exception ex) {
            /* our Debug system will no be up now, so can't Debug */
        }
        String location = lm.getProperty(LogConstants.LOG_LOCATION);
        String type = lm.getProperty(LogConstants.BACKEND);
        if ((location != null) && type.equals("File")) {
            File dir = new File(location);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Debug.error("Logger:Creation of Log Directory failed: " +
                        location);
                }
            }
        }

        /* Check if hostnames have to be resolved */
        resolveHostName = Boolean.valueOf(SystemProperties.get(
            LogConstants.LOG_RESOLVE_HOSTNAME, "true")).booleanValue();
    }
    
    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers true.
     *
     * @param name A name for the logger.  This should be a
     *        dot-separated name and should normally be based on the
     *        package name or class name of the subsystem, such as java.net
     *        or javax.swing.  It may be null for anonymous Loggers.
     * @param resourceBundleName Name of the ResourceBundle to be used for
     *        localizing messages for this logger.  May be null if none
     *        of the messages require localization.
     * @throws MissingResourceException if the ResourceBundleName is
     *         non-null and no corresponding resource can be found.
     */
    protected Logger(String name,String resourceBundleName) {
        super(name,resourceBundleName);
    }

    /**
     * To add handlers and formatters to the new logger object
     */
    private static void processNewLoggerObject (Logger result) {
        Formatter formatter = null;
        String handlerClass = LogManager.HANDLER;
        String formatterClass = LogManager.FORMATTER;
        String levelProp = LogConstants.LOG_PROP_PREFIX + "." +
            result.logName + ".level";
        String levelString = lm.getProperty(levelProp);
        if (levelString != null) {
            result.setLevel(Level.parse(levelString));
        } else {
            levelString = SystemProperties.get (levelProp);
            if (levelString == null) {
                levelString = "INFO";
            }
            result.setLevel(Level.parse(levelString));
        }
        String logStatus = lm.getProperty(LogConstants.LOG_STATUS);
        if (logStatus != null && logStatus.startsWith("INACTIVE")) {
            result.setLevel(Level.OFF);
        }
        Class clz = null;
        Class [] parameters = {String.class};
        Object [] parameterObjects = {new String(result.logName)};
        Constructor cons = null;
        Handler handler = null;
        
        if (handlerClass == null) {
            Debug.error("Logger:processNewLoggerObject:" +
                "HandlerClass not in classpath ");
            return;
        }
        try {
            clz = Class.forName(handlerClass);
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "HandlerClass not in classpath: " + handlerClass, e);
            return;
        }
        try {
            if(clz != null) {
                cons = clz.getDeclaredConstructor(parameters);
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "constructor parameter mismatch ", e);
            return;
        }
        try {
            if(cons != null) {
                handler = (Handler) cons.newInstance(parameterObjects);
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not instantiate handler: " + handlerClass, e);
            return;
        }
        if (formatterClass == null) {
            Debug.error("Logger:processNewLoggerObject:" +
                "formatterClass not in classpath ");
            return;
        }
        try {
            clz = Thread.currentThread().getContextClassLoader().
                loadClass(formatterClass);
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not load Formatter Class: " + formatterClass, e);
            return;
        }
        try {
            if(clz != null) {
                formatter = (Formatter) clz.newInstance();
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not get Formatter instance " + formatterClass, e);
            return;
        }
        try {
            handler.setFormatter(formatter);
            result.addHandler(handler);
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Unable to add Handler", e);
            return;
        }
        String filterClassName = lm.getProperty(LogConstants.FILTER_CLASS_NAME);
        try {
            if (filterClassName != null) {
                Filter filter =
                    (Filter)Class.forName(filterClassName).newInstance();
                result.setFilter(filter);
            }
        } catch (Exception e) {
            Debug.error("Logger:processNewLoggerObject:" +
                "Could not set Filter: "+ filterClassName, e);
        }
        
        result.setUseParentHandlers(false);
    }
    
    /**
     * Directs every log call to <code>log(LogRecord, Object)</code>
     * And thus the default authorization check does not allow logging 
     * when an application uses this interface.
     *
     * @param record The <code>LogRecord</code> to be logged.
     */
    public void log(LogRecord record) {
        log(record, null);
    }

    /**
     * Calls super.log after checking authorization.
     * Data is not logged at all if this check fails.
     *
     * @param record The <code>LogRecord</code> to be logged.
     * @param cred To prove authorization for log WRITE.
     *        The default authorization hook checks validity of the single
     *        sign on token which should be passed as the <code>cred</code>.
     */
    public void log(LogRecord record, Object cred) {
        String logName = this.getName();

        if (!LogManager.isLocal) {        
            if (cred == null) {
                /* In case of remote sso token must be provide. */
                Debug.error("Logger:log:" + logName + 
                    ": remote logging, ssoToken is null; Will not log");
                return;
            }
        } else {        
            /* Authorizer need not be called in the case of remote. */
            if (!Authorizer.isAuthorized(logName, "MODIFY", cred)) {
                Debug.error("Logger:log:" + logName + 
                    ": authorization failed; Will not log");
                throw new AMLogException(logName + ":" +
                    AMLogException.LOG_WRT_AUTH_FAILED);
            }
        }
        /* add LoggedBy info */
        SSOToken ssoToken = null;
        com.sun.identity.log.LogRecord ourRecord = 
            (com.sun.identity.log.LogRecord)record;
        if (cred instanceof SSOToken) {
            ssoToken = (SSOToken)cred;
            String loggedBySID = ssoToken.getTokenID().toString();
            ourRecord.addLogInfo(LogConstants.LOGGED_BY_SID, loggedBySID);
            String clientID = null;
            try {
                clientID = ssoToken.getPrincipal().getName();
            } catch (SSOException ssoe) {
                Debug.error("Logger:log:" + logName + 
                    ": could not get clientID from ssoToken:", ssoe);
            }
            ourRecord.addLogInfo(LogConstants.LOGGED_BY, clientID);
        } 
        /* add module name */
        String existModuleName = 
            (String)ourRecord.getLogInfoMap().get(LogConstants.MODULE_NAME);
        if (existModuleName == null || existModuleName.length() <= 0) {
            /* add module name only if it's already not added. */
            ourRecord.addLogInfo(LogConstants.MODULE_NAME, this.getName());
        }
        /*
         * These are normally done by the LogManager private method
         * doLog(). But since this record is not passing through that
         * method we have to explicitly do this.
         * ResourceBundle logic has been simplified.
         */
        ourRecord.setLoggerName(this.getName());
        String rbName = this.getResourceBundleName();
        ResourceBundle bundle = null;
        if (rbName != null) {
            bundle = ResourceBundle.getBundle(rbName);
            ourRecord.setResourceBundle(bundle);
        }
        try {
            rwLock.readRequest();
            /*
             * this is to serialize logging,signing and verifying
             * threads so that no signing or verification takes
             * place once a logging thread has gone past this point
             */
            if(lm.isSecure()) {
                synchronized(this) {
                    super.log(ourRecord);
                }
            } else {
                super.log(ourRecord);
            }
        } catch (Exception ex) {
            Debug.error("Logger.log:" + logName + ":" + ex.getMessage());
            throw new AMLogException(logName + ":" + ex.getMessage());
        } finally {
            rwLock.readDone();
        }
    }

    /** Writes all the buffered log records.
     */
    public void flush() {
        /*
         * Post the LogRecord to all our Handlers, and then to
         * our parents' handlers, all the way up the tree.
         */
        Logger logger = this;
        Handler targets[] = logger.getHandlers();
        if (targets != null) {
            for (int i = 0; i < targets.length; i++) {
                targets[i].flush();
            }
        }
    }
   
    /**
     * Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created its log level will be configured
     * based on the <code>LogManager</code> and it will be configured NOT to
     * send logging output to its parent loggers Handlers.  It will be
     * registered in the <code>LogManager</code> global namespace.
     *
     * @param name A name for the logger.  This should be a dot-separated name
     *        and should be the file name you want to have for your logs,
     *        such as <code>amSSO.access</code>, or audit.
     * @return a suitable <code>Logger</code>.
     */
    public static synchronized java.util.logging.Logger getLogger(String name)
    {
        if ((name == null) || (name.length() == 0) || name.indexOf("..") >= 0)
        {
            /* Do not allow logging if logName has "..". */
            return null;
        }
        Logger result;

        boolean loggerExists = false;
        Enumeration e = lm.getLoggerNames();
        while (e.hasMoreElements()) {
            if (((String) e.nextElement()).equals(name)) {
                loggerExists = true;
            }
        }

        if (loggerExists)  {
            result = (Logger) lm.getLogger(name);
            return result;
        }
        java.util.logging.Logger newLog = (java.util.logging.Logger)
            java.util.logging.Logger.getLogger(name);
        lm.addLogger(newLog);
        result = (Logger) lm.getLogger(name);

        result.logName = name;
        processNewLoggerObject(result);
        return result;
    }
    
    /** Find or create a logger for a named subsystem.  If a logger has
     * already been created with the given name it is returned.  Otherwise
     * a new logger is created.
     * <p>
     * If a new logger is created, its log level will be configured
     * based on the <code>LogManager</code> and it will configured to also
     * send logging output to its parent logger's Handlers.  It will be
     * registered in the <code>LogManager</code> global namespace.
     * <p>
     * If the named Logger already exists and does not yet have a
     * localization resource bundle then the given resource bundle
     * name is used.  If the named Logger already exists and has
     * a different resource bundle name then an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @param name A name for the logger.  This should be a dot-separated name
     *        and should be the file name you want to have for your logs, such
     *        as <code>amSSO.access</code> or audit.
     * @param rbName A resource bundle to be used for localizing the log
     *        messages.
     * @return logger for a named subsystem.
     */
    public static synchronized java.util.logging.Logger getLogger(
        String name, String rbName)
    {
        if ((name == null) || (name.length() == 0) || name.indexOf("..") >= 0) {
            /* Do not allow logging if logName has "..". */
            return null;
        }
        boolean loggerExists = false;
        Enumeration e = lm.getLoggerNames();
        while (e.hasMoreElements()) {
            if (((String) e.nextElement()).equals(name)) {
                loggerExists = true;
            }
        }
        Logger result = (Logger)
        java.util.logging.Logger.getLogger(name, rbName);
        result.logName = name;
        if (loggerExists)  {
            return result;
        }
        /*
         * if the logger is a new object, we have to set the appropriate
         * handlers and formatters to the logger before returning the result.
         */
        
        processNewLoggerObject(result);
        return result;
    }
    
    /**
     * Returns the current file to which the logger's handler is writing.
     * This is useful only in case of file..
     *
     * @return the current file to which the logger's handler is writing.
     */
    public String getCurrentFile() {
        return currentFileName;
    }
    
    /**
     * Set the current file to which the logger's handler is writing.
     *
     * @param fileName name of file.
     */
    public void setCurrentFile(String fileName) {
        currentFileName = fileName;
    }
    
    /**
     * Return whether resolve host name is enabled
     *
     * @return <code>resolveHostName</code> 
     */
    public static boolean resolveHostNameEnabled() {
        return resolveHostName;
    }
}
