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
 * $Id: LDAPConnectionPool.java,v 1.8 2008-01-15 22:12:44 ww203982 Exp $
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.common;

import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.common.SystemTimerPool;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.sun.identity.common.FallBackManager;
import com.sun.identity.shared.debug.Debug;
import netscape.ldap.LDAPConnection;
import netscape.ldap.LDAPException;
import netscape.ldap.LDAPSearchConstraints;

/**
 * Class to maintain a pool of individual connections to the
 * same server. Specify the initial size and the max size
 * when constructing a pool. Call getConnection() to obtain
 * a connection from the pool and close() to return it. If
 * the pool is fully extended and there are no free connections,
 * getConnection() blocks until a connection has been returned
 * to the pool.<BR>
 * Call destroy() to release all connections.
 *<BR><BR>Example:<BR>
 *<PRE>
 * LDAPConnectionPool pool = null;
 * try {
 *     pool = new LDAPConnectionPool("test", 10, 30,
 *                                "foo.acme.com",389,
 *                                "uid=me, o=acme.com",
 *                                "password" );
 * } catch ( LDAPException e ) {
 *    System.err.println( "Unable to create connection pool" );
 *    System.exit( 1 );
 * }
 * while ( clientsKnocking ) {
 *     String filter = getSearchFilter();
 *     LDAPConnection ld = pool.getConnection();
 *     try {
 *         LDAPSearchResults res = ld.search( BASE, ld.SCOPE_SUB,
 *                                            filter, attrs,
 *                                            false );
 *         pool.close( ld );
 *         while( res.hasMoreElements() ) {
 *             ...
 *</PRE>
 */

/**
 * LDAPConnection pool, typically used by a server to avoid creating
 * a new connection for each client
 *
 **/
public class LDAPConnectionPool {

    private static ArrayList hostArrList = new ArrayList();
    private static Debug debug;  // Debug object
    private static HashSet retryErrorCodes = new HashSet();
    private static final String LDAP_CONNECTION_ERROR_CODES =
        "com.iplanet.am.ldap.connection.ldap.error.codes.retries";
 
    /**
     * Constructor for specifying all parameters
     *
     * @param name name of connection pool
     * @param min initial number of connections
     * @param max maximum number of connections
     * @param host hostname of LDAP server
     * @param port port number of LDAP server
     * @param authdn DN to authenticate as
     * @param authpw password for authentication
     * @exception LDAPException on failure to create connections
     */
    public LDAPConnectionPool(
        String name,
        int min,
        int max,
        String host,
        int port,
        String authdn,
        String authpw
    ) throws LDAPException {
        this(name, min, max, host, port, authdn, authpw, null, null);
    }

    /**
     * Constructor for specifying all parameters, anonymous
     * identity
     *
     * @param name name of connection pool
     * @param min initial number of connections
     * @param max maximum number of connections
     * @param host hostname of LDAP server
     * @param port port number of LDAP server
     * @exception LDAPException on failure to create connections
     */
    public LDAPConnectionPool(
        String name,
        int min,
        int max,
        String host,
        int port
    ) throws LDAPException {
        this(name, min, max, host, port, "", ""); 
    }

    /**
     * Constructor for specifying connection option parameters in addition
     * to all other parameters.
     *
     * @param name name of connection pool
     * @param min initial number of connections
     * @param max maximum number of connections
     * @param host hostname of LDAP server
     * @param port port number of LDAP server
     * @param authdn DN to authenticate as
     * @param authpw password for authentication
     * @param connOptions connection option parameters set at serviceconfig
     * @exception LDAPException on failure to create connections
     */
    public LDAPConnectionPool(
        String name, 
        int min, 
        int max,
        String host, 
        int port,
        String authdn, 
        String authpw,
        HashMap connOptions
    ) throws LDAPException {
        this(name, min, max, host, port, authdn, authpw, null, connOptions);
    }

    /**
     * Constructor for using default parameters, anonymous identity
     *
     * @param name name of connection pool
     * @param host hostname of LDAP server
     * @param port port number of LDAP server
     * @exception LDAPException on failure to create connections
     */
    public LDAPConnectionPool(String name, String host, int port)
        throws LDAPException
    {
        // poolsize=10,max=20,host,port,
        // noauth,nopswd
        this(name, 10, 20, host, port, "", "", null);
    }

