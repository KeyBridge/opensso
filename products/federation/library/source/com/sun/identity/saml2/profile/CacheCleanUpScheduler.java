/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CacheCleanUpScheduler.java,v 1.3 2008-06-25 05:47:53 qcheng Exp $
 *
 */


package com.sun.identity.saml2.profile;

import com.sun.identity.common.SystemTimerPool;
import com.sun.identity.common.TimerPool;
import java.util.Date;

public class CacheCleanUpScheduler {
    
    /* Schedule the periodic containers to SystemTimerPool. */
    public static void doSchedule() {
        TimerPool pool = SystemTimerPool.getTimerPool();
        Date nextRun = new Date(((System.currentTimeMillis() +
            (SPCache.interval * 1000)) / 1000) * 1000);
        pool.schedule(SPCache.requestHash, nextRun);
        pool.schedule(SPCache.responseHash, nextRun);
        pool.schedule(SPCache.mniRequestHash, nextRun);
        pool.schedule(SPCache.relayStateHash, nextRun);
        pool.schedule(IDPCache.authnRequestCache, nextRun);
        pool.schedule(IDPCache.assertionCache, nextRun);
    }
    
}
