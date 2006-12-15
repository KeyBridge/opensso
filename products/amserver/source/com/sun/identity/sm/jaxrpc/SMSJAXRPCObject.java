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
 * $Id: SMSJAXRPCObject.java,v 1.5 2006-12-15 01:19:53 goodearth Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.sm.jaxrpc;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import netscape.ldap.util.DN;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.services.comm.client.NotificationHandler;
import com.iplanet.services.comm.client.PLLClient;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.naming.WebtopNaming;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.jaxrpc.JAXRPCUtil;
import com.sun.identity.shared.jaxrpc.SOAPClient;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSObject;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SMSUtils;

public class SMSJAXRPCObject extends SMSObject implements SMSObjectListener {

    private static SOAPClient client;

    /**
     * JAXRPC Version String variable name.
     */
    public static final String AMJAXRPCVERSIONSTR = "AM_JAXRPC_VERSION";

    /**
     * JAXRPC Version String.
     */
    public static final String AMJAXRPCVERSION = "10";

    public SMSJAXRPCObject() {
        if (!initialized) {
            synchronized (SERVICE_NAME) {
                if (!initialized) {
                    // Construct the SOAP client
                    client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);

                    // Check if notification URL is provided
                    try {
                        URL url = WebtopNaming.getNotificationURL();
                        // Register with PLLClient for notificaiton
                        PLLClient.addNotificationHandler(
                                JAXRPCUtil.SMS_SERVICE,
                                new SMSNotificationHandler());
                        // Register for notification with SMS Server
                        client.send("registerNotificationURL", url.toString(),
                                null, null);
                        if (debug.messageEnabled()) {
                            debug.message("SMSJAXRPCObject: Using notification "
                                            + "mechanism for cache updates: "
                                            + url.toString());
                        }
                    } catch (Exception e) {
                        // Use polling mechanism to update caches
                        if (debug.warningEnabled()) {
                            debug.warning("SMSJAXRPCObject: Registering for "
                                    + "notification via URL failed: "
                                    + e.getMessage()
                                    + "\nUsing polling mechanism for updates");
                        }
                        // Start the daemon thread to check for changes
                        NotificationThread nt = new NotificationThread(this);
                        nt.start();
                    }
                    // Add this object to receive notifications
                    registerCallbackHandler(this);
                    initialized = true;
                }
            }
        }
    }

    /**
     * Reads in the object from persistent store. It assumes the object name and
     * the ssoToken are valid. If the entry does not exist the method should
     * return <code>null</code>
     */
    public Map read(SSOToken token, String objName) throws SMSException,
            SSOException {
        try {
            String[] objs = { token.getTokenID().toString(), objName };
            return ((Map) client.send(client.encodeMessage("read", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:read -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-read"));
        }
    }

    /**
     * Creates an entry in the persistent store. Throws an exception if the
     * entry already exists
     */
    public void create(SSOToken token, String objName, Map attributes)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), objName,
                    attributes };
            client.send(client.encodeMessage("create", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:create -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-create"));
        }
    }

    /**
     * Modifies the attributes to the object.
     */
    public void modify(SSOToken token, String objName, ModificationItem[] mods)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), objName,
                    toMods(mods) };
            client.send(client.encodeMessage("modify", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:modify -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-modify"));
        }
    }

    /**
     * Delete the entry in the datastore. This should delete sub-entries also
     */
    public void delete(SSOToken token, String objName) throws SMSException,
            SSOException {
        try {
            String[] objs = { token.getTokenID().toString(), objName };
            client.send(client.encodeMessage("delete", objs), 
                Session.getLBCookie(token.getTokenID().toString()), null);
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:delete -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-cannot-delete"));
        }
    }

    /**
     * Returns the suborganization names. Returns a set of SMSEntry objects that
     * are suborganization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchSubOrgNames(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            boolean recursive) throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn, filter,
                    new Integer(numOfEntries), new Boolean(sortResults),
                    new Boolean(ascendingOrder), new Boolean(recursive) };
            return ((Set) client.send(client.encodeMessage("searchSubOrgNames",
                    objs), Session.getLBCookie(token.getTokenID().toString()),
                    null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:searchSubOrgNames -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-suborg-cannot-search"));
        }
    }

    /**
     * Returns the organization names. Returns a set of SMSEntry objects that
     * are organization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchOrganizationNames(SSOToken token, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn,
                    new Integer(numOfEntries), new Boolean(sortResults),
                    new Boolean(ascendingOrder), serviceName, attrName, values};
            return ((Set) client.send(client.encodeMessage(
                    "searchOrganizationNames", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:searchOrganizationNames -- Exception:",
                            re);
            throw (new SMSException(re, "sms-JAXRPC-org-cannot-search"));
        }
    }

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public Set subEntries(SSOToken token, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn, filter,
                    new Integer(numOfEntries), new Boolean(sortResults),
                    new Boolean(ascendingOrder) };
            return ((Set) client.send(client.encodeMessage("subEntries", objs),
                    Session.getLBCookie(token.getTokenID().toString()), null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:subEntries -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-subentry-cannot-search"));
        }
    }

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public Set schemaSubEntries(SSOToken token, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults,
            boolean ascendingOrder) throws SMSException, SSOException {
        try {
            Object[] objs = { token.getTokenID().toString(), dn, filter,
                    sidFilter, new Integer(numOfEntries),
                    new Boolean(sortResults), new Boolean(ascendingOrder) };
            return ((Set) client.send(client.encodeMessage("schemaSubEntries",
                    objs), Session.getLBCookie(token.getTokenID().toString()),
                    null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:subEntries -- Exception:", re);
            throw (new SMSException(re,
                    "sms-JAXRPC-schemasubentry-cannot-search"));
        }
    }

    /**
     * Searchs the data store for objects that match the filter
     */
    public Set search(SSOToken token, String startDN, String filter)
            throws SMSException, SSOException {
        try {
            String[] objs = { token.getTokenID().toString(), startDN, filter };
            return ((Set) client.send(client.encodeMessage("search", objs),
                    Session.getLBCookie(token.getTokenID().toString()),
                    null));
        } catch (SSOException ssoe) {
            throw ssoe;
        } catch (SMSException smse) {
            throw smse;
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:search -- Exception:", re);
            throw (new SMSException(re, "sms-JAXRPC-error-in-searching"));
        }
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public boolean entryExists(SSOToken token, String dn) {
        // Check the caches
        if (entriesPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: entry present in cache: " + dn);
            }
            return (true);
        } else if (entriesNotPresent.contains(dn)) {
            if (debug.messageEnabled()) {
                debug.message("SMSLdapObject: entry present in "
                        + "not-present-cache: " + dn);
            }
            return (false);
        }

        // Since not present in cache, make a RPC
        boolean entryExists = false;
        try {
            String[] objs = { token.getTokenID().toString(), dn };
            Boolean b = (Boolean) client.send(client.encodeMessage(
                    "entryExists", objs), 
                    Session.getLBCookie(token.getTokenID().toString()), null);
            entryExists = b.booleanValue();
        } catch (Exception re) {
            debug.error("SMSJAXRPCObject:entryExists -- Exception:", re);
            return (false);
        }

        // Update the cache
        if (entryExists) {
            Set ee = new HashSet(entriesPresent);
            ee.add(dn);
            entriesPresent = ee;
        } else {
            Set enp = new HashSet(entriesNotPresent);
            enp.add(dn);
            entriesNotPresent = enp;
        }
        return (entryExists);
    }

    /**
     * Returns the root suffix (i.e., base DN) for the SMS objects. All
     * SMSEntries will end with this root suffix.
     */
    public String getRootSuffix() {
        if (baseDN == null) {
            try {
                baseDN = (String) client.send(client.encodeMessage(
                        "getRootSuffix", null), null, null);
            } catch (Exception re) {
                debug.error("SMSJAXRPCObject:getRootSuffix:Exception:", re);
            }
        }
        return (baseDN);
    }


    /**
     * Returns the root suffix (i.e., base DN) for the UMS objects.
     * All UMSEntries will end with this root suffix.
     */
    public String getAMSdkBaseDN() {
        if (amsdkbaseDN == null) {
            try {
                amsdkbaseDN = (String) client.send(client.encodeMessage(
                    "getAMSdkBaseDN", null), null, null);
            } catch (Exception re) {
                debug.error("SMSJAXRPCObject:getAMSdkBaseDN:Exception:", re);
            }
        }
        return (amsdkbaseDN);
    }

    /**
     * Registration of Notification Callbacks
     */
    public synchronized String registerCallbackHandler(SSOToken token,
            SMSObjectListener changeListener) throws SMSException, SSOException
            {
        return registerCallbackHandler(changeListener);
    }

    // Protected method to register callback objects
    protected String registerCallbackHandler(SMSObjectListener changeListener) {
        String id = SMSUtils.getUniqueID();
        objectListeners.put(id, changeListener);
        return (id);
    }

    /**
     * De-Registration of Notification Callbacks
     */
    public synchronized void deregisterCallbackHandler(String id) {
        objectListeners.remove(id);
    }

    public void objectChanged(String dn, int type) {
        dn = (new DN(dn)).toRFCString().toLowerCase();
        synchronized (entriesPresent) {
            if (type == DELETE) {
                // Remove from entriesPresent Set
                Set enp = new HashSet();
                for (Iterator items = entriesPresent.iterator(); items
                        .hasNext();) {
                    String odn = (String) items.next();
                    if (!dn.equals((new DN(odn)).toRFCString().toLowerCase())) {
                        enp.add(odn);
                    }
                }
                entriesPresent = enp;
            } else if (type == ADD) {
                // Remove from entriesNotPresent set
                Set enp = new HashSet();
                for (Iterator items = entriesNotPresent.iterator(); items
                        .hasNext();) {
                    String odn = (String) items.next();
                    if (!dn.equals((new DN(odn)).toRFCString().toLowerCase())) {
                        enp.add(odn);
                    }
                }
                entriesNotPresent = enp;
            }
        }
    }

    public void allObjectsChanged() {
        // do nothing
    }

    // Converts ModificationItem to String
    static String toMods(ModificationItem[] mods) throws SMSException {
        if (mods == null)
            return (null);
        StringBuffer sb = new StringBuffer(100);
        sb.append("<Modifications size=\"");
        sb.append(mods.length);
        sb.append("\">");
        for (int i = 0; i < mods.length; i++) {
            sb.append("<AttributeValuePair event=\"");
            switch (mods[i].getModificationOp()) {
            case DirContext.ADD_ATTRIBUTE:
                sb.append("ADD");
                break;
            case DirContext.REPLACE_ATTRIBUTE:
                sb.append("REPLACE");
                break;
            case DirContext.REMOVE_ATTRIBUTE:
                sb.append("DELETE");
                break;
            }
            sb.append("\"><Attribute name=\"");
            Attribute attr = mods[i].getAttribute();
            sb.append(attr.getID());
            sb.append("\"/>");
            int size = attr.size();
            for (int j = 0; j < size; j++) {
                sb.append("<Value>");
                try {
                    sb.append(attr.get(j));
                } catch (NamingException ne) {
                    throw (new SMSException(ne,
                            "sms-JAXRPC-cannot-copy-fromModItemToString"));
                }
                sb.append("</Value>");
            }
            sb.append("</AttributeValuePair>");
        }
        sb.append("</Modifications>");
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObject::ModsToString: " + sb.toString());
        }
        return (sb.toString());
    }

    // sends notifications
    static void sendNotification(String modItem) {
        String dn = modItem.substring(4);
        int type = SMSObjectListener.MODIFY;
        if (modItem.startsWith("ADD:"))
            type = SMSObjectListener.ADD;
        if (modItem.startsWith("DEL:"))
            type = SMSObjectListener.DELETE;
        // Send notifications
        for (Iterator objs = objectListeners.values().iterator(); objs
                .hasNext();) {
            SMSObjectListener listener = (SMSObjectListener) objs.next();
            listener.objectChanged(dn, type);
        }
    }

    // Static variables
    private static String baseDN;

    private static String amsdkbaseDN;

    private static Set entriesPresent = new HashSet();

    private static Set entriesNotPresent = new HashSet();

    private static boolean initialized;

    protected static boolean isLocal;

    private static Debug debug = Debug.getInstance("amSMS");

    private static Map objectListeners = new HashMap();

    private static final String SERVICE_NAME = "SMSJAXRPCObject";

    // Inner class to check for notifications
    static class NotificationThread extends Thread {

        static final String CACHE_TIME_PROPERTY = 
            "com.sun.identity.sm.cacheTime";

        static int pollingTime = 1;

        static int sleepTime = 1000 * 60;

        NotificationThread(SMSJAXRPCObject jaxclient) {
            // Set this as a daemon thread
            setDaemon(true);
            // Read cache polling time (in minutes)
            String cacheTime = SystemProperties.get(CACHE_TIME_PROPERTY);
            if (cacheTime != null) {
                try {
                    pollingTime = Integer.parseInt(cacheTime);
                    if (pollingTime > 0) {
                        sleepTime = pollingTime * 1000 * 60;
                    }
                } catch (NumberFormatException nfe) {
                    debug.error("SMSJAXRPCObject::NotificationThread:: "
                            + "Cache Time error: " + cacheTime, nfe);
                }
            }
        }

        // Get the modification list and send notifications
        public void run() {
            boolean gotoSleep = false;
            SOAPClient client = new SOAPClient(JAXRPCUtil.SMS_SERVICE);
            while (true) {
                try {
                    if (gotoSleep)
                        sleep(sleepTime);
                    Object obj[] = { new Integer(pollingTime) };
                    Set mods = (Set) client.send(client.encodeMessage(
                            "objectsChanged", obj), null, null);
                    if (debug.messageEnabled()) {
                        debug.message("SMSJAXRPCObject:"
                                + "NotificationThread retrived changes: "
                                + mods);
                    }
                    Iterator items = mods.iterator();
                    while (items.hasNext()) {
                        sendNotification((String) items.next());
                    }
                    gotoSleep = true;
                } catch (NumberFormatException nfe) {
                    // Should not happend
                    debug.warning("SMSJAXRCPObject::NotificationThread:run "
                            + "Number Format Exception for polling Time: "
                            + pollingTime, nfe);
                } catch (SMSException smse) {
                    if (smse.getExceptionCode() != 
                        SMSException.STATUS_REPEATEDLY_FAILED)
                        gotoSleep = false;
                    debug.warning("SMSJAXRPCObject::NotificationThread:run "
                            + "SMSException", smse);
                } catch (InterruptedException ie) {
                    gotoSleep = false;
                    debug.warning("SMSJAXRPCObject::NotificationThread:run "
                            + "Interrupted Exception", ie);
                } catch (Exception re) {
                    gotoSleep = true;
                    debug.warning("SMSJAXRPCObject::NotificationThread:run "
                            + "Exception", re);
                }
            }
        }
    }

    // Inner class handle SMS change notifications
    static class SMSNotificationHandler implements NotificationHandler {
        SMSNotificationHandler() {
            // Empty constructor
        }

        // Process the notification objects
        public void process(Vector notifications) {
            for (int i = 0; i < notifications.size(); i++) {
                Notification notification = (Notification) notifications
                        .elementAt(i);
                String content = notification.getContent();
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObject:SMSNotificationHandler: "
                            + " received notification: " + content);
                }
                // Send notification
                sendNotification(content);
            }
        }
    }
}
