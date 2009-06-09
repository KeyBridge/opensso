<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

   Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved

   The contents of this file are subject to the terms
   of the Common Development and Distribution License
   (the License). You may not use this file except in
   compliance with the License.

   You can obtain a copy of the License at
   https://opensso.dev.java.net/public/CDDLv1.0.html or
   opensso/legal/CDDLv1.0.txt
   See the License for the specific language governing
   permission and limitations under the License.

   When distributing Covered Code, include this CDDL
   Header Notice in each file and include the License file
   at opensso/legal/CDDLv1.0.txt.
   If applicable, add the following below the CDDL Header,
   with the fields enclosed by brackets [] replaced by
   your own identifying information:
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: logout.jsp,v 1.1 2009-06-09 20:28:30 exu Exp $

--%><%@ page language="java"
        import="com.sun.identity.saml2.common.SAML2Utils,
java.util.Enumeration"
%><%
    // log user out
    if (SAML2Utils.debug.messageEnabled()) {
        SAML2Utils.debug.message("in fedlet logout.jsp.");
    }
    Enumeration headers = request.getHeaderNames();
    while (headers.hasMoreElements()) {
        String name = (String) headers.nextElement();
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message(
                "header name=" + name + " value=" + request.getHeader(name));
        }
    }
%>
