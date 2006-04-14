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
 * $Id: IAuthorizer.java,v 1.2 2006-04-14 09:05:25 veiming Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.log.spi;

/**
 * This interface defines method for authorization of log operation.
 * @supported.all.api
 */
public interface IAuthorizer {
    /**
     * Returns <code>true</code> if a given log record should be published.
     *
     * @param logName Log name on which operation is to be performed.
     * @param operation The log operation to be performed.
     * @param credential The credential to be authorized.
     * @return <code>true</code> if the credential is authorized.
     */
    public boolean isAuthorized (
        String logName,
        String operation,
        Object credential);
    
    /**
     * Returns <code>true</code> if given subject is authorized to change the
     * password.
     *
     * @param credential Credential to be checked for authorization.
     * @return <code>true</code> if given subject is authorized to change the
     *         password.
     */
    public boolean isAuthorized(Object credential);
}