    /** 
     * Constructor for using an existing connection to clone
     * from.
     * <P>
     * The connection to clone must be already established and
     * the user authenticated.
     * 
     * @param name name of connection pool
     * @param min initial number of connections
     * @param max maximum number of connections
     * @param ldc connection to clone 
     * @exception LDAPException on failure to create connections 
     */
    public LDAPConnectionPool(
        String name,
        int min,
        int max,
        LDAPConnection ldc
    ) throws LDAPException {
        this(name, min, max, ldc.getHost(), ldc.getPort(),
              ldc.getAuthenticationDN(), ldc.getAuthenticationPassword(),
              (LDAPConnection)ldc.clone(), null);
    }

    /* 
     * Constructor for using an existing connection to clone
     * from
     * 
     * @param name name of connection pool
     * @param min initial number of connections
     * @param max maximum number of connections
     * @param host hostname of LDAP server
     * @param port port number of LDAP server
     * @param authdn DN to authenticate as
     * @param authpw password for authentication
     * @param ldc connection to clone 
     * @param connOptions connection option parameters set at serviceconfig
     * @exception LDAPException on failure to create connections 
     */ 
    public LDAPConnectionPool(
        String name,
        int min,
        int max,
        String host,
        int port,
        String authdn,
        String authpw,
        LDAPConnection ldc,
        HashMap connOptions
    ) throws LDAPException {
        this(name, min, max, host, port,
             authdn, authpw, ldc, getIdleTime(name), connOptions);
    }

    private static final int getIdleTime(String poolName) {
        String idleStr =
            SystemProperties.get(Constants.LDAP_CONN_IDLE_TIME_IN_SECS);
        int idleTimeInSecs = 0;
        if (idleStr != null && idleStr.length() > 0) {
            try {
                idleTimeInSecs = Integer.parseInt(idleStr);
            } catch(NumberFormatException nex) {
                debug.error("LDAPConnection pool: " + poolName +
                            ": Cannot parse idle time: " + idleStr +
                            " Connection reaping is disabled.");
            }
        }
        return idleTimeInSecs;
    }


    /**
     * Most generic constructor which initializes all variables.
     */
    private LDAPConnectionPool(
        String name,
        int min,
        int max,
        String host,
        int port,
        String authdn,
        String authpw,
        LDAPConnection ldc,
        int idleTimeInSecs,
        HashMap connOptions
    ) throws LDAPException {
        this.name = name;
        this.minSize = min;
        this.maxSize  = max;
        if (connOptions != null) {
            // createHostList and assign the first one to the this.host & 
            // this.port
            createHostList(host);
        } else {
            this.host = host;
            this.port = port;
        }
        this.authdn = authdn;
        this.authpw = authpw;
        this.ldc = ldc;
        this.idleTime = idleTimeInSecs * 1000;
        this.defunct = false;
        this.reinitInProgress = false;

        createPool();
        if (debug.messageEnabled()) {
            debug.message("LDAPConnection pool: " + name +
                          ": successfully created: Min:" + minSize +
                          " Max:" + maxSize + " Idle time:" + idleTimeInSecs);
        }
        createIdleCleanupThread();
    }

    /**
     * Destroy the whole pool - called during a shutdown
     */
    public void destroy() {
        if (cleaner != null) {
            cleaner.cancel();
        }
        synchronized (this) {
            if (!defunct) {
                defunct = true;
                for (int i = 0; 
                    i < currentConnectionCount - busyConnectionCount; i++) {
                    if ((pool[i] != null) && (pool[i].isConnected())) {
                        try {
                            backupPool.remove(pool[i]);
                            currentPool.remove(pool[i]);
                            pool[i].disconnect();
                        } catch (LDAPException e) {
                            debug.error("LDAPConnection pool:" + name +
                                ":Error during disconnect.", e);
                        }
                    }
                }
                currentConnectionCount = busyConnectionCount = 0;
                pool = null;
            }
            if ((ldc != null) && (ldc.isConnected())) {
                try {
                    ldc.disconnect();
                } catch(LDAPException e) {
                    debug.error("LDAPConnection pool:" + name +
                        ":Error during disconnect.", e);
                }
            }
        }
    }

    /**
     * Gets a connection from the pool
     *
     * If no connections are available, the pool will be
     * extended if the number of connections is less than
     * the maximum; if the pool cannot be extended, the method
     * blocks until a free connection becomes available.
     *
     * @return an active connection.
     */
    public LDAPConnection getConnection() {
        return getConnection(0);
    }

