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

   $Id: fedletSampleApp.jsp,v 1.2 2008-05-29 00:40:42 veiming Exp $

   Copyright 2008 Sun Microsystems Inc. All Rights Reserved
--%>


<%@page
import="com.sun.identity.saml2.common.SAML2Exception,
com.sun.identity.saml2.common.SAML2Constants,
com.sun.identity.saml2.assertion.Assertion,
com.sun.identity.saml2.assertion.Subject,
com.sun.identity.saml2.profile.SPACSUtils,
com.sun.identity.saml2.protocol.Response,
com.sun.identity.saml2.assertion.NameID,
com.sun.identity.plugin.session.SessionException,
java.io.IOException,
java.util.Iterator,
java.util.List,
java.util.Map"
%>
<%
    String deployuri = request.getRequestURI();
    int slashLoc = deployuri.indexOf("/", 1);
    if (slashLoc != -1) {
        deployuri = deployuri.substring(0, slashLoc);
    }
%>
<html>
<head>
    <title>Fedlet Sample Application</title>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
    <link rel="stylesheet" type="text/css" href="<%= deployuri %>/com_sun_web_ui/css/css_ns6up.css" />
</head>

<body>
<div class="MstDiv"><table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblTop" title="">
<tbody><tr>
<td nowrap="nowrap">&nbsp;</td>
<td nowrap="nowrap">&nbsp;</td>
</tr></tbody></table>

<table width="100%" border="0" cellpadding="0" cellspacing="0" class="MstTblBot" title="">
<tbody><tr>
<td class="MstTdTtl" width="99%">
<div class="MstDivTtl"><img name="ProdName" src="<%= deployuri %>/console/images/PrimaryProductName.png" alt="" /></div></td><td class="MstTdLogo" width="1%"><img name="RMRealm.mhCommon.BrandLogo" src="<%= deployuri %>/com_sun_web_ui/images/other/javalogo.gif" alt="Java(TM) Logo" border="0" height="55" width="31" /></td></tr></tbody></table>
<table class="MstTblEnd" border="0" cellpadding="0" cellspacing="0" width="100%"><tbody><tr><td><img name="RMRealm.mhCommon.EndorserLogo" src="<%= deployuri %>/com_sun_web_ui/images/masthead/masthead-sunname.gif" alt="Sun(TM) Microsystems,
Inc." align="right" border="0" height="10" width="108" /></td></tr></tbody></table></div><div class="SkpMedGry1"><a name="SkipAnchor2089" id="SkipAnchor2089"></a></div>
<div class="SkpMedGry1"><a href="#SkipAnchor4928"><img src="<%= deployuri %>/com_sun_web_ui/images/other/dot.gif" alt="Jump Over Tab Navigation Area. Current Selection is: Access Control" border="0" height="1" width="1" /></a></div>
<%
    // BEGIN : following code is a must for Fedlet (SP) side application
    Map map;
    try {
        // invoke the Fedlet processing logic. this will do all the
        // necessary processing conforming to SAMLv2 specifications,
        // such as XML signature validation, Audience and Recipient
        // validation etc.  
        map = SPACSUtils.processResponseForFedlet(request, response);
    } catch (SAML2Exception sme) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR, sme.getMessage());
        return;
    } catch (IOException ioe) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR, ioe.getMessage());
        return;
    } catch (SessionException se) {
        response.sendError(response.SC_INTERNAL_SERVER_ERROR, se.getMessage());
        return;
    } catch (ServletException se) {
        response.sendError(response.SC_BAD_REQUEST, se.getMessage());
        return;
    }
    // END : code is a must for Fedlet (SP) side application
    
    String relayUrl = (String) map.get(SAML2Constants.RELAY_STATE);
    if ((relayUrl != null) && (relayUrl.length() != 0)) {
        // something special for validation to send redirect
        int stringPos  = relayUrl.indexOf("sendRedirectForValidationNow=true");
        if (stringPos != -1) {
            response.sendRedirect(relayUrl);
        }
    } 

    // Following are sample code to show how to retrieve information,
    // such as Reponse/Assertion/Attributes, from the returned map. 
    // You might not need them in your real application code. 
    Response samlResp = (Response) map.get(SAML2Constants.RESPONSE); 
    Assertion assertion = (Assertion) map.get(SAML2Constants.ASSERTION);
    Subject subject = (Subject) map.get(SAML2Constants.SUBJECT);
    String entityID = (String) map.get(SAML2Constants.IDPENTITYID);
    NameID nameId = assertion.getSubject().getNameID();
    String value = nameId.getValue();
    String format = nameId.getFormat();
    out.println("<br><br><b>Single Sign-On successful with IDP " 
        + entityID + ".</b>");
    out.println("<br><br>");
    out.println("<table border=0>");
    if (format != null) {
        out.println("<tr>");
        out.println("<td valign=top><b>Name ID format: </b></td>");
        out.println("<td>" + format + "</td>");
        out.println("</tr>");
    }
    if (value != null) {
        out.println("<tr>");
        out.println("<td valign=top><b>Name ID value: </b></td>");
        out.println("<td>" + value + "</td>");
        out.println("</tr>");
    }    
    Map attrs = (Map) map.get(SAML2Constants.ATTRIBUTE_MAP);
    if (attrs != null) {
        out.println("<tr>");
        out.println("<td valign=top><b>Attributes: </b></td>");
        Iterator iter = attrs.keySet().iterator();
        out.println("<td>");
        while (iter.hasNext()) {
            String attrName = (String) iter.next();
            List attrVals = (List) attrs.get(attrName);
            out.println(attrName + "="
                + attrVals.get(0) + "<br>");
        }
        out.println("</td>");
        out.println("</tr>");
    }
    out.println("</table>");
    out.println("<br><br><b><a href=# onclick=toggleDisp('resinfo')>Click to view SAML2 Response XML</a></b><br>");
    out.println("<span style='display:none;' id=resinfo><textarea rows=40 cols=100>" + samlResp.toXMLString(true, true) + "</textarea></span>");

    out.println("<br><b><a href=# onclick=toggleDisp('assr')>Click to view Assertion XML</a></b><br>");
    out.println("<span style='display:none;' id=assr><br><textarea rows=40 cols=100>" + assertion.toXMLString(true, true) + "</textarea></span>");

    out.println("<br><b><a href=# onclick=toggleDisp('subj')>Click to view Subject XML</a></b><br>");
    out.println("<span style='display:none;' id=subj><br><textarea rows=10 cols=100>" + subject.toXMLString(true, true) + "</textarea></span>");

    if ((relayUrl != null) && (relayUrl.length() != 0)) {
        out.println("<br><br>Click <a href=\"" + relayUrl 
            + "\">here</a> to redirect to final destination.");
    }
%>
<script>
function toggleDisp(id)
{
    var elem = document.getElementById(id);
    if (elem.style.display == 'none')
        elem.style.display = '';
    else
        elem.style.display = 'none';
}
</script>
</body>
</html>
