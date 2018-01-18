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
 * DTO class for store Expect4j expect-send pair values
 * Need for telnet-ssh auth
 */
public class DTOExpectSendPair {

    private final String expect;
    private final String send;

    /**
     * Constructor
     *
     * @param expect String - expected Expect4j string
     * @param send   String - answer
     */
    public DTOExpectSendPair(String expect, String send) {
        this.expect  = expect;
        this.send    = send;
    }

    /**
     * @return String
     */
    public String getExpect() {
        return this.expect;
    }

    /**
     * @return String
     */
    public String getSend() {
        return this.send;
    }

}
