package com.sun.identity.xacml.context;

import com.sun.identity.shared.test.UnitTestBase;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Status;
import com.sun.identity.xacml.context.StatusCode;
import com.sun.identity.xacml.context.StatusMessage;
import com.sun.identity.xacml.context.StatusDetail;

import java.util.logging.Level;

import org.testng.annotations.Test;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class StatusTest extends UnitTestBase {

    public StatusTest() {
        super("OpenFed-xacml-StatusTest");
    }

    //@Test(groups={"xacml"}, expectedExceptions={XACMLException.class})
    @Test(groups={"xacml"})
    public void getStatus() throws XACMLException {

        entering("getStatus()", null);
        log(Level.INFO,"getStatus()","\n");
        log(Level.INFO,"getStatus()","status-test-1-b");
        log(Level.INFO,"getStatus()","\n");

        log(Level.INFO,"getStatus()","create status code");
        StatusCode code = ContextFactory.getInstance().createStatusCode();
        code.setValue("10");
        code.setMinorCodeValue("5");
        String statusCodeXml = code.toXMLString(true, true);
        log(Level.INFO,"getStatus()","status code xml:" + statusCodeXml);
        log(Level.INFO,"getStatus()","\n");

        log(Level.INFO,"getStatus()","create status message");
        StatusMessage message = ContextFactory.getInstance().createStatusMessage();
        message.setValue("success");
        String statusMessageXml = message.toXMLString(true, true);
        log(Level.INFO,"getStatus()","status message xml:" + statusMessageXml);
        log(Level.INFO,"getStatus()","\n");

        log(Level.INFO,"getStatus()","create empty statusDetail");
        StatusDetail detail = ContextFactory.getInstance().createStatusDetail();
        log(Level.INFO,"getStatus()","detail-xml:" + detail.toXMLString());
        log(Level.INFO,"getStatus()","add a child");
        detail.getElement().insertBefore(detail.getElement().cloneNode(true), null);
        log(Level.INFO,"getStatus()","detail-xml:" + detail.toXMLString());
        log(Level.INFO,"getStatus()","create statusDetail from xml string");
        StatusDetail detail1 = ContextFactory.getInstance().createStatusDetail(
                detail.toXMLString(true, true));
        log(Level.INFO,"getStatus()","detail-xml:" + detail1.toXMLString());
        log(Level.INFO,"getStatus()","\n");

        log(Level.INFO,"getStatus()","create empty status");
        log(Level.INFO,"getStatus()","get status 10");
        Status status = ContextFactory.getInstance().createStatus();
        log(Level.INFO,"getStatus()","get status 15");
        log(Level.INFO,"getStatus()","status-xml:" + status.toXMLString());
        log(Level.INFO,"getStatus()","get status 20");
        log(Level.INFO,"getStatus()","set status code");
        status.setStatusCode(code);
        log(Level.INFO,"getStatus()","status-xml:" + status.toXMLString());
        log(Level.INFO,"getStatus()","set status message");
        status.setStatusMessage(message);
        log(Level.INFO,"getStatus()","status-xml:" + status.toXMLString());
        log(Level.INFO,"getStatus()","set status detail");
        status.setStatusDetail(detail1);
        log(Level.INFO,"getStatus()","status-xml:" + status.toXMLString());
        log(Level.INFO,"getStatus()","\n");
        log(Level.INFO,"getStatus()","status-xml, with ns declared:" + status.toXMLString(true,
                true));
        log(Level.INFO,"getStatus()","create status from xml");
        Status status1 = ContextFactory.getInstance().createStatus(
                status.toXMLString(true, true));
        log(Level.INFO,"getStatus()","status-xml, with ns declared:" + status1.toXMLString(true,
                true));
        log(Level.INFO,"getStatus()","\n");

        log(Level.INFO,"getStatus()","status-test-1-e");
        log(Level.INFO,"getStatus()","\n");
        exiting("getStatus()");

    }

}
