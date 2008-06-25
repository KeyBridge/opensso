/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: EventService.java,v 1.11 2008-06-25 05:41:38 qcheng Exp $
 *
 */

package com.iplanet.services.ldap.event;

import java.security.AccessController;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPControl;
import netscape.ldap.LDAPEntry;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPInterruptedException;
import netscape.ldap.LDAPMessage;
import netscape.ldap.LDAPResponse;
import netscape.ldap.LDAPSearchConstraints;
import netscape.ldap.LDAPSearchListener;
import netscape.ldap.LDAPSearchResult;
import netscape.ldap.LDAPSearchResultReference;
import netscape.ldap.controls.LDAPEntryChangeControl;
import netscape.ldap.controls.LDAPPersistSearchControl;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.common.TimerPool;
import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.ldap.LDAPUser;
import com.iplanet.services.ldap.ServerInstance;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.providers.dpro.SSOProviderBundle;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.security.ServerInstanceAction;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;

/**
 * Event Service monitors changes on the server. Implemented with the persistant
 * search control. Uses ldapjdk asynchronous interfaces so that multiple search
 * requests can be processed by a single thread
 * 
 * The Type of changes that can be monitored are: - 
 * LDAPPersistSearchControl.ADD -
 * LDAPPersistSearchControl.DELETE - LDAPPersistSearchControl.MODIFY -
 * LDAPPersistSearchControl.MODDN
 * 
 * A single connection is established initially and reused to service all
 * notification requests.
 * @supported.api
 */
public class EventService implements Runnable {

    protected static DSConfigMgr cm = null;

    // list that holds notification requests
    protected Map _requestList = null;

    // Thread that listens to DS notifications
    static Thread _monitorThread = null;

    // search listener for asynch ldap searches
    static LDAPSearchListener _msgQueue;

    // A singelton patern
    protected static EventService _instance = null;

    // Don't want the server to return all the
    // entries. return only the changes.
    private static final boolean CHANGES_ONLY = true;

    // Want the server to return the entry
    // change control in the search result
    private static final boolean RETURN_CONTROLS = true;

    // Don't perform search if Persistent
    // Search control is not supported.
    private static final boolean IS_CRITICAL = true;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    protected static Debug debugger = Debug.getInstance("amEventService");

    // Parameters in AMConfig, that provide values for connection retries
    protected static final String EVENT_CONNECTION_NUM_RETRIES = 
        "com.iplanet.am.event.connection.num.retries";

    protected static final String EVENT_CONNECTION_RETRY_INTERVAL = 
        "com.iplanet.am.event.connection.delay.between.retries";

    protected static final String EVENT_CONNECTION_ERROR_CODES = 
        "com.iplanet.am.event.connection.ldap.error.codes.retries";

    // Idle timeout in minutes
    protected static final String EVENT_IDLE_TIMEOUT_INTERVAL = 
        "com.sun.am.event.connection.idle.timeout";
    
    protected static final String EVENT_LISTENER_DISABLE_LIST =
        "com.sun.am.event.connection.disable.list";
          
    private static boolean _allDisabled = false;    

    private static int _numRetries = 3;

    private static int _retryInterval = 3000;

    protected static HashSet _retryErrorCodes;

    // Connection Time Out parameters
    protected static int _idleTimeOut = 0; // Idle timeout in minutes.

    protected static long _idleTimeOutMills;

    protected static final String[] listeners = {
            "com.iplanet.am.sdk.ldap.ACIEventListener",
            "com.iplanet.am.sdk.ldap.EntryEventListener",
            "com.sun.identity.sm.ldap.LDAPEventManager" };

    protected static Hashtable _ideListenersMap = new Hashtable();   
    
    protected static volatile boolean _isThreadStarted = false;
    
    protected static volatile boolean _shutdownCalled = false;

