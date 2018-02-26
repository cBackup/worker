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

import abstractions.DTOExpectSendPair;
import abstractions.DTOVariableConvertResult;
import java.util.Map;


/**
 * Vendor Mikrotik general ssh class
 * @noinspection unused
 */
public class _Mikrotik_Ssh extends GeneralSsh {

    /**
     * Constructor
     *
     * @param coordinates   - schedule, task, node, etc..
     * @param settings      - app settings
     * @param credentials   - credentials
     * @param jobs          - sorted jobs
     * @param variables     - variable list
     */
    public _Mikrotik_Ssh(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables)
    {
        /*
         * Parent constructor
         */
        super(coordinates, settings, credentials, jobs, variables);

        /*
         * Set ENTER_CHARACTER
         */
        ENTER_CHARACTER = "\r\n";
        this.controlSeqences.put("%%SEQ(ENTER)%%", this.ENTER_CHARACTER);

    }


    /**
     * @noinspection Duplicates
     * {@inheritDoc}
     */
    @Override
    protected Boolean setCredentials()
    {

        this.sshLogin          = this.credentials.get("ssh_login") + "+tc1000h200w";
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

}
