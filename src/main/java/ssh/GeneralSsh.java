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
package ssh;

import abstractions.AbstractProtocol;
import abstractions.DTOExpectSendPair;
import abstractions.DTOSendExpectPair;
import abstractions.DTOProtocolResult;
import abstractions.DTOVariableConvertResult;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import expect4j.Expect4j;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * SSH Main class
 * @noinspection WeakerAccess
 */
public class GeneralSsh extends AbstractProtocol {
    protected static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    protected String ENTER_CHARACTER                            = "\n";

    protected String sshPromptChar;
    protected String sshEscapedRealPrompt;
    protected String currentCommand;

    protected Integer sshPort;
    protected Integer sshBeforeSendDelay;
    protected Integer sshTimeout;

    protected String sshLogin;
    protected String sshPassword;
    protected String sshEnablePassword;

    protected ArrayList<DTOExpectSendPair> sshAuthSequence = new ArrayList<>();
    protected ArrayList<DTOSendExpectPair> sshCommands     = new ArrayList<>();

    /*
     * Expect4j object
     */
    protected Expect4j expect      = null;
    protected JSch jsch            = null;
    protected Session session      = null;
    protected ChannelShell channel = null;


    /**
     * Constructor
     *
     * @param coordinates   - schedule, task, node, etc..
     * @param settings      - app settings
     * @param jobs          - sorted jobs
     * @param credentials   - credentials
     * @param variables     - variable list
     */
    public GeneralSsh(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables)
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
            this.closeSsh();
            return this.result;
        }

        // success
        this.closeSsh();
        this.result.success = true;
        return this.result;
    }


    /**
     * @noinspection Duplicates
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
            nodeIdVariableObject.setStatus("success");
            nodeIdVariableObject.setAction("process");
            nodeIdVariableObject.setVariableName("%%NODE_ID%%");
            nodeIdVariableObject.setVariableValue(coordinates.get("nodeId"));
            nodeIdVariableObject.setResult(coordinates.get("nodeId"));
            this.variables.put("%%NODE_ID%%", nodeIdVariableObject);
        }
        if(coordinates.get("taskName") != null && coordinates.get("taskName").length() > 0) {
            DTOVariableConvertResult taskNameVariableObject = new DTOVariableConvertResult();
            taskNameVariableObject.setStatus("success");
            taskNameVariableObject.setAction("process");
            taskNameVariableObject.setVariableName("%%TASK%%");
            taskNameVariableObject.setVariableValue(coordinates.get("taskName"));
            taskNameVariableObject.setResult(coordinates.get("taskName"));
            this.variables.put("%%TASK%%", taskNameVariableObject);
        }

        String timeout = this.settings.get("sshTimeout");
        String beforeSendDelay = this.settings.get("sshBeforeSendDelay");

        /*
         * Set SSH timeout
         */
        if (timeout == null || timeout.length() == 0) {

            String sshTimeoutNotSetMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SSH timeout is not set.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", sshTimeoutNotSetMessage);
            return false;
        }
        else {
            /*
             * Parse SSH timeout
             */
            try {
                this.sshTimeout = Integer.parseInt(timeout);
            } catch (NumberFormatException e) {

                String sshTimeoutParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": can't parse SSH timeout setting to integer.";
                this.logException("ERROR", "NODE PARSE SETTINGS", sshTimeoutParseMessage, e);
                return false;
            }
        }

        /*
         * Validate SSH timeout
         */
        if (this.sshTimeout < 100 || this.sshTimeout > 120000) {

            String sshTimeoutValueFailMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": SSH timeout must be between 100 and 120000 milliseconds.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", sshTimeoutValueFailMessage);
            return false;
        }

        /*
         * Set SSH before send delay
         */
        if (beforeSendDelay == null || beforeSendDelay.length() == 0) {

            String sshDelayNotSetMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SSH before-send delay is not set.";
            this.logMessage("ERROR", "NODE PARSE SETTINGS", sshDelayNotSetMessage);
            return false;
        } else {

            /*
             * Parse SSH before-send delay
             */
            try {
                this.sshBeforeSendDelay = Integer.parseInt(beforeSendDelay);
            } catch (NumberFormatException e) {

                String sshDelayParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": can't parse SSH before-send delay setting to integer.";
                this.logException("ERROR", "NODE PARSE SETTINGS", sshDelayParseMessage, e);
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
    protected Boolean setCredentials() {

        this.sshLogin          = this.credentials.get("ssh_login");
        this.sshPassword       = this.credentials.get("ssh_password");
        this.sshEnablePassword = this.credentials.get("enable_password");
        String port            = this.credentials.get("port_ssh");
        String authSequence    = this.credentials.get("auth_sequence");

        /*
         * Validate SSH login
         */
        if (this.sshLogin == null || this.sshLogin.length() == 0) {
            String sshNoLoginMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SSH login is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", sshNoLoginMessage);
            return false;
        }

        /*
         * Set empty enable password
         */
        if (this.sshEnablePassword == null) {
            this.sshEnablePassword = "";
        }

        /*
         * Validate SSH port
         */
        if (port == null || port.length() == 0) {
            String noSshPortMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SSH port is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", noSshPortMessage);
            return false;
        }

        /*
         * Set SSH port
         */
        try {
            this.sshPort = Integer.parseInt(port);
        } catch (NumberFormatException e) {
            String portParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse SSH port to integer.";
            this.logException("WARNING", "NODE PARSE CREDENTIALS", portParseMessage, e);
            return false;
        }

        /*
         * Set auth sequence
         */
        if (authSequence == null || authSequence.length() == 0) {
            String sshAuthSequenceMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": model auth sequence is not set.";
            this.logMessage("ERROR", "NODE PARSE CREDENTIALS", sshAuthSequenceMessage);
            return false;
        } else {

            String[] splitAuthSequence = authSequence.split("\n");

            Integer sequenceCount = splitAuthSequence.length;

            if (sequenceCount % 2 != 1) {
                String sshAuthSequenceMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": model auth sequence error. Wrong elements count.";
                this.logMessage("ERROR", "NODE PARSE CREDENTIALS", sshAuthSequenceMessage);
                return false;
            }

            for (int i = 0; i < sequenceCount; i++) {

                String currentExpect = splitAuthSequence[i].trim();
                String currentResponse;

                i++;

                if (i >= sequenceCount) {
                    this.sshPromptChar = currentExpect;
                    currentResponse = null;
                } else {
                    currentResponse = splitAuthSequence[i].trim();
                    // if template variable
                    if (currentResponse.contains("{{") && currentResponse.contains("}}")) {
                        switch (currentResponse) {
                            case "{{telnet_login}}":
                                currentResponse = "{{SKIP}}";
                                break;
                            case "{{telnet_password}}":
                                currentResponse = "{{SKIP}}";
                                break;
                            case "{{enable_password}}":
                                currentResponse = this.sshEnablePassword;
                                break;
                            default:
                                String sshAuthSequenceMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                        ": model auth sequence error. Unknown type of template variable.";
                                this.logMessage("ERROR", "NODE PARSE CREDENTIALS", sshAuthSequenceMessage);
                                return false;
                        }
                    }
                }

                if (currentResponse == null || !currentResponse.equals("{{SKIP}}")) {
                    DTOExpectSendPair currentPair = new DTOExpectSendPair(currentExpect, currentResponse);
                    this.sshAuthSequence.add(currentPair);
                }
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
         * SSH connect
         */
        try {

            this.jsch = new JSch();
            this.session = jsch.getSession(this.sshLogin, this.coordinates.get("nodeIp"), this.sshPort);
            this.session.setPassword(this.sshPassword);

            if (this.sshPassword != null) {
                this.session.setPassword(this.sshPassword);
            }

            Hashtable<String,String> config = new Hashtable<>();
            config.put("StrictHostKeyChecking", "no");
            config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256,ecdh-sha2-nistp256,ecdh-sha2-nistp384,ecdh-sha2-nistp521");

            this.session.setConfig(config);
            this.session.connect(150000);

            this.channel = (ChannelShell) this.session.openChannel("shell");
            this.expect  = new Expect4j(this.channel.getInputStream(), this.channel.getOutputStream());
            this.expect.setDefaultTimeout(this.sshTimeout);
            this.channel.connect();

        }
        catch (Exception e) {
            String sshObjectInitMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't establish SSH connection.";
            this.logException("ERROR", "NODE REQUEST", sshObjectInitMessage, e);
            return false;
        }


        /*
         * Ssh after auth action
         * (like "enable mode" and wait for first prompt)
         */
        if(!this.sshAuth()) {
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
             * Parsing command SSH timeout to integer
             */
            if(currentTimeout != null && currentTimeout.length() > 0) {
                try {
                    currentTimeoutInt = Integer.parseInt(currentTimeout);
                }
                catch(NumberFormatException e) {
                    String sshCurrentTimeoutParseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                            ": can't parse job SSH timeout to integer.";
                    this.logException("ERROR", "NODE REQUEST", sshCurrentTimeoutParseMessage, e);
                    return false;
                }
            }
            else {
                currentTimeoutInt = this.sshTimeout;
            }

            /*
             * Set prompt to expect(custom or not)
             */
            String customPrompt  = jobInfo.get("cli_custom_prompt");
            String currentPrompt;
            Boolean replaceExpect = true;
            if (customPrompt == null || customPrompt.length()==0) {
                currentPrompt = this.sshEscapedRealPrompt;
            }
            else {
                currentPrompt = customPrompt;
                replaceExpect = false;
            }

            DTOSendExpectPair currentPair = new DTOSendExpectPair(currentPrompt, currentCommand, currentTableField, currentTimeoutInt, currentVariable, replaceExpect);
            this.sshCommands.add(currentPair);

        }

        /*
         * Job commands exec
         */
        return this.sshCommands();

    }


    /**
     * Sending device post auth credentials
     * (enable mode and waiting for first prompt)
     *
     * @return Boolean
     */
    protected Boolean sshAuth() {

        for(DTOExpectSendPair currentPair : this.sshAuthSequence) {
            if(!this.executeAuth(currentPair)) {
                return false;
            }
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
                    Thread.sleep(this.sshBeforeSendDelay);
                }
                catch(InterruptedException e) {
                    String sshDelayInterruptedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SSH auth command timeout exceeded.";
                    this.logException("ERROR", "NODE AUTH", sshDelayInterruptedMessage, e);
                    return false;
                }

                if(pair.getSend() != null) {
                    this.expect.send(pair.getSend() + ENTER_CHARACTER);
                }

                return true;
            }
            else {
                String sshAuthExecMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": timeout. SSH response expect failed. Check device auth sequence.";
                this.logMessage("ERROR", "NODE AUTH", sshAuthExecMessage);
                return false;
            }
        }
        catch(Exception e) {
            String sshAuthExecMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": exception upon SSH auth command execution.";
            this.logException("ERROR", "NODE AUTH", sshAuthExecMessage, e);
            return false;
        }
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
        DTOSendExpectPair pairForPrompt = new DTOSendExpectPair(this.sshPromptChar, "", "", this.sshTimeout, null, false);
        if (!this.executeCommand(pairForPrompt, false)) {
            return false;
        }

        /*
         * Get device prompt
         */
        if(this.expect.getLastState().getBuffer() == null) {
            String sshEmptyPromptMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": SSH prompt response is empty.";
            this.logMessage("ERROR", "NODE REQUEST", sshEmptyPromptMessage);
            return false;
        }
        else {
            this.sshEscapedRealPrompt = this.expect.getLastState().getBuffer()
                    .replaceAll("\u001B\\[\\?[\\d;]*[^\\d;]|\u001B\\[[\\d;]*[^\\d;]|\u001B[^\\d;]|\u001B[\\d;]|\u001B\\[[^\\d;]","")
                    .replace("[", "\\[") // Escape for expect4j
                    .trim();
        }

        if(this.sshEscapedRealPrompt.length() == 0) {
            String sshEmptyPromptMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                    ": SSH prompt is empty.";
            this.logMessage("ERROR", "NODE REQUEST", sshEmptyPromptMessage);
            return false;
        }

        return true;
    }


    /**
     * Executing SSH command:
     * Sending command
     * Expecting response
     *
     * @param pair DTOSendExpectPair
     * @param noOutput Boolean - output requirement
     * @return Boolean - SSH command success
     */
    protected Boolean executeCommand(DTOSendExpectPair pair, Boolean noOutput) {

        try {
            /*
             * Wait before send next command
             */
            try {
                Thread.sleep(this.sshBeforeSendDelay);
            }
            catch(InterruptedException e) {
                String sshDelayInterruptedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": SSH command timeout exceeded.";
                this.logException("ERROR", "NODE REQUEST", sshDelayInterruptedMessage, e);
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
                String sshCommandFailedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                        ": timeout. Command: " + pair.getSend() + ". SSH response expect is failed. Check command timeouts.";
                this.logMessage("ERROR", "NODE REQUEST", sshCommandFailedMessage);
                return false;
            }

        }
        catch(Exception e) {
            String sshCommandFailedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": exception upon SSH command execution. Command: " + pair.getSend() + ".";
            this.logException("ERROR", "NODE REQUEST", sshCommandFailedMessage, e);
            return false;
        }

        return true;
    }


    /**
     * Sending scope of SSH commands
     * Saving responses to result.data
     *
     * @noinspection Duplicates
     * @return Boolean
     */
    protected Boolean sshCommands() {

        /*
         * Clear buffer
         */
        this.expect.getLastState().setBuffer("");

        Integer commandCount = this.sshCommands.size();

        /*
         * Executing all commands
         */
        for (int i = 0; i < commandCount; i++) {

            DTOSendExpectPair currentPair = this.sshCommands.get(i);

            Boolean saveRequired = currentPair.getTableField() != null && currentPair.getTableField().length() > 0;
            Boolean putVarRequired = currentPair.getVariable() != null && currentPair.getVariable().length() > 0;
            Boolean skipCommand = false;

            /*
             * Injecting variables to commands
             */
            if(currentPair.getSend().contains("%%")) {

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
                                String sshSetVarFailedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                        ": empty variable value returned. Command: " + currentPair.getSend() + ". SSH request: set custom variable failed. Check your command.";
                                this.logMessage("ERROR", "NODE REQUEST", sshSetVarFailedMessage);
                                return false;
                            }
                        }
                        else {
                            switch (entry.getValue().getStatus()) {
                                // If variable converted with error, sending exception log
                                case "exception":
                                    String variableConvertErrorMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                            ": variable convertion error. Variable: " + entry.getValue().getVariableName() + ". Message: " + entry.getValue().getMessage();
                                    this.logMessage("ERROR", "NODE REQUEST", variableConvertErrorMessage);
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
                Boolean changeTimeout = !currentPair.getTimeout().equals(this.sshTimeout);

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
                 * Change timeout back to standart
                 */
                if (changeTimeout) {
                    this.expect.setDefaultTimeout(this.sshTimeout);
                }
            }

            /*
             * Saving to result.data
             */
            if(saveRequired || putVarRequired) {

                String valueToSave;
                if(!skipCommand) {
                    valueToSave = this.expect.getLastState().getBuffer()
                        .replace(this.currentCommand.trim(), "")
                        .trim()
                        // replacing ANSI control chars
                        .replaceAll("\u001B\\[\\?[\\d;]*[^\\d;]|\u001B\\[[\\d;]*[^\\d;]|\u001B[^\\d;]|\u001B[\\d;]|\u001B\\[[^\\d;]","")
                        .trim();

                    if(currentPair.getReplaceExpect()) {
                        valueToSave = valueToSave.replaceAll(Pattern.quote(currentPair.getExpect().replace("\\[", "[")), "").trim();
                    }
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
     * Check SSH response code
     *
     * @param intRetVal int - return code
     * @return boolean
     */
    protected boolean checkResult(int intRetVal) {
        return intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE;
    }


    /**
     * Trying to close SSH
     */
    protected void closeSsh() {

        if(this.channel != null) {
            this.channel.disconnect();
        }

        if(this.session != null) {
            session.disconnect();
        }

        if (this.expect!=null) {
            try {
                this.expect.close();
            } catch (Exception e) {
                String sshCloseMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't close SSH expect tool.";
                this.logException("ERROR", "NODE REQUEST", sshCloseMessage, e);
            }
        }
    }

}
