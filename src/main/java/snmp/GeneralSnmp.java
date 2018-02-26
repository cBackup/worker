/*
 * This file is part of cBackup, network equipment configuration backup tool
 * Copyright (C) 2017, Oļegs Čapligins, Imants Černovs, Dmitrijs Galočkins
 *
 * cBackup is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package snmp;

import abstractions.AbstractProtocol;
import abstractions.DTOProtocolResult;
import abstractions.DTOVariableConvertResult;

import java.util.Map;
import java.util.Vector;

/*
 * SNMP
 */
import java.io.IOException;

import abstractions.DTOVariableInjectResult;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;


/**
 * Snmp Main class
 */
public class GeneralSnmp extends AbstractProtocol {

    private Integer snmpVer;
    private Integer snmpPort;
    private String  snmpRead;
    private String  snmpSet;

    private Integer snmpRetries;
    private Integer snmpTimeout;

    /*
     * SNMP objects
     */
    private Snmp snmp;
    private CommunityTarget target;
    private PDU requestPDU;


    /**
     * Constructor
     *
     * @param coordinates  - schedule, task, node, etc..
     * @param settings     - app settings
     * @param credentials  - credentials
     * @param jobs         - sorted jobs
     * @param variables    - variable list
     */
    GeneralSnmp(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables)
    {
        this.coordinates.putAll(coordinates);
        this.settings.putAll(settings);
        this.credentials.putAll(credentials);
        this.jobs.putAll(jobs);
        this.variables.putAll(variables);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DTOProtocolResult execute()
    {
        // parse snmpRetries, snmpTimeout
        if(!this.extractSettings()) {
            return this.result;
        }

        // parse snmpVer, snmpPort; set snmpRead, snmpSet
        if(!this.setCredentials()) {
            return this.result;
        }

        // executing all jobs, saving results to this.result.data
        if(!this.performJobs()) {
            this.closeSnmp();
            return this.result;
        }

        // success
        this.closeSnmp();
        this.result.success = true;
        return this.result;
    }


    /**
     * {@inheritDoc}
     * @noinspection Duplicates
     */
    @Override
    protected Boolean extractSettings()
    {
        /*
         * Put some dynamic variables to map(nodeId, taskName)
         */
        if(coordinates.get("nodeId") != null && coordinates.get("nodeId").length() > 0) {
            DTOVariableConvertResult nodeIdVariableObject = new DTOVariableConvertResult();
            nodeIdVariableObject.setAction("process");
            nodeIdVariableObject.setStatus("success");
            nodeIdVariableObject.setVariableName("%%NODE_ID%%");
            nodeIdVariableObject.setResult(coordinates.get("nodeId"));
            nodeIdVariableObject.setVariableValue(coordinates.get("nodeId"));
            this.variables.put("%%NODE_ID%%", nodeIdVariableObject);
        }
        if(coordinates.get("taskName") != null && coordinates.get("taskName").length() > 0) {
            DTOVariableConvertResult taskNameVariableObject = new DTOVariableConvertResult();
            taskNameVariableObject.setAction("process");
            taskNameVariableObject.setStatus("success");
            taskNameVariableObject.setVariableName("%%TASK%%");
            taskNameVariableObject.setResult(coordinates.get("taskName"));
            taskNameVariableObject.setVariableValue(coordinates.get("taskName"));
            this.variables.put("%%TASK%%", taskNameVariableObject);
        }
        if(this.coordinates.get("nodeIp") != null && this.coordinates.get("nodeIp").length() > 0) {
            DTOVariableConvertResult nodeIpVariableObject = new DTOVariableConvertResult();
            nodeIpVariableObject.setAction("process");
            nodeIpVariableObject.setStatus("success");
            nodeIpVariableObject.setVariableName("%%NODE_IP%%");
            nodeIpVariableObject.setVariableValue(this.coordinates.get("nodeIp"));
            nodeIpVariableObject.setResult(this.coordinates.get("nodeIp"));
            this.variables.put("%%NODE_IP%%", nodeIpVariableObject);
        }

        String retries = this.settings.get("snmpRetries");
        String timeout = this.settings.get("snmpTimeout");

        /*
         * Set SNMP retries
         */
        if(retries == null || retries.length() == 0) {
            String retriesNotSetMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SNMP retries not set.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", retriesNotSetMessage);
            return false;
        }
        else {
            /*
             * Parse SNMP retries
             */
            try {
                this.snmpRetries  = Integer.parseInt(retries);
            }
            catch(NumberFormatException e) {

                String snmpRetriesParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": can't parse SNMP retries setting to integer.";
                this.logException("ERROR", "NODE PARSE SETTINGS", snmpRetriesParseMessage, e);
                return false;
            }
        }

        /*
         * Validating retries count
         */
        if(this.snmpRetries < 1 || this.snmpRetries > 10) {

            String snmpRetriesCountFailMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": SNMP retries count must be between 1 and 10.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", snmpRetriesCountFailMessage);

            return false;
        }

        /*
         * Set SNMP timeout
         */
        if(timeout == null || timeout.length() == 0) {
            this.snmpTimeout = 500;
        }
        else {
            /*
             * Parse SNMP timeout
             */
            try {
                this.snmpTimeout  = Integer.parseInt(timeout);
            }
            catch(NumberFormatException e) {
                String snmpTimeoutParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": can't parse SNMP timeout setting to integer.";
                this.logException("ERROR", "NODE PARSE SETTINGS", snmpTimeoutParseMessage, e);
                return false;
            }
        }

        return true;
    }