    /**
     * Gets a connection from the pool within a time limit.
     *
     * If no connections are available, the pool will be
     * extended if the number of connections is less than
     * the maximum; if the pool cannot be extended, the method
     * blocks until a free connection becomes available or the
     * time limit is exceeded. 
     *
     * @param timeout timeout in milliseconds
     * @return an active connection or <CODE>null</CODE> if timed out. 
     */
    public LDAPConnection getConnection(int timeout) {
        LDAPConnection con = null;
        synchronized (this) {
            try {
                if (busyConnectionCount == maxSize) {
                    waitCount++;
                    if (timeout > 0) {
                        this.wait(timeout);
                    } else {
                        this.wait();
                    }
                    waitCount--;
                }
                con = getConnFromPool();
            } catch (InterruptedException e) {
            }
        }
        return con;
    }
    
    /**
     * Gets a connection from the pool
     *
     * If no connections are available, the pool will be
     * extended if the number of connections is less than
     * the maximum; if the pool cannot be extended, the method
     * returns null.
     *
     * @return an active connection or null.
     */
    protected LDAPConnection getConnFromPool() {
        LDAPConnection con = null;
        if ((currentConnectionCount == busyConnectionCount) &&
            (currentConnectionCount < maxSize)) {
            // create connection
            try {
                con = createConnection(LDAPConnPoolUtils.connectionPoolsStatus);
                backupPool.add(con);
                currentConnectionCount++;
                busyConnectionCount++;
            } catch(Exception ex) {
                debug.error("LDAPConnection pool:" + name +
                    ":Error while adding a connection.", ex);
            }
        } else {
            if (currentConnectionCount > busyConnectionCount) {
                con = pool[currentConnectionCount - busyConnectionCount - 1];
                pool[currentConnectionCount - busyConnectionCount - 1] = null;
                busyConnectionCount++;
                currentPool.remove(con);
                if ((cleaner != null) && 
                    ((currentConnectionCount - busyConnectionCount) >=
                    minSize)) {
                    cleaner.removeElement(null);
                }
            }
        }
        return con;
    }

    /**
     * This is our soft close - all we do is mark
     * the connection as available for others to use.
     * We also reset the auth credentials in case
     * they were changed by the caller.
     *
     * @param ld a connection to return to the pool
     */
    public void close(LDAPConnection ld) {
        synchronized(this) {
            if (defunct) {
                if (ld != null) {
                    if (backupPool.remove(ld) || deprecatedPool.remove(ld)) {
                        if (ld.isConnected()) {
                            try {
                                ld.disconnect();
                            } catch(LDAPException ex) {
                                debug.error("LDAPConnection pool:" + name +
                                    ":Error during disconnect.", ex);
                            }
                        }
                    }
                }
            } else {
                if (ld != null) {
                    if (reinitInProgress) {
                        if (deprecatedPool.remove(ld)) {
                            try{
                                ld.disconnect();
                            } catch(LDAPException ex) {
                                debug.error("LDAPConnection pool:" + name +
                                    ":Error during disconnect.", ex);
                            }
                            if (deprecatedPool.isEmpty()) {
                                reinitInProgress = false;
                            }
                            return;
                        }
                    }
                    if (backupPool.contains(ld) && currentPool.add(ld)) {
                        busyConnectionCount--;
                        // return connections from the end of array                    
                        pool[currentConnectionCount - busyConnectionCount - 1] =
                            ld;
                        if ((cleaner != null) &&
                            ((currentConnectionCount - busyConnectionCount) > 
                            minSize)) {
                            cleaner.addElement(null);
                        }
                        if (waitCount > 0) {
                            this.notify();
                        }
                    }
                }
            }
        }
    }

    /**
     * This is our soft close - all we do is mark
     * the connection as available for others to use.
     * We also reset the auth credentials in case
     * they were changed by the caller.
     *
     * @param ld a connection to return to the pool
     */
    public void close(LDAPConnection ld , int errCode) {
        if (debug.messageEnabled()) {
            debug.message("LDAPConnectionPool:close(): errCode "+errCode);
        }
        // Do manual failover to the secondary server, if ldap error code is
        // 80 or 81 or 91.
        if (retryErrorCodes.contains(Integer.toString(errCode))) {
            failOver(ld);
            if ( (LDAPConnPoolUtils.connectionPoolsStatus != null)
                && (!LDAPConnPoolUtils.connectionPoolsStatus.isEmpty()) ) {
                // Initiate the fallback to primary server by invoking a
                // FallBackManager thread which pings if the primary is up.
                if (fMgr == null) {
                    fMgr = new FallBackManager();
                }
                if (fMgr.scheduledExecutionTime() == -1) {
                    SystemTimer.getTimer().schedule(fMgr, new Date((
                        System.currentTimeMillis() / 1000) * 1000));
                }
                
            }
        }
        // need to check failover.
        close(ld);
    }
 
