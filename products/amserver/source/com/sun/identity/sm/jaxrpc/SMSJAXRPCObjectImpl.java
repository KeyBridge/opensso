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
 * $Id: SMSJAXRPCObjectImpl.java,v 1.13 2008-03-18 19:51:39 goodearth Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.sm.jaxrpc;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.comm.server.PLLServer;
import com.iplanet.services.comm.server.SendNotificationException;
import com.iplanet.services.comm.share.Notification;
import com.iplanet.services.comm.share.NotificationSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.jaxrpc.JAXRPCUtil;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.CachedSMSEntry;
import com.sun.identity.sm.CachedSubEntries;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SMSUtils;

public class SMSJAXRPCObjectImpl implements SMSObjectIF, SMSObjectListener {

    static Debug debug = SMSEntry.debug;

    static Map notificationURLs = new HashMap();

    static SSOTokenManager tokenMgr;

    static SSOException initializationError;

    static String baseDN;

    static String amsdkbaseDN;

    static boolean initialized;

    static String serverURL;

    // Cache of modified DNs for the last 30 minutes
    static int cacheSize = 30;

    static LinkedList cacheIndices = new LinkedList();

    static HashMap cache = new HashMap(cacheSize);

    // Default constructor
    public SMSJAXRPCObjectImpl() {
        initialize();
    }

    // Initialization to register the callback handler
    private void initialize() {
        if (!initialized) {
            try {
                tokenMgr = SSOTokenManager.getInstance();
            } catch (SSOException ssoe) {
                debug.error("SMSJAXRPCObject: "
                        + "Unable to get SSO Token Manager");
                initializationError = ssoe;
            }
            synchronized (notificationURLs) {
                if (!initialized) {
                    try {
                        SMSEntry.registerCallbackHandler(null, this);
                    } catch (Exception e) {
                        debug.warning("SMSJAXRPCObjectImpl(): unable to "
                                + "register for callback handler", e);
                    }
                }
            }
            // Construct server URL
            String namingURL = SystemProperties.get(Constants.AM_NAMING_URL);
            if (namingURL != null) {
                int index = namingURL.toLowerCase().indexOf("/namingservice");
                if (index != -1) {
                    serverURL = namingURL.substring(0, index);
                } else {
                    serverURL = "";
                }
            } else {
                serverURL = SystemProperties.getServerInstanceName();
                if (serverURL == null) {
                    serverURL = "";
                }
            }
            initialized = true;
        }
    }

    // Method to check if service is local and also to
    // test if the server is down
    public void checkForLocal() {
        SMSJAXRPCObject.isLocal = true;
    }

