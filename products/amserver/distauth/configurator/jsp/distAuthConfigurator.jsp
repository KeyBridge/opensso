<%--
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

   $Id: distAuthConfigurator.jsp,v 1.2 2008-03-21 06:28:04 manish_rustagi Exp $

   Copyright 2007 Sun Microsystems Inc. All Rights Reserved
--%>


<html>
<head>
<title>Configure DistAuth</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="css/css_ns6up.css" />


<%@ page import="
com.iplanet.am.util.SystemProperties,
com.sun.identity.security.EncodeAction,
com.iplanet.services.util.Crypt,
com.sun.identity.distauth.setup.SetupDistAuthWAR,
java.io.*,
java.security.AccessController,
java.util.Properties"
%>

<%
    String configFile = System.getProperty("user.home") +
        File.separator + "AMDistAuthConfig.properties";
    String configTemplate = "/WEB-INF/classes/AMDistAuthConfig.properties.template";
    String errorMsg = null;
    boolean configured = false;
    String famProt = null; 
    String famHost = null;
    String famPort = null;
    String famDeploymenturi = null;
    String debugDir = null;
    String debugLevel = "error";
    String encryptionKey = SetupDistAuthWAR.generateKey();    
    String distAuthProt = request.getScheme(); 
    String distAuthHost = request.getServerName();
    String distAuthPort = String.valueOf(request.getServerPort());
    String distAuthDeploymenturi = request.getContextPath();
    String distAuthCookieName = "AMDistAuthCookie";
    String appUser = null;
    String appPassword = null;
    String confirmAppPassword = null;

    File configF = new File(configFile);
    if (configF.exists()) {
        errorMsg = "The DistAuth application has already been configued.<br>" +
            "Configuration file : " + configFile + "<br><p><br>" +
            "Click <a href=\"index.html\">here</a> to go to login page.";
        // reinitialize properties
        Properties props = new Properties();
        props.load(new FileInputStream(configFile));
        SystemProperties.initializeProperties(props);
        configured = true;
    } else {
        String submit = request.getParameter("submit");
        String servletPath = request.getServletPath();        

        if (submit != null) { 
            famProt = request.getParameter("famProt");
            famHost = request.getParameter("famHost");
            famPort = request.getParameter("famPort");
            famDeploymenturi = request.getParameter("famDeploymenturi");
            distAuthProt = request.getParameter("distAuthProt");
            distAuthHost = request.getParameter("distAuthHost");
            distAuthPort = request.getParameter("distAuthPort");
            distAuthDeploymenturi = request.getParameter("distAuthDeploymenturi");
            distAuthCookieName = request.getParameter("distAuthCookieName");
            debugDir = request.getParameter("debugDir");
            debugLevel = request.getParameter("debugLevel");
            encryptionKey = request.getParameter("encryptionKey");
            appUser = request.getParameter("appUser");
            appPassword = request.getParameter("appPassword");
            confirmAppPassword = request.getParameter("confirmAppPassword");
        
            if ((famProt != null) && !famProt.equals("") && 
                (famHost != null) && !famHost.equals("") && 
                (famPort != null) && !famPort.equals("") &&
                (famDeploymenturi != null) && !famDeploymenturi.equals("") && 
                (distAuthProt != null) && !distAuthProt.equals("") && 
                (distAuthHost != null) && !distAuthHost.equals("") && 
                (distAuthPort != null) && !distAuthPort.equals("") &&  
                (distAuthDeploymenturi != null) && !distAuthDeploymenturi.equals("") &&
                (distAuthCookieName != null) && !distAuthCookieName.equals("") && 
                (debugLevel != null) && !debugLevel.equals("") &&  
                (debugDir != null) && !debugDir.equals("") &&
                (encryptionKey != null) && !encryptionKey.equals("") &&                  
                (appUser != null) && !appUser.equals("") &&
                (appPassword != null) && !appPassword.equals("") &&
                appPassword.equals(confirmAppPassword)){
                Properties props = new Properties();
                props.setProperty("SERVER_PROTOCOL", famProt);
                props.setProperty("SERVER_HOST", famHost);
                props.setProperty("SERVER_PORT", famPort);
                props.setProperty("DEPLOY_URI", famDeploymenturi);
                props.setProperty("DISTAUTH_SERVER_PROTOCOL", distAuthProt);
                props.setProperty("DISTAUTH_SERVER_HOST", distAuthHost);
                props.setProperty("DISTAUTH_SERVER_PORT", distAuthPort);
                props.setProperty("DISTAUTH_DEPLOY_URI", distAuthDeploymenturi);
                props.setProperty("DISTAUTH_COOKIE_NAME", distAuthCookieName);                
                props.setProperty("DEBUG_DIR", debugDir);
                props.setProperty("DEBUG_LEVEL", debugLevel);
                props.setProperty("ENCRYPTION_KEY", encryptionKey);
                props.setProperty("ENCRYPTION_KEY_LOCAL", encryptionKey);
                props.setProperty("APPLICATION_USER", appUser);
                props.setProperty("APPLICATION_PASSWD", "");
                SystemProperties.initializeProperties("am.encryption.pwd",encryptionKey);
                props.setProperty("ENCODED_APPLICATION_PASSWORD",Crypt.encrypt(appPassword));
                props.setProperty("AM_COOKIE_NAME", "iPlanetDirectoryPro");                
                props.setProperty("AM_COOKIE_SECURE", "false");
                props.setProperty("AM_COOKIE_ENCODE", "false");
                props.setProperty("NAMING_URL", famProt + "://" + famHost + ":"
                    + famPort + famDeploymenturi + "/namingservice");
                props.setProperty("NOTIFICATION_URL", distAuthProt + "://" + distAuthHost + ":"
                    + distAuthPort + distAuthDeploymenturi + "/notificationservice");                
                
                try {
                    SetupDistAuthWAR configurator = 
                        new SetupDistAuthWAR(
                        getServletConfig().getServletContext());
                    configurator.createAMDistAuthConfigProperties(configFile, 
                        configTemplate, props);
                    configurator.setAMDistAuthConfigProperties(configFile);
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                    errorMsg = "Unable to create sample AMDistAuthConfig.properties " +
                       "file: " + ioex.getMessage();
                }
                configured = true;
            } else {
                errorMsg = "All the fields are required."; 
                if((appPassword != null) && !appPassword.equals("")){
                    if(!appPassword.equals(confirmAppPassword)){
                        errorMsg = "Application user password and confirm " +
                        "Application user password does not match";
                    }
                }
            }
        }
    }
