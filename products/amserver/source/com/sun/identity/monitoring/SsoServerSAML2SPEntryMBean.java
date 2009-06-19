package com.sun.identity.monitoring;

//
// Generated by mibgen version 5.1 (05/20/05) when compiling SUN-OPENSSO-SERVER-MIB in standard metadata mode.
//


// jmx imports
//
import com.sun.management.snmp.SnmpStatusException;

/**
 * This interface is used for representing the remote management interface for the "SsoServerSAML2SPEntry" MBean.
 */
public interface SsoServerSAML2SPEntryMBean {

    /**
     * Getter for the "SsoServerSAML2SPInvalidArtifactsRcvd" variable.
     */
    public Long getSsoServerSAML2SPInvalidArtifactsRcvd() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerSAML2SPValidAssertionsRcvd" variable.
     */
    public Long getSsoServerSAML2SPValidAssertionsRcvd() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerSAML2SPRqtsSent" variable.
     */
    public Long getSsoServerSAML2SPRqtsSent() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerSAML2SPName" variable.
     */
    public String getSsoServerSAML2SPName() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerSAML2SPIndex" variable.
     */
    public Integer getSsoServerSAML2SPIndex() throws SnmpStatusException;

    /**
     * Getter for the "SsoServerRealmIndex" variable.
     */
    public Integer getSsoServerRealmIndex() throws SnmpStatusException;

}