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
package abstractions;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Abstract class for all protocols realizations
 */
public abstract class AbstractProtocol extends AbstractCoreUnit {

    // Protocol credentials
    protected Map<String, String> credentials                 = new HashMap<>();
    // Custom variables
    protected Map<String, DTOVariableConvertResult> variables = new HashMap<>();
    // Sorted jobs
    protected Map<String, Map<String, String>> jobs           = new TreeMap<>();
    // Class work result
    protected DTOProtocolResult result                        = new DTOProtocolResult();

    // Control sequences
    protected HashMap<String, String> controlSeqences = new HashMap<String, String>() {{
        put("%%SEQ(CTRLY)%%","\u0019");
        put("%%SEQ(CTRLC)%%","\u0003");
        put("%%SEQ(CTRLZ)%%","\u001A");
        put("%%SEQ(ESC)%%","\u001B");
        put("%%SEQ(SPACE)%%"," ");
        put("%%SEQ(ENTER)%%","\n");
    }};

    /**
     * Executing all commands(this.jobs). Returning DTO with results.
     *
     * @return ProtocolResultDTO - result of class execution
     */
    public abstract DTOProtocolResult execute();

    /**
     * Extracting/converting settings to class fields
     *
     * @return Boolean - extract settings success
     */
    protected abstract Boolean extractSettings();

    /**
     * Credentials set-validate
     *
     * @return Boolean - returns result of node credentials validation
     */
    protected abstract Boolean setCredentials();

    /**
     * Protocol util init
     * Send all data
     *
     * @return Boolean - execute jobs success
     */
    protected abstract Boolean performJobs();

    /**
     * Transformation of variables, obtained as a result of jobs
     * You can convert the values of variables, depending on tasks, table fields or variable names
     * Can be useful in such cases as: STP port numbers converting before usage in snmp query
     * Redefine this method for vendors or specific models if necessary
     *
     * @param variableName  - variable name
     * @param variableValue - hmm...variable value
     * @return DTOVariableConvertResult - variable object after converting
     */
    protected DTOVariableConvertResult convertVariable(String taskName, String tableField, String variableName, String variableValue) {

        DTOVariableConvertResult result = new DTOVariableConvertResult();
        result.setAction("process");
        result.setStatus("success");
        result.setVariableName(variableName);
        result.setVariableValue(variableValue);
        result.setResult(variableValue);
        return result;
    }

    /**
     * Inject variable to command
     *
     * @param command - command
     * @return StrDTOVariableInjectResulting - result object
     */
    protected DTOVariableInjectResult injectVariable(String command) {

        DTOVariableInjectResult result = new DTOVariableInjectResult();
        result.setResult(command);

        if(command.contains("%%")) {

            Boolean finishSearch = false;

            Pattern pattern = Pattern.compile("(%%SEQ\\(.*?\\)%%)");
            Matcher matcher = pattern.matcher(command);

            // Search for sequence variable
            if(matcher.find()){

                String allSequence = matcher.group(1);

                pattern = Pattern.compile("\\((.*?)\\)");
                matcher = pattern.matcher(allSequence);

                if(matcher.find() && matcher.group(1).length() == 1) {
                    // Send single character or number
                    result.setResult(matcher.group(1));
                    finishSearch = true;
                }
                else {
                    // Search for all sequence
                    for (Map.Entry<String, String> curSeqVar : this.controlSeqences.entrySet()) {
                        if(finishSearch) break;
                        if(curSeqVar.getKey().equals(allSequence)) {
                            result.setResult(curSeqVar.getValue());
                            finishSearch = true;
                        }
                    }
                }

                if(finishSearch) result.setCtrlSeqInjected(true);
            }

            // Searching variable in command
            if(!finishSearch) {
                for (Map.Entry<String, DTOVariableConvertResult> entry : this.variables.entrySet()) {

                    if(finishSearch) break;

                    if (command.contains(entry.getKey())) {
                        // Variable found
                        // If variable is converted, sending command with injected variable
                        if (entry.getValue().getAction().equals("process")) {
                            // check if empty
                            if (entry.getValue() != null && entry.getValue().getResult() != null && entry.getValue().getResult().length() > 0) {
                                result.setResult(command.replaceAll(entry.getKey(), entry.getValue().getResult()));
                            } else {
                                String SetVarFailedMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                        ": empty variable value returned. Command: " + command + ". Variable:" + entry.getValue().getVariableName() + ". Variable set is failed. Check your command.";
                                this.logMessage("ERROR", "NODE REQUEST", SetVarFailedMessage);
                                result.setStatus(2);
                                finishSearch = true;
                            }
                        } else {

                            finishSearch = true;

                            switch (entry.getValue().getStatus()) {
                                // If variable converted with error, sending exception log
                                case "exception":
                                    String variableConvertMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                            ": variable conversion error. Variable: " + entry.getValue().getVariableName() + ". Message: " + entry.getValue().getMessage();
                                    this.logMessage("ERROR", "NODE REQUEST", variableConvertMessage);
                                    result.setStatus(2);
                                    break;
                                // If variable converted successfully, but action is restrict, skip command and use empty string as command result
                                case "success":
                                    result.setStatus(1);
                                    break;
                                default:
                                    String variableConvertUnknownStatus = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") +
                                            ": unknown status of variable conversion. Variable: " + entry.getValue().getVariableName() + ". Status: " + entry.getValue().getStatus();
                                    this.logMessage("ERROR", "NODE REQUEST", variableConvertUnknownStatus);
                                    result.setStatus(2);
                                    break;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}