    private void createPool() throws LDAPException {
        // Called by the constructors
        if (minSize <= 0) {
            throw new LDAPException("LDAPConnection pool:" + name +
                                    ":ConnectionPoolSize invalid");
        }
        if (maxSize < minSize) {
            debug.error("LDAPConnection pool:" + name +
                        ":ConnectionPoolMax is invalid, set to " +
                        minSize);
            maxSize = minSize;
        }

        if (debug.messageEnabled()) {
            StringBuffer buf = new StringBuffer();
            buf.append("");
            buf.append("New Connection pool name =" + name);
            buf.append(" LDAP host =").append(host);
            buf.append(" Port =").append(port);
            buf.append(" Min =").append(minSize);
            buf.append(" Max =").append(maxSize);
            debug.message("LDAPConnectionPool:createPool(): buf.toString()" +
                buf.toString());
        }

        // To avoid resizing we set the size to twice the max pool size.
        pool = new LDAPConnection[maxSize];
        backupPool = new HashSet();
        currentPool = new HashSet();
        for (int i = 0; i < minSize; i++) {
            pool[i] = createConnection(LDAPConnPoolUtils.connectionPoolsStatus);
            backupPool.add(pool[i]);
            currentPool.add(pool[i]);
        }
        currentConnectionCount = minSize;
        busyConnectionCount = 0;
        waitCount = 0;
    }

    private LDAPConnection createConnection(HashMap aConnectionPoolsStatus)
        throws LDAPException {

        // Make LDAP connection, using template if available
        LDAPConnection newConn =
            (ldc != null) ? (LDAPConnection)ldc.clone() :
            new LDAPConnection();
        String key = name + ":" + host + ":" + port + ":" + authdn;
        try {
            if (newConn.isConnected()) {
                /*
                 * If using a template, then reconnect
                 * to create a separate physical connection
                 */
                newConn.reconnect();
                if (debug.messageEnabled()) {
                    debug.message("LDAPConnectionPool: "+
                        "createConnection(): with template primary host: " +
                         host + "primary port: " + port);
                }
            } else {
                /*
                 * Not using a template, so connect with
                 * simple authentication using ldap v3
                 */
                try { 
                    newConn.connect (3, host, port, authdn, authpw); 
                   if (debug.messageEnabled()) {
                       debug.message("LDAPConnectionPool: "+
                           "createConnection():No template primary host: " +
                           host + "primary port: " + port);
                   }
                } catch (LDAPException connEx) {
                    // fallback to ldap v2 if v3 is not supported
                    if (connEx.getLDAPResultCode() ==
                        LDAPException.PROTOCOL_ERROR)
                    {
                        newConn.connect (2, host, port, authdn, authpw); 
                        if (debug.messageEnabled()) {
                            debug.message("LDAPConnectionPool: "+
                            "createConnection():No template primary host: " +
                            host + "primary port: with v2 " + port);
                        }
                    } else {
                        // Mark the host to be down and failover
                        // to the next server in line.
                        if (aConnectionPoolsStatus != null) {
                            synchronized(aConnectionPoolsStatus) {
                                aConnectionPoolsStatus.put(key, this);
                            }
                        }
                        if (debug.messageEnabled()) {
                            debug.message("LDAPConnectionPool: "+
                                "createConnection():primary host" + host +
                                    "primary port-" + port + " :is down."+
                                    "Failover to the secondary server.");
                        }
                        // Release this connection as it cannot connect
                        // to the primary server, in order for failover
                        // to happen. use case:primary is shut down and
                        // AM server(web container) is restarted.
                        close(ldc, connEx.getLDAPResultCode());
                    }
                }
            }
        } catch (LDAPException le) {
            debug.error("LDAPConnection pool:createConnection():" + 
                "Error while Creating pool.", le);
            // Mark the host to be down and failover
            // to the next server in line.
            if (aConnectionPoolsStatus != null) {
                synchronized(aConnectionPoolsStatus) {
                    aConnectionPoolsStatus.put(key, this);
                }
            }
            throw le;
        }
        return newConn;
    }
    
