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
 * $Id: Migrate.java,v 1.1 2008-01-24 00:20:18 bina Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.upgrade.MigrateTasks;
import com.sun.identity.upgrade.UpgradeException;
import com.sun.identity.upgrade.UpgradeUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Updates <code>iPlanetAMLoggingService</code> service schema.
 * This class is invoked during migration from older versions
 * of Access Manager to the latest version.
 */
public class Migrate implements MigrateTasks {

    final static String SERVICE_NAME = "iPlanetAMLoggingService";
    final static String SERVICE_DIR = "50_iPlanetAMLoggingService/40_50";
    final static String SCHEMA_FILE = "amLogging_addAttrs.xml";
    final static String LOG_ATTR_NAME = "iplanet-am-logging-logfields";
    final static String LOG_ALL_FIELDS_ATTR_NAME =
            "iplanet-am-logging-allfields";
    final static String LOG_STATUS_ATTR = "logStatus";
    final static String RESOLVE_HOSTNAME_ATTR = "resolveHostName";
    final static String schemaType = "Global";

    /**
     * Updates the <code>iPlanetAMLoggingService<code> service schema.
     *
     * @return true if successful otherwise false.
     */
    public boolean migrateService() {
        boolean isSuccess = false;
        try {
            //read the values from AMConfig.properties
            String logStatus =
                    SystemProperties.get(
                    "com.iplanet.am.logStatus");
            String resolveHostName =
                    SystemProperties.get(
                    "com.sun.identity.log.resolveHostName");

            // update default value
            Set defaultValues = new HashSet();
            defaultValues.add("NameID");
            UpgradeUtils.addAttributeDefaultValues(
                    SERVICE_NAME, null, schemaType,
                    LOG_ALL_FIELDS_ATTR_NAME, defaultValues);

            // update choice value
            Map choiceValuesMap = new HashMap();
            choiceValuesMap.put("choiceNameID", defaultValues);
            UpgradeUtils.addAttributeChoiceValues(
                    SERVICE_NAME, null, schemaType,
                    LOG_ATTR_NAME, choiceValuesMap);

            // add new attribute schema
            String fileName =
                    UpgradeUtils.getAbsolutePath(SERVICE_DIR, SCHEMA_FILE);
            UpgradeUtils.addAttributeToSchema(
                    SERVICE_NAME, schemaType, fileName);

            // populate the default values for logstatus & resolveHostName
            // from amconfig.properties
            defaultValues.clear();
            defaultValues.add(logStatus);
            UpgradeUtils.setAttributeDefaultValues(
                    SERVICE_NAME, null, schemaType,
                    LOG_STATUS_ATTR, defaultValues);
            defaultValues.clear();
            defaultValues.add(logStatus);
            UpgradeUtils.setAttributeDefaultValues(
                    SERVICE_NAME, null, schemaType,
                    RESOLVE_HOSTNAME_ATTR, defaultValues);
            isSuccess = true;
        } catch (UpgradeException e) {
            UpgradeUtils.debug.error("Error loading data:" + SERVICE_NAME, e);
        }
        return isSuccess;
    }

    /**
     * Post Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean postMigrateTask() {
        return true;
    }

    /**
     * Pre Migration operations.
     *
     * @return true if successful else error.
     */
    public boolean preMigrateTask() {
        return true;
    }
}
