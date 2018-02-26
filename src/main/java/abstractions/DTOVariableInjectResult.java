package abstractions;/*
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

/**
 * Command inject result DTO
 */
public class DTOVariableInjectResult {

    /**
     * 0 - success
     * 1 - skip command
     * 2 - error
     */
    private Integer status = 0;

    /**
     * Result command
     */
    private String result;

    /**
     * Is control sequence injected?
     */
    private Boolean ctrlSeqInjected = false;

    public int getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getResult() {
        return this.result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Boolean isCtrlSeqInjected() {
        return this.ctrlSeqInjected;
    }

    public void setCtrlSeqInjected(Boolean isInjected) {
        this.ctrlSeqInjected = isInjected;
    }
}
