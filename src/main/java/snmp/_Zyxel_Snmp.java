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

import abstractions.DTOVariableConvertResult;
import java.util.Map;


/**
 * Vendor Zyxel general snmp class
 * @noinspection unused
 */
public class _Zyxel_Snmp extends GeneralSnmp {

    /**
     * Constructor
     *
     * @param coordinates   - schedule, task, node, etc..
     * @param settings      - app settings
     * @param credentials   - credentials
     * @param jobs          - sorted jobs
     * @param variables     - variable list
     */
    public _Zyxel_Snmp(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables)
    {
        /*
         * Parent constructor
         */
        super(coordinates, settings, credentials, jobs, variables);
    }

    /**
     * Transformation of variables, obtained as a result of jobs
     * You can convert variables' values, depending on tasks, table fields or variable names
     * Can be useful in such cases as: STP port numbers converting before usage in snmp query
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


        if(taskName.equals("stp")) {
            switch(tableField) {
                case "root_port":

                    Integer rootPort;
                    Integer calculatedRootPort;

                    try {
                        rootPort  = Integer.parseInt(variableValue);

                        if(rootPort >= 32768){
                            calculatedRootPort = rootPort - 32768;
                        }
                        else {
                            calculatedRootPort = rootPort;
                        }

                        if(calculatedRootPort < 0) {
                            result.setAction("restrict");
                            result.setStatus("exception");
                            result.setMessage("Wrong port number. Port number: " + calculatedRootPort.toString() + ".");
                        }
                        // No snmp requests with root port 0
                        if(calculatedRootPort == 0) {
                            result.setAction("restrict");
                            result.setResult("0");
                        }
                        else {
                            result.setResult(calculatedRootPort.toString());
                        }
                    }
                    catch(NumberFormatException e) {
                        result.setAction("restrict");
                        result.setStatus("exception");
                        result.setMessage("Can't parse port number to integer. Port number: " + variableValue + ".");
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
