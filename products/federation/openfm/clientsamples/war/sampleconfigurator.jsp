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

   $Id: sampleconfigurator.jsp,v 1.2 2007-10-16 22:09:36 exu Exp $

   Copyright 2007 Sun Microsystems Inc. All Rights Reserved
--%>


<html>
<head>
<title>Configure Identity Provider</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="com_sun_web_ui/css/css_ns6up.css" />


<%@ page import="
com.sun.identity.security.EncodeAction,
com.sun.identity.setup.SetupClientWARSamples,
java.io.*,
java.security.AccessController,
java.util.Properties"
%>

<%
    String configFile = System.getProperty("java.io.tmpdir") +
        File.separator + "AMConfig.properties";
    String configTemplate = "/WEB-INF/classes/AMConfig.properties.template";
    String errorMsg = null;
    boolean configured = false;
    String famProt = null; 
    String famHost = null;
    String famPort = null;
    String famDeploymenturi = null;
    String debugDir = null;
    String appPassword = null;

    File configF = new File(configFile);
    if (configF.exists()) {
        errorMsg = "The Client Samples have already been configued.<br>" +
            "Configuration file : " + configFile + "<br><p><br>" +
            "Click <a href=\"index.html\">here</a> to go to samples.";
        configured = true;
    } else {
        famProt = request.getParameter("famProt");
        famHost = request.getParameter("famHost");
        famPort = request.getParameter("famPort");
        famDeploymenturi = request.getParameter("famDeploymenturi");
        debugDir = request.getParameter("debugDir");
        appPassword = request.getParameter("appPassword");
        String submit = request.getParameter("submit");
        String servletPath = request.getServletPath();

        if (submit != null) { 
            if ((famProt != null) && !famProt.equals("") && 
                (famHost != null) && !famHost.equals("") && 
                (famPort != null) && !famPort.equals("") && 
                (famDeploymenturi != null) && !famDeploymenturi.equals("") && 
                (debugDir != null) && !debugDir.equals("") &&
                (appPassword != null) && !appPassword.equals("")) {
                Properties props = new Properties();
                props.setProperty("SERVER_PROTOCOL", famProt);
                props.setProperty("SERVER_HOST", famHost);
                props.setProperty("SERVER_PORT", famPort);
                props.setProperty("DEPLOY_URI", famDeploymenturi);
                props.setProperty("DEBUG_DIR", debugDir);
                props.setProperty("NAMING_URL", famProt + "://" + famHost + ":"
                    + famPort + famDeploymenturi + "/namingservice");
                props.setProperty("DEBUG_LEVEL", "message");
                props.setProperty("APPLICATION_USER", "UrlAccessAgent");
                //props.setProperty("ENCODED_APPLICATION_PASSWORD", (String) 
                //   AccessController.doPrivileged(new EncodeAction(appPassword)));
                props.setProperty("APPLICATION_PASSWD", appPassword); 
                props.setProperty("AM_COOKIE_NAME", "iPlanetDirectoryPro");
                props.setProperty("ENCRYPTION_KEY", "SAMPLE_RAND");
                props.setProperty("ENCRYPTION_KEY_LOCAL", "SAMPLE_RAND");
                props.setProperty("SESSION_PROVIDER_CLASS", 
                    "com.sun.identity.plugin.session.impl.FMSessionProvider");
                props.setProperty("CONFIGURATION_PROVIDER_CLASS", 
                    "com.sun.identity.plugin.configuration.impl.ConfigurationInstanceImpl");
                props.setProperty("DATASTORE_PROVIDER_CLASS", 
                    "com.sun.identity.plugin.datastore.impl.IdRepoDataStoreProvider");
                try {
                    SetupClientWARSamples configurator = 
                        new SetupClientWARSamples(
                        getServletConfig().getServletContext());
                    configurator.createAMConfigProperties(configFile, 
                        configTemplate, props);
                    configurator.setAMConfigProperties(configFile);
                } catch (IOException ioex) {
                    ioex.printStackTrace();
                    errorMsg = "Unable to create sample AMConfig.properties " +
                       "file: " + ioex.getMessage();
                }
                configured = true;
            } else {
                errorMsg = "Missing one or more required fields."; 
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
<div class="MstDivTtl"><img name="ProdName" src="console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>
                                                                                
<table border="0" cellpadding="10" cellspacing="0" width="100%">
<tr><td>


<%
    if (!configured) {
%>

<h3>Configuring Client Samples</h3>

<form action="sampleconfigurator.jsp" method="GET">
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
    <td><input name="famProt" type="text" size="6" value="<%= famProt == null ? "" : famProt %>" /></td>
    </tr>
    <tr>
    <td>Server Host:</td>
    <td><input name="famHost" type="text" size="30" value="<%= famHost == null ? "" : famHost %>" /></td>
    </tr>
    <tr>
    <td>Server Port:</td>
    <td><input name="famPort" type="text" size="6" value="<%= famPort == null ? "" : famPort %>" /></td>
    </tr>
    <tr>
    <td>Server Deployment URI:</td>
    <td><input name="famDeploymenturi" type="text" size="15" value="<%= famDeploymenturi == null ? "" : famDeploymenturi %>" /></td>
    </tr>
    <tr>
    <td>Debug directory</td>
    <td><input name="debugDir" type="text" size="15" value="<%= debugDir == null ? "" : debugDir %>" /></td>
    </tr>
    <tr>
    <td>Application user password</td>
    <td><input name="appPassword" type="password" size="15" value="<%= appPassword == null ? "" : appPassword %>" /></td>
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
Client Samples are successfully configured.<br>
AMConfig.properties created at <%= configFile %><br>
<br>
<p>
Click <a href="index.html">here</a> to go to samples. 
<%
    }
}
%>
</td></tr></table>
</body>
</html>
