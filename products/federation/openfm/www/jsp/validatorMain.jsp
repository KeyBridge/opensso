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

   $Id: validatorMain.jsp,v 1.1 2008-04-10 23:15:05 veiming Exp $

   Copyright 2008 Sun Microsystems Inc. All Rights Reserved
--%>

<%@ page import="com.sun.identity.common.SystemConfigurationUtil" %>
<%@ page import="com.sun.identity.shared.Constants" %>
<%@ page import="com.sun.identity.workflow.ValidateSAML2" %>
<%@ page import="java.net.URLEncoder" %>

<%
    String deployuri = SystemConfigurationUtil.getProperty(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR);

    String pageTitle = ValidateSAML2.getMessage("federation.connectivity.test");
    String cancelButton = ValidateSAML2.getMessage("button.cancel");
%>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
<meta name="Copyright" content="Copyright &copy; 2008 Sun Microsystems, Inc. All Rights Reserved. Use is subject to license terms." /><link rel="shortcut icon" href="<%= deployuri %>/com_sun_web_ui/images/favicon/favicon.ico" type="image/x-icon"></link>
<link rel="stylesheet" type="text/css" href="<%= deployuri %>/console/css/opensso.css" />

<html>
</head>
<body class="DefBdy" onload="onLoad()">
<iframe name="hidden1" style="display:none"></iframe>
<iframe name="hidden2" style="display:none"></iframe>
<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody>
<tr>
<td class="MstTdTtl" width="99%" bgcolor="#637583" valign="bottom">
<div class="MstDivTtl"><br /><img name="Home.mhCommon.ProdName" src="<%= deployuri %>/console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%" bgcolor="#637583"><img src="<%= deployuri %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table border="0" width="100%" cellpadding="0" cellspacing="0">
<tr valign="bottom">
<td nowrap="nowrap" valign="bottom" bgcolor="#4e606e">
<div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="1" /></div>
</td>
</tr>
</table>
<table border="0" width="100%" cellpadding="0" cellspacing="0">
<tr><td bgcolor="#677784"><img name="Home.mhCommon.EndorserLogo" src="/opensso/com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems, Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table>

<table border="0" width="100%" cellpadding="0" cellspacing="0">
<tr valign="bottom">
<td nowrap="nowrap" valign="bottom">
<div class="TtlTxtDiv">

<h1 class="TaskTitle"><%= pageTitle %></h1>
</div>
</td>
<td align="right" nowrap="nowrap" valign="bottom">
<!-- div class="TtlBtnDiv"> <input name="btnCancel" type="submit" class="Btn1" value="<%= cancelButton %>" onmouseover="javascript: this.className='Btn1Hov'" onmouseout="javascript: this.className='Btn1'" onblur="javascript: this.className='Btn1'" onfocus="javascript: this.className='Btn1Hov'" onClick="cancelOp();return false;"/ -->
</div>
</td>
</tr>
</table>
<div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="1" /></div>
<iframe src ="" name="controller" width="100%" height="20%" frameborder=0></iframe>
<div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="1" /></div>
<div style="text-align:center"> 
<iframe src ="" width="95%" height="50%" name="worker" frameborder=1></iframe>
</center>
<div><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="" border="0" height="10" width="1" /></div>

<iframe src ="validatorFooter.jsp" width="100%" height="40" name="footer" frameborder=0></iframe>

<script language="Javascript">
var accLinkTimer;
var sloTimer;
var ssoTimer;
var accTermTimer;

function cancelOp() {
    logout();
    top.location = "<%= deployuri %>";
}

function logout() {
    frames['controller'].logout();
}

function errorOccured() {
    frames['worker'].location = 'validatorStatus.jsp';
    logout();
}

function onLoad() {
<%
    String realm = request.getParameter("realm");
    String cot = request.getParameter("cot");
    String idp = request.getParameter("idp");
    String sp = request.getParameter("sp");
    String msgInitializing = ValidateSAML2.getMessage("validate.initializing");
    out.println("frames['worker'].location = 'validateWait.jsp?m=" +
        URLEncoder.encode(msgInitializing) + "';");
%>
    startTest();
}

function gotoHiddenFrame1(url) {
    frames['hidden1'].location = url;
}

function gotoHiddenFrame2(url) {
    frames['hidden2'].location = url;
}

function startTest() {
<%
    out.println("frames['controller'].location = 'validator.jsp?" +
        "realm=" + URLEncoder.encode(realm) +
        "&cot=" + URLEncoder.encode(cot) +
        "&idp=" + URLEncoder.encode(idp) +
        "&sp=" + URLEncoder.encode(sp) + "';");
%>
}

function gotoURL(url) {
    frames['worker'].location = url;
}

function showFooter(msg) {
    frames['footer'].location = 'validatorFooter.jsp?m=' + msg;
}

function authIdpPassed() {
    frames['controller'].authIdpPassed();
}

function authIdpFailed() {
    frames['controller'].authIdpFailed();
}

function authSpPassed() {
    frames['controller'].authSpPassed();
}

function authSpFailed() {
    frames['controller'].authSpFailed();
}

function accLinkPassed() {
    clearTimeout(accLinkTimer);
    frames['controller'].accLinkPassed();
    frames['worker'].location = "validatorStatus.jsp";
}

function accLinkFailed() {
    frames['controller'].accLinkFailed();
    frames['worker'].location = "validatorStatus.jsp";
}

function trackAccountLink() {
    accLinkTimer = setTimeout("accLinkFailed()", 5000);
}

function singleLogoutPassed() {
    clearTimeout(sloTimer);
    frames['controller'].sloPassed();
}

function sloFailed() {
    frames['controller'].sloFailed();
    frames['worker'].location = "validatorStatus.jsp";
}

function trackSingleLogout() {
    sloTimer = setTimeout("sloFailed()", 5000);
}

function singleLoginPassed() {
    clearTimeout(ssoTimer);
    frames['worker'].location = "validatorStatus.jsp";
    frames['controller'].ssoPassed();
}

function ssoFailed() {
    frames['controller'].ssoFailed();
}

function trackSingleLogin() {
    ssoTimer = setTimeout("ssoFailed()", 30000);
}

function accTermPassed() {
    clearTimeout(accTermTimer);
    frames['controller'].accTermPassed();
    logout();
    frames['controller'].getReport();
}

function accTermFailed() {
    frames['controller'].accTermFailed();
    frames['worker'].location = "validatorStatus.jsp";
}

function trackAccountTermination() {
    accTermTimer = setTimeout("accTermFailed()", 5000);
}

function authIdp() {
    frames['controller'].authIdp();
}

function authSp() {
    frames['controller'].authSp();
}

function singleLogin() {
    frames['controller'].singleLogin();
}


</script>

</body>
</html>
