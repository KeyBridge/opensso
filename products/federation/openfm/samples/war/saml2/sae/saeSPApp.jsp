<%--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
  
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

   $Id: saeSPApp.jsp,v 1.3 2008-07-16 22:46:50 rajeevangal Exp $

--%>

<%@ page import="com.sun.identity.sae.api.SecureAttrs"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="com.sun.identity.common.SystemConfigurationUtil"%>
<%!
public void jspInit()
{
}
%>
<html>
<%
    String deployuri = SystemConfigurationUtil.getProperty(
        "com.iplanet.am.services.deploymentDescriptor");
    if ((deployuri == null) || (deployuri.length() == 0)) {
        deployuri = "../../..";
    }
%>
<html>
<head>
<title>Secure Attributes Exchange IDP APP SAMPLE</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>
<body>
<%@ include file="header.jsp" %>
<br><b>SAE SP APP SAMPLE</b><br>
<br>
<% 
    try {
        String cryptotype = SecureAttrs.SAE_CRYPTO_TYPE_SYM;
        // Symmetric : Shared secret between this SP-App and local FM-SP
        // Asymmetric : pub key alias of FM-SP 
        String secret = "secret12";

        // Use a cached instance if available
        String mySecAttrInstanceName = "sample_"+cryptotype;
        SecureAttrs sa = SecureAttrs.getInstance(mySecAttrInstanceName);
        if (sa == null) {
            Properties saeparams = new Properties();
            if (SecureAttrs.SAE_CRYPTO_TYPE_ASYM.equals(cryptotype)) {
              saeparams.setProperty(SecureAttrs.SAE_CONFIG_KEYSTORE_TYPE, "JKS");
              saeparams.put(SecureAttrs.SAE_CONFIG_PRIVATE_KEY_ALIAS, "testcert");
              saeparams.put(SecureAttrs.SAE_CONFIG_KEYSTORE_FILE, "/export/home/test/mykeystore");;
              saeparams.put(SecureAttrs.SAE_CONFIG_KEYSTORE_PASS, "22222222");
              saeparams.put(SecureAttrs.SAE_CONFIG_PRIVATE_KEY_PASS, "11111111");
            }
            SecureAttrs.init(mySecAttrInstanceName, cryptotype, saeparams);
            sa = SecureAttrs.getInstance(mySecAttrInstanceName);
        }

        String sunData = request.getParameter(SecureAttrs.SAE_PARAM_DATA);
        Map secureAttrs = sa.verifyEncodedString(sunData, secret);
        if (secureAttrs == null) {
%>
             <br>Secure Attrs Verification failed.
<%
        } else {
            Iterator iter = secureAttrs.entrySet().iterator();
%>
    <table>
            <br>Secure Attrs :
<%
            while(iter.hasNext()) {
                Map.Entry entry = (Map.Entry)iter.next();
                String key = (String)entry.getKey();
                String value = (String)entry.getValue();
%>
                <tr><td><%=key%></td><td><%=value%></td></tr>
<%
            }
%>
    </table>
<%
            if (SecureAttrs.SAE_CMD_LOGOUT.equals(
                 secureAttrs.get(SecureAttrs.SAE_PARAM_CMD))) {
%>
                <br><br><a href="<%=secureAttrs.get("sun.returnurl")%>">Logout URL</a>
<%
            }
%>
<br><br>

<%
        }
    } catch (Exception ex) {
%>
         <br>Secure Attrs Verification failed. Exception : <%=ex%>
         <br><br>
<%    
         ex.printStackTrace(new PrintWriter(out));
    }
%>
</body>
</html>
