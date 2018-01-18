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
package api;

/*
 * Google gson
 */
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

import java.util.HashMap;
import java.util.Map;


/**
 * Containing standard Api-call functions
 */
public class ApiLogHelper {

    /*
     * Instances not allowed
     */
    private ApiLogHelper() {}


    /**
     * Sending system log record to log_system table
     *
     * @param severity      - log message severity INFO|ERROR|WARNING..
     * @param action        - current java core action
     * @param message       - log message header (before coordinates)
     * @param coordinates   - schedule, task, node, etc..
     */
    public static void setSystemLog(String severity, String action, String message, Map<String, String> coordinates)
    {
        Map<String, String> dto = new HashMap<>();

        dto.put("severity", severity);
        dto.put("action", action);
        dto.put("message", ApiLogHelper.getMessage(message, coordinates));

        ApiLogHelper.sendSystemLog(dto, coordinates);

    }


    /**
     * Sending log record to log_scheduler table
     *
     * @param severity      - log message severity INFO|ERROR|WARNING..
     * @param action        - current java core action
     * @param message       - log message header (before coordinates)
     * @param coordinates   - schedule, task, node, etc..
     */
    public static void setLog(String severity, String action, String message, Map<String, String> coordinates)
    {
        Map<String, String> dto = new HashMap<>();

        dto.put("severity", severity);

        if(!(coordinates.get("scheduleId") ==null)) {
            dto.put("schedule_id", coordinates.get("scheduleId"));
        }
        if(!(coordinates.get("nodeId") == null)) {
            dto.put("node_id", coordinates.get("nodeId"));
        }
        dto.put("action", action);
        dto.put("message", ApiLogHelper.getMessage(message, coordinates));

        ApiLogHelper.sendLog(dto, coordinates);

    }


    /**
     * Sending mailer log record to log_scheduler table
     *
     * @param severity      - log message severity INFO|ERROR|WARNING..
     * @param action        - current java core action
     * @param message       - log message header (before coordinates)
     * @param coordinates   - schedule, event, etc..
     */
    public static void setMailerLog(String severity, String action, String message, Map<String, String> coordinates)
    {
        Map<String, String> dto = new HashMap<>();

        dto.put("severity", severity);
        dto.put("action", action);
        dto.put("message", ApiLogHelper.getMailerMessage(message, coordinates));

        ApiLogHelper.sendLog(dto, coordinates);
    }


    /**
     * Sending exception log record to log_system table
     *
     * @noinspection Convert2Diamond
     * @param severity      - log message severity INFO|ERROR|WARNING..
     * @param action        - current java core action
     * @param message       - log message header (before coordinates)
     * @param coordinates   - schedule, task, node, etc..
     * @param e             - Exception
     */
    public static void setSystemLogException(String severity, String action, String message, Map<String, String> coordinates, Exception e)
    {
        Map<String, String> dto = new HashMap<String, String>();

        dto.put("severity", severity);
        dto.put("action", action);
        dto.put("message", ApiLogHelper.getMessage(message, coordinates, e));

        ApiLogHelper.sendSystemLog(dto, coordinates);

    }


    /**
     * Sending exception log record to log_scheduler table
     *
     * @noinspection Convert2Diamond
     * @param severity      - log message severity INFO|ERROR|WARNING..
     * @param action        - current java core action
     * @param message       - log message header (before coordinates)
     * @param coordinates   - schedule, task, node, etc..
     * @param e             - Exception
     */
    public static void setLogException(String severity, String action, String message, Map<String, String> coordinates, Exception e)
    {
        Map<String, String> dto = new HashMap<String, String>();

        dto.put("severity", severity);

        if(!(coordinates.get("scheduleId") == null)) {
            dto.put("schedule_id", coordinates.get("scheduleId"));
        }
        if(!(coordinates.get("nodeId") == null)) {
            dto.put("node_id", coordinates.get("nodeId"));
        }
        dto.put("action", action);
        dto.put("message", ApiLogHelper.getMessage(message, coordinates, e));

        ApiLogHelper.sendLog(dto, coordinates);

    }


    /**
     * Sending bad api response log record to log_scheduler table
     *
     * @param severity      - log message severity INFO|ERROR|WARNING..
     * @param action        - current java core action
     * @param message       - log message header (before coordinates)
     * @param coordinates   - coordinates dto: schedule, task, node, etc..
     * @param response      - response dto: response codes, exceptions, stack traces etc...
     */
    public static void setSystemLogBadResponse(String severity, String action, String message, Map<String, String> coordinates, ApiResponse response)
    {

        Map<String, String> dto = new HashMap<>();

        dto.put("severity", severity);
        dto.put("action", action);
        dto.put("message", ApiLogHelper.getMessage(message, coordinates, response));

        ApiLogHelper.sendSystemLog(dto, coordinates);

    }


    /**
     * Sending bad api response log record to log_scheduler table
     *
     * @param severity      - log message severity INFO|ERROR|WARNING..
     * @param action        - current java core action
     * @param message       - log message header (before coordinates)
     * @param coordinates   - coordinates dto: schedule, task, node, etc..
     * @param response      - response dto: response codes, exceptions, stack traces etc...
     */
    public static void setLogBadResponse(String severity, String action, String message, Map<String, String> coordinates, ApiResponse response)
    {

        Map<String, String> dto = new HashMap<>();

        dto.put("severity", severity);

        if(!(coordinates.get("scheduleId") == null)) {
            dto.put("schedule_id", coordinates.get("scheduleId"));
        }
        if(!(coordinates.get("nodeId") == null)) {
            dto.put("node_id", coordinates.get("nodeId"));
        }
        dto.put("action", action);
        dto.put("message", ApiLogHelper.getMessage(message, coordinates, response));

        ApiLogHelper.sendLog(dto, coordinates);

    }


