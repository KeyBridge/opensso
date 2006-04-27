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
 * $Id: LogManagerUtil.java,v 1.2 2006-04-27 07:53:30 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.log;

import java.util.logging.LogManager;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.naming.WebtopNaming;

/**
 * This class is a work around for situations where our
 * log manager conflicts with an existing log manager in
 * the container.
 */
public class LogManagerUtil {
    private static LogManager lmgr = null;
    static {
       /*
        * Uses AM's log manager if in Server Mode
        * or COMPATMODE is OFF (in case of client side)
        */
       String compatMode = SystemProperties.get("LOG_COMPATMODE", "Off");
       if ((compatMode.trim().equalsIgnoreCase("Off")) ||
           WebtopNaming.isServerMode()) {
           lmgr = new com.sun.identity.log.LogManager();
       }
    }

    /**
     * Returns a local LogManager object if LOG_COMPATMODE
     * environment variable is set to "Off". Otherwise returns
     * the global LogManager in the JVM.
     *
     * @return LogManager object.
     */
    public static LogManager getLogManager() {
        if (lmgr != null) {
            return lmgr;
        } else {
            return java.util.logging.LogManager.getLogManager();
        }
    }

    static String oldcclass = null;
    static String newcclass = null;
    static String oldcfile = null;
    static String newcfile = null;

    /**
     * Sets up the log configuration reader class or file in the
     * environment, so that our LogManager's custom configuration
     * will be read.
     */
    public static void setupEnv() {
        if (lmgr != null) {
            oldcclass = SystemProperties.get("java.util.logging.config.class");
            newcclass = SystemProperties.get(
                "s1is.java.util.logging.config.class");
            oldcfile = SystemProperties.get("java.util.logging.config.file");
            newcfile = SystemProperties.get(
                "s1is.java.util.logging.config.file");
            try {
                if (newcclass != null) {
                    System.setProperty("java.util.logging.config.class", 
                        newcclass);
                }
                if (newcfile != null) {
                    System.setProperty("java.util.logging.config.file", 
                        newcfile);
                }
            } catch (Throwable err) {
            }
        }
    }

    /**
     * Resets the environment to the default one.
     */
    public static void resetEnv() {
        if (lmgr != null) {
            if (oldcclass != null) {
                System.setProperty("java.util.logging.config.class",
                    oldcclass);
            }
            if (oldcfile != null) {
                System.setProperty("java.util.logging.config.file",oldcfile);
            }
        }
    }
}
