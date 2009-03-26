/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ResourceSearchIndexes.java,v 1.2 2009-03-25 16:14:26 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.Set;

/**
 * This class encapsulates the result of resource splitting.
 */
public class ResourceSearchIndexes {
    private Set<String> hostIndexes;
    private Set<String> pathIndexes;
    private Set<String> path;
    
    public ResourceSearchIndexes(
        Set<String> hostIndexes,
        Set<String> pathIndexes,
        Set<String> path
    ) {
        this.hostIndexes = hostIndexes;
        this.pathIndexes = pathIndexes;
        this.path = path;
    }

    public Set<String> getHostIndexes() {
        return hostIndexes;
    }

    public Set<String> getPath() {
        return path;
    }

    public Set<String> getPathIndexes() {
        return pathIndexes;
    }
}