%>

</head>

<body class="DefBdy">
                                                                                
<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>
                                                                                
<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>
                                                                                
<table border="0" cellpadding="10" cellspacing="0" width="100%">
<tr><td>


<%
    if (!configured) {
%>

<h3>Configuring DistAuth Application</h3>

<form action="distAuthConfigurator.jsp" method="GET">
    Please provide the Federated Access Manager Server Information.
    <p>&nbsp;</p>    

    <table border=0 cellpadding=5 cellspacing=0>

<%
    if (errorMsg != null) {
%>
    <tr>
    <td colspan="2" align="left">
    <b><font color="red"><%= errorMsg %></font>
    <br><br>
    </td>
    </tr>
<%
}
%>

    <tr>
    <td>Server Protocol:</td>
    <td><input name="famProt" type="text" size="30" value="<%= famProt == null ? "" : famProt %>" /></td>
    </tr>
    <tr>
    <td>Server Host:</td>
    <td><input name="famHost" type="text" size="30" value="<%= famHost == null ? "" : famHost %>" /></td>
    </tr>
    <tr>
    <td>Server Port:</td>
    <td><input name="famPort" type="text" size="30" value="<%= famPort == null ? "" : famPort %>" /></td>
    </tr>
    <tr>
    <td>Server Deployment URI:</td>
    <td><input name="famDeploymenturi" type="text" size="30" value="<%= famDeploymenturi == null ? "" : famDeploymenturi %>" /></td>
    </tr>
    <tr>
    <td>DistAuth Server Protocol:</td>
    <td><input name="distAuthProt" type="text" size="30" value="<%= distAuthProt == null ? "" : distAuthProt %>" readonly/></td>
    </tr>
    <tr>
    <td>DistAuth Server Host:</td>
    <td><input name="distAuthHost" type="text" size="30" value="<%= distAuthHost == null ? "" : distAuthHost %>" readonly/></td>
    </tr>
    <tr>
    <td>DistAuth Server Port:</td>
    <td><input name="distAuthPort" type="text" size="30" value="<%= distAuthPort == null ? "" : distAuthPort %>" readonly/></td>
    </tr>
    <tr>
    <td>DistAuth Server Deployment URI:</td>
    <td><input name="distAuthDeploymenturi" type="text" size="30" value="<%= distAuthDeploymenturi == null ? "" : distAuthDeploymenturi %>" readonly/></td>
    </tr>
    <tr>
    <td>DistAuth Cookie Name:</td>
    <td><input name="distAuthCookieName" type="text" size="30" value="<%= distAuthCookieName == null ? "" : distAuthCookieName %>" /></td>
    </tr>            
    <tr>
    <td>Debug directory</td>
    <td><input name="debugDir" type="text" size="30" value="<%= debugDir == null ? "" : debugDir %>" /></td>
    </tr>
    <tr>
    <td>Debug level</td>
    <td><input name="debugLevel" type="text" size="30" value="<%= debugLevel == null ? "" : debugLevel %>" /></td>
    </tr>
    <tr>
    <td>Encryption Key</td>
    <td><input name="encryptionKey" type="text" size="30" value="<%= encryptionKey == null ? "" : encryptionKey %>" /></td>
    </tr>        
    <tr>
    <td>Application user name</td>
    <td><input name="appUser" type="text" size="30" value="<%= appUser == null ? "" : appUser %>" /></td>
    </tr>
    <tr>
    <td>Application user password</td>
    <td><input name="appPassword" type="password" size="30" value="<%= appPassword == null ? "" : appPassword %>" /></td>
    </tr>
    <tr>
    <td>Confirm Application user password</td>
    <td><input name="confirmAppPassword" type="password" size="30" value="<%= confirmAppPassword == null ? "" : confirmAppPassword %>" /></td>
    </tr>    
    <tr>
        <td>  </td>
    </tr>
    <tr>
    <td colspan="2" align="center">
    <input type="submit" name="submit" value="Configure" />
    <input type="reset" value="Reset" />
    </td>
    </tr>
    </table>
</form>

<%
} else {
%>
<p>&nbsp;</p>
<%
    if (errorMsg != null) {
%>
<%= errorMsg %>
<%
} else {
%>
DistAuth application is successfully configured.<br>
AMDistAuthConfig.properties created at <%= configFile %><br>
<br>
<p>
Click <a href="index.html">here</a> to go to login page. 
<%
    }
}
%>
</td></tr></table>
</body>
</html>
