/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: TuneAS9Container.java,v 1.1 2008-07-02 18:53:19 kanduls Exp $
 */

package com.sun.identity.tune.impl;

import com.sun.identity.tune.common.FileHandler;
import com.sun.identity.tune.common.MessageWriter;
import com.sun.identity.tune.common.AMTuneException;
import com.sun.identity.tune.common.AMTuneLogger;
import com.sun.identity.tune.config.AS9ContainerConfigInfo;
import com.sun.identity.tune.config.AMTuneConfigInfo;
import com.sun.identity.tune.constants.WebContainerConstants;
import com.sun.identity.tune.intr.TuneAppServer;
import com.sun.identity.tune.util.AMTuneUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class tunes Application server 9.1
 */
public class TuneAS9Container extends TuneAppServer implements 
        WebContainerConstants {
    
    private AMTuneLogger pLogger;
    private MessageWriter mWriter;
    private AMTuneConfigInfo configInfo;
    private AS9ContainerConfigInfo asConfigInfo;
    private Map curCfgMap;
    
    /**
     * Constructs instance of TuneAS9Container
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public TuneAS9Container() throws AMTuneException {
        pLogger = AMTuneLogger.getLoggerInst();
        mWriter = MessageWriter.getInstance();
    }
    
    /**
     * Initialize the configuration data.
     * 
     * @param confInfo
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void initialize(AMTuneConfigInfo confInfo) 
    throws AMTuneException {
        try {
            this.configInfo = confInfo;
            asConfigInfo = (AS9ContainerConfigInfo) confInfo.getWSConfigInfo();
            curCfgMap = asConfigInfo.getCurASConfigInfo();       
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "initialize", 
                    "Error initializing Application server 9.1.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    
    /**
     * Tunes Application server 9.1
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    public void startTuning() 
    throws AMTuneException {
        try {
            mWriter.writelnLocaleMsg("pt-app-tuning-msg");
            mWriter.writeln(LINE_SEP);
            tuneDomainXML();
            mWriter.writeln(PARA_SEP);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "startTuning", 
                    "Error Tuning Application server 9.1.");
            throw new AMTuneException(ex.getMessage());
        } finally {
            try {
                deletePasswordFile();
            } catch (Exception ex) {
                //ignore
            }
        }
    }
    
    /**
     * Tunes domain.xml file
     * 
     * @throws com.sun.identity.tune.common.AMTuneException
     */
    protected void tuneDomainXML() 
    throws AMTuneException {
        try {
            String tuneFile = asConfigInfo.getContainerInstanceDir() + 
                    FILE_SEP + "config" + FILE_SEP + "domain.xml";
            mWriter.writelnLocaleMsg("pt-app-srv-tuning-inst");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile + " (using asadmin command line tool)");
            mWriter.writelnLocaleMsg("pt-param-tuning");
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-as-acceptor-threads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(ACCEPTOR_THREADS + "=" +
                    curCfgMap.get(ACCEPTOR_THREAD_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(ACCEPTOR_THREADS + "=" +
                    configInfo.getAcceptorThreads());
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-as-pending-count-threads-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(MAX_PENDING_COUNT + "=" +
                    curCfgMap.get(COUNT_THREAD_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(MAX_PENDING_COUNT + "=" + AMTUNE_NUM_TCP_CONN_SIZE);
            mWriter.writeln(" ");
            
            mWriter.writelnLocaleMsg("pt-as-queue-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(QUEUE_SIZE + "=" +
                    curCfgMap.get(QUEUE_SIZE_PARAM));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(QUEUE_SIZE + "=" + AMTUNE_NUM_TCP_CONN_SIZE);
            mWriter.writeln(" ");
            
            String asAdminNewMinHeap = MIN_HEAP_FLAG + 
                    configInfo.getMaxHeapSize() + "M";
            String asAdminNewMaxHeap = MAX_HEAP_FLAG + 
                    configInfo.getMaxHeapSize() + "M";
            mWriter.writelnLocaleMsg("pt-as-heap-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.write("Min Heap: " + curCfgMap.get(MIN_HEAP_FLAG));
            mWriter.writeln(" Max Heap: " + curCfgMap.get(MAX_HEAP_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewMinHeap + " " + asAdminNewMaxHeap);
            mWriter.writeln(" ");
            
            String asAdminNewLoggcOutput = GC_LOG_FLAG + ":" +
                    asConfigInfo.getContainerInstanceDir() + FILE_SEP +
                    "logs" + FILE_SEP + "gc.log";
            mWriter.writelnLocaleMsg("pt-as-loggc-output-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(GC_LOG_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewLoggcOutput);
            mWriter.writeln(" ");
            
            String asAdminNewServerMode = SERVER_FLAG;
            mWriter.writelnLocaleMsg("pt-as-server-mode-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            String modeFlag = (String)curCfgMap.get(CLIENT_FLAG);
            String modeFlagToDel = null; 
            if (modeFlag != null && modeFlag.trim().length() > 0) {
                mWriter.writeln((String)curCfgMap.get(CLIENT_FLAG));
                modeFlagToDel = "server.java-config.jvm-options = " + 
                        CLIENT_FLAG;
            } else {
                mWriter.writeln((String)curCfgMap.get(SERVER_FLAG));
            }
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewServerMode);
            mWriter.writeln(" ");
            
            String asAdminNewStackSize = STACK_SIZE_FLAG +
                        configInfo.getFAMTunePerThreadStackSizeInKB() + "k";
            mWriter.writelnLocaleMsg("pt-as-stack-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(STACK_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewStackSize);
            mWriter.writeln(" ");
            
            String asAdminNewNewSize = NEW_SIZE_FLAG + "=" +
                    configInfo.getMaxNewSize() + "M";
            mWriter.writelnLocaleMsg("pt-as-new-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(NEW_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewNewSize);
            mWriter.writeln(" ");

            String asAdminNewMaxNewSize = MAX_NEW_SIZE_FLAG + "=" +
                    configInfo.getMaxNewSize() + "M";
            mWriter.writelnLocaleMsg("pt-as-max-new-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(MAX_NEW_SIZE_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewMaxNewSize);
            mWriter.writeln(" ");

            String asAdminNewDisableExplicitGc = DISABLE_EXPLICIT_GC_FLAG;

            mWriter.writelnLocaleMsg("pt-as-diable-gc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewDisableExplicitGc);
            mWriter.writeln(" ");

            String asAdminNewUseParallelGc = PARALLEL_GC_FLAG;

            mWriter.writelnLocaleMsg("pt-as-use-parallel-gc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(PARALLEL_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewUseParallelGc);
            mWriter.writeln(" ");

            String asAdminNewPrintClassHistogram = HISTOGRAM_FLAG;

            mWriter.writelnLocaleMsg("pt-as-histo-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(HISTOGRAM_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewPrintClassHistogram);
            mWriter.writeln(" ");

            String asAdminNewPrintGcTimeStamps = GC_TIME_STAMP_FLAG;
            mWriter.writelnLocaleMsg("pt-as-gc-time-stamp-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(GC_TIME_STAMP_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewPrintGcTimeStamps);
            mWriter.writeln(" ");

            String asAdminNewUseConMarkSweepGc = MARK_SWEEP_GC_FLAG;
            mWriter.writelnLocaleMsg("pt-as-sweep-mark-gc-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln((String)curCfgMap.get(MARK_SWEEP_GC_FLAG));
            mWriter.writeLocaleMsg("pt-rec-val");
            mWriter.writeln(asAdminNewUseConMarkSweepGc);
            mWriter.writeln(" ");
            String asAdminNewServerpolicy = "";
            if (asConfigInfo.isTuneWebContainerJavaPolicy()) {
                asAdminNewServerpolicy =
                        "${com.sun.aas.instanceRoot}/config/" +
                        "server.policy.NOTUSED";
                mWriter.writelnLocaleMsg("pt-as-server-sec-policy-check-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                mWriter.write(JAVA_SECURITY_POLICY + "=");
                mWriter.writeln((String) curCfgMap.get(JAVA_SECURITY_POLICY));
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.write(JAVA_SECURITY_POLICY + "=");
                mWriter.writeln(asAdminNewServerpolicy);
                mWriter.writeln(" ");
            }
            String asadminNewParallelGCThreads = "";
            if (AMTuneUtil.isNiagara()) {
                asadminNewParallelGCThreads = PARALLEL_GC_THREADS + "=" + 
                        AMTuneUtil.getNumberOfCPUS();
                mWriter.writelnLocaleMsg("pt-as-parallel-gc-threads-msg");
                mWriter.writeLocaleMsg("pt-cur-val");
                mWriter.writeln((String) curCfgMap.get(PARALLEL_GC_THREADS));
                mWriter.writeLocaleMsg("pt-rec-val");
                mWriter.writeln(asadminNewParallelGCThreads);
                mWriter.writeln(" ");
            }
            if (configInfo.isReviewMode()) {
                return;
            }
            AMTuneUtil.backupConfigFile(tuneFile);
            setASParams();
            List delOptList = new ArrayList();
            delOptList.add(curCfgMap.get(MIN_HEAP_FLAG));
            delOptList.add(curCfgMap.get(MAX_HEAP_FLAG));
            delOptList.add(curCfgMap.get(GC_LOG_FLAG));
            delOptList.add(curCfgMap.get(CLIENT_FLAG));
            delOptList.add(curCfgMap.get(STACK_SIZE_FLAG));
            delOptList.add(curCfgMap.get(NEW_SIZE_FLAG));
            delOptList.add(curCfgMap.get(MAX_NEW_SIZE_FLAG));
            delOptList.add(curCfgMap.get(DISABLE_EXPLICIT_GC_FLAG));
            delOptList.add(curCfgMap.get(PARALLEL_GC_FLAG));
            delOptList.add(curCfgMap.get(MARK_SWEEP_GC_FLAG));
            delOptList.add(curCfgMap.get(HISTOGRAM_FLAG));
            delOptList.add(curCfgMap.get(GC_TIME_STAMP_FLAG));
            if (modeFlagToDel != null && modeFlagToDel.trim().length() > 0) {
                delOptList.add(modeFlagToDel);
            }
            if (asConfigInfo.isTuneWebContainerJavaPolicy()) {
                delOptList.add(JAVA_SECURITY_POLICY + "=" + 
                        curCfgMap.get(JAVA_SECURITY_POLICY));
            }
            if (AMTuneUtil.isNiagara()) {
                if (curCfgMap.get(PARALLEL_GC_THREADS) != null &&
                        curCfgMap.get(PARALLEL_GC_THREADS) != 
                        NO_VAL_SET) {
                    delOptList.add(PARALLEL_GC_THREADS + "=" +
                            curCfgMap.get(PARALLEL_GC_THREADS));
                }
            }
            deleteCurJVMOptions(delOptList);
            
            List newOptList = new ArrayList();
            newOptList.add(asAdminNewMinHeap);
            newOptList.add(asAdminNewMaxHeap);
            newOptList.add(asAdminNewLoggcOutput);
            if (modeFlag != null && modeFlag.trim().length() > 0) {
                newOptList.add(asAdminNewServerMode);
            }
            newOptList.add(asAdminNewStackSize);
            newOptList.add(asAdminNewNewSize);
            newOptList.add(asAdminNewMaxNewSize);
            newOptList.add(asAdminNewDisableExplicitGc);
            newOptList.add(asAdminNewUseParallelGc);
            newOptList.add(asAdminNewUseConMarkSweepGc);
            newOptList.add(asAdminNewPrintClassHistogram);
            newOptList.add(asAdminNewPrintGcTimeStamps);
            if (asConfigInfo.isTuneWebContainerJavaPolicy()) {
                newOptList.add(JAVA_SECURITY_POLICY + "=" + 
                        asAdminNewServerpolicy);
            }
            if (AMTuneUtil.isNiagara() &&
                    asadminNewParallelGCThreads != null) {
                newOptList.add(asadminNewParallelGCThreads);
            }
            insertNewJVMOptions(newOptList);
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "tuneDomainXML", 
                    "Error tuning Application server 9.1 domain xml file.");
            throw new AMTuneException(ex.getMessage());
        }
    }
    /**
     * This method Construct a parameter string to perform an asadmin 
     * set for acceptor-thread, queue-size, and count-thread parameters
     */
    private void setASParams() {
        try {
            StringBuffer asAdminSetParams = new StringBuffer();
            asAdminSetParams.append(asConfigInfo.getAcceptorThreadString());
            asAdminSetParams.append("=");
            asAdminSetParams.append(configInfo.getAcceptorThreads());
            asAdminSetParams.append(" ");
            asAdminSetParams.append(COUNT_THREAD_PARAM);
            asAdminSetParams.append("=");
            asAdminSetParams.append(AMTUNE_NUM_TCP_CONN_SIZE);
            asAdminSetParams.append(" ");
            asAdminSetParams.append(QUEUE_SIZE_PARAM);
            asAdminSetParams.append("=");
            asAdminSetParams.append(AMTUNE_NUM_TCP_CONN_SIZE);

            StringBuffer resultBuffer = new StringBuffer();
            StringBuffer setCmd =
                    new StringBuffer(asConfigInfo.getASAdminCmd());
            setCmd.append("set ");
            setCmd.append(asConfigInfo.getAsAdminCommonParamsNoTarget());
            setCmd.append(" ");
            setCmd.append(asAdminSetParams.toString());
            int retVal = AMTuneUtil.executeCommand(setCmd.toString(),
                    resultBuffer);
            if (retVal != 0) {
                mWriter.writelnLocaleMsg("pt-set-param-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                pLogger.log(Level.SEVERE, "setASParams", "Error executing " +
                        "asadmin " + resultBuffer.toString());
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "setASParams",
                    "Application Server Parameters couldn't be set. " +
                    ex.getMessage());
        }
    }
    
    /**
     * Deletes current JVM options using asadmin.
     * @param curJvmOptions List of Application server 9.2 options.
     */
    private void deleteCurJVMOptions(List curJvmOptions) {
        try {
            StringBuffer delOpts = new StringBuffer();
            if (AMTuneUtil.isWindows2003()) {
                delOpts.append(" \"");
            } else {
                delOpts.append(" :");
            }
            Iterator optItr = curJvmOptions.iterator();
            while (optItr.hasNext()) {
                String val = optItr.next().toString().trim();
                if (val.length() > 0 && 
                        !val.equalsIgnoreCase(NO_VAL_SET)) {
                    delOpts.append(AS_PARAM_DELIM);
                    val = val.replace(AS_PARAM_DELIM, "\\" + AS_PARAM_DELIM);
                    delOpts.append(val);
                }
            }
            if (AMTuneUtil.isWindows2003()) {
                delOpts.append("\"");
            }
            if(delOpts.toString().trim().length() < 3) {
                pLogger.log(Level.INFO, "deleteCurJVMOptions", 
                        "No JVM options to delete");
                return;
            }
            StringBuffer depOptCmd = 
                    new StringBuffer(asConfigInfo.getASAdminCmd());
            depOptCmd.append("delete-jvm-options ");
            depOptCmd.append(asConfigInfo.getAsAdminCommonParams());
            depOptCmd.append(delOpts.toString());
            StringBuffer resultBuffer = new StringBuffer();
            int retVal = AMTuneUtil.executeCommand(depOptCmd.toString(), 
                    resultBuffer);
            if (retVal != 0) {
                mWriter.writelnLocaleMsg("pt-del-jvm-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                pLogger.log(Level.SEVERE, "deleteCurJVMOptions", 
                        "Error running cmd " + depOptCmd + " \n Error msg :" +
                         resultBuffer.toString());
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "deleteCurJVMOptions",
                    "Error deleting JVM options. " + ex.getMessage());
        }
    }
    
    /**
     * Inserts new JVM options using asadmin
     * 
     * @param newJvmOptions
     */
    private void insertNewJVMOptions(List newJVMOpts) {
        try {
            StringBuffer newOpts = new StringBuffer();
            if (AMTuneUtil.isWindows2003()) {
                newOpts.append(" \"");
            } else {
                newOpts.append(" :");
            }
            Iterator optItr = newJVMOpts.iterator();
            while(optItr.hasNext()) {
                String val = optItr.next().toString().trim();
                if(val.length() > 0) {
                    newOpts.append(AS_PARAM_DELIM);
                    val = val.replace(AS_PARAM_DELIM, "\\" + AS_PARAM_DELIM);
                    newOpts.append(val);
                }
            }
            if (AMTuneUtil.isWindows2003()) {
                newOpts.append("\"");
            }
            if(newOpts.toString().trim().length() < 3) {
                pLogger.log(Level.INFO, "insertNewJVMOptions", 
                        "No JVM options to insert");
                return;
            }
            StringBuffer resultBuffer = new StringBuffer();
            StringBuffer newOptCmd = 
                    new StringBuffer(asConfigInfo.getASAdminCmd());
            newOptCmd.append("create-jvm-options ");
            newOptCmd.append(asConfigInfo.getAsAdminCommonParams());
            newOptCmd.append(newOpts.toString());
            int retVal = AMTuneUtil.executeCommand(newOptCmd.toString(), 
                    resultBuffer);
            if (retVal != 0) {
                mWriter.writelnLocaleMsg("pt-create-jvm-opts-error-msg");
                mWriter.writelnLocaleMsg("pt-check-dbg-logs-msg");
                pLogger.log(Level.SEVERE, "insertNewJVMOptions", 
                        "Error running cmd " + newOptCmd.toString() + 
                        " \n Error msg :" + resultBuffer.toString());
            }
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "insertNewJVMOptions", 
                    "Error setting new JVM options. " + ex.getMessage());
        }
    }
    
    /**
     * 
     */
    protected void tuneSecurityLimits() 
    throws AMTuneException {
        try {
            String tuneFile = "/etc/security/limits.conf";
            mWriter.writeln(LINE_SEP);
            mWriter.writelnLocaleMsg("pt-app-stack-size-tuning");
            mWriter.writeln(" ");
            mWriter.writeLocaleMsg("pt-file");
            mWriter.writeln(tuneFile);
            mWriter.writelnLocaleMsg("pt-param-tuning");
            FileHandler fh = new FileHandler(tuneFile);
            String[] mLines = fh.getMattchingLines("^#", true);
            mLines = AMTuneUtil.getMatchedLines(mLines, "stack");
            mLines = AMTuneUtil.getMatchedLines(mLines, "hard");
            String newStackSize = "";
            String curStackSizeStr = " ";
            if (mLines.length > 0) {
                String firCol = mLines[0].substring(0, mLines[0].indexOf(" "));
                newStackSize = firCol + "               " + "hard    " +
                        "stack          " + AMTUNE_LINUX_STACK_SIZE_LIMITS;
                curStackSizeStr = mLines[0];
            } else {
                newStackSize = "*               hard    stack          " +
                        AMTUNE_LINUX_STACK_SIZE_LIMITS;
            }
            mWriter.writelnLocaleMsg("pt-stack-size-msg");
            mWriter.writeLocaleMsg("pt-cur-val");
            mWriter.writeln(curStackSizeStr);
            mWriter.writelnLocaleMsg("pt-rec-val");
            mWriter.writeln(newStackSize);
            if (configInfo.isReviewMode()) {
                return;
            }
            String[] delLines = new String[2];
            delLines[0] = "Start: AS9.1 Federated Access Manager Tuning :";
            delLines[1] = "End: AS9.1 Federated Access Manager Tuning :";
            if (curStackSizeStr != null && curStackSizeStr.trim().length() >0) {
                delLines[2] = curStackSizeStr;
            }
            fh.removeMatchingLines(delLines);
            fh.appendLine("# " + delLines[0] + AMTuneUtil.getTodayDateStr());
            fh.appendLine(newStackSize);
            fh.appendLine("# " + delLines[1] + AMTuneUtil.getTodayDateStr());
            fh.close();
        } catch (Exception ex) {
            pLogger.log(Level.SEVERE, "tuneSecurityLimits",
                    "Error tuning security limits " + ex.getMessage());
            throw new AMTuneException(ex.getMessage());
        }
    }

    private void deletePasswordFile() {
        File passFile = new File(asConfigInfo.getAdminPassfilePath());
        if (passFile.isFile()) {
            passFile.delete();
        }
    }
}
