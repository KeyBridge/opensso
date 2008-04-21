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
 * $Id: FAMRecordJMQPersister.java,v 1.1 2008-04-21 18:54:21 weisun2 Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.ha.jmqdb;

import java.util.Hashtable;
import java.util.HashMap;
import java.util.Random;
import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import java.util.Iterator;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.ha.FAMRecord;
import com.sun.identity.ha.FAMRecordPersister;

/**
 * This class <code>FAMRecordJMQPersister</code> implements 
 * </code> MessageListener</code> which is to receive 
 * asynchronously delivered messages. It also sends FAMRecord,  
 * processes return message and reformat it to FAMRecord. 
 */
public class FAMRecordJMQPersister implements FAMRecordPersister,
    MessageListener { 
     
     /** Represents not found */
    static public final String NOT_FOUND = "notfound";

    /** Represents Operation status */
    static public final String OP_STATUS = "opstatus";
    /* JMQ Queue/Topic names */
    static public final String DBREQUEST = "AM_DBREQUEST";

    static public final String DBRESPONSE = "AM_DRESPONSE";

    /* JMQ Properties */
    static public final String ID = "ID";

    static public int TIMEOUT = 1000;
    
    // Private data members
    private String _id;

    TopicConnectionFactory tFactory = null;

    TopicConnection tConn = null;

    TopicSession tSession = null;

    Topic reqTopic = null;

    Topic resTopic = null;

    TopicPublisher reqPub = null;

    TopicSubscriber resSub = null;

    Hashtable processedMsgs = new Hashtable();

    Random rdmGen = new Random();

    /* Config data */
    int msgcount = 0;

   /**
    *
    * Constructs new FAMRecordJMQPersister
    * @param id SessionId
    *
    */
   public FAMRecordJMQPersister(String id) throws Exception {
        _id = id;
        // Initialize all message queues/topics
        ConnectionFactoryProvider provider = ConnectionFactoryProviderFactory
                .getProvider();
        tFactory = provider.newTopicConnectionFactory();
        tConn = tFactory.createTopicConnection();
        int flag = Session.DUPS_OK_ACKNOWLEDGE;
        tSession = tConn.createTopicSession(false, flag);
        reqTopic = tSession.createTopic(DBREQUEST);
        resTopic = tSession.createTopic(DBRESPONSE);

        reqPub = tSession.createPublisher(reqTopic);
        String selector = "ID = '" + _id + "'";
        resSub = tSession.createSubscriber(resTopic, selector, true);
        resSub.setMessageListener(this);
        tConn.start();
    }

    private String serverList = null;

    private String userName = null;

    private String password = null;

    // The read timout for retrieving the session (in SFO) needs
    // to be as small as possible since in the case where there
    // is an existing session cookie in client's browser and
    // there is no corresponding session entry in the repository
    // (e.g. timeout), client is forced to wait until this timeout
    // to be able to be redirected back to the login page.
    private int readTimeOut = 5 * 1000; /* 5 sec in millisec */

    // The read timout for getting the session count (for session
    // constraint) is different from the SFO case because the
    // master BDB node will send the response message to the
    // client even though the session count is 0.
    private int readTimeOutForConstraint = 6 * 1000;

   /**
    *
    * Constructs new FAMRecordJMQPersister
    * @param id SessionId
    * @param sList Server list
    * @param uName user name
    * @param pwd password
    * @param conTimeOut Connection Timeout
    * @param maxWaitTimeForConstraint Maximum Wait Time
    */
   public FAMRecordJMQPersister(String id, String sList,String uName,String pwd,
       int conTimeOut, int maxWaitTimeForConstraint) throws Exception {
        _id = id;
        // Initialize all message queues/topics
        serverList = sList;
        userName = uName;
        password = pwd;
        readTimeOut = conTimeOut;
        readTimeOutForConstraint = maxWaitTimeForConstraint;
        ConnectionFactoryProvider provider = ConnectionFactoryProviderFactory
                .getProvider();
        tFactory = provider
        .newTopicConnectionFactory(serverList,true, true, userName, password);
        tConn = tFactory.createTopicConnection();
        int flag = Session.DUPS_OK_ACKNOWLEDGE;
        tSession = tConn.createTopicSession(false, flag);
        reqTopic = tSession.createTopic(DBREQUEST);
        resTopic = tSession.createTopic(DBRESPONSE);

        reqPub = tSession.createPublisher(reqTopic);
        reqPub.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        String selector = "ID = '" + _id + "'";
        resSub = tSession.createSubscriber(resTopic, selector, true);
        resSub.setMessageListener(this); 
        tConn.start();
    }
   
   /** 
    * Passes a message to the listener.
    *
    * @param message the message passed to the listener
    */
   public void onMessage(Message message) {
        try {
            BytesMessage msg = (BytesMessage) message;
            long rndnum = msg.readLong();
            Long random = new Long(rndnum);

            // Determine if we have a read thread waiting...
            Object rnd = processedMsgs.get(random);
            if (rnd != null) {
                processedMsgs.put(rnd, message);
                synchronized (rnd) {
                    rnd.notify();
                }
            }
        } catch (Exception ex) {
            // Since we dont know the thread, not much we can do here -
            // we will just let the thread timeout.
            // TODO Debug.error.
        }
    }
   
   public FAMRecord send(FAMRecord famRecord) throws Exception {
       BytesMessage msg =(BytesMessage) tSession.createBytesMessage();
        
       // Write Primary key   
       String pKey = famRecord.getPrimaryKey(); 
       if (pKey != null && (!pKey.equals(""))) {
           msg.writeLong(pKey.length());
           msg.writeBytes(pKey.getBytes());
       }
       //Write expiration date 
       long expirationTime = famRecord.getExpDate(); 
       if (expirationTime > 0) {
           msg.writeLong(expirationTime);
       }
       // Write Secondary Key such as UUID
       String tmp = famRecord.getSecondarykey(); 
       if (tmp != null && (!tmp.equals(""))) {
           msg.writeLong(tmp.length());
           msg.writeBytes(tmp.getBytes());
       }
       // Write AuxData such as Master ID 
       tmp = famRecord.getAuxData();
       if (tmp != null && (!tmp.equals(""))) {
           msg.writeLong(tmp.length());
           msg.writeBytes(tmp.getBytes());
       }
       int state = famRecord.getState(); 
       if (state > 0) {
           msg.writeInt(state);
       } 
       byte[] blob = famRecord.getBlob(); 
       if (blob != null) {
           msg.writeLong(blob.length);
           msg.writeBytes(blob);
       }
       // Write extra bytes 
       HashMap mm = famRecord.getExtraByteAttributes();
       Iterator it; 
       if (mm != null) {
           it = mm.keySet().iterator(); 
       while (it.hasNext()) {
           byte[] bt = famRecord.getBytes((String) it.next()); 
           msg.writeLong(bt.length);
           msg.writeBytes(bt);
       }
       }
       // Write extra String 
       mm = famRecord.getExtraStringAttributes(); 
       if (mm != null) {
            it = mm.keySet().iterator();
            String key = null; 
        while (it.hasNext()) {
           key = (String) it.next(); 
           tmp = famRecord.getString(key); 
           msg.setStringProperty(key, tmp);
       }
       }
       // Call for action 
       msg.setStringProperty(ID, _id);
       String op =  famRecord.getOperation();
       msg.setStringProperty("op", op);
       String service = famRecord.getService();
       msg.setStringProperty("service", service);
       
       if (op.equals(FAMRecord.DELETE) || op.equals(FAMRecord.DELETEBYDATE)||
           op.equals(FAMRecord.WRITE) || op.equals(FAMRecord.SHUTDOWN)) {
           reqPub.publish(msg);
           return null; 
       } else if (op.equals(FAMRecord.READ)) {
           // Allocate a random string for onMessage to find us.
           Long random = new Long(rdmGen.nextLong());
           processedMsgs.put(random, random);
           msg.writeLong(random.longValue());
           // onMessage thread will wake us up when data is ready
           synchronized (random) {
               reqPub.publish(msg);
               random.wait(readTimeOut);
          }
          // TODO : process timeout
          BytesMessage message1 = (BytesMessage) processedMsgs.remove(random);
          String opStatus = message1.getStringProperty(OP_STATUS);
          if (opStatus != null && opStatus.equals(NOT_FOUND)) {
              throw new Exception("Session not found in repository");
          }
          
          // Fill in the return value in FAMRecord 
          // Data is in blob field 
          long len = message1.readLong();
          byte[] bytes = new byte[(int) len];
          message1.readBytes(bytes);
          FAMRecord ret = new FAMRecord(service,
              op, pKey, 0, null, 0, null, bytes); 
          return ret; 
       } else if (op.equals(FAMRecord.GET_RECORD_COUNT)){
           // Allocate a random string for onMessage to find us
           Long random = new Long(rdmGen.nextLong());
           processedMsgs.put(random, random);
           msg.writeLong(random.longValue());
           // onMessage thread will wake us up when data is ready
           synchronized (random) {
               reqPub.publish(msg);
               random.wait(readTimeOutForConstraint);
           }
           Object retMsg = processedMsgs.remove(random);
           BytesMessage message1; 
           if (retMsg instanceof Long) {
               // timeout
               return null;
           } else {
                message1 = (BytesMessage) retMsg;
           }
           //Fill in the return value in FAMRecord 
           int retCount = 0; 
           HashMap aMap = new HashMap(); 
           if (message1 != null) {
               retCount = message1.readInt();
               String hKey = null;
               for (int i = 0; i < retCount; i++) {
                   int len = message1.readInt();
                   byte[] bytes = new byte[len];
                   message1.readBytes(bytes);
                   hKey = new String(bytes);
                   Long expireTime = new Long(message1.readLong());
                   aMap.put(hKey, expireTime);
               }
            }
            FAMRecord ret = new FAMRecord(service,
                op, pKey, 0, null, 0, null, null);
            ret.setStringAttrs(aMap);
            return ret; 
           
       }  
       return null;   
   }
}