    /**
     * @noinspection Duplicates
     * {@inheritDoc}
     */
    @Override
    protected Boolean setCredentials()
    {

        this.snmpRead             = this.credentials.get("snmp_read");
        this.snmpSet              = this.credentials.get("snmp_set");
        String version            = this.credentials.get("snmp_version");
        String port               = this.credentials.get("port_snmp");

        if(this.snmpRead == null || this.snmpRead.length() == 0) {
            String noReadCommunityMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SNMP read community is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", noReadCommunityMessage);
            return false;
        }

        if(version == null || version.length() == 0) {
            String noVersionMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SNMP version is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", noVersionMessage);
            return false;
        }

        if(port == null || port.length() == 0) {
            String noSnmpPortMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SNMP port is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", noSnmpPortMessage);
            return false;
        }

        // SNMP write community is required but not set
        if(this.snmpSet == null || this.snmpSet.length() == 0) {

            for(Map.Entry<String, Map<String, String>> entry : this.jobs.entrySet()) {

                Map<String, String> jobInfo = entry.getValue();
                String snmpRequestType = jobInfo.get("snmp_request_type");

                if(snmpRequestType != null && snmpRequestType.equals("set")) {
                    String noWriteCommunityMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                            ": SNMP write community is required but not set.";
                    this.logMessage("ERROR", "NODE PARSE CREDENTIALS", noWriteCommunityMessage);
                    return false;
                }
            }
        }

        /*
         * Set SNMP version
         */
        try {
            this.snmpVer  = Integer.parseInt(version);
        }
        catch(NumberFormatException e) {
            String parseVersionMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse SNMP version to integer.";
            this.logException("WARNING", "NODE PARSE CREDENTIALS", parseVersionMessage, e);
            return false;
        }

        /*
         * Set SNMP port
         */
        try {
            this.snmpPort = Integer.parseInt(port);
        }
        catch(NumberFormatException e) {
            String parsePortMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse SNMP port to integer.";
            this.logException("WARNING", "NODE PARSE CREDENTIALS", parsePortMessage, e);
            return false;
        }

        return true;
    }


