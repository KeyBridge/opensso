package com.sun.identity.xacml2.context;

import com.sun.identity.shared.test.UnitTestBase;

import com.sun.identity.xacml2.common.XACML2Exception;
import com.sun.identity.xacml2.context.ContextFactory;
import com.sun.identity.xacml2.context.StatusCode;

import java.util.logging.Level;

import org.testng.annotations.Test;

public class StatusCodeTest extends UnitTestBase {

    public StatusCodeTest() {
        super("OpenFed-xacml2-StatusCodeTest");
    }

    //@Test(groups={"xacml2"}, expectedExceptions={XACML2Exception.class})
    @Test(groups={"xacml2"})
    public void getStatusCode() throws XACML2Exception {

        entering("getStatusCode()", null);
        log(Level.INFO,"getStatusCode()","\n");
        log(Level.INFO,"getStatusCode()","code-test-1-b");
        StatusCode code = ContextFactory.getInstance().createStatusCode();

        log(Level.INFO,"getStatusCode()","set value to Permit");
        code.setValue("Permit");
        String xml = code.toXMLString();
        log(Level.INFO,"getStatusCode()","status code xml:" + xml);
        log(Level.INFO,"getStatusCode()","status code value:" + code.getValue());
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","set value to Deny");
        code.setValue("Deny");
        log(Level.INFO,"getStatusCode()","set minor code value to allow");
        code.setValue("Deny");
        code.setMinorCodeValue("allow");
        xml = code.toXMLString();
        log(Level.INFO,"getStatusCode()","status code xml:" + xml);
        log(Level.INFO,"getStatusCode()","status code value:" + code.getValue());
        log(Level.INFO,"getStatusCode()","minor code value:" + code.getMinorCodeValue());
        log(Level.INFO,"getStatusCode()","mutable value:" + code.isMutable());
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","make immutable");
        code.makeImmutable();
        xml = code.toXMLString(true, true);
        log(Level.INFO,"getStatusCode()","status code xml, include prefix, ns:" + xml);
        log(Level.INFO,"getStatusCode()","code value:" + code.getValue());
        log(Level.INFO,"getStatusCode()","mutable value:" + code.isMutable());
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","creating status code from xml");
        StatusCode code1 = ContextFactory.getInstance().createStatusCode(xml);
        log(Level.INFO,"getStatusCode()","status code value:" + code1.getValue());
        xml = code1.toXMLString();
        log(Level.INFO,"getStatusCode()","status code xml:" + xml);
        log(Level.INFO,"getStatusCode()","\n");

        log(Level.INFO,"getStatusCode()","code-test-1-e");
        log(Level.INFO,"getStatusCode()","\n");
        exiting("getStatusCode()");

    }

}
