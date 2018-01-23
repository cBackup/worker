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
package telnet;

import abstractions.DTOExpectSendPair;
import abstractions.DTOSendExpectPair;
import abstractions.DTOProtocolResult;
import abstractions.DTOVariableConvertResult;
import abstractions.AbstractProtocol;

import java.util.ArrayList;
import java.util.Map;

/*
 * Expect4j
 */
import expect4j.Expect4j;
import expect4j.ExpectUtils;


/**
 * Telnet
 * @noinspection WeakerAccess
 */
public class GeneralTelnet extends AbstractProtocol {

    protected static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    protected static String ENTER_CHARACTER                     = "\r\n";

    protected String telnetPromptChar;
    protected String telnetRealPrompt;
    protected String telnetEscapedRealPrompt;
    protected String currentCommand;

    protected Integer telnetPort;
    protected Integer telnetBeforeSendDelay;
    protected Integer telnetTimeout;

    protected String telnetLogin;
    protected String telnetPassword;
    protected String telnetEnablePassword;

    protected ArrayList<DTOExpectSendPair> telnetAuthSequence = new ArrayList<>();
    protected ArrayList<DTOSendExpectPair> telnetCommands     = new ArrayList<>();

    /*
     * Expect4j object
     */
    protected Expect4j expect = null;

    /**
     * Constructor
     *
     * @param coordinates   - schedule, task, node, etc..
     * @param settings      - app settings
     * @param jobs          - sorted jobs
     * @param credentials   - credentials
     * @param variables     - variable list
     */
    public GeneralTelnet(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables)
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

        if(!this.extractSettings()) {
            return this.result;
        }

        if(!this.setCredentials()) {
            return this.result;
        }

        // executing all jobs, saving results to this.result.data
        if(!this.performJobs()) {
            this.closeTelnet();
            return this.result;
        }