    private void createIdleCleanupThread() {
        if (idleTime > 0) {
            cleaner = new CleanupTask(this, idleTime / 2, idleTime);
            SystemTimerPool.getTimerPool().schedule(cleaner, new
                Date(((System.currentTimeMillis() + idleTime) / 1000) * 1000));
            if (debug.messageEnabled()) {
                debug.message("LDAPConnection pool: " + name +
                              ": Cleanup task created successfully.");
            }
        }
    }
    
    static {
        debug = Debug.getInstance("LDAPConnectionPool");
        String retryErrs = SystemProperties.get(LDAP_CONNECTION_ERROR_CODES);
        if (retryErrs != null) {
            StringTokenizer stz = new StringTokenizer(retryErrs, ",");
            while(stz.hasMoreTokens()) {
                retryErrorCodes.add(stz.nextToken().trim());
            }
        }
        if (debug.messageEnabled()) {
            debug.message("LDAPConnectionPool: retry error codes = " +
                             retryErrorCodes);
        }
    }
    
    private void createHostList(String hostNameFromConfig) {
        StringTokenizer st = new StringTokenizer(hostNameFromConfig);
        while(st.hasMoreElements()) {
            String str = (String)st.nextToken();
            if (str != null && str.length() != 0) {
                if (debug.messageEnabled()) {
                    debug.message("LDAPConnectionPool:createHostList():" +
                        "host name:"+str);
                }
                hostArrList.add(str);
            }
        }
        String hpName = (String) hostArrList.get(0);
        StringTokenizer stn = new StringTokenizer(hpName,":");
        this.host = (String)stn.nextToken();
        this.port = (Integer.valueOf((String)stn.nextToken())).intValue();
    }

    /**
     * Reinitialize the connection pool with a new connection
     * template.  This method will reap all existing connections
     * and create new connections with the master connection passed
     * in this parameter.
     *
     * @param ld master LDAP connection with new parameters.
     */
    
    public synchronized void reinitialize(LDAPConnection ld)
        throws LDAPException
    {
        if (!reinitInProgress) {
            reinitInProgress = true;
        }
        if (cleaner != null) {
            cleaner.cancel();
        }
        for (int i = 0; i < currentConnectionCount - busyConnectionCount;
            i++) {
            if ((pool[i] != null) && (pool[i].isConnected())) {
                currentPool.remove(pool[i]);
                backupPool.remove(pool[i]);
                pool[i].disconnect();
            }
        }
        if (deprecatedPool == null) {
            deprecatedPool = backupPool;
        } else {
            deprecatedPool.addAll(backupPool);
        }
        this.host = ld.getHost();
        this.port = ld.getPort();
        this.authdn = ld.getAuthenticationDN();
        this.authpw = ld.getAuthenticationPassword();
        this.ldc = (LDAPConnection) ld.clone();
        if (debug.messageEnabled()) {
            debug.message("LDAPConnection pool: " + name +
                ": reinitializing connection pool: Host:" +
                host + " Port:" + port + "Auth DN:" + authdn);
        }
        createPool();
        createIdleCleanupThread();
        if (debug.messageEnabled()) {
            debug.message("LDAPConnection pool: " + name +
                ": reinitialized successfully.");
        }
    }

    private void decreaseCurrentConnection() {
        synchronized (this) {
            try {
                LDAPConnection con = pool[currentConnectionCount -
                    busyConnectionCount - 1];
                currentPool.remove(con);
                backupPool.remove(con);
                con.disconnect();
            } catch (LDAPException e) {
                debug.error("LDAPConnection pool:" + name +
                    ":Error during disconnect.", e);
            }
            pool[currentConnectionCount - busyConnectionCount - 1] = null;
            currentConnectionCount--;
        }
    }
    
    public class CleanupTask extends GeneralTaskRunnable {
        
        private LDAPConnectionPool pool;
        private long runPeriod;
        private long timeoutPeriod;
        private int counterNeeded;
        private int thisTurn;
        private int[] nextTurn;
        private Object nextTurnLock;
        
