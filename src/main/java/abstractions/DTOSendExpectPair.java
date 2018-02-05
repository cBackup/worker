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
 * DTO class for store Expect4j send-expect pair values
 * Need for telnet-ssh commands
 */
public class DTOSendExpectPair {

    private final String expect;
    private String send;
    private final String tableField;
    private final Integer timeout;
    private final String variable;
    private final Boolean replaceExpect;

    /**
     * Constructor
     *
     * @param expect        String  - expected Expect4j string
     * @param send          String  - answer
     * @param tableField    String  - database table field
     * @param timeout       Integer - request timeout
     * @param variable      String  - variable for response saving
     * @param replaceExpect Boolean - replace real prompt from output?
     */
    public DTOSendExpectPair(String expect, String send, String tableField, Integer timeout, String variable, Boolean replaceExpect) {
        this.expect     = expect;
        this.send       = send;
        this.tableField = tableField;
        this.timeout    = timeout;
        this.variable   = variable;
        this.replaceExpect = replaceExpect;
    }

    public void setSend(String send) { this.send = send; }

    public String getExpect() {
        return this.expect;
    }

    public String getSend() {
        return this.send;
    }

    public String getTableField() { return this.tableField; }

    public Integer getTimeout() { return this.timeout; }

    public String getVariable() { return this.variable; }

    public Boolean getReplaceExpect() { return this.replaceExpect; }
}