        // success
        this.closeTelnet();
        this.result.success = true;
        return this.result;
    }


    /**
     * {@inheritDoc}
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
            nodeIdVariableObject.setVariableValue(coordinates.get("nodeId"));
            nodeIdVariableObject.setResult(coordinates.get("nodeId"));
            this.variables.put("%%NODE_ID%%", nodeIdVariableObject);
        }
        if(coordinates.get("taskName") != null && coordinates.get("taskName").length() > 0) {
            DTOVariableConvertResult taskNameVariableObject = new DTOVariableConvertResult();
            taskNameVariableObject.setAction("process");
            taskNameVariableObject.setStatus("success");
            taskNameVariableObject.setVariableName("%%TASK%%");
            taskNameVariableObject.setVariableValue(coordinates.get("taskName"));
            taskNameVariableObject.setResult(coordinates.get("taskName"));
            this.variables.put("%%TASK%%", taskNameVariableObject);
        }

        String timeout         = this.settings.get("telnetTimeout");
        String beforeSendDelay = this.settings.get("telnetBeforeSendDelay");

        /*
         * Set telnet timeout
         */
        if(timeout == null || timeout.length() == 0) {

            String telnetTimeoutNotSetMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": telnet timeout is not set.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", telnetTimeoutNotSetMessage);
            return false;
        }
        else {
            /*
             * Parse telnet timeout
             */
            try {
                this.telnetTimeout  = Integer.parseInt(timeout);
            }
            catch(NumberFormatException e) {

                String telnetTimeoutParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": can't parse telnet timeout setting to integer.";
                this.logException("ERROR", "NODE PARSE SETTINGS", telnetTimeoutParseMessage, e);
                return false;
            }
        }

        /*
         * Validate telnet timeout
         */
        if(this.telnetTimeout < 100 || this.telnetTimeout > 120000) {

            String telnetTimeoutValueFailMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": telnet timeout must be between 100 and 120000 milliseconds.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", telnetTimeoutValueFailMessage);
            return false;
        }

        /*
         * Set telnet before send delay
         */
        if(beforeSendDelay == null || beforeSendDelay.length() == 0) {

            String telnetDelayNotSetMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": telnet before-send delay is not set.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", telnetDelayNotSetMessage);
            return false;
        }
        else {
            /*
             * Parse telnet before-send delay
             */
            try {
                this.telnetBeforeSendDelay  = Integer.parseInt(beforeSendDelay);
            }
            catch(NumberFormatException e) {

                String telnetDelayParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": can't parse telnet before-send delay setting to integer.";
                this.logException("ERROR", "NODE PARSE SETTINGS", telnetDelayParseMessage, e);
                return false;
            }
        }

        /*
         * Validating telnet timeout
         */
        if(this.telnetBeforeSendDelay < 10 || this.telnetBeforeSendDelay > 10000) {

            String telnetDelayValueFailMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": telnet telnet before-send delay must be between 10 and 10000 milliseconds.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", telnetDelayValueFailMessage);
            return false;
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

        this.telnetLogin          = this.credentials.get("telnet_login");
        this.telnetPassword       = this.credentials.get("telnet_password");
        this.telnetEnablePassword = this.credentials.get("enable_password");
        String port               = this.credentials.get("port_telnet");
        String authSequence       = this.credentials.get("auth_sequence");


        /*
         * Validate telnet login
         */
        if(this.telnetLogin == null || this.telnetLogin.length() == 0) {
            String noLoginMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": telnet login is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", noLoginMessage);
            return false;
        }

        /*
         * Set empty passwords
         */
        if(this.telnetPassword == null) {
            this.telnetPassword = "";
        }
        if(this.telnetEnablePassword == null) {
            this.telnetEnablePassword = "";
        }

        /*
         * Validate telnet port
         */
        if(port == null || port.length() == 0) {
            String noTelnetPortMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": telnet port is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", noTelnetPortMessage);
            return false;
        }

        /*
         * Set telnet port
         */
        try {
            this.telnetPort = Integer.parseInt(port);
        }
        catch(NumberFormatException e) {
            String portParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse telnet port to integer.";
            this.logException("WARNING", "NODE PARSE CREDENTIALS", portParseMessage, e);
            return false;
        }

        /*
         * Set auth sequence
         */
        if(authSequence == null || authSequence.length() == 0) {
            String telnetAuthSequenceMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": model auth sequence is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", telnetAuthSequenceMessage);
            return false;
        }
        else {

            String[] splitAuthSequence = authSequence.split("\n");

            Integer sequenceCount = splitAuthSequence.length;

            if(sequenceCount % 2 != 1) {
                String telnetAuthSequenceMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": model auth sequence error. Wrong elements count.";
                this.logMessage("ERROR", "NODE PARSE CREDENTIALS", telnetAuthSequenceMessage);
                return false;
            }

            for(int i=0; i<sequenceCount; i++) {

                String currentExpect = splitAuthSequence[i].trim();
                String currentResponse;

                i++;

                if(i >= sequenceCount) {
                    this.telnetPromptChar = currentExpect;
                    currentResponse = null;
                }
                else {
                    currentResponse = splitAuthSequence[i].trim();
                    // if template variable
                    if(currentResponse.contains("{{") && currentResponse.contains("}}")) {
                        switch(currentResponse) {
                            case "{{telnet_login}}":
                                currentResponse = this.telnetLogin;
                                break;
                            case "{{telnet_password}}":
                                currentResponse = this.telnetPassword;
                                break;
                            case "{{enable_password}}":
                                currentResponse = this.telnetEnablePassword;
                                break;
                            default:
                                String telnetAuthSequenceMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                        ": model auth sequence error. Unknown type of template variable.";
                                this.logMessage("ERROR", "NODE PARSE CREDENTIALS", telnetAuthSequenceMessage);
                                return false;
                        }
                    }
                }

                DTOExpectSendPair currentPair = new DTOExpectSendPair(currentExpect, currentResponse);
                this.telnetAuthSequence.add(currentPair);
            }
        }

        return true;
    }


    /**
     * Expect init
     * Send credentials
     * Get device prompt
     * Send commands
     *
     * @return Boolean
     */
    @Override
    protected Boolean performJobs()
    {

        /*
         * Object Expect4j init
         */
        try {
            this.expect = ExpectUtils.telnet(this.coordinates.get("nodeIp"), this.telnetPort);
            this.expect.setDefaultTimeout(this.telnetTimeout);
        }
        catch (Exception e) {
            String telnetObjectInitMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't create telnet expect object.";
            this.logException("ERROR", "NODE REQUEST", telnetObjectInitMessage, e);
            return false;
        }


        /*
         * Telnet authorization
         */
        if(!this.telnetAuth()) {
            return false;
        }

        /*
         * Get device prompt
         */
        if(!this.setRealPrompt()) {
            return false;
        }

        /*
         * Setting jobs to expect-send pairs
         */
        for(Map.Entry<String, Map<String, String>> entry : this.jobs.entrySet()) {

            Map<String, String> jobInfo = entry.getValue();

            String currentCommand     = jobInfo.get("command_value");
            String currentTableField  = jobInfo.get("table_field");
            String currentTimeout     = jobInfo.get("timeout");
            String currentVariable    = jobInfo.get("command_var");

            if(currentVariable != null && currentVariable.length() > 0) {
                this.variables.put(currentVariable, null);
            }

            Integer currentTimeoutInt;

            if (currentTableField != null && currentTableField.length() > 0) {
                this.result.data.put(currentTableField, "");
            }

            /*
             * Parsing command telnet timeout to integer
             */
            if(currentTimeout != null && currentTimeout.length() > 0) {
                try {
                    currentTimeoutInt = Integer.parseInt(currentTimeout);
                }
                catch(NumberFormatException e) {
                    String telnetCurrentTimeoutParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                            ": can't parse job telnet timeout to integer.";
                    this.logException("ERROR", "NODE REQUEST", telnetCurrentTimeoutParseMessage, e);
                    return false;
                }
            }
            else {
                currentTimeoutInt = this.telnetTimeout;
            }

            DTOSendExpectPair currentPair = new DTOSendExpectPair(this.telnetEscapedRealPrompt, currentCommand, currentTableField, currentTimeoutInt, currentVariable);
            this.telnetCommands.add(currentPair);

        }

        /*
         * Job commands exec
         */
        return this.telnetCommands();
    }


    /**
     * Extract device prompt string
     *
     * @return Boolean
     */
    protected Boolean setRealPrompt() {

        /*
         * Clear buffer
         */
        this.expect.getLastState().setBuffer("");

        /*
         * Send enter
         */
        DTOSendExpectPair pairForPrompt = new DTOSendExpectPair(this.telnetPromptChar, "", "", this.telnetTimeout, null);
        if (!this.executeCommand(pairForPrompt, false)) {
            return false;
        }

        /*
         * Get device prompt
         */
        if(this.expect.getLastState().getBuffer() == null) {
            String telnetEmptyPromptMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": telnet prompt response is empty.";
            this.logMessage("ERROR", "NODE REQUEST", telnetEmptyPromptMessage);
            return false;
        }
        else {

            this.telnetEscapedRealPrompt = this.expect.getLastState().getBuffer().replace(this.currentCommand, "");
            // replacing ANSI control chars
            this.telnetEscapedRealPrompt = this.telnetEscapedRealPrompt.replaceAll("\u001B\\[\\?[\\d;]*[^\\d;]|\u001B\\[[\\d;]*[^\\d;]|\u001B[^\\d;]|\u001B[\\d;]|\u001B\\[[^\\d;]","");

            this.telnetRealPrompt        = this.telnetEscapedRealPrompt.trim();
            this.telnetEscapedRealPrompt = this.telnetEscapedRealPrompt.replace("[", "\\[").trim();

        }

        if(this.telnetEscapedRealPrompt.length() == 0) {
            String telnetEmptyPromptMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": telnet prompt is empty.";
            this.logMessage("ERROR", "NODE REQUEST", telnetEmptyPromptMessage);
            return false;
        }

        return true;
    }


    /**
     * Sending scope of telnet commands
     * Saving responses to result.data
     *
     * @noinspection Duplicates
     * @return Boolean
     */
    protected Boolean telnetCommands() {

        /*
         * Clear buffer
         */
        this.expect.getLastState().setBuffer("");

        Integer commandCount = this.telnetCommands.size();

        /*
         * Executing all commands
         */
        for (int i = 0; i < commandCount; i++) {

            DTOSendExpectPair currentPair = this.telnetCommands.get(i);

            Boolean saveRequired = currentPair.getTableField() != null && currentPair.getTableField().length() > 0;
            Boolean putVarRequired = currentPair.getVariable() != null && currentPair.getVariable().length() > 0;
            Boolean skipCommand = false;

            /*
             * Injecting variables to commands
             */
            if(currentPair.getSend().contains("%%")) {

                // Searching variable in command
                for(Map.Entry<String, DTOVariableConvertResult> entry : this.variables.entrySet()) {
                    if( currentPair.getSend().contains(entry.getKey())) {
                        // Variable found
                        // If variable is converted, sending command with injected variable
                        if(entry.getValue().getAction().equals("process")) {
                            // check if empty
                            if(entry.getValue() != null && entry.getValue().getResult() != null && entry.getValue().getResult().length() > 0) {
                                String tempCommand = currentPair.getSend().replaceAll(entry.getKey(), entry.getValue().getResult());
                                currentPair.setSend(tempCommand);
                            }
                            else {
                                String telnetSetVarFailedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                        ": empty variable value returned. Command: " + currentPair.getSend() + ". Variable:" + entry.getValue().getVariableName() + ". Telnet request: set custom variable failed. Check your command.";
                                this.logMessage("ERROR", "NODE REQUEST", telnetSetVarFailedMessage);
                                return false;
                            }
                        }
                        else {
                            switch (entry.getValue().getStatus()) {
                                // If variable converted with error, sending exception log
                                case "exception":
                                    String variableConvertMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                            ": variable convertion error. Variable: " + entry.getValue().getVariableName() + ". Message: " + entry.getValue().getMessage();
                                    this.logMessage("ERROR", "NODE REQUEST", variableConvertMessage);
                                    return false;
                                // If variable converted successfully, but action is restrict, skip command and use empty string as command result
                                case "success":
                                    skipCommand = true;
                                    break;
                                default:
                                    String variableConvertUnknownStatus = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                            ": unknown status of variable convertion. Variable: " + entry.getValue().getVariableName() + ". Status: " + entry.getValue().getStatus();
                                    this.logMessage("ERROR", "NODE REQUEST", variableConvertUnknownStatus);
                                    return false;
                            }
                        }
                    }
                }
            }

            if(!skipCommand) {

                /*
                 * Timeout change required?
                 */
                Boolean changeTimeout = !currentPair.getTimeout().equals(this.telnetTimeout);

                /*
                 * Change timeout to custom
                 */
                if (changeTimeout) {
                    this.expect.setDefaultTimeout(currentPair.getTimeout());
                }

                /*
                 * Wait for output?
                 */
                Boolean noOutput = (i + 1 >= commandCount) && !saveRequired;

                /*
                 * Executing commands
                 */
                if (!this.executeCommand(currentPair, noOutput)) {
                    return false;
                }

                /*
                 * Change timeout back to default
                 */
                if (changeTimeout) {
                    this.expect.setDefaultTimeout(this.telnetTimeout);
                }
            }

            /*
             * Saving to result.data
             */
            if(saveRequired || putVarRequired) {

                String valueToSave;

                if(!skipCommand) {
                    valueToSave = this.expect.getLastState().getBuffer().replace(this.currentCommand, "")
                        // replacing ANSI control chars
                        .replaceAll("\u001B\\[\\?[\\d;]*[^\\d;]|\u001B\\[[\\d;]*[^\\d;]|\u001B[^\\d;]|\u001B[\\d;]|\u001B\\[[^\\d;]","")
                        .replace(this.telnetRealPrompt, "")
                        .trim();
                }
                else {
                    valueToSave = "";
                }

                /*
                 * Converting variable, if necessary
                 */
                DTOVariableConvertResult currentResultDTO = this.convertVariable(this.coordinates.get("taskName"), currentPair.getTableField(), currentPair.getVariable(), valueToSave);

                if(saveRequired) {
                    this.result.data.put(currentPair.getTableField(), currentResultDTO.getResult());
                }

                /*
                 * Put variable
                 */
                if(putVarRequired) {
                    this.variables.put(currentPair.getVariable(), currentResultDTO);
                }
            }
        }

        return true;
    }


    /**
     * Sending device auth credentials scope
     *
     * @return Boolean
     */
    protected Boolean telnetAuth() {

        for(DTOExpectSendPair currentPair : this.telnetAuthSequence) {
            if(!this.executeAuth(currentPair)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Executing telnet command:
     * Sending command
     * Expecting response
     *
     * @param pair DTOSendExpectPair
     * @param noOutput Boolean - output requirement
     * @return Boolean - telnet command success
     */
    protected Boolean executeCommand(DTOSendExpectPair pair, Boolean noOutput) {

        try {
            /*
             * Wait before send next command
             */
            try {
                Thread.sleep(this.telnetBeforeSendDelay);
            }
            catch(InterruptedException e) {
                String telnetDelayInterruptedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": telnet command timeout exceeded.";
                this.logException("ERROR", "NODE REQUEST", telnetDelayInterruptedMessage, e);
                return false;
            }

            /*
             * Sending command
             */
            this.currentCommand = pair.getSend() + ENTER_CHARACTER;

            this.expect.send(this.currentCommand);

            boolean isFailed = false;

            /*
             * Expect result if we need it
             */
            if(!noOutput) {
                isFailed = checkResult(this.expect.expect(pair.getExpect()));
            }

            if (isFailed) {
                String telnetCommandFailedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": timeout. Command: " + pair.getSend() + ". Telnet response expect failed. Check command timeouts.";
                this.logMessage("ERROR", "NODE REQUEST", telnetCommandFailedMessage);
                return false;
            }

        }
        catch(Exception e) {
            String telnetCommandFailedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": exception upon telnet command execution. Command: " + pair.getSend() + ".";
            this.logException("ERROR", "NODE REQUEST", telnetCommandFailedMessage, e);
            return false;
        }

        return true;
    }


    /**
     * Executing authorization command:
     * Expecting prompt,
     * Sending credential
     *
     * @param pair DTOExpectSendPair
     * @return Boolean - authorization command success
     */
    protected Boolean executeAuth(DTOExpectSendPair pair) {

        try{

            boolean isFailed = checkResult(this.expect.expect(pair.getExpect()));

            if (!isFailed) {

                // Using delay before send
                try {
                    Thread.sleep(this.telnetBeforeSendDelay);
                }
                catch(InterruptedException e) {
                    String telnetDelayInterruptedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": telnet auth command timeout exceeded.";
                    this.logException("ERROR", "NODE AUTH", telnetDelayInterruptedMessage, e);
                    return false;
                }

                if(pair.getSend() != null) {
                    this.expect.send(pair.getSend() + ENTER_CHARACTER);
                }

                return true;
            }
            else {
                String telnetAuthExecMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": timeout. Telnet response expect failed. Check device auth sequence and credentials.";
                this.logMessage("ERROR", "NODE AUTH", telnetAuthExecMessage);
                return false;
            }
        }
        catch(Exception e) {
            String telnetAuthExecMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": exception upon telnet command execution.";
            this.logException("ERROR", "NODE AUTH", telnetAuthExecMessage, e);
            return false;
        }
    }


    /**
     * Check telnet response code
     *
     * @param intRetVal int - return code
     * @return boolean
     */
    protected boolean checkResult(int intRetVal) {
        return intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE;
    }


    /**
     * Trying to close telnet
     * @noinspection EmptyCatchBlock
     */
    protected void closeTelnet() {
        if (this.expect!=null) {
            try {
                this.expect.close();
            } catch (Exception e) {}
        }
    }
}