    static {
        // Determine the Number of retries for Event Service Connections
        _numRetries = getPropertyIntValue(EVENT_CONNECTION_NUM_RETRIES,
                _numRetries);

        // Determine the Event Service retry error codes
        _retryInterval = getPropertyIntValue(EVENT_CONNECTION_RETRY_INTERVAL,
                _retryInterval);

        // Determine the Event Service retry error codes
        _retryErrorCodes = getPropertyRetryErrorCodes(
                EVENT_CONNECTION_ERROR_CODES);

        // Determine the Idle time out value for Event Service (LB/Firewall)
        // scenarios. Value == 0 imples no idle timeout.
        _idleTimeOut = getPropertyIntValue(EVENT_IDLE_TIMEOUT_INTERVAL,
                _idleTimeOut);
        _idleTimeOutMills = _idleTimeOut * 60000;
        
        getListenerList();
    }

    private static HashSet getPropertyRetryErrorCodes(String key) {
        HashSet codes = new HashSet();
        String retryErrorStr = SystemProperties.get(key);
        if (retryErrorStr != null && retryErrorStr.trim().length() > 0) {
            StringTokenizer stz = new StringTokenizer(retryErrorStr, ",");
            while (stz.hasMoreTokens()) {
                codes.add(stz.nextToken().trim());
            }
        }
        return codes;
    }

    private static int getPropertyIntValue(String key, int defaultValue) {
        int value = defaultValue;
        String valueStr = SystemProperties.get(key);
        if (valueStr != null && valueStr.trim().length() > 0) {
            try {
                value = Integer.parseInt(valueStr);
            } catch (NumberFormatException e) {
                value = defaultValue;
                if (debugger.warningEnabled()) {
                    debugger.warning("EventService.getPropertyIntValue(): "
                            + "Invalid value for property: "
                            + EVENT_CONNECTION_NUM_RETRIES
                            + " Defaulting to value: " + defaultValue);
                }
            }
        }

        if (debugger.messageEnabled()) {
            debugger.message("EventService.getPropertyIntValue(): " + key
                    + " = " + value);
        }
        return value;
    }
    
