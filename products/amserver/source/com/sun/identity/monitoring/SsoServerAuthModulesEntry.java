package com.sun.identity.monitoring;

//
// Generated by mibgen version 5.1 (05/20/05) when compiling SUN-OPENSSO-SERVER-MIB.
//

// java imports
//
import java.io.Serializable;

// jmx imports
//
import com.sun.management.snmp.SnmpStatusException;

// jdmk imports
//
import com.sun.management.snmp.agent.SnmpMib;

/**
 * The class is used for implementing the "SsoServerAuthModulesEntry" group.
 * The group is defined with the following oid: 1.3.6.1.4.1.42.2.230.3.1.1.2.1.10.5.1.
 */
public class SsoServerAuthModulesEntry implements SsoServerAuthModulesEntryMBean, Serializable {

    /**
     * Variable for storing the value of "SsoServerAuthModuleFailureCount".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.10.5.1.5".
     */
    protected Long SsoServerAuthModuleFailureCount = new Long(1);

    /**
     * Variable for storing the value of "SsoServerAuthModuleSuccessCount".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.10.5.1.4".
     */
    protected Long SsoServerAuthModuleSuccessCount = new Long(1);

    /**
     * Variable for storing the value of "SsoServerAuthModuleType".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.10.5.1.3".
     */
    protected String SsoServerAuthModuleType = new String("JDMK 5.1");

    /**
     * Variable for storing the value of "SsoServerAuthModuleName".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.10.5.1.2".
     */
    protected String SsoServerAuthModuleName = new String("JDMK 5.1");

    /**
     * Variable for storing the value of "SsoServerAuthModuleIndex".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.10.5.1.1".
     */
    protected Integer SsoServerAuthModuleIndex = new Integer(1);

    /**
     * Variable for storing the value of "SsoServerRealmIndex".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.9.1.1".
     */
    protected Integer SsoServerRealmIndex = new Integer(1);


    /**
     * Constructor for the "SsoServerAuthModulesEntry" group.
     */
    public SsoServerAuthModulesEntry(SnmpMib myMib) {
    }

    /**
     * Getter for the "SsoServerAuthModuleFailureCount" variable.
     */
    public Long getSsoServerAuthModuleFailureCount() throws SnmpStatusException {
        return SsoServerAuthModuleFailureCount;
    }

    /**
     * Getter for the "SsoServerAuthModuleSuccessCount" variable.
     */
    public Long getSsoServerAuthModuleSuccessCount() throws SnmpStatusException {
        return SsoServerAuthModuleSuccessCount;
    }

    /**
     * Getter for the "SsoServerAuthModuleType" variable.
     */
    public String getSsoServerAuthModuleType() throws SnmpStatusException {
        return SsoServerAuthModuleType;
    }

    /**
     * Getter for the "SsoServerAuthModuleName" variable.
     */
    public String getSsoServerAuthModuleName() throws SnmpStatusException {
        return SsoServerAuthModuleName;
    }

    /**
     * Getter for the "SsoServerAuthModuleIndex" variable.
     */
    public Integer getSsoServerAuthModuleIndex() throws SnmpStatusException {
        return SsoServerAuthModuleIndex;
    }

    /**
     * Getter for the "SsoServerRealmIndex" variable.
     */
    public Integer getSsoServerRealmIndex() throws SnmpStatusException {
        return SsoServerRealmIndex;
    }

}