        public CleanupTask(LDAPConnectionPool pool, long runPeriod,
            long timeoutPeriod) throws IllegalArgumentException {   
            if ((runPeriod < 0) || (timeoutPeriod < 0)){
                throw new IllegalArgumentException();
            }
            counterNeeded = (int) (timeoutPeriod / runPeriod);
            if ((timeoutPeriod % runPeriod) > 0) {
                counterNeeded++;
            }
            this.runPeriod = runPeriod;
            this.timeoutPeriod = timeoutPeriod;
            this.pool = pool;
            this.thisTurn = 0;
            this.nextTurn = new int[counterNeeded];
            this.nextTurnLock = new Object();
        }
        
        public long getRunPeriod() {
            return runPeriod;
        }
        
        public long getTimeoutPeriod() {
            return timeoutPeriod;
        }
        
        public boolean isEmpty() {
            return true;
        }
        
        public boolean removeElement(Object obj) {
            synchronized (this) {
                if (thisTurn > 0) {
                    thisTurn--;
                    return true;
                }
            }
            synchronized (nextTurnLock) {
                for (int i = 0; i < counterNeeded - 1; i++) {
                    if (nextTurn[i] > 0) {
                        nextTurn[i]--;
                        return true;
                    }
                }
            }
            return false;
        }
        
        public boolean addElement(Object obj) {
            synchronized (nextTurn) {
                nextTurn[counterNeeded - 1]++;
            }
            return true;
        }
        
        public void run() {
            synchronized (this) {
                for (int i = 0; i < thisTurn; i++) {
                    pool.decreaseCurrentConnection();
                }
                thisTurn = 0;
            }
            synchronized (nextTurnLock) {
                for (int i = 0; i < counterNeeded + 1; i++) {
                    if (i == 0) {
                        synchronized (this) {
                            thisTurn = nextTurn[0];
                        }
                    } else {
                        if (i == counterNeeded) {
                            nextTurn[counterNeeded - 1] = 0;
                        } else {
                            nextTurn[i - 1] = nextTurn[i];
                        }
                    }
                }
            }
        }
        
        public void cancel() {
            if (headTask != null) {
                synchronized(headTask) {
                    previousTask.setNext(nextTask);
                    if (nextTask != null) {
                        nextTask.setPrevious(previousTask);
                        nextTask = null;
                    }
                }
            }
        }
        
    }

    /**
     * Sets a valid ldapconnection after fallback to primary server.
     * @param con ldapconnection
     */
    public void fallBack(LDAPConnection con) {

        /*
         * Logic here is first get the HashMap key and value where the key
         * is the hostname:portnumber and value is this LDAPConnection pool
         * object, for each server configured in the serverconfig.xml.
         * Then go through the arraylist which has the list of servers
         * configured in serverconfig.xml and check the status of the primary
         * server and connect to that server if it up as well create a new
         * pool for this connection. Also remove the key from the HashMap if
         * primary is up and if there is a successfull fallback.
         */

        // If primary is up, do not fallback to any other server,eventhough
        // there are servers that are down in the HashMap.
        if (!isPrimaryUP()) {

            LDAPConnection newConn = new LDAPConnection();
            int sze = hostArrList.size();

            for (int i = 0; i < sze; i++) {
                String hpName = (String) hostArrList.get(i);
                StringTokenizer stn = new StringTokenizer(hpName,":");
                String upHost = (String)stn.nextToken();
                String upPort = (String)stn.nextToken();

                /*
                 * This 'if' check is to ensure that the shutdown server from
                 * the incoming LDAPConnection from the FallBackManager
                 * thread which got pinged and succeeded by the FallBackManager
                 * thread, matches with the host array list and gets connected
                 * for fallback to happen exactly for that shutdown server.
                 */
                if ( (upHost != null) && (upHost.length() != 0)
                    && (upPort != null) && (upPort.length() != 0)
                    && ((con.getHost()!=null) && (con.getHost().
                        equalsIgnoreCase(upHost))) ) {
                    newConn = failoverAndfallback(upHost, upPort, newConn,
                        "fallback");
                    break;
                }
            }
            reinit(newConn);
        }
    }