    /**
     * Determine the listener list based on the hidden property
     */
    private static void getListenerList() {
        String list = SystemProperties.get(EVENT_LISTENER_DISABLE_LIST, "");
        if (debugger.messageEnabled()) {
            debugger.message("EventService.getListenerList(): " +
                    EVENT_LISTENER_DISABLE_LIST + " = " + list);
        }

        // nothing will be disabled
        if (list.length() == 0) {
            return;
        }

        StringTokenizer st = new StringTokenizer(list, ",");
        boolean disableACI = false, disableUM = false, disableSM = false;
        String listener = "";
        while(st.hasMoreTokens()) {
            listener = st.nextToken().trim();
            if (listener.equalsIgnoreCase("aci")) {
                listeners[0] = null;
                disableACI = true;
            } else if (listener.equalsIgnoreCase("um")) {
                listeners[1] = null;
                disableUM = true;
            } else if (listener.equalsIgnoreCase("sm")) {
                listeners[2] = null;
                    disableSM = true;
            } else {
                debugger.error("EventService.getListenerList() - " +
                    "Invalid listener name: "+listener);
            }
        }

        // all disabled, signal to not start the thread
        if (disableACI && disableUM && disableSM) {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.getListenerList() - " +
                        "all listeners are disabled, EventService won't start");
                }
            _allDisabled = true;
        }
    }

    /**
     * Private Constructor
     */
    protected EventService() throws EventException {
        getConfigManager();
        _requestList = Collections.synchronizedMap(new HashMap());
    }

    /**
     * create the singelton EventService object if it doesn't exist already.
     * Check if directory server supports the Persistent Search Control and the
     * Proxy Auth Control
     * @supported.api
     */
    public synchronized static EventService getEventService()
            throws EventException, LDAPException {
        
        if (_allDisabled || _shutdownCalled) {
            return null;
        }
        
        // Make sure only one instance of this class is created.
        if (_instance == null) {
            if (_idleTimeOut == 0) {
                _instance = new EventService();
            } else {
                _instance = new EventServicePolling();
            }
            ShutdownManager.getInstance().addShutdownListener(new
                ShutdownListener() {
                public void shutdown() {
                    if (_instance != null) {
                        _instance.finalize();
                    }
                }
            });
            initListeners();
        }
        return _instance;
    }
    
    protected static String getName() {
        return "EventService";
    }

    /**
     * At the end, close THE Event Manager's connections Abandon all previous
     * persistent search requests
     * @supported.api
     */
    public void finalize() {
        synchronized (this) {
            _shutdownCalled = true;
            if ((_monitorThread != null) && (_monitorThread.isAlive())) {
                _monitorThread.interrupt();
                _isThreadStarted = false;
            }
        }
        synchronized (_requestList) {
            Collection requestObjs = _requestList.values();
            Iterator iter = requestObjs.iterator();
            while (iter.hasNext()) {
                Request request = (Request) iter.next();
                removeListener(request);
            }
            _requestList.clear();
        }
    }

    /**
     * Adds a listener to the directory.
     * @supported.api
     */
    public synchronized String addListener(SSOToken token,
            IDSEventListener listener, String base, int scope, String filter,
            int operations) throws LDAPException, EventException {

        if (_shutdownCalled) {
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_CONNECTFAIL));
        }
        
        LDAPConnection lc = null;
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(token,
                IdConstants.REPO_SERVICE, "1.0");
            ServiceSchema idRepoSubSchema = scm.getOrganizationSchema();
            Set idRepoPlugins = idRepoSubSchema.getSubSchemaNames();

            // This amSDK plugin check is to avoid getting
            // connection and initiate these two listeners if amSDK plugin
            // is not loaded.
            // "com.iplanet.am.sdk.ldap.ACIEventListener" and
            // "com.iplanet.am.sdk.ldap.EntryEventListener"

            // Check for SMS listener and use "sms" group if present
            if ((listener.getClass().getName().equals(
                "com.sun.identity.sm.ldap.LDAPEventManager")) &&
                (cm.getServerGroup("sms") != null)) {
                    lc = cm.getNewConnection("sms", LDAPUser.Type.AUTH_ADMIN);

            } else if (idRepoPlugins.contains("amSDK")) {
                // if amSDK plugin exists, use admin connection from 'default'
                // servergroup for all um,aci,sm listeners.
                lc = cm.getNewAdminConnection();
            } else {
                return "0";
            }
        } catch (SSOException ssoe) {
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_CONNECTFAIL), ssoe);
        } catch (SMSException smse) {
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_CONNECTFAIL), smse);
        } catch (LDAPServiceException le) {
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_CONNECTFAIL), le);
        }

        LDAPSearchConstraints cons = lc.getSearchConstraints();

        // Create Persistent Search Control object
        LDAPPersistSearchControl psearchCtrl = new LDAPPersistSearchControl(
                operations, CHANGES_ONLY, RETURN_CONTROLS, IS_CRITICAL);

        // Add LDAPControl array to the search constraint object
        cons.setServerControls(psearchCtrl);
        cons.setBatchSize(1);

        // Listeners can not read attributes from the event.
        // Request only javaClassName to be able to determine object type
        String[] attrs = new String[] { "objectclass" };
        LDAPSearchListener searchListener = null;
        // Set (asynchronous) persistent search request in the DS
        try {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.addListener() - Submiting "
                        + "Persistent Search on: " + base + " for listener: "
                        + listener);
            }
            searchListener = lc.search(base, scope, filter, attrs, false,
                    null, cons);
        } catch (LDAPException le) {
            if ((lc != null) && lc.isConnected()) {
                try {
                    lc.disconnect();
                } catch (Exception ex) {
                    //ignored
                }
            }
            debugger.error("EventService.addListener() - Failed to set "
                    + "Persistent Search" + le.getMessage());
            throw le;
        }

        int[] outstandingRequests = searchListener.getMessageIDs();
        int id = outstandingRequests[outstandingRequests.length - 1];

        String reqID = Integer.toString(id);
        long startTime = System.currentTimeMillis();
        Request request = new Request(id, reqID, token, base, scope, filter,
                attrs, operations, listener, lc, startTime);
        _requestList.put(reqID, request);

        // Add this search request to the m_msgQueue so it can be
        // processed by the monitor thread
        if (_msgQueue == null) {
            _msgQueue = searchListener;
        } else {
            _msgQueue.merge(searchListener);
        }

        if (!_isThreadStarted) {
            startMonitorThread();
        } else {
            if (_requestList.size() == 1) {
                notify();
            }
        }
        
        if (debugger.messageEnabled()) {
            outstandingRequests = _msgQueue.getMessageIDs();
            debugger.message("EventService.addListener(): merged Listener: "
                    + " requestID: " + reqID + " & Request: " + request
                    + " on to message Queue. No. of current outstanding "
                    + "requests = " + outstandingRequests.length);
        }

        // Create new (EventService) Thread, if one doesn't exist.
        return reqID;
    }

    public IDSEventListener getIDSListeners(String className) {
        return (IDSEventListener) _ideListenersMap.get(className);
    }
    
    public static boolean isThreadStarted() {
        return _isThreadStarted;
    }
      
    protected static void initListeners() {
        int size = listeners.length;
        for (int i = 0; i < size; i++){
            String l1 = listeners[i];
            
            if (l1 == null ) {
                continue;
            }
            
            try {
                if (l1.equals("com.sun.identity.sm.ldap.LDAPEventManager")) {
                    String enableDataStoreNotification = SystemProperties.get(
                        "com.sun.identity.sm.enableDataStoreNotification", 
                        "true");

                    // if UM and SM root suffix are different, then inmemory
                    // notification is enabled through
                    // enableDataStoreNotification=false

                    if (!SMSEntry.getRootSuffix().equalsIgnoreCase(
                        SMSEntry.getAMSdkBaseDN())) {
                        enableDataStoreNotification = "false";
                    }

                    if (debugger.messageEnabled()) {
                        debugger.message("EventService.initListeners()-" 
                                + "com.sun.identity.sm." 
                                + "enableDataStoreNotification:"
                                + enableDataStoreNotification);
                    }
                    if (enableDataStoreNotification.equals("false") &&
                        com.sun.identity.sm.ServiceManager.isRealmEnabled()) {
                        debugger.message("EventService.initListeners() - " 
                                + "Skipping " 
                                + "com.sun.identity.sm.ldap.LDAPEventManager");
                        continue;
                    }
                }
                Class thisClass = Class.forName(l1);
                IDSEventListener listener = (IDSEventListener)
                    thisClass.newInstance();
                _ideListenersMap.put(l1, listener);
                _instance.addListener(getSSOToken(), listener, 
                    listener.getBase(), listener.getScope(), 
                    listener.getFilter(), listener.getOperations());
                if (debugger.messageEnabled()) {
                    debugger.message("EventService.initListeners() - " +
                            "successfully initialized listener: " + l1);
                }
            } catch (Exception e) {
                debugger.error("EventService.initListeners() Unable to start " 
                        + "listener " + l1, e);
            }
        }
    }

    /**
     * Main monitor thread loop. Wait for persistent search change notifications
     *
     * @supported.api
     */    
    public void run() {
        try {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.run(): Event Thread is running! "
                        + "No Idle timeout Set: " + _idleTimeOut + " minutes.");
            }
            
            boolean successState = true;
            LDAPMessage message = null;
            while (successState) {
                try {
                    if (debugger.messageEnabled()) {
                        debugger.message("EventService.run(): Waiting for "
                                + "response");
                    }
                    
                    message = _msgQueue.getResponse();
                    synchronized (this) {
                        if ((message == null) && (_requestList.isEmpty())) {
                            wait();
                            continue;
                        }
                    }
                    successState = processResponse(message);
                } catch (LDAPInterruptedException ex) {
                    if (_shutdownCalled) {
                        break;
                    } else {
                        if (debugger.warningEnabled()) {
                            debugger.warning("EventService.run() " +
                                "LDAPInterruptedException received:", ex);
                        }
                    }
                } catch (LDAPException ex) {
                    if (_shutdownCalled) {                        
                        break;
                    } else {
                        int resultCode = ex.getLDAPResultCode();
                        if (debugger.warningEnabled()) {
                            debugger.warning("EventService.run() LDAPException "
                                + "received:", ex);
                        }
                        if (_retryErrorCodes.contains("" + resultCode)) {
                            resetErrorSearches(true);
                        } else { // Some other network error
                            processNetworkError(ex);
                        }
                    }
                }
            } // end of while loop
        } catch (InterruptedException ex) {
            if (!_shutdownCalled) {
                if (debugger.warningEnabled()) {
                    debugger.warning("EventService.run(): Interrupted exception"
                        + " caught.", ex);
                }
            }
        } catch (RuntimeException ex) {
            if (debugger.warningEnabled()) {
                debugger.warning("EventService.run(): Runtime exception "
                    + "caught.", ex);
            }
            // rethrow the Runtime exception to let the container handle the
            // exception.
            throw ex;
        } catch (Exception ex) {
            if (debugger.warningEnabled()) {
                debugger.warning("EventService.run(): Unknown exception "
                    + "caught.", ex);
            }
            // no need to rethrow.
        } catch (Throwable t) {
            // Catching Throwable to prevent the thread from exiting.
            if (debugger.warningEnabled()) {
                debugger.warning("EventService.run(): Unknown exception "
                    + "caught. Sleeping for a while.. ", t);
            }
            // rethrow the Error to let the container handle the error.
            throw new Error(t);
        } finally {
            synchronized (this) {
                if (!_shutdownCalled) {
                    // try to restart the monitor thread.
                    _monitorThread = null;
                    startMonitorThread();
                }
            }
        }
    } // end of thread
    
    private static synchronized void startMonitorThread() {
        if (_monitorThread == null || (!_monitorThread.isAlive()) &&
            !_shutdownCalled) {
            // Even if the monitor thread is not alive, we should use the
            // same instance of Event Service object (as it maintains all
            // the listener information)
            _monitorThread = new Thread(_instance, getName());
            _monitorThread.start();
            
            // Since this is a singleton class once a getEventService() 
            // is invoked the thread will be started and the variable 
            // will be set to true. This will help other components 
            // to avoid starting it once again if the thread has 
            // started.
            _isThreadStarted = true;            
        }
    }

    /**
     * Method which process the Response received from the DS.
     * 
     * @param message -
     *            the LDAPMessage received as response
     * @return true if the reset was successful. False Otherwise.
     */        
    protected boolean processResponse(LDAPMessage message) {
        if ((message == null) && (!_requestList.isEmpty())) {
            // Some problem with the message queue. We should
            // try to reset it.
            debugger.warning("EventService.processResponse() - Received a "
                    + "NULL Response. Attempting to re-start persistent "
                    + "searches");
            resetErrorSearches(false);
            return true;
        }
        
        if (debugger.messageEnabled()) {
            debugger.message("EventService.processResponse() - received "
                    + "DS message  => " + message.toString());
        }

        // To determine if the monitor thread needs to be stopped.
        boolean successState = true;

        Request request = getRequestEntry(message.getMessageID());

        // If no listeners, abandon this message id
        if (request == null) {
            // We do not have anything stored about this message id.
            // So, just log a message and do nothing.
            if (debugger.messageEnabled()) {
                debugger.message("EventService.processResponse() - Received "
                        + "ldap message with unknown id = "
                        + message.getMessageID());
            }
        } else if (message instanceof LDAPSearchResult) {
            // then must be a LDAPSearchResult carrying change control
            processSearchResultMessage((LDAPSearchResult) message, request);
            request.setLastUpdatedTime(System.currentTimeMillis());
        } else if (message instanceof LDAPResponse) {
            // Check for error message ...
            LDAPResponse rsp = (LDAPResponse) message;
            successState = processResponseMessage(rsp, request);
        } else if (message instanceof LDAPSearchResultReference) { // Referral
            processSearchResultRef(
                    (LDAPSearchResultReference) message, request);
        }
        return successState;
    }

    /**
     * removes the listener from the list of Persistent Search listeners of the
     * asynchronous seach for the given search ID.
     * 
     * @param requestID
     *            The request ID returned by the addListener
     * @supported.api
     */   
    protected void removeListener(Request request) {
        LDAPConnection connection = request.getLDAPConnection();
        if (connection != null) {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.removeListener(): Removing "
                        + "listener requestID: " + request.getRequestID()
                        + " Listener: " + request.getListener());
            }
            try {
                if ((connection != null) && (connection.isConnected())) {
                    connection.abandon(request.getId());
                    connection.disconnect();
                }
            } catch (LDAPException le) {
                // Might have to check the reset codes and try to reset
                if (debugger.warningEnabled()) {
                    debugger.warning("EventService.removeListener(): "
                            + "LDAPException, when trying to remove listener",
                            le);
                }
            }
        }
    }

    
    /**
     * Reset error searches. Clear cache only if true is passed to argument
     * 
     * @param clearCaches
     */    
    protected void resetErrorSearches(boolean clearCaches) {
        
        Hashtable tmpReqList = new Hashtable(_requestList);
       
        int[] ids = _msgQueue.getMessageIDs();
        for (int i = 0; i < ids.length; i++) {
            String reqID = Integer.toString(ids[i]);
            tmpReqList.remove(reqID);
        }
        Collection reqList = tmpReqList.values();
        for (Iterator iter = reqList.iterator(); iter.hasNext();) {
            Request req = (Request) iter.next();
            _requestList.remove(req.getRequestID());
            if (clearCaches) {
                req.getListener().allEntriesChanged();
            }
        }
        RetryTask task = new RetryTask(tmpReqList, _numRetries);
        SystemTimer.getTimer().schedule(task, new Date(((
            System.currentTimeMillis() + _retryInterval) / 1000) * 1000));
    }
    
    /**
     * Reset all searches. Clear cache only if true is passed to argument
     * 
     * @param clearCaches
     * @return
     */    
    public synchronized boolean resetAllSearches(boolean clearCaches) {

        if (_shutdownCalled) {
            return false;
        }
        Hashtable tmpReqList = new Hashtable(_requestList);
        _requestList.clear(); // Will be updated in addListener method
        Collection reqList = tmpReqList.values();

        int retry = 1;
        boolean doItAgain = ((_numRetries == -1) || ((_numRetries != 0)
                && (retry <= _numRetries))) ? true
                : false;

        if (clearCaches) {
            for (Iterator iter = reqList.iterator(); iter.hasNext();) {
                Request req = (Request) iter.next();
                IDSEventListener el = req.getListener();
                el.allEntriesChanged();
            }
        }
        while (doItAgain) { // Re-try starts from 0.
            sleepRetryInterval();
            if (debugger.messageEnabled()) {
                String str = (_numRetries == -1) ? "indefinitely" : Integer
                    .toString(retry);
                debugger.message("EventService.resetAllSearches(): "
                    + "retrying = " + str);
            }

            // Note: Avoid setting the messageQueue to null and just
            // try to disconnect the connections. That way we can be sure
            // that we have not lost any responses.

            // we want to do the addListener in a seperate loop from the
            // above removeListener because we want to remove all the
            // listener first then do the add.
            for (Iterator iter = reqList.iterator(); iter.hasNext();) {
                try {
                    Request request = (Request) iter.next();

                    // First add a new listener and then remove the old one
                    // that we do don't loose any responses to the message
                    // Queue.
                    addListener(request.getRequester(), request.getListener(),
                        request.getBaseDn(), request.getScope(),
                        request.getFilter(), request.getOperations());
                    removeListener(request);
                    iter.remove();
                } catch (LDAPServiceException e) {
                    // Ignore exception and retry as we are in the process of
                    // re-establishing the searches. Notify Listeners after the
                    // attempt
                    if (retry == _numRetries) {
                        processNetworkError(e);
                    }
                } catch (LDAPException le) {
                    // Ignore exception and retry as we are in the process of
                    // re-establishing the searches. Notify Listeners after the
                    // attempt
                    if (retry == _numRetries) {
                        processNetworkError(le);
                    }
                }       
            }
            if (reqList.isEmpty()) {
                return true;
            } else {
                if (_numRetries != -1) {
                   doItAgain = (++retry <= _numRetries) ? true : false;
                   if (!doItAgain) {
                       // remove the requests fail to be resetted eventually.
                       for (Iterator iter = reqList.iterator();
                           iter.hasNext();) {
                           removeListener((Request) iter.next());
                       }
                   }
                }
            }
        } // end while loop
        return false;
    }
       
    protected void sleepRetryInterval() {
        try {
            Thread.sleep(_retryInterval);
        } catch (InterruptedException ie) {
            // ignore
        }
    }

    /**
     * get a handle to the Directory Server Configuration Manager sets the value
     */    
    protected static void getConfigManager() throws EventException {
        try {
            cm = DSConfigMgr.getDSConfigMgr();
        } catch (LDAPServiceException lse) {
            debugger.error("EventService.getConfigManager() - Failed to get "
                    + "handle to Configuration Manager", lse);
            throw new EventException(i18n
                    .getString(IUMSConstants.DSCFG_NOCFGMGR), lse);
        }
    }
    
    private void dispatchException(Exception e, Request request) {
        IDSEventListener el = request.getListener();
        debugger.error("EventService.dispatchException() - dispatching "
                + "exception to the listener: " + request.getRequestID()
                + " Listener: " + request.getListener(), e);
        el.eventError(e.toString());
    }

    /**
     * Dispatch naming event to all listeners
     */    
    private void dispatchEvent(DSEvent dirEvent, Request request) {
        IDSEventListener el = request.getListener();
        el.entryChanged(dirEvent);
    }

    /**
     * On network error, create ExceptionEvent and delever it to all listeners
     * on all events.
     */    
    protected void processNetworkError(Exception ex) {
        Hashtable tmpRequestList = new Hashtable(_requestList);
        int[] ids = _msgQueue.getMessageIDs();
        for (int i = 0; i < ids.length; i++) {
            tmpRequestList.remove(Integer.toString(ids[i]));
        }
        Collection reqList = tmpRequestList.values();
        for (Iterator iter = reqList.iterator(); iter.hasNext();) {
            Request request = (Request) iter.next();
            dispatchException(ex, request);
        }
    }

    /**
     * Response message carries a LDAP error. Response with the code 0
     * (SUCCESS), should never be received as persistent search never completes,
     * it has to be abandon. Referral messages are ignored
     */    
    protected boolean processResponseMessage(LDAPResponse rsp,
        Request request) {
        if (_retryErrorCodes.contains("" + rsp.getResultCode())) {
            if (debugger.messageEnabled()) {
                debugger.message("EventService.processResponseMessage() - "
                        + "received LDAP Response for requestID: "
                        + request.getRequestID() + " Listener: "
                        + request.getListener() + "Need restarting");
            }
            resetErrorSearches(false);
        } else if (rsp.getResultCode() != 0
                || rsp.getResultCode() != LDAPException.REFERRAL) { 
            // If not neither of the cases then
            LDAPException ex = new LDAPException("Error result", rsp
                    .getResultCode(), rsp.getErrorMessage(), 
                    rsp.getMatchedDN());
            dispatchException(ex, request);
        }
        return true;
    }

    /**
     * Process change notification attached as the change control to the message
     */    
    protected void processSearchResultMessage(LDAPSearchResult res,
            Request req) {
        LDAPEntry modEntry = res.getEntry();

        if (debugger.messageEnabled()) {
            debugger.message("EventService.processSearchResultMessage() - "
                    + "Changed " + modEntry.getDN());
        }

        /* Get any entry change controls. */
        LDAPControl[] ctrls = res.getControls();

        // Can not create event without change control
        if (ctrls == null) {
            Exception ex = new Exception("EventService - Cannot create "
                    + "NamingEvent, no change control info");
            dispatchException(ex, req);
        } else {
            // Multiple controls might be in the message
            for (int i = 0; i < ctrls.length; i++) {
                LDAPEntryChangeControl changeCtrl = null;

                if (ctrls[i] instanceof LDAPEntryChangeControl) {
                    changeCtrl = (LDAPEntryChangeControl) ctrls[i];
                    if (debugger.messageEnabled()) {
                        debugger.message("EventService."
                                + "processSearchResultMessage() changeCtrl = "
                                + changeCtrl.toString());
                    }

                    // Can not create event without change control
                    if (changeCtrl.getChangeType() == -1) {
                        Exception ex = new Exception("EventService - Cannot "
                                + "create NamingEvent, no change control info");
                        dispatchException(ex, req);
                    }

                    // Convert control into a DSEvent and dispatch to listeners
                    try {
                        DSEvent event = createDSEvent(
                                            modEntry, changeCtrl, req);
                        dispatchEvent(event, req);
                    } catch (Exception ex) {
                        dispatchException(ex, req);
                    }
                }
            }
        }
    }

    /**
     * Search continuation messages are ignored.
     */    
    protected void processSearchResultRef(LDAPSearchResultReference ref,
            Request req) {
        // Do nothing, message ignored, do not dispatch ExceptionEvent
        if (debugger.messageEnabled()) {
            debugger.message("EventService.processSearchResultRef() - "
                    + "Ignoring..");
        }
    }
    
    protected static SSOToken getSSOToken() throws SSOException {
        try {
            DSConfigMgr cfgMgr = DSConfigMgr.getDSConfigMgr();
            ServerInstance serInstance = cfgMgr
                    .getServerInstance(LDAPUser.Type.AUTH_ADMIN);
            AuthPrincipal user = new AuthPrincipal(serInstance.getAuthID());
            String adminPW = (String) AccessController
                    .doPrivileged(new ServerInstanceAction(serInstance));
            AuthContext authCtx = new AuthContext(user, adminPW.toCharArray());
            return (authCtx.getSSOToken());
        } catch (Exception e) {
            throw new SSOException(SSOProviderBundle.rbName, "invalidadmin",
                    null);
        }
    }

    /**
     * Find event entry by message ID
     */    
    protected Request getRequestEntry(int id) {
        return (Request) _requestList.get(Integer.toString(id));
    }

    /**
     * Create naming event from a change control
     */    
    private DSEvent createDSEvent(LDAPEntry entry,
            LDAPEntryChangeControl changeCtrl, Request req) throws Exception {
        DSEvent dsEvent = new DSEvent();

        if (debugger.messageEnabled()) {
            debugger.message("EventService.createDSEvent() - Notifying event "
                    + "to: " + req.getListener());
        }

        // Get the dn from the entry
        String dn = entry.getDN();
        dsEvent.setID(dn);

        // Get information on the type of change made
        int changeType = changeCtrl.getChangeType();
        dsEvent.setEventType(changeType);

        // Pass the search ID as the event's change info
        dsEvent.setSearchID(req.getRequestID());

        // set the object class name
        String className = entry.getAttribute("objectclass").toString();
        dsEvent.setClassName(className);

        return dsEvent;
    }
    
    class RetryTask extends GeneralTaskRunnable {
        
        private long runPeriod;
        private Map requests;
        private int numOfRetries;
        private int retry;
        
        public RetryTask(Map requests, int numOfRetries) {
            this.runPeriod = (long) EventService._retryInterval;
            this.requests = requests;
            this.numOfRetries = numOfRetries;
            this.retry = 1;
        }
        
        public void run() {
            for (Iterator iter = requests.values().iterator();
                iter.hasNext();) {
                Request req = (Request) iter.next();
                try {
                    // First add a new listener and then remove the old one
                    // that we do don't loose any responses to the message
                    // Queue.
                    addListener(req.getRequester(), req.getListener(),
                        req.getBaseDn(), req.getScope(), 
                        req.getFilter(), req.getOperations());
                    removeListener(req);
                    iter.remove();
                } catch (Exception e) {
                    // Ignore exception and retry as we are in the process of
                    // re-establishing the searches. Notify Listeners after the
                    // attempt
                    if (retry == numOfRetries) {
                        dispatchException(e, req);
                    }
                }
            }
            if (requests.isEmpty()) {
                runPeriod = -1;
            } else {
                if (numOfRetries != -1) {
                    if (++retry > numOfRetries) {
                        runPeriod = -1;
                    }
                }
            }
        }
        
        public long getRunPeriod() {
            return runPeriod;
        }
        
        public boolean isEmpty() {
            return true;
        }
        
        public boolean addElement(Object obj) {
            return false;
        }
        
        public boolean removeElement(Object obj) {
            return false;
        }
    }
}
