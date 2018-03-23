package telnet;/*
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

import abstractions.DTOSendExpectPair;
import abstractions.DTOVariableConvertResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/*
 * Custom telnet init
 */
import expect4j.Expect4j;
import org.apache.commons.net.io.FromNetASCIIInputStream;
import org.apache.commons.net.io.ToNetASCIIOutputStream;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;

/**
 * Vendor Nortel general telnet class
 * @noinspection unused
 */
public class _Nortel_Telnet extends GeneralTelnet  {

    /**
     * Constructor
     *
     * @param coordinates   - schedule, task, node, etc..
     * @param settings      - app settings
     * @param jobs          - sorted jobs
     * @param credentials   - credentials
     * @param variables     - variable list
     */
    public _Nortel_Telnet(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables)
    {
        /*
         * Parent constructor
         */
        super(coordinates, settings, credentials, jobs, variables);

        /*
         * Set ENTER_CHARACTER
         */
        this.ENTER_CHARACTER = "\r";
        this.controlSeqences.put("%%SEQ(ENTER)%%", this.ENTER_CHARACTER);
    }


    /**
     * Expect init
     * Send credentials
     * Get device prompt
     * Send commands
     *
     * @return Boolean
     * @noinspection Duplicates
     */
    @Override
    protected Boolean performJobs()
    {

        /*
         * Object Expect4j init
         */
        try {

            final TelnetClient client = new TelnetClient("VT100");

            TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, true);
            EchoOptionHandler echoopt = new EchoOptionHandler(false, false, false, true);
            SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(false, false, false, false);

            client.addOptionHandler(ttopt);
            client.addOptionHandler(echoopt);
            client.addOptionHandler(gaopt);

            client.connect(this.coordinates.get("nodeIp"), this.telnetPort);
            InputStream is = new FromNetASCIIInputStream(client.getInputStream());
            OutputStream os = new ToNetASCIIOutputStream(client.getOutputStream());


            this.expect  = new Expect4j(is, os) {
                public void close() {
                    super.close();
                    try {
                        client.disconnect();
                    } catch (IOException var2) {

                    }
                }
            };
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

            /*
             * Set prompt to expect(custom or not)
             */
            String customPrompt  = jobInfo.get("cli_custom_prompt");
            String currentPrompt;
            Boolean replaceExpect = true;
            if (customPrompt == null || customPrompt.length()==0) {
                currentPrompt = this.telnetEscapedRealPrompt;
            }
            else {
                currentPrompt = customPrompt;
                replaceExpect = false;
            }

            DTOSendExpectPair currentPair = new DTOSendExpectPair(currentPrompt, currentCommand, currentTableField, currentTimeoutInt, currentVariable, replaceExpect);
            this.telnetCommands.add(currentPair);

        }

        /*
         * Job commands exec
         */
        return this.telnetCommands();
    }

}