    /**
     * SNMP4J init
     * Send all SNMP requests
     *
     * @noinspection Duplicates
     * @return perform jobs success
     */
    @Override
    protected Boolean performJobs()
    {

        /*
         * SNMP object init
         */
        try {

            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen();

            Address address = new UdpAddress(this.coordinates.get("nodeIp") + "/" + this.snmpPort.toString());

            this.target = new CommunityTarget();
            this.target.setAddress(address);
            this.target.setTimeout(this.snmpTimeout);
            this.target.setRetries(this.snmpRetries);
            this.target.setCommunity(new OctetString(this.snmpRead));

            this.requestPDU = new PDU();
            this.requestPDU.setType(PDU.GET);

            switch (this.snmpVer) {
                case 0:
                    this.target.setVersion(SnmpConstants.version1);
                    break;
                case 1:
                    this.target.setVersion(SnmpConstants.version2c);
                    break;
                // ver 3 is not supported yet
                default:
                    String credentialsMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": wrong SNMP version.";
                    this.logMessage("ERROR", "NODE REQUEST", credentialsMessage);
                    return false;
            }

        }
        catch (Exception e) {
            String snmpObjectInitMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't create SNMP object.";
            this.logException("ERROR", "NODE REQUEST", snmpObjectInitMessage, e);
            return false;
        }

        /*
         * Executing jobs
         */
        for(Map.Entry<String, Map<String, String>> entry : this.jobs.entrySet()) {

            Boolean skipCommand = false;

            Map<String, String> jobInfo = entry.getValue();

            String snmpRequestType  = jobInfo.get("snmp_request_type");
            String snmpSetValue     = jobInfo.get("snmp_set_value");
            String snmpSetValueType = jobInfo.get("snmp_set_value_type");
            String command          = jobInfo.get("command_value");
            String timeoutStr       = jobInfo.get("timeout");
            Integer timeoutInt      = this.snmpTimeout;
            String tableField       = jobInfo.get("table_field");
            String currentVariable  = jobInfo.get("command_var");

            /*
             * Put empty variable to map
             */
            if(currentVariable != null && currentVariable.length() > 0) {
                this.variables.put(currentVariable, null);
            }

            /*
             * Injecting variables to commands
             */
            DTOVariableInjectResult varInjectResult = this.injectVariable(command);
            switch(varInjectResult.getStatus()) {
                case 0:
                    command = varInjectResult.getResult();
                    break;
                case 1:
                    skipCommand = true;
                    break;
                case 2:
                    return false;
            }

            /*
             * Parsing job timeout to integer
             */
            if(jobInfo.get("timeout") != null && jobInfo.get("timeout").length() > 0) {
                try {
                    timeoutInt = Integer.parseInt(timeoutStr);
                }
                catch(NumberFormatException e) {
                    String snmpCurrentTimeoutParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                            ": can't parse job SNMP timeout.";
                    this.logException("ERROR", "NODE REQUEST", snmpCurrentTimeoutParseMessage, e);
                    return false;
                }
            }

            if(snmpRequestType.equals("set")) {
                /*
                 * --------SNMP SET CASE----------
                 */

                // Case: empty SNMP set value
                if(snmpSetValue == null || snmpSetValue.length() == 0) {
                    String snmpEmptySetValueMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                            ": empty snmpset() value.";
                    this.logMessage("ERROR", "NODE REQUEST", snmpEmptySetValueMessage);
                    return false;
                }

                // adding keys to result, adding data to temp map
                if (tableField != null && tableField.length() > 0) {
                    this.result.data.put(tableField, "");
                }

                if(!skipCommand) {
                    /*
                     * Change community to SNMP-write community
                     */
                    this.target.setCommunity(new OctetString(this.snmpSet));

                    // Change PDU type to SET
                    this.requestPDU.setType(PDU.SET);

                    OID oid;

                    /*
                     * Trying convert text to OID
                     */
                    try {
                        oid = new OID(command);
                    } catch (Exception e) {
                        String snmpSetOidConvertMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": " + command +
                                " can't convert to SNMP OID";
                        this.logException("ERROR", "NODE REQUEST", snmpSetOidConvertMessage, e);
                        return false;
                    }

                    /*
                     * Convert values to types
                     * Add OID+set-var to PDU
                     */
                    try {
                        switch (snmpSetValueType) {
                            case "hex_string":
                                this.requestPDU.add(new VariableBinding(oid, new OctetString(snmpSetValue)));
                                break;
                            case "int":
                                Integer setInt = Integer.parseInt(snmpSetValue);
                                this.requestPDU.add(new VariableBinding(oid, new Integer32(setInt)));
                                break;
                            case "null":
                                this.requestPDU.add(new VariableBinding(oid, new Null()));
                                break;
                            case "octet_string":
                                this.requestPDU.add(new VariableBinding(oid, new OctetString(snmpSetValue)));
                                break;
                            case "uint":
                                Integer setUint = Integer.parseInt(snmpSetValue);
                                this.requestPDU.add(new VariableBinding(oid, new UnsignedInteger32(setUint)));
                                break;
                            case "ip_address":
                                this.requestPDU.add(new VariableBinding(oid, new IpAddress(snmpSetValue)));
                                break;
                            default:
                                String snmpSetValueTypeMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": " +
                                        oid.toString() + " - unknown snmpset() value type.";
                                this.logMessage("ERROR", "NODE REQUEST", snmpSetValueTypeMessage);
                                return false;
                        }
                    } catch (Exception e) {
                        String snmpSetConvertTypeMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": " +
                                oid.toString() + " - can't convert snmpset() value to type.";
                        this.logException("WARNING", "NODE REQUEST", snmpSetConvertTypeMessage, e);
                        return false;
                    }
                }

                /*
                 * SNMP-set send request
                 */
                if(!this.sendRequest(timeoutInt, currentVariable, tableField, skipCommand)) {
                    return false;
                }

                this.requestPDU.clear(); // clear PDU

            }
            else {
                /*
                 * --------SNMP GET CASE----------
                 */

                // adding keys to result, adding data to temp map
                if (tableField != null && tableField.length() > 0) {
                    this.result.data.put(tableField, "");
                }

                if(!skipCommand) {
                    /*
                     * set PDU-type to GET
                     */
                    this.requestPDU.setType(PDU.GET);
                    /*
                     * set read-community
                     */
                    this.target.setCommunity(new OctetString(this.snmpRead));
                    OID oid;


                    /*
                     * Trying convert text to OID
                     */
                    try {
                        oid = new OID(command);
                    } catch (Exception e) {
                        String snmpGetOidConvertMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": " + command +
                                " can't convert to SNMP OID.";
                        this.logException("ERROR", "NODE REQUEST", snmpGetOidConvertMessage, e);
                        return false;
                    }

                    /*
                     * Add GET-oids to PDU
                     */
                    try {
                        this.requestPDU.add(new VariableBinding(oid));
                    } catch (Exception e) {
                        String snmpAddGetToPduMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                ": can't add SNMP OID to PDU. Please check SNMP OID.";
                        this.logException("WARNING", "NODE REQUEST", snmpAddGetToPduMessage, e);
                        return false;
                    }
                }

                /*
                 * SNMP-get send request
                 */
                if(!this.sendRequest(timeoutInt, currentVariable, tableField, skipCommand)) {
                    return false;
                }

                this.requestPDU.clear(); // clear PDU

            }
        }

        return true;
    }