    /**
     * Sets a valid ldapconnection after failover to secondary server.
     * @param ld ldapconnection
     */
    public void failOver(LDAPConnection ld) {

        /* Since we are supporting fallback in FallBackManager class,
         * do the failover here, instead of relying on
         * jdk's failover mechanism.
         * Logic is look for retry error codes from releaseConnection()
         * and in the close() api,
         * from LDAPException and do the failover by calling this api.
         * Then go through the arraylist which has the list of servers
         * configured in serverconfig.xml and check the status of the
         * secondary server and connect to that server as well create a new
         * pool for this connection.
         * Also update the HashMap with the key and this LDAPConnectionPool
         * object if the server is down.
         */
        LDAPConnection newConn = new LDAPConnection();
        // Update the HashMap with the key and this LDAPConnectionPool
        // object if the server is down.
        String downKey = name + ":" + ld.getHost() + ":" +
            ld.getPort() + ":" + authdn;
        if (LDAPConnPoolUtils.connectionPoolsStatus != null) {
            synchronized(LDAPConnPoolUtils.connectionPoolsStatus) {
                LDAPConnPoolUtils.connectionPoolsStatus.put(downKey, this);
            }
        }

        int size = hostArrList.size();
        for (int i = 0; i < size; i++) {
            String hpName = (String) hostArrList.get(i);
            StringTokenizer stn = new StringTokenizer(hpName,":");
            String upHost = (String)stn.nextToken();
            String upPort = (String)stn.nextToken();

            /*
             * This 'if' check is to ensure that the shutdown server from
             * the incoming LDAPConnection from the close() method
             * do not get tried to failover.
             * failover is to happen for the next available server in line.
             */
            if ((upHost != null) && (upHost.length() != 0)
                && (upPort != null) && (upPort.length() != 0)
                && ((ld.getHost() !=null) && (!ld.getHost().
                    equalsIgnoreCase(upHost)))) {
                newConn =
                    failoverAndfallback(upHost, upPort, newConn, "failover");
                break;
            }
            // This check is for MMR DS instances on the same machine, but
            // different port numbers.
            if ((upHost != null) && (upHost.length() != 0)
                && (upPort != null) && (upPort.length() != 0)
                && (ld.getHost() !=null)) {
                int thisPort = (Integer.valueOf((String)upPort)).intValue();

                if ((ld.getHost().equalsIgnoreCase(upHost)) &&
                    (ld.getPort() != thisPort)) {
                    newConn =
                        failoverAndfallback(upHost, upPort, newConn,
                        "failover");
                    break;
                }
            }
        }
        reinit(newConn);
    }

    private LDAPConnection failoverAndfallback(
        String upHost,
        String upPort,
        LDAPConnection newConn,
        String caller) {

        if (debug.messageEnabled()) {
            debug.message("In LDAPConnectionPool:failoverAndfallback()");
        }
        int intPort = (Integer.valueOf(upPort)).intValue();
        String upKey = name + ":" + upHost + ":" +upPort + ":" + authdn;
        try {
            newConn.connect(3, upHost, intPort, authdn, authpw);
            // After successful connection, remove the key/value
            // from the hashmap to denote that the server which was
            // down earlier is up now.
            if (LDAPConnPoolUtils.connectionPoolsStatus != null) {
                synchronized(LDAPConnPoolUtils.connectionPoolsStatus) {
                    if (LDAPConnPoolUtils.connectionPoolsStatus.containsKey(
                        upKey)) {
                        LDAPConnPoolUtils.connectionPoolsStatus.remove(upKey);
                    }
                }
            }
            if (debug.messageEnabled()) {
                if (caller.equalsIgnoreCase("fallback")) {
                    debug.message("LDAPConnectionPool.failoverAndfallback()"+
                        "fall back successfully to primary host- " + upHost +
                        " primary port: " + upPort);

                } else {
                    debug.message("LDAPConnectionPool.failoverAndfallback()"+
                        "fail over success to secondary host- " + upHost +
                        " secondary port: " + upPort );
                }
            }
            return (newConn);
        } catch (LDAPException connEx) {
            // fallback to ldap v2 if v3 is not
            // supported
            if (connEx.getLDAPResultCode() == LDAPException.PROTOCOL_ERROR) {
                try {
                    newConn.connect(2, upHost, intPort, authdn, authpw);
                } catch (LDAPException conn2Ex) {
                    if (debug.messageEnabled()) {
                        if (caller.equalsIgnoreCase("fallback")) {
                            debug.message("LDAPConnectionPool."+
                                "failoverAndfallback():fallback failed.");
                        } else {
                            // Mark the host to be down and failover
                            // to the next server in line.
                            if (LDAPConnPoolUtils.
                                connectionPoolsStatus != null) {
                                synchronized(LDAPConnPoolUtils.
                                    connectionPoolsStatus) {
                                    LDAPConnPoolUtils.
                                    connectionPoolsStatus.put(upKey, this);
                                }
                            }
                            debug.message("LDAPConnectionPool."+
                                "failoverAndfallback():primary host-" +
                                upHost +" primary port-" + upPort +
                                " :is down. Failover to the"+
                                " secondary server. in catch1 ");
                        }
                    }
                }
            } else {
                if (debug.messageEnabled()) {
                    if (caller.equalsIgnoreCase("fallback")) {
                         debug.message("LDAPConnectionPool."+
                             "failoverAndfallback():continue fallback"+
                             " to next server");
                    } else {
                         // Mark the host to be down and failover
                         // to the next server in line.
                         if (LDAPConnPoolUtils.connectionPoolsStatus != null) {
                             synchronized(LDAPConnPoolUtils.
                                 connectionPoolsStatus) {
                                 LDAPConnPoolUtils.connectionPoolsStatus.put(
                                 upKey, this);
                             }
                         }
                         debug.message("LDAPConnectionPool. "+
                             "failoverAndfallback():primary host-" + upHost +
                             "primary port-" + upPort +
                             " :is down. Failover to the" +
                             " secondary server. in else");
                    }
                }
            }
        }
        return (newConn);
    }