    /**
     * Log sending
     *
     * @param dto           - data object, containing db table log_scheduler fields
     * @param coordinates   - schedule, task, node, etc..
     */
    private static void sendLog(Map<String, String> dto, Map<String, String> coordinates)
    {
        Gson gson = new Gson();

        ApiRequest log = new ApiRequest(coordinates)
                .setRequestMethod(ApiRequestMethods.POST)
                .setApiMethod("v1/core/set-schedule-log")
                .setPostJson(gson.toJson(dto));

        ApiCaller.request(log);
    }


    /**
     * System log sending
     *
     * @param dto           - data object, containing db table log_scheduler fields
     * @param coordinates   - schedule, task, node, etc..
     */
    private static void sendSystemLog(Map<String, String> dto, Map<String, String> coordinates)
    {
        Gson gson = new Gson();

        ApiRequest log = new ApiRequest(coordinates)
                .setRequestMethod(ApiRequestMethods.POST)
                .setApiMethod("v1/core/set-system-log")
                .setPostJson(gson.toJson(dto));

        ApiCaller.request(log);
    }


    /**
     * Error message info concat
     *
     * @noinspection WeakerAccess
     * @param header       - custom error message header
     * @param coordinates  - process coordinates - schedule, task, node...
     * @return String      - returning concatinated message
     */
    public static String getMessage(String header, Map<String, String> coordinates)
    {
        String footer =
                "Schedule id: " + ((coordinates.get("scheduleId") == null) ? "NONE" : coordinates.get("scheduleId")) + "\n" +
                "Task name: "   + ((coordinates.get("taskName") == null) ? "NONE" : coordinates.get("taskName"))     + "\n" +
                "Node id: "     + ((coordinates.get("nodeId") == null) ? "NONE" : coordinates.get("nodeId"))         + "\n" +
                "Worker id: "   + ((coordinates.get("workerId") == null) ? "NONE" : coordinates.get("workerId"))     + "\n" ;
        return header + "\n" + footer;
    }


    /**
     * Error message info concat
     *
     * @noinspection WeakerAccess
     * @param header       - custom error message header
     * @param coordinates  - process coordinates - schedule, task, node...
     * @param e            - exception dto
     * @return String      - returning concatinated message
     */
    public static String getMessage(String header, Map<String, String> coordinates, Exception e)
    {

        StringBuilder sb = new StringBuilder();
        String exception = e.getClass().getSimpleName() + ". Message: " +e.getMessage();

        /*
         * Stack trace to string
         */
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }

        String footer =
                "Schedule id: " + ((coordinates.get("scheduleId") == null) ? "NONE" : coordinates.get("scheduleId")) + "\n" +
                "Task name: "   + ((coordinates.get("taskName") == null) ? "NONE" : coordinates.get("taskName"))     + "\n" +
                "Node id: "     + ((coordinates.get("nodeId") == null) ? "NONE" : coordinates.get("nodeId"))         + "\n" +
                "Worker id: "   + ((coordinates.get("workerId") == null) ? "NONE" : coordinates.get("workerId"))     + "\n" +
                "Exception: "   + exception                     + "\n" +
                "Stack trace: \n" + sb.toString()               + "\n" ;

        return header + "\n" + footer;
    }


    /**
     * Error message info concat
     *
     * @noinspection WeakerAccess
     * @param header       - custom error message header
     * @param coordinates  - process coordinates - schedule, task, node...
     * @param response       - ApiResponse instance. Containing http response code, response, stack trace, etc
     * @return String      - returning concatinated message
     */
    public static String getMessage(String header, Map<String, String> coordinates, ApiResponse response)
    {
        String footer =
                "Schedule id: "   + ((coordinates.get("scheduleId") == null) ? "NONE" : coordinates.get("scheduleId")) + "\n" +
                "Task name: "     + ((coordinates.get("taskName") == null) ? "NONE" : coordinates.get("taskName"))     + "\n" +
                "Node id: "       + ((coordinates.get("nodeId") == null) ? "NONE" : coordinates.get("nodeId"))         + "\n" +
                "Worker id: "     + ((coordinates.get("workerId") == null) ? "NONE" : coordinates.get("workerId"))     + "\n" +
                "API method: "    + response.apiMethod            + "\n" ;

        if(response.responseCode > 0) {
            footer += "Response code: " + response.responseCode + "\n";
        }

        if(!response.success) {
            /*
             * Trying to get message from bad API response
             */
            try {
                Gson gson = new Gson();
                Type responseType = new TypeToken<HashMap<String, String>>(){}.getType();
                Map<String, String> responseBody;
                responseBody = gson.fromJson(response.response, responseType);
                if(responseBody.get("message") != null) {
                    footer += "Response message: " + responseBody.get("message") + "\n";
                }
            }
            catch (Exception e) {
                // no action - yii response json no found
            }
        }

        if(response.exception.length() > 0) {
            footer += "Exception: " + response.exception + "\n";
        }

        if(response.stackTrace.length() > 0) {
            footer += "Stack trace: \n" + response.stackTrace + "\n";
        }

        return header + "\n" + footer;

    }


    /**
     * Error message info concat
     *
     * @noinspection WeakerAccess
     * @param header       - custom error message header
     * @param coordinates  - process coordinates - schedule, event, etc...
     * @return String      - returning concatinated message
     */
    public static String getMailerMessage(String header, Map<String, String> coordinates)
    {
        String footer =
                "Schedule id: " + ((coordinates.get("scheduleId") == null) ? "NONE" : coordinates.get("scheduleId")) + "\n" +
                "Event name: "  + ((coordinates.get("eventName") == null)  ? "NONE" : coordinates.get("eventName"))  + "\n" ;
        return header + "\n" + footer;
    }

}
