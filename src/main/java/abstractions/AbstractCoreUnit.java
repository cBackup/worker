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

import api.ApiLogHelper;
import api.ApiResponse;

import java.util.HashMap;
import java.util.Map;


/**
 * Abstract class for Scheduler and Tasks
 * @noinspection WeakerAccess
 */
public abstract class AbstractCoreUnit {

    protected Map<String, String> coordinates = new HashMap<>();
    protected Map<String, String> settings    = new HashMap<>();
    protected static final Map<String, Integer> logLevels = new HashMap<>();
    static {
        logLevels.put("DEBUG", 0);
        logLevels.put("INFO", 1);
        logLevels.put("NOTICE", 2);
        logLevels.put("WARNING", 3);
        logLevels.put("ALERT", 4);
        logLevels.put("CRITICAL", 5);
        logLevels.put("ERROR", 6);
        logLevels.put("EMERG", 7);
    }

    /**
     * Is message level sufficient for logging
     * Message with unknown log level ALWAYS going to log
     *
     * @param messageSeverity - log message severity INFO|ERROR|WARNING..
     * @return boolean        - is message level sufficient for logging
     */
    protected final boolean logLevelIsSufficient(String messageSeverity) {

        Integer severityLvl    = 1; // Default severity lvl
        Integer msgSeverityLvl = 8;
        String severity = this.settings.get("systemLogLevel");

        if(severity != null && AbstractCoreUnit.logLevels.get(severity) != null) {
            severityLvl = AbstractCoreUnit.logLevels.get(severity);
        }

        if(AbstractCoreUnit.logLevels.get(messageSeverity) != null) {
            msgSeverityLvl = AbstractCoreUnit.logLevels.get(messageSeverity);
        }

        return msgSeverityLvl >= severityLvl;
    }


    /**
     * Log action - table log_system
     *
     * @noinspection SameParameterValue
     * @param severity - log message severity INFO|ERROR|WARNING..
     * @param action   - current java core action
     * @param message  - log message
     */
    protected final void logSystemMessage(String severity, String action, String message)
    {
        if(this.logLevelIsSufficient(severity)) ApiLogHelper.setSystemLog(severity, action, message, this.coordinates);
    }


    /**
     * Log action
     *
     * @param severity - log message severity INFO|ERROR|WARNING..
     * @param action   - current java core action
     * @param message  - log message
     */
    protected final void logMessage(String severity, String action, String message)
    {
        if(this.logLevelIsSufficient(severity)) ApiLogHelper.setLog(severity, action, message, this.coordinates);
    }


    /**
     * Log mailer actions
     *
     * @param severity - log message severity INFO|ERROR|WARNING..
     * @param action   - current java core action
     * @param message  - log message
     */
    protected final void logMailerMessage(String severity, String action, String message)
    {
        if(this.logLevelIsSufficient(severity)) ApiLogHelper.setMailerLog(severity, action, message, this.coordinates);
    }


    /**
     * Log exception to log_system table
     *
     * @noinspection SameParameterValue
     * @param severity  - log message severity INFO|ERROR|WARNING..
     * @param action    - current java core action
     * @param message   - log message
     * @param e         - Exception
     */
    protected final void logSystemException(String severity, String action, String message, Exception e)
    {
        if(this.logLevelIsSufficient(severity)) ApiLogHelper.setSystemLogException(severity, action, message, this.coordinates, e);
    }


    /**
     * Log exception
     *
     * @param severity  - log message severity INFO|ERROR|WARNING..
     * @param action    - current java core action
     * @param message   - log message
     * @param e         - Exception
     */
    protected final void logException(String severity, String action, String message, Exception e)
    {
        if(this.logLevelIsSufficient(severity)) ApiLogHelper.setLogException(severity, action, message, this.coordinates, e);
    }


    /**
     * Log bad api response
     *
     * @noinspection SameParameterValue
     * @param severity - log message severity INFO|ERROR|WARNING..
     * @param action   - current java core action
     * @param message  - log message
     * @param response - response dto: response codes, exceptions, stack traces etc...
     */
    protected final void logSystemBadResponse(String severity, String action, String message, ApiResponse response)
    {
        if(this.logLevelIsSufficient(severity)) ApiLogHelper.setSystemLogBadResponse(severity, action, message, this.coordinates, response);
    }


    /**
     * Log bad api response
     *
     * @param severity - log message severity INFO|ERROR|WARNING..
     * @param action   - current java core action
     * @param message  - log message
     * @param response - response dto: response codes, exceptions, stack traces etc...
     */
    protected final void logBadResponse(String severity, String action, String message, ApiResponse response)
    {
        if(this.logLevelIsSufficient(severity)) ApiLogHelper.setLogBadResponse(severity, action, message, this.coordinates, response);
    }

}
