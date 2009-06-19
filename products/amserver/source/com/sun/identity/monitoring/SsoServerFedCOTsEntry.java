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
 * The class is used for implementing the "SsoServerFedCOTsEntry" group.
 * The group is defined with the following oid: 1.3.6.1.4.1.42.2.230.3.1.1.2.1.18.1.1.
 */
public class SsoServerFedCOTsEntry implements SsoServerFedCOTsEntryMBean, Serializable {

    /**
     * Variable for storing the value of "FedCOTName".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.18.1.1.2".
     */
    protected String FedCOTName = new String("JDMK 5.1");

    /**
     * Variable for storing the value of "FedCOTIndex".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.18.1.1.1".
     */
    protected Integer FedCOTIndex = new Integer(1);

    /**
     * Variable for storing the value of "SsoServerRealmIndex".
     * The variable is identified by: "1.3.6.1.4.1.42.2.230.3.1.1.2.1.9.1.1".
     */
    protected Integer SsoServerRealmIndex = new Integer(1);


    /**
     * Constructor for the "SsoServerFedCOTsEntry" group.
     */
    public SsoServerFedCOTsEntry(SnmpMib myMib) {
    }

    /**
     * Getter for the "FedCOTName" variable.
     */
    public String getFedCOTName() throws SnmpStatusException {
        return FedCOTName;
    }

    /**
     * Getter for the "FedCOTIndex" variable.
     */
    public Integer getFedCOTIndex() throws SnmpStatusException {
        return FedCOTIndex;
    }

    /**
     * Getter for the "SsoServerRealmIndex" variable.
     */
    public Integer getSsoServerRealmIndex() throws SnmpStatusException {
        return SsoServerRealmIndex;
    }

}