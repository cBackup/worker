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

/**
 * Data Object dynamic variable conversion result
 */
public class DTOVariableConvertResult {

    private String variableName;    // variable name
    private String variableValue;   // variable value
    private String result;          // variable conversion result

    /*
     * ----------------------------------------------------------------------------------------
     * Possible combinations
     * action: process                      - we can use result in commands
     * action: restrict + status: exception - we cannot use result, send logException, job is failed
     * action: restrict + status: success   - skip command, if variable required
     * ----------------------------------------------------------------------------------------
     */
    /**
     * Action for variable - process | restrict
     * Process - var can be used in commands
     */
    private String action;

    /**
     * Status of variable conversion success | exception
     */
    private String status;

    /**
     * Message (if required)...like exception message
     */
    private String message;


    public void   setVariableName(String variableName) { this.variableName = variableName; }
    public String getVariableName() {
        return this.variableName;
    }

    public void   setVariableValue(String variableValue) {
        if(variableValue == null ) variableValue = "";
        this.variableValue = variableValue;
    }

    /**
     * @noinspection unused
     */
    public String getVariableValue() { return this.variableValue; }

    public void   setResult(String result) {
        if(result == null ) result = "";
        this.result = result;
    }
    public String getResult() {
        return this.result;
    }

    public void   setAction(String action) { this.action = action; }
    public String getAction() {
        return this.action;
    }

    public void   setStatus(String status) { this.status = status; }
    public String getStatus() {
        return this.status;
    }

    public void   setMessage(String message) { this.message = message; }
    public String getMessage() {
        return this.message;
    }

}
