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

import java.util.Map;
import java.util.TreeMap;


/**
 * Data Object for sending worker result to API
 * @noinspection WeakerAccess
 */
public class DTOWorkerResult
{
    public String put;
    public String table;
    public String dataPath;
    public String taskName;
    public String nodeId;
    public String hash;
    public Map<String, String> data = new TreeMap<>();
}