    /**
     * Sending get request
     *
     * @return Boolean SNMP GET-SET send success
     */
    private Boolean sendRequest(Integer timeout, String currentVariable, String currentTableField, Boolean skipCommand)
    {

        Boolean saveRequired   = currentTableField != null && currentTableField.length() > 0;
        Boolean putVarRequired = currentVariable != null && currentVariable.length() > 0;

        String valueToSave;

        if(!skipCommand) {
            // set custom timeout
            if (!timeout.equals(this.snmpTimeout)) {
                this.target.setTimeout(timeout);
            }

            PDU responsePDU;
            ResponseEvent responseEvent;

            /*
             * Sending request
             */
            try {
                responseEvent = this.snmp.send(this.requestPDU, this.target);
                // unset custom timeout
                if (!timeout.equals(this.snmpTimeout)) {
                    this.target.setTimeout(this.snmpTimeout);
                }
            } catch (Exception e) {
                String snmpSendGetMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't send snmpget() request.";
                this.logException("WARNING", "NODE REQUEST", snmpSendGetMessage, e);
                return false;
            }

            /*
             * Response processing
             */
            if (responseEvent == null) {
                String responseEventMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": agent timeout. Node offline or wrong community.";
                this.logMessage("ERROR", "NODE REQUEST", responseEventMessage);
                return false;
            }

            responsePDU = responseEvent.getResponse();

            if (responsePDU == null) {
                String responsePduMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": empty response PDU. Node offline or wrong community.";
                this.logMessage("ERROR", "NODE REQUEST", responsePduMessage);
                return false;
            }

            Vector tempVector = responsePDU.getVariableBindings();

            // Checking errors
            Integer errorStatus    = responsePDU.getErrorStatus();
            String errorStatusText = responsePDU.getErrorStatusText();

            if (errorStatus != PDU.noError) {
                String responsePduVectorMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": SNMP request error - " + errorStatusText + ".";
                this.logMessage("ERROR", "NODE REQUEST", responsePduVectorMessage);
                return false;
            }

            if (tempVector == null) {
                String responsePduVectorMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": empty response PDU vector. Node offline or wrong community.";
                this.logMessage("ERROR", "NODE REQUEST", responsePduVectorMessage);
                return false;
            }

            /*
             * SNMP response
             * if !exception add response to result
             * (we can iterate over tempVector if sending request pack)
             */
            if (tempVector.size() == 1) {
                VariableBinding vb = (VariableBinding) tempVector.get(0);

                if (vb.isException()) {
                    String responsePduVectorExceptionMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                            ": SNMP variable binding exception - " + vb.getOid().toString() + " " + vb.getVariable().getSyntaxString() + ".";
                    this.logMessage("ERROR", "NODE REQUEST", responsePduVectorExceptionMessage);
                    return false;
                }
                else {
                    /*
                     * Get SNMP responses, set required values to result
                     */
                    try {
                        String sVar = vb.getVariable().toString();
                        if (sVar == null) {
                            sVar = "";
                        }
                        valueToSave = sVar;
                    } catch (Exception e) {
                        String responsePduVectorConvertMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                ": " + vb.getOid().toString() + " - can't convert SNMP response to string.";
                        this.logMessage("ERROR", "NODE REQUEST", responsePduVectorConvertMessage);
                        return false;
                    }
                }
            }
            else {
                String responsePduVectorMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": empty response PDU vector. Node offline or wrong community.";
                this.logMessage("ERROR", "NODE REQUEST", responsePduVectorMessage);
                return false;
            }
        }
        else {
            valueToSave = "";
        }

        if (saveRequired || putVarRequired) {

            /*
            * Converting variable, if necessary
            */
            DTOVariableConvertResult currentResultDTO = this.convertVariable(this.coordinates.get("taskName"), currentTableField, currentVariable, valueToSave);


            if (saveRequired) {
                this.result.data.put(currentTableField, currentResultDTO.getResult());
            }

            if (putVarRequired) {
                this.variables.put(currentVariable, currentResultDTO);
            }
        }

        return true;
    }


