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
 * $Id: PasswordValidator.java,v 1.1 2006-09-28 07:37:32 rarcot Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.install.tools.configurator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class PasswordValidator extends ValidatorBase {

    public PasswordValidator() throws InstallException {
        super();
    }

    public ValidationResult isPasswordValid(String passfileName, Map props,
            IStateAccess state) {

        ValidationResultStatus validRes = ValidationResultStatus.STATUS_FAILED;
        LocalizedMessage returnMessage = null;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(passfileName));
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                if (lineCount == 0) {
                    lineCount++;
                    int passLen = line.length();
                    String minSize = (String) props.get(STR_VAL_MIN_DIGITS);
                    String maxSize = (String) props.get(STR_IN_MAX_DIGITS);

                    if ((minSize != null) && (minSize.length() > 0)
                            && (maxSize != null) && (maxSize.length() > 0)) {

                        int minLen = Integer.parseInt(minSize);
                        int maxLen = Integer.parseInt(maxSize);

                        Debug.log("PasswordValidator : Password : "
                                + " Min length = " + minLen + ", "
                                + "Max Length = " + maxLen);
                        if (maxLen > minLen) {
                            if ((passLen >= minLen) && (passLen <= maxLen)) {
                                validRes = 
                                    ValidationResultStatus.STATUS_SUCCESS;

                            } else {
                                Debug.log("PasswordValidator : Length of "
                                        + "password field is invalid");
                            }
                        }
                    } else {
                        // min and max not present; so if length of pass > 0
                        // it will be valid pass
                        validRes = ValidationResultStatus.STATUS_SUCCESS;
                        Debug.log("Password entry is valid");
                    }
                } else {
                    Debug.log("PasswordValidator : Invalid password file"
                            + " format, file had more than one line.");
                    validRes = ValidationResultStatus.STATUS_FAILED;
                    break;
                }
            }
        } catch (Exception ex) {
            Debug.log("PasswordValidator : Failed to read password with ex :",
                    ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    Debug.log("PasswordValidator : Failed to close "
                            + "password file :", ex);
                }
            }
        }

        if (validRes.getIntValue() == ValidationResultStatus.INT_STATUS_FAILED)
        {
            returnMessage = LocalizedMessage.get(LOC_VA_WRN_IN_VAL_PASS);
        }

        Debug.log("PasswordValidator : Is password valid ? "
                + validRes.isSuccessful());
        return new ValidationResult(validRes, null, returnMessage);
    }

    public void initializeValidatorMap() throws InstallException {

        Class[] paramObjs = { String.class, Map.class, IStateAccess.class };

        try {
            getValidatorMap().put("VALID_PASSWORD",
                    this.getClass().getMethod("isPasswordValid", paramObjs));

        } catch (NoSuchMethodException nsme) {
            Debug.log("PasswordValidator: "
                    + "NoSuchMethodException thrown while loading method :",
                    nsme);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), nsme);
        } catch (SecurityException se) {
            Debug.log("PasswordValidator: "
                    + "SecurityException thrown while loading method :", se);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), se);
        } catch (Exception ex) {
            Debug.log("PasswordValidator: "
                    + "Exception thrown while loading method :", ex);
            throw new InstallException(LocalizedMessage
                    .get(LOC_VA_ERR_VAL_METHOD_NOT_FOUND), ex);
        }

    }

    /** Hashmap of Validator names and integers */
    Map validMap = new HashMap();

    /*
     * Localized constants
     */
    public static String LOC_VA_WRN_IN_VAL_PASS = "VA_WRN_IN_VAL_PASS";

    /*
     * String constants
     */
    public static String STR_VAL_MIN_DIGITS = "minLen";

    public static String STR_IN_MAX_DIGITS = "maxLen";

}
