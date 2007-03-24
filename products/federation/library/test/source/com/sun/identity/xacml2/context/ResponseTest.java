package com.sun.identity.xacml2.context;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml2.common.XACML2Exception;
import com.sun.identity.xacml2.common.XACML2SDKUtils;
import com.sun.identity.xacml2.context.ContextFactory;
import com.sun.identity.xacml2.context.Decision;
import com.sun.identity.xacml2.context.Response;
import com.sun.identity.xacml2.context.Result;
import com.sun.identity.xacml2.context.Status;
import com.sun.identity.xacml2.context.StatusCode;
import com.sun.identity.xacml2.context.StatusMessage;
import com.sun.identity.xacml2.context.StatusDetail;

import java.util.logging.Level;

import org.testng.annotations.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ResponseTest extends UnitTestBase {

    public ResponseTest() {
        super("OpenFed-xacml2-ResponseTest");
    }

    //@Test(groups={"xacml2"}, expectedExceptions={XACML2Exception.class})
    @Test(groups={"xacml2"})
    public void getResponse() throws XACML2Exception {

        entering("getResponse()", null);
        log(Level.INFO, "getResponse", "\n");
        log(Level.INFO, "getResponse", "response-test-1-b");
        log(Level.INFO, "getResponse", "\n");

        log(Level.INFO, "getResponse", "create empty status code");
        StatusCode code = ContextFactory.getInstance().createStatusCode();
        code.setValue("10");
        code.setMinorCodeValue("5");
        String statusCodeXml = code.toXMLString(true, true);
        log(Level.INFO, "getResponse", "status code xml:" + statusCodeXml);
        log(Level.INFO, "getResponse", "\n");

        log(Level.INFO, "getResponse", "create empty status message");
        StatusMessage message 
                = ContextFactory.getInstance().createStatusMessage();
        message.setValue("success");
        String statusMessageXml = message.toXMLString(true, true);
        log(Level.INFO, "getResponse", "status message xml:" 
                + statusMessageXml);
        log(Level.INFO, "getResponse", "\n");

        log(Level.INFO, "getResponse", "create empty statusDetail");
        StatusDetail detail 
                = ContextFactory.getInstance().createStatusDetail();
        log(Level.INFO, "getResponse", "detail-xml:" + detail.toXMLString());
        log(Level.INFO, "getResponse", "add a child");
        detail.getElement().insertBefore(detail.getElement().cloneNode(true)
                , null);
        log(Level.INFO, "getResponse", "detail-xml:" + detail.toXMLString());
        log(Level.INFO, "getResponse", "create statusDetail from xml string");
        StatusDetail detail1 = ContextFactory.getInstance().createStatusDetail(
                detail.toXMLString(true, true));
        log(Level.INFO, "getResponse", "detail-xml:" + detail1.toXMLString());
        log(Level.INFO, "getResponse", "\n");

        log(Level.INFO, "getResponse", "create empty status");
        Status status = ContextFactory.getInstance().createStatus();
        log(Level.INFO, "getResponse", "status-xml:" + status.toXMLString());
        log(Level.INFO, "getResponse", "set status code");
        status.setStatusCode(code);
        log(Level.INFO, "getResponse", "status-xml:" + status.toXMLString());
        log(Level.INFO, "getResponse", "set status message");
        status.setStatusMessage(message);
        log(Level.INFO, "getResponse", "status-xml:" + status.toXMLString());
        log(Level.INFO, "getResponse", "set status detail");
        status.setStatusDetail(detail1);
        log(Level.INFO, "getResponse", "status-xml:" + status.toXMLString());
        log(Level.INFO, "getResponse", "\n");
        log(Level.INFO, "getResponse", "status-xml, with ns declared:" 
                + status.toXMLString(true, true));
        log(Level.INFO, "getResponse", "create status from xml");
        Status status1 = ContextFactory.getInstance().createStatus(
                status.toXMLString(true, true));
        log(Level.INFO, "getResponse", "status-xml, with ns declared:" 
                + status1.toXMLString(true, true));
        log(Level.INFO, "getResponse", "\n");

        log(Level.INFO, "getResponse", "create empty decision");
        Decision decision = ContextFactory.getInstance().createDecision();
        log(Level.INFO, "getResponse",  "decision-xml:" 
                + decision.toXMLString());
        log(Level.INFO, "getResponse",  "set value to Permit");
        decision.setValue("Permit");
        log(Level.INFO, "getResponse",  "detail-xml:" 
                + decision.toXMLString());
        log(Level.INFO, "getResponse",  "create decision from xml string");
        Decision decision1 = ContextFactory.getInstance().createDecision(
                decision.toXMLString(true, true));
        log(Level.INFO, "getResponse",  "decision-xml:" 
                + decision1.toXMLString());
        log(Level.INFO, "getResponse",  "\n");

        log(Level.INFO, "getResponse",  "create empty result");
        Result result = ContextFactory.getInstance().createResult();
        log(Level.INFO, "getResponse",  "result-xml:" + result.toXMLString());
        log(Level.INFO, "getResponse",  "resource id:" 
                + result.getResourceId());
        log(Level.INFO, "getResponse", "set resource id");
        log(Level.INFO, "getResponse", 
                "http://insat.red.iplanet.com/banner.html");
        result.setResourceId("http://insat.red.iplanet.com/banner.html");
        log(Level.INFO, "getResponse", "get resource id:" 
                + result.getResourceId());
        log(Level.INFO, "getResponse", "result-xml:" + result.toXMLString());
        log(Level.INFO, "getResponse", "set decision");
        result.setDecision(decision1);
        log(Level.INFO, "getResponse", "result-xml:" + result.toXMLString());
        log(Level.INFO, "getResponse", "set status");
        result.setStatus(status1);
        log(Level.INFO, "getResponse", "result-xml:" + result.toXMLString());
        log(Level.INFO, "getResponse", "result-xml, with nsDeclaration:" 
                + result.toXMLString(true, true));
        log(Level.INFO, "getResponse", "create result from xml string");
        Result result1 = ContextFactory.getInstance().createResult(
                result.toXMLString(true, true));
        log(Level.INFO, "getResponse", "result-xml:" + result1.toXMLString());
        log(Level.INFO, "getResponse", "\n");

        log(Level.INFO, "getResponse", "create empty response");
        Response response = ContextFactory.getInstance().createResponse();
        log(Level.INFO, "getResponse", "response-xml:" 
                + response.toXMLString());
        log(Level.INFO, "getResponse", "add result");
        response.addResult(result1);
        log(Level.INFO, "getResponse", "response-xml:" 
                + response.toXMLString());
        log(Level.INFO, "getResponse", "add result");
        response.addResult(result1);
        log(Level.INFO, "getResponse", "response-xml:" 
                + response.toXMLString());
        log(Level.INFO, "getResponse", "\n");
        log(Level.INFO, "getResponse", "response-xml, with nsDeclaration:" 
                + response.toXMLString(true, true));
        log(Level.INFO, "getResponse", "create response from xml string");
        Response response1 = ContextFactory.getInstance().createResponse(
                response.toXMLString(true, true));
        log(Level.INFO, "getResponse", "response-xml:" 
                + response1.toXMLString());
        log(Level.INFO, "getResponse", "\n");

        log(Level.INFO, "getResponse", "response-test-1-e");
        log(Level.INFO, "getResponse", "\n");
        exiting("getResponse()");

    }

}