    private boolean isPrimaryUP() {
        boolean retVal = false;
        String hpName = (String) hostArrList.get(0);
        StringTokenizer stn = new StringTokenizer(hpName,":");
        String upHost = (String)stn.nextToken();
        String upPort = (String)stn.nextToken();
        if ( (upHost != null) && (upHost.length() != 0)
            && (upPort != null) && (upPort.length() != 0) ) {
            String upKey = name + ":" + upHost + ":" +upPort + ":" + authdn;
            if (LDAPConnPoolUtils.connectionPoolsStatus != null) {
                synchronized(LDAPConnPoolUtils.connectionPoolsStatus) {
                    if (!LDAPConnPoolUtils.connectionPoolsStatus.
                        containsKey(upKey)) {
                        retVal = true;
                    }
                }
            }
        }
        return (retVal);
    }

    private void reinit(LDAPConnection newConn) {
        try {
            reinitialize(newConn);
            /*
             * Set the following default LDAPConnection options for failover
             * and fallback servers/connections.
             * searchConstraints, LDAPConnection.MAXBACKLOG,
             * LDAPConnection.REFERRALS
             */
            if ( (connOptions != null) && (!connOptions.isEmpty()) ) {
                Iterator itr = connOptions.keySet().iterator();
                while (itr.hasNext()) {
                    String optName = (String) itr.next();
                    if (optName.equalsIgnoreCase("maxbacklog")) {
                        newConn.setOption(newConn.MAXBACKLOG,
                            connOptions.get(optName));
                    }
                    if (optName.equalsIgnoreCase("referrals")) {
                        newConn.setOption(newConn.REFERRALS,
                            connOptions.get(optName));
                    }
                    if (optName.equalsIgnoreCase("searchconstraints")) {
                        newConn.setSearchConstraints(
                            (LDAPSearchConstraints)connOptions.get(optName));
                    }
                }
            }
            //Since reinitialize is cloning the LDAPConnection, disconnect the
            //original one here to avoid memory leak.
            if ((newConn != null) && (newConn.isConnected())) {
                newConn.disconnect();
            }
        } catch ( LDAPException lde ) {
            debug.error("LDAPConnectionPool:reinit()" +
                ":Error while reinitializing connection from pool.", lde);
        }
    }

    private String name;          // name of connection pool;
    private int minSize;          // Min pool size
    private int maxSize;          // Max pool size
    private String host;          // LDAP host
    private int port;             // Port to connect at
    private String authdn;        // Identity of connections
    private String authpw;        // Password for authdn
    // Following default LDAPConnection options are sent by
    // DataLayer/SMDataLayer thru constructor and are in the HashMap
    // 'connOptions'.
    // searchConstraints,LDAPConnection.MAXBACKLOG,LDAPConnection.REFERRALS
    private HashMap connOptions;
    private LDAPConnection ldc = null;          // Connection to clone
    private LDAPConnection[] pool;
    
    private long idleTime;        // idle time in milli seconds
    private boolean defunct;      // becomes true after calling destroy
    private CleanupTask cleaner;  // cleaner object
    private int busyConnectionCount;
    private int currentConnectionCount;
    private int waitCount;
    private boolean reinitInProgress;
    private HashSet deprecatedPool;
    private HashSet backupPool;
    private HashSet currentPool;
    static FallBackManager fMgr;
}