    /**
     * Trying to close SNMP session
     */
    private void closeSnmp()
    {
        try {
            this.snmp.close();
        }
        catch (IOException e) {
            String snmpCloseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't close SNMP session.";
            this.logException("ERROR", "NODE REQUEST", snmpCloseMessage, e);
        }
    }


    /**
     * Transformation of variables, obtained as a result of jobs
     * You can convert variables' values, depending on tasks, table fields or variable names
     * Can be useful in such cases as: STP port numbers converting before usage in SNMP query
     *
     * @param variableName  - variable name
     * @param variableValue - hmm...variable value
     * @return DTOVariableConvertResult - variable object after converting
     */
    @Override
    protected DTOVariableConvertResult convertVariable(String taskName, String tableField, String variableName, String variableValue) {

        DTOVariableConvertResult result = new DTOVariableConvertResult();
        result.setAction("process");
        result.setStatus("success");
        result.setVariableName(variableName);
        result.setVariableValue(variableValue);
        result.setResult(variableValue);


        if(taskName.equals("stp") && tableField != null) {
            switch(tableField) {
                case "root_port":
                    // No SNMP requests with root port 0
                    if(variableValue.equals("0") || variableValue.length() == 0) {
                        result.setAction("restrict");
                        result.setResult("0");
                    }
                    break;
                case "root_mac":
                    variableValue = variableValue.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    if(variableValue.length() == 16) {
                        variableValue = variableValue.substring(4);
                    }
                    result.setResult(variableValue);
                    break;
                case "bridge_mac":
                    variableValue = variableValue.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    if(variableValue.length() == 16) {
                        variableValue = variableValue.substring(4);
                    }
                    result.setResult(variableValue);
                    break;
                case "node_mac":
                    variableValue = variableValue.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                    if(variableValue.length() == 16) {
                        variableValue = variableValue.substring(4);
                    }
                    result.setResult(variableValue);
                    break;
            }
        }

        return result;
    }

}
