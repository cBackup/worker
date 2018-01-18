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
package core;

import it.sauronsoftware.cron4j.TaskExecutionContext;

import java.util.HashMap;
import java.util.Map;


/*
 * Task class for Cron4j
 */
public class CronTask extends it.sauronsoftware.cron4j.Task  {

    private Map<String, String> settings    = new HashMap<>();
    private Map<String, String> coordinates = new HashMap<>();

    /**
     * Constructor
     *
     * @param coordinates  - schedule, task, node, etc..
     * @param settings     - app settings
     */
    CronTask(Map<String, String> settings, Map<String, String> coordinates) {
        this.settings    = settings;
        this.coordinates = coordinates;
    }

    /**
     * Execution
     */
    @Override
    public void execute(TaskExecutionContext context) throws RuntimeException
    {
        Task currentTask = new Task(this.coordinates, this.settings);
        currentTask.run();
    }
}