    /**
     * Returns the attribute names and values of the provided object using the
     * identity of the provided SSO Token
     */
    public Map read(String tokenID, String objName)
        throws SMSException, SSOException, RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::read dn: " + objName);
        }

        Map returnAttributes = null;
        if (objName.equals("o=" + SMSJAXRPCObject.AMJAXRPCVERSIONSTR)) {

            returnAttributes = new HashMap();
            returnAttributes.put(SMSJAXRPCObject.AMJAXRPCVERSIONSTR,
                   SMSJAXRPCObject.AMJAXRPCVERSION);
        } else {
            CachedSMSEntry ce = CachedSMSEntry.getInstance(getToken(tokenID),
                objName, null);                    
            Map attrs = ce.getSMSEntry().getAttributes();
            if ((attrs != null) && (attrs instanceof CaseInsensitiveHashMap)) {
                returnAttributes = new HashMap();
                for (Iterator items = attrs.keySet().iterator(); 
                    items.hasNext();) {
                    String attrName = items.next().toString();
                    Object o = attrs.get(attrName);
                    returnAttributes.put(attrName, o);
                }            
            } else { // could be null or instance of HashMap - return as it is.
                returnAttributes = attrs;
            }                             
        }
        return returnAttributes;

    }

    /**
     * Creates an entry in the persistent store. Throws an exception if the
     * entry already exists
     */
    public void create(String tokenID, String objName, Map attributes)
            throws SMSException, SSOException, RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::create dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.setAttributes(attributes);
        entry.save();
    }

    /**
     * Modifies the attributes to the object.
     */
    public void modify(String tokenID, String objName, String mods)
            throws SMSException, SSOException, RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::modify dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.modifyAttributes(getModItems(mods));
        entry.save();
    }

    /**
     * Delete the entry in the datastore. This should delete sub-entries also
     */
    public void delete(String tokenID, String objName) throws SMSException,
            SSOException, RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::delete dn: " + objName);
        }
        SMSEntry entry = new SMSEntry(getToken(tokenID), objName);
        entry.delete();
    }

    /**
     * Returns the suborganization names. Returns a set of SMSEntry objects that
     * are suborganization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if code>0</code> returns
     * all the entries.
     */
    public Set searchSubOrgNames(String tokenID, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            boolean recursive) throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::searchSubOrgNames dn: " + dn);
        }
        CachedSubEntries ce = CachedSubEntries.getInstance(
            getToken(tokenID), dn);
        return (ce.searchSubOrgNames(getToken(tokenID), filter, recursive));
    }

    /**
     * Returns the organization names. Returns a set of SMSEntry objects that
     * are organization names. The paramter <code>numOfEntries</code>
     * identifies the number of entries to return, if <code>0</code> returns
     * all the entries.
     */
    public Set searchOrganizationNames(String tokenID, String dn,
            int numOfEntries, boolean sortResults, boolean ascendingOrder,
            String serviceName, String attrName, Set values)
            throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::searchOrganizationNames dn: "
                    + dn);
        }

        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.searchOrgNames(getToken(tokenID), serviceName,
                attrName, values));
    }

    /**
     * Returns the sub-entries. Returns a set of SMSEntry objects that are
     * sub-entries. The paramter <code>numOfEntries</code> identifies the
     * number of entries to return, if <code>0</code> returns all the entries.
     */
    public Set subEntries(String tokenID, String dn, String filter,
            int numOfEntries, boolean sortResults, boolean ascendingOrder)
            throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::subentries dn: " + dn);
        }
        
        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.getSubEntries(getToken(tokenID), filter));        
    }

    /**
     * Returns the sub-entries matching the schema id. Returns a set of SMSEntry
     * objects that are sub-entries for the provided schema id. The paramter
     * <code>numOfEntries</code> identifies the number of entries to return,
     * if <code>0</code> returns all the entries.
     */
    public Set schemaSubEntries(String tokenID, String dn, String filter,
            String sidFilter, int numOfEntries, boolean sortResults, boolean ao)
            throws SMSException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::subentries dn: " + dn);
        }
        
        CachedSubEntries ce = CachedSubEntries.getInstance(
                getToken(tokenID), dn);
        return (ce.getSchemaSubEntries(getToken(tokenID), filter, sidFilter));
    }

    /**
     * Searchs the data store for objects that match the filter
     */
    public Set search(String tokenID, String startDN, String filter)
            throws SMSException, SSOException, RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::search dn: " + startDN
                    + " filter: " + filter);
        }
        return (SMSEntry.search(getToken(tokenID), startDN, filter));
    }

    /**
     * Checks if the provided DN exists. Used by PolicyManager.
     */
    public boolean entryExists(String tokenID, String objName)
            throws SSOException, RemoteException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObjectImpl::exists dn: " + objName);
        }
        boolean entryExists = false;
        try {
            CachedSMSEntry ce = CachedSMSEntry.getInstance(getToken(tokenID),
                objName, null);
            entryExists = !(ce.getSMSEntry().isNewEntry());
        } catch (SMSException smse) {
            // Ignore the exception
        }
        return (entryExists);
    }

    /**
     * Returns the root suffix (i.e., base DN) for the SMS objects. All
     * SMSEntries will end with this root suffix.
     */
    public String getRootSuffix() throws RemoteException {
        if (baseDN == null) {
            baseDN = SMSEntry.getRootSuffix();
        }
        return (baseDN);
    }

    /**
     * Returns the root suffix (i.e., amsdkbase DN) for the UMS objects.
     * All UMSEntries will end with this root suffix.
     */
    public String getAMSdkBaseDN() throws RemoteException {
        if (amsdkbaseDN == null) {
            amsdkbaseDN = SMSEntry.getAMSdkBaseDN();
        }
        return (amsdkbaseDN);
    }

    // Implementation to receive requests from clients
    // Returns changes in the past <i>time</i> minutes
    public synchronized Set objectsChanged(int time) throws RemoteException {
        Set answer = new HashSet();
        // Get the cache index for times upto time+2
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        // Add 1 minute to offset, the initial lookup
        calendar.add(Calendar.MINUTE, 1);
        for (int i = 0; i < time + 3; i++) {
            calendar.add(Calendar.MINUTE, -1);
            String cacheIndex = calendarToString(calendar);
            Set modDNs = (Set) cache.get(cacheIndex);
            if (modDNs != null)
                answer.addAll(modDNs);
        }
        return (answer);
    }

    // Implementation for SMSObjectListener
    public synchronized void objectChanged(String name, int type) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        String cacheIndex = calendarToString(calendar);
        Set modDNs = (Set) cache.get(cacheIndex);
        if (modDNs == null) {
            modDNs = new HashSet();
            cache.put(cacheIndex, modDNs);
            // Maintain cacheIndex
            cacheIndices.addFirst(cacheIndex);
            if (cacheIndices.size() > cacheSize) {
                String index = (String) cacheIndices.removeLast();
                cache.remove(index);
            }
        }
        String modItem = null;
        switch (type) {
        case ADD:
            modItem = "ADD:" + name;
            break;
        case DELETE:
            modItem = "DEL:" + name;
            break;
        default:
            modItem = "MOD:" + name;
        }
        modDNs.add(modItem);

        // If notification URLs are present, send notifications
        synchronized (notificationURLs) {
            for (Iterator entries = notificationURLs.entrySet().iterator(); 
                entries.hasNext();) {
                synchronized (entries) {
                    Map.Entry entry = (Map.Entry) entries.next();
                    String id = (String) entry.getKey();
                    URL url = (URL) entry.getValue();

                    // Construct NotificationSet
                    Notification notification = new Notification(modItem);
                    NotificationSet ns = 
                        new NotificationSet(JAXRPCUtil.SMS_SERVICE);
                    ns.addNotification(notification);
                    try {
                        PLLServer.send(url, ns);
                        if (debug.messageEnabled()) {
                            debug.message("SMSJAXRPCObjectImpl:sentNotification "
                            + "URL: " + url + " Data: " + ns);
                        }
                    } catch (SendNotificationException ne) {
                        if (debug.warningEnabled()) {
                            debug.warning("SMSJAXRPCObject: failed sending "
                                + "notification to: " + url + "\nRemoving "
                                + "URL from notification list.", ne);
                        }
                        // Remove the URL from Notification List
                        notificationURLs.remove(id);
                    }
                }
            }
        }
    }

    public void allObjectsChanged() {
        // do nothing
    }

    // Methods to register notification URLs
    public String registerNotificationURL(String url)
            throws RemoteException {
        String id = SMSUtils.getUniqueID();
        try {
            // Check URL is not the local server
            if (!url.toLowerCase().startsWith(serverURL)) {
                synchronized (notificationURLs) {
                    notificationURLs.put(id, new URL(url));
                }
                if (debug.messageEnabled()) {
                    debug.message("SMSJAXRPCObjectImpl:register for "
                            + "notification URL: " + url);
                }
            } else {
                // Cannot add this server for notifications
                if (debug.warningEnabled()) {
                    debug.warning("SMSJAXRPCObjectImpl:registerURL "
                            + "cannot add local server: " + url);
                }
                throw (new RemoteException("invalid-notification-URL"));
            }
        } catch (MalformedURLException e) {
            if (debug.warningEnabled()) {
                debug.warning("SMSJAXRPCObjectImpl:registerNotificationURL "
                        + " invalid URL: " + url, e);
            }
        }
        return (id);
    }

    public void deRegisterNotificationURL(String id)
            throws RemoteException {
        synchronized (notificationURLs) {
            notificationURLs.remove(id);
        }
    }

    public void notifyObjectChanged(String name, int type)
            throws RemoteException {
        SMSEntry.objectChanged(name, type);
    }

    private static String calendarToString(Calendar calendar) {
        // Get year, month, date, hour and minute
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        // Clear the calendar, set the params and get the string
        calendar.clear();
        calendar.set(year, month, date, hour, minute);
        return (serverURL + calendar.toString());
    }

    /**
     * Returns SSOToken from token ID
     */
    private static SSOToken getToken(String tokenID) throws SSOException {
        if (initializationError != null)
            throw (initializationError);
        return (tokenMgr.createSSOToken(tokenID));
    }

    /**
     * Returns an array of ModificationItems converted from string
     * representation of mods. The string representation is of the format:
     * <pre>
     * <Modifications size="xx"> <AttributeValuePair event="ADD | REPLACE |
     * DELETE"> <Attribute name="attrName" /> <Value>...</Value>
     * </AttributeValuePair> </Modifications>
     * </pre>
     */
    static ModificationItem[] getModItems(String mods) throws SMSException {
        if (debug.messageEnabled()) {
            debug.message("SMSJAXRPCObject::StringToMods: " + mods);
        }
        ModificationItem[] answer = null;
        try {
            if (mods != null) {
                mods = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + mods;
                Document doc = XMLUtils.toDOMDocument(mods, debug);
                Node root = XMLUtils.getRootNode(doc, "Modifications");
                int modsSize = Integer.parseInt(XMLUtils.getNodeAttributeValue(
                        root, "size"));
                answer = new ModificationItem[modsSize];
                NodeList nl = root.getChildNodes();
                for (int i = 0; i < modsSize; i++) {
                    Node node = nl.item(i);
                    if (node.getNodeName().equals("AttributeValuePair")) {
                        String eventS = XMLUtils.getNodeAttributeValue(node,
                                "event");
                        int event = DirContext.ADD_ATTRIBUTE;
                        if (eventS.equals("REPLACE"))
                            event = DirContext.REPLACE_ATTRIBUTE;
                        else if (eventS.equals("DELETE"))
                            event = DirContext.REMOVE_ATTRIBUTE;
                        Node attrNode = XMLUtils
                                .getChildNode(node, "Attribute");
                        String attrName = XMLUtils.getNodeAttributeValue(
                                attrNode, "name");
                        Set values = XMLUtils.getAttributeValuePair(node);
                        // Construct ModificationItem
                        BasicAttribute attr = new BasicAttribute(attrName);
                        for (Iterator it = values.iterator(); it.hasNext();)
                            attr.add(it.next());
                        answer[i] = new ModificationItem(event, attr);
                    }
                }
            }
        } catch (Exception e) {
            throw (new SMSException(e,
                    "sms-JAXRPC-cannot-copy-fromModStringToModItem"));
        }
        return (answer);
    }
}
