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
import abstractions.DTOVariableConvertResult;
import java.util.Map;


/**
 * Vendor Mikrotik general telnet class
 * @noinspection unused
 */
public class _Mikrotik_Telnet extends GeneralTelnet {

    /**
     * Constructor
     *
     * @param coordinates   - schedule, task, node, etc..
     * @param settings      - app settings
     * @param jobs          - sorted jobs
     * @param credentials   - credentials
     * @param variables     - variable list
     */
    public _Mikrotik_Telnet(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables)
    {
        /*
         * Parent constructor
         */
        super(coordinates, settings, credentials, jobs, variables);

        /*
         * Set ENTER_CHARACTER
         */
        this.ENTER_CHARACTER = "\r\n";
        this.controlSeqences.put("%%SEQ(ENTER)%%", this.ENTER_CHARACTER);
    }

    /**
     * @noinspection Duplicates
     * {@inheritDoc}
     */
    @Override
    protected Boolean setCredentials()
    {

        this.telnetLogin          = this.credentials.get("telnet_login") + "+tc1000h200w";
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
         * Validate telnet ports
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

                String currentExpect   = splitAuthSequence[i].trim();
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

}
