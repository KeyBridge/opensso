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
 * $Id: NamingRequest.java,v 1.2 2007-11-29 23:14:28 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.iplanet.services.naming.share;

/**
 * This <code>NamingRequest</code> class represents a NamingRequest XML
 * document. The NamingRequest DTD is defined as the following:
 * </p>
 * 
 * <pre>
 *     &lt;?xml version=&quot;1.0&quot;&gt;
 *     &lt; !DOCTYPE NamingRequest [
 *     &lt; !ELEMENT NamingRequest (GetNamingProfile)&gt;
 *     &lt; !ATTLIST NamingRequest
 *       vers   CDATA #REQUIRED
 *       reqid  CDATA #REQUIRED
 *       sessid CDATA #REQUIRED&gt;
 *     &lt; !ELEMENT GetNamingProfile EMPTY&gt;
 *     ]&gt;
 * </pre>
 * 
 * </p>
 */

public class NamingRequest {

    static final String QUOTE = "\"";

    static final String NL = "\n";

    private String requestVersion = null;

    private String requestID = null;

    private String sessionId = null;

    private static int requestCount = 0;

    public static final String reqVersion = "3.0";

    /*
     * Constructors
     */

    /**
     * This constructor shall only be used at the client side to construct a
     * NamingRequest object.
     * 
     * @param ver
     *            The naming request version.
     */
    public NamingRequest(String ver) {
        float version = Float.valueOf(ver).floatValue();
        requestVersion = (version <= 1.0) ? reqVersion : ver;
        requestID = Integer.toString(requestCount++);
    }

    /*
     * This constructor is used by NamingRequestParser to reconstruct a
     * NamingRequest object.
     */
    NamingRequest() {
    }

    /**
     * This method is used primarily at the server side to reconstruct a
     * NamingRequest object based on the XML document received from client. The
     * DTD of this XML document is described above.
     * 
     * @param xml
     *            The NamingRequest XML document String.
     */
    public static NamingRequest parseXML(String xml) {
        NamingRequestParser parser = new NamingRequestParser(xml);
        return parser.parseXML();
    }

    /**
     * Sets the request version.
     * 
     * @param version
     *            A string representing the request version.
     */
    void setRequestVersion(String version) {
        requestVersion = version;
    }

    /**
     * Gets the request version.
     * 
     * @return The request version.
     */
    public String getRequestVersion() {
        return requestVersion;
    }

    /**
     * Sets the request ID.
     * 
     * @param id
     *            A string representing the request ID.
     */
    void setRequestID(String id) {
        requestID = id;
    }

    /**
     * Gets the request ID.
     * 
     * @return The request ID.
     */
    public String getRequestID() {
        return requestID;
    }

    /**
     * Sets the session ID.
     * 
     * @param id
     *            A string representing the session ID.
     */
    void setSessionId(String id) {
        sessionId = id;
    }

    /**
     * Gets the session ID.
     * 
     * @return The session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * This method translates the request to an XML document String based on the
     * NamingRequest DTD described above.
     * 
     * @return An XML String representing the request.
     */
    public String toXMLString() {
        StringBuffer xml = new StringBuffer(150);
        xml.append("<NamingRequest vers=").append(QUOTE).append(requestVersion)
                .append(QUOTE).append(" reqid=").append(QUOTE)
                .append(requestID).append(QUOTE).append(">").append(NL);
        xml.append("<GetNamingProfile>").append(NL);
        xml.append("</GetNamingProfile>").append(NL);
        xml.append("</NamingRequest>");

        return xml.toString();
    }
}
