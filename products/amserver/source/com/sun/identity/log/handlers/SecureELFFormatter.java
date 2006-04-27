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
 * $Id: SecureELFFormatter.java,v 1.3 2006-04-27 07:53:32 veiming Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */



package com.sun.identity.log.handlers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Handler;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.secure.MACGenerator;
import com.sun.identity.log.spi.Debug;
import com.sun.identity.log.spi.IGenerator;
import com.sun.identity.log.spi.ITimestampGenerator;

/**
 * This Formatter is the custom defined Formatter for DSAME which formats the
 * logRecord according to ELF. For details of ELF specification pls refer
 * www.w3.org/TR/WD-logfile.html. This Formatter will put the version number
 * and Fields as the first 2 lines and the data corresponding to each field
 * follows. If a field is not selected a "-" is put.
 */
public class SecureELFFormatter extends Formatter {
    
    private LogManager lmanager = (com.sun.identity.log.LogManager)
    LogManagerUtil.getLogManager();
    private String [] allFields;
    private IGenerator fieldGenerator = new MACGenerator();
    private ITimestampGenerator secureTimestampGenerator;
    
    /**
     * returns a ELF Formatter
     */
    public SecureELFFormatter() {
        
        String timestampGeneratorClass = 
            lmanager.getProperty(LogConstants.SECURE_TIMESTAMP_GENERATOR);
        try {
            Class clz = Class.forName(timestampGeneratorClass);
            secureTimestampGenerator = (ITimestampGenerator)clz.newInstance();
        } catch (ClassNotFoundException cnfe) {
            Debug.error("SecureELFFormatter: TimeStamp Generator Class " +
                "not found", cnfe);
        } catch (InstantiationException ie) {
            Debug.error("SecureELFFormatter: Timestamp Generator Could " +
                "not be Instantiated", ie);
        } catch (IllegalAccessException iae) {
            Debug.error("SecureELFFormatter: Timestamp Generator Could " +
                "not be Instantiated", iae);
        }
    }
    
    private void getAllFields() {
        String strAllFields = lmanager.getProperty(LogConstants.ALL_FIELDS);
        StringTokenizer strToken = new StringTokenizer(strAllFields, ", ");
        int count = strToken.countTokens();
        allFields = new String[count];
        count = 0;
        while(strToken.hasMoreElements()) {
            allFields[count++] = strToken.nextToken().trim();
        }
        String temp = "";
        for ( int i = 0; i < count; i ++ ) {
            temp += allFields[i] + "\t";
        }
    }
    
    /**
     * Format the given record as per ELF and return a formatted string. <p>
     *
     * For ELF Specifications refer <LI> www.w3.org/TR/WD-logfile.html</LI>
     * @param logRecord The logRecord to be formatted
     * @return The string formed by formatting the logRecord
     */
    public String format(java.util.logging.LogRecord logRecord) {
        StringBuffer sbuffer = new StringBuffer();
        StringBuffer stringForMAC = new StringBuffer();
        Map logInfoTable = 
            ((com.sun.identity.log.LogRecord)logRecord).getLogInfoMap();
        try {
            getAllFields();
            Set selectedFields = getSelectedFieldSet();
            String strTime = secureTimestampGenerator.getTimestamp();
            sbuffer.append("\"").append(strTime).append("\"\t");
            stringForMAC.append("\"").append(strTime).append("\"");
            String message = processString(formatMessage(logRecord));
            if ((message.indexOf(' ') != -1) || (message.indexOf('\t') != -1)) {
                sbuffer.append("\"").append(message).append("\"\t");
                stringForMAC.append("\"").append(message).append("\"");
            } else {
                sbuffer.append(message).append("\t");
                stringForMAC.append(message);
            }
            
            String key = null;
            String value = null;
            for (int i = 2; i < allFields.length; i ++) {
                key = allFields[i];
                if ((key != null) && (key.length() != 0) &&
                (selectedFields != null) &&
                (logInfoTable != null) &&
                (selectedFields.contains(key))) {
                    value = (String) logInfoTable.get(key);
                    if ((value != null) && (value.length() != 0)) {
                        value = processString(value);
                    } else {
                        value = "Not Available";
                    }
                    // the following check is to check if the string has
                    // a white space ... if it does then it should be
                    // enclosed within quotes.
                    if ((value.indexOf(' ') != -1) ||
                        (value.indexOf('\t') != -1)
                    ) {
                        sbuffer.append("\"").append(value).append("\"\t");
                        stringForMAC.append("\"").append(value).append("\"");
                    } else {
                        sbuffer.append(value).append("\t");
                        stringForMAC.append(value);
                    }
                } else {
                    sbuffer.append("-").append("\t");
                    stringForMAC.append("-");
                }
                
            }
        } catch (Exception e) {
            Debug.error("SecureELFFormatter: Exception in String Handling", e);
        }
        // adding Secure logging related code here. Check whether the security
        // status is "on", if yes then add MAC field and SIGN field to the
        // headers and also generate MAC and write it to the file.
        String key = LogConstants.SIGNATURE_FIELDNAME;
        if (logInfoTable.containsKey(key)) {
            sbuffer.append("-\t").append(logInfoTable.get(key));
        } else {
            try {
                Object [] obj = {logRecord.getLoggerName()};
                String mac = 
                   fieldGenerator.generateLogField(stringForMAC.toString(),obj);
                sbuffer.append(mac).append("\t").append("-");
            } catch (Exception e) {
                Debug.error("SecureLFFormatter: couldnot generate mac", e);
                sbuffer.append("\"MAC NOT AVAILABLE\"")
                       .append("\t").append("-");
            }
        }
        sbuffer.append("\n");
        return sbuffer.toString();
    }
    
