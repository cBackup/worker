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


}
