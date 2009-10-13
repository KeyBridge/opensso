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
 * $Id: EntityRequest.java,v 1.2 2009-10-09 07:38:36 pbryan Exp $
 *
 * Copyright 2009 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.proxy.client;

import com.sun.identity.proxy.http.Request;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.entity.InputStreamEntity;

/**
 * A request that encloses an entity, suitable for submission to the Apache
 * HttpComponents Client.
 *
 * @author Paul C. Bryan
 */
public class EntityRequest extends HttpEntityEnclosingRequestBase
{
	/** The request method. */
	private String method = null;

    /**
     * Parse a string for an integer value.
     *
     * @param str The string to parse.
     * @return the integer value, or -1 if could not be parsed.
     */
    private static final int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException nfe) {
            return -1;
        }
    }

	/**
	 * Creates a new entity enclosing request for the specified incoming proxy
	 * request.
	 *
	 * @param request the incoming proxy request.
	 */
	public EntityRequest(Request request)
	{
	    this.method = request.method;

        // these headers will be suppressed by the client handler before submitting to target
        String contentEncoding = request.headers.first("Content-Encoding");
        int contentLength = parseInt(request.headers.first("Content-Length"));
        String contentType = request.headers.first("Content-Type");

        InputStreamEntity entity = new InputStreamEntity(request.entity, contentLength);
        
        if (contentEncoding != null) {
            entity.setContentEncoding(contentEncoding);
        }
        
        if (contentType != null) {
            entity.setContentType(contentType);
        }

        setEntity(entity);
	}

    @Override
    public String getMethod() {
        return method;
    }
}