    /**
     * According to ELF, the log file should start with a #Version which
     * specifies the ELF version used followed by a #Fields line which specifies
     * all the fields that are being logged.
     * <p>
     * <tt> SecureELFFormatter </tt> uses this method to return back the header
     * containing these two lines. <tt> FileHandler </TT> checks if the header
     * is already written, if not uses this method to get the header and puts it
     * at the beginning of the file.
     * @param handler The target handler (can be null)
     * @return the set of all fields converted into a # prefixed fields string.
     */
    public String getHead(Handler handler) {
        StringBuffer sbuffer = new StringBuffer();
        sbuffer.append("#Version: 1.0").append("\n");
        sbuffer.append("#Fields: ").append(constructHeader()).append("\n");
        // to be done: append correct Fields after gettting from logmanager.
        return sbuffer.toString();
    }
    
    /**
     * Returns an empty string.
     * @param handler The target handler (can be null)
     * @return an empty string.
     */
    public String getTail(Handler handler) {
        return "";
    }
    
    private String constructHeader() {
        StringBuffer sbuffer = new StringBuffer();
        for (int i = 0; i < allFields.length; i ++) {
            sbuffer.append(allFields[i]).append("\t");
        }
        sbuffer.append(LogConstants.MAC_FIELDNAME).append("\t");
        sbuffer.append(LogConstants.SIGNATURE_FIELDNAME);
        
        return sbuffer.toString();
    }
    
    private Set getSelectedFieldSet() {
        Set selectedFields = new HashSet();
        String strSelectedFields = 
            lmanager.getProperty(LogConstants.LOG_FIELDS);
        
        if ((strSelectedFields != null) && (strSelectedFields.length() != 0)) {
            StringTokenizer stoken = 
                new StringTokenizer(strSelectedFields, ", ");
            while(stoken.hasMoreElements()) {
                selectedFields.add( stoken.nextToken());
            }
        }
        return selectedFields;
    }
    /**
     * This method is used to process each field to see if it has a quote,
     * if it does then append another quote. If any field has a \r or \n
     * replace them by \\r and \\n. This is to take care of the multiline
     * messages.
     */
    private String processString(String field) {
        StringBuffer sbuffer = new StringBuffer();
        int len = field.length();
        boolean hasUniqueChar = false;
        for (int i = 0; i < len; i ++) {
            char currentCharacter = field.charAt(i);
            if (currentCharacter == '"') {
                sbuffer.append("\"\""); // appends 2 quotes if it finds one.
                hasUniqueChar = true;
            }
            if (currentCharacter == '\r') {
                sbuffer.append("\\\\r"); // append \\r if it finds \r
                hasUniqueChar = true;
            }
            if (currentCharacter == '\n') {
                sbuffer.append("\\\\n"); // append \\n if it finds a \n
                hasUniqueChar = true;
            }
            if (!hasUniqueChar) {
                sbuffer.append(currentCharacter);
            }
            hasUniqueChar = false;
        }
        return sbuffer.toString();
    }
}
