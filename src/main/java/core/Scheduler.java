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

import abstractions.AbstractCoreUnit;
import abstractions.DTOShellResponse;
import api.ApiCaller;
import api.ApiRequest;
import api.ApiRequestMethods;
import api.ApiResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;
import org.springframework.stereotype.Component;
import sshd.shell.springboot.autoconfiguration.SshdShellCommand;
import org.springframework.beans.factory.annotation.Value;
import javax.annotation.PostConstruct;


/**
 * Main daemon class
 * @noinspection unused
 */
@Component
@SshdShellCommand(value = "cbackup", description = "Type 'cbackup' for supported subcommands")
public class Scheduler extends AbstractCoreUnit {

    private List<String> scheduleIds = new ArrayList<>();
    private Gson gson = new Gson();

    /* Bind cbackup properties from application.properties */
    @Value("${cbackup.scheme}")
    private String scheme;
    @Value("${cbackup.site}")
    private String site;
    @Value("${cbackup.token}")
    private String token;


    /**
     * Init and start after construct
     */
    @PostConstruct
    private void PostConstruct() {
        this.start();
    }


    /**
     * Shell command 'cbackup start'
     *
     * @param arg String - command argument
     * @return String    - command result
     */
    @SshdShellCommand(value = "start", description = "Start scheduler. Usage: 'cbackup start [-json]'")
    public String shellCommandStart(String arg) {

        Boolean returnJson = false;
        DTOShellResponse response;

        if( arg != null && arg.contains("-json")) {
            returnJson = true;
            arg = arg.replace("-json", "");
        }

        if(arg != null && arg.trim().length() > 0) {
            response = new DTOShellResponse();
            response.message = "Command 'cbackup start' has no valid arguments. Use command 'cbackup start [-json]' instead.";
        }
        else {
            response = this.start();
        }

        return (returnJson)? this.gson.toJson(response) : response.message;
    }


    /**
     * Shell command 'cbackup restart'
     *
     * @param arg String - command argument
     * @return String    - command result
     */
    @SshdShellCommand(value = "restart", description = "Restart scheduler. Usage: 'cbackup restart [-json]'")
    public String shellCommandRestart(String arg) {

        Boolean returnJson = false;
        DTOShellResponse response;

        if( arg != null && arg.contains("-json")) {
            returnJson = true;
            arg = arg.replace("-json", "");
        }

        if(arg != null && arg.trim().length() > 0) {
            response = new DTOShellResponse();
            response.message = "Command 'cbackup restart' has no valid arguments. Use command 'cbackup restart [-json]' instead.";
        }
        else {
            response = this.restart();
        }

        return (returnJson)? this.gson.toJson(response) : response.message;
    }


    /**
     * Shell command 'cbackup stop'
     *
     * @param arg String - command argument
     * @return String    - command result
     */
    @SshdShellCommand(value = "stop", description = "Stop scheduler. Usage: 'cbackup stop [-json]'")
    public String shellCommandStop(String arg) {

        Boolean returnJson = false;
        DTOShellResponse response;

        if( arg != null && arg.contains("-json")) {
            returnJson = true;
            arg = arg.replace("-json", "");
        }

        if(arg != null && arg.trim().length() > 0) {
            response = new DTOShellResponse();
            response.message = "Command 'cbackup stop' has no valid arguments. Use command 'cbackup stop [-json]' instead.";
        }
        else {
            response = this.stop();
        }

        return (returnJson)? this.gson.toJson(response) : response.message;
    }


    /**
     * Shell command 'cbackup backup <NODE ID>'
     *
     * @param arg String - command argument
     * @return String    - command result
     */
    @SshdShellCommand(value = "backup", description = "Single node backup. Usage: 'cbackup backup <NODE ID> [-json]'")
    public String shellCommandRunNodeBackup(String arg) {

        Boolean returnJson = false;
        DTOShellResponse response;
        Integer nodeId;

        if( arg != null && arg.contains("-json")) {
            returnJson = true;
            arg = arg.replace("-json", "");
        }

        if(arg == null || arg.trim().length() == 0) {
            response = new DTOShellResponse();
            response.message = "Please enter node id. Example: 'cbackup backup <NODE ID> [-json]'";
        }
        else {
            try {
                nodeId = Integer.parseInt(arg.trim());
                response = this.runTaskOnNode(nodeId.toString(), "backup");
            } catch (NumberFormatException e) {
                response = new DTOShellResponse();
                response.message = "Please enter node id. Node id is a number";
                response.exception = e.getMessage();
            }
        }

        return (returnJson)? this.gson.toJson(response) : response.message;
    }

    /**
     * Shell command 'cbackup runtask <TASK NAME>'
     *
     * @param arg String - command argument
     * @return String    - command result
     */
    @SshdShellCommand(value = "runtask", description = "Run task by name. Usage: 'cbackup stop <TASK NAME> [-json]'")
    public String shellCommandRuntask(String arg) {

        Boolean returnJson = false;
        DTOShellResponse response;

        if( arg != null && arg.contains("-json")) {
            returnJson = true;
            arg = arg.replace("-json", "");
        }

        if(arg == null || arg.trim().length() == 0) {
            response = new DTOShellResponse();
            response.message = "Please enter task name. Example: 'cbackup status <TASK NAME> [-json]'";
        }
        else {
            response = this.runTask(arg.trim());
        }

        return (returnJson)? this.gson.toJson(response) : response.message;
    }


    /**
     * Shell command 'cbackup status'
     *
     * @param arg String - command argument
     * @return String    - command result
     */
    @SshdShellCommand(value = "status", description = "Get scheduler status. Usage: 'cbackup status [-json]'")
    public String shellCommandStatus(String arg) {

        Boolean returnJson = false;
        DTOShellResponse response;

        if( arg != null && arg.contains("-json")) {
            returnJson = true;
            arg = arg.replace("-json", "");
        }

        if(arg != null && arg.trim().length() > 0) {
            response = new DTOShellResponse();
            response.message = "Command 'cbackup status' has no valid arguments. Use command 'cbackup status [-json]' instead.";
        }
        else {
            response  = this.status();
        }

        return (returnJson)? this.gson.toJson(response) : response.message;
    }


    /**
     * Shell command 'cbackup version'
     *
     * @param arg String - command argument
     * @return String    - command result
     */
    @SshdShellCommand(value = "version", description = "Get scheduler version. Usage: 'cbackup version [-json]'")
    public String shellCommandVersion(String arg) {

        Boolean returnJson = false;
        DTOShellResponse response = new DTOShellResponse();

        if( arg != null && arg.contains("-json")) {
            returnJson = true;
            arg = arg.replace("-json", "");
        }

        if(arg != null && arg.trim().length() > 0) {
            response.message = "Command 'cbackup version' has no valid arguments. Use command 'cbackup version [-json]' instead.";
        }
        else {
            response.success = true;
            response.object  = Cbackup.packageVersion;
            response.message = Cbackup.packageVersion;
        }

        return (returnJson)? this.gson.toJson(response) : response.message;
    }


    /**
     * Start tasks and mailer scheduler
     *
     * @return DTOShellResponse
     */
    private DTOShellResponse start() {

        DTOShellResponse response = new DTOShellResponse();

        try {

            /* Stop executing if Scheduler is already started */
            if (CronSingleton.getInstance().isStarted()) {
                response.message = "Scheduler is already started";
                response.success = true;
                response.object  = false;
                return response;
            }

            /* Get settings from API */
            this.init();

            /* Create task schedules */
            this.createTaskSchedule();

            /* Create mail schedules */
            this.createMailSchedule();

            /* Start scheduler */
            try {
                CronSingleton.getInstance().startScheduler();
            }
            catch (Exception e) {
                this.logSystemMessage("ERROR", "SCHEDULER START", "Can't start cron service.");
                this.clearAndStopScheduler();
                response.message   = "Can't start cron service.";
                response.exception = e.getMessage();
                return response;
            }
            response.message = "Scheduler started";
            response.success = true;
            response.object  = true;
            return response;

        }
        catch (Exception e) {
            this.clearAndStopScheduler();
            response.message   = "Can't start cron service.";
            response.exception = e.getMessage();
            return response;
        }
    }


    /**
     * Restart scheduler
     *
     * @return DTOShellResponse
     */
    private DTOShellResponse restart() {

        DTOShellResponse response = new DTOShellResponse();

        try {

            /* Stop executing if Scheduler is not started */
            if (!CronSingleton.getInstance().isStarted()) {
                response.message = "Scheduler is not started yet. Please use command 'cbackup start' to start scheduler";
                response.success = true;
                response.object  = false;
                return response;
            }

            /* Stop Scheduler if started */
            if (CronSingleton.getInstance().isStarted()) {
                this.stop();
                Thread.sleep(2000);
            }

            this.start();

            response.message = "Scheduler restarted";
            response.success = true;
            response.object  = true;
            return response;

        }
        catch (Exception e) {
            this.logSystemMessage("ERROR", "SCHEDULER RESTART", "Can't restart cron service.");
            response.message   = "Can't restart cron service.";
            response.exception = e.getMessage();
            return response;
        }

    }


    /**
     * Stop scheduler
     *
     * @return DTOShellResponse
     */
    private DTOShellResponse stop() {

        DTOShellResponse response = new DTOShellResponse();

        try {

            /* Check if Scheduler is already stopped */
            if (!CronSingleton.getInstance().isStarted()) {
                response.message = "Scheduler is already stopped";
                response.success = true;
                response.object  = false;
                return response;
            }

            /* Clear all schedules */
            this.clearAndStopScheduler();

            response.message = "Scheduler stopped";
            response.success = true;
            response.object  = true;
            return response;

        }
        catch (Exception e) {
            this.logSystemMessage("ERROR", "SCHEDULER STOP", "Can't stop cron service");
            response.message   = "Can't stop cron service";
            response.exception = e.getMessage();
            return response;
        }
    }


    /**
     * Run single node task
     * (only for node tasks)
     *
     * @param nodeId - node id
     * @return DTOShellResponse
     * @noinspection Duplicates
     */
    private DTOShellResponse runTaskOnNode(String nodeId, String taskName) {

        DTOShellResponse response = new DTOShellResponse();
        ArrayList<Map<String, String>> task;

        try {

            /* Stop executing if Scheduler is not started */
            if (!CronSingleton.getInstance().isStarted()) {
                response.message = "Scheduler is not started. Please first start scheduler, then launch node backup";
                response.success = true;
                response.object  = false;
                return response;
            }

            /*
             * Getting tasks backup info
             * Data example:
             * [[
             * "scheduleId"   => null,
             * "taskName"     => ..,
             * "put"          => ..,
             * "table"        => ..,
             * ]]
             */
            Map<String, String> params = new HashMap<>();
            params.put("task_name", taskName);

            ApiRequest taskBackupRequest = new ApiRequest(this.coordinates)
                    .setRequestMethod(ApiRequestMethods.GET)
                    .setApiMethod("v1/core/get-task")
                    .setParams(params);

            ApiResponse taskBackupResponse = ApiCaller.request(taskBackupRequest);

            if(!taskBackupResponse.success) {
                /*
                 * Log record
                 * Cannot get task
                 */
                this.logSystemBadResponse("ERROR", "SCHEDULER RUN TASK", "Can't get task from API.", taskBackupResponse);
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: can't get task from API";
                return response;
            }

            String taskJson = taskBackupResponse.response;

            Type tasksType = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();

            try {
                task = this.gson.fromJson(taskJson, tasksType);
            }
            catch(Exception e) {
                this.logSystemException("ERROR", "SCHEDULER RUN TASK", "Can't parse task list from json.", e);
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: can't parse task data from json";
                response.exception = e.getMessage();
                return response;
            }

            /* Check if API returned task */
            if (task.isEmpty()) {
                this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't find task with name backup");
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: can't find task with name backup";
                return response;
            }

            /*
             * Task data validation
             */
            Map<String, String> currentTask = task.get(0);

            String curTaskName = currentTask.get("taskName");
            String curTaskType = currentTask.get("taskType");
            String curPut      = currentTask.get("put");
            String curTable    = currentTask.get("table");

            if(curTaskName == null || curTaskName.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add single node task. Task name is empty.");
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: task name is empty";
                return response;
            }
            if(curTaskType == null || curTaskType.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add single node task. Task type is empty.");
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: task type is empty";
                return response;
            }
            if(!curTaskType.equals("node_task")) {
                this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add single node task. Wrong task type.");
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: task type is empty";
                return response;
            }
            if((curPut != null && curPut.equals("db")) && (curTable ==null || curTable.length() == 0)) {
                this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add single node task. Task db table is empty.");
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: db table is empty";
                return response;
            }

            Map<String, String> currentCoordinates = new HashMap<>();
            currentCoordinates.put("runOnNode", nodeId);
            currentCoordinates.putAll(currentTask);
            currentCoordinates.putAll(this.coordinates);

            CronTask taskObject = new CronTask(this.settings, currentCoordinates);

            try {
                CronSingleton.getInstance().getScheduler().launch(taskObject);
            }
            catch (Exception e) {
                this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add task. " + e.getMessage());
                response.message   = "Node " + nodeId + " task " + taskName + " has not been started: exception upon task launch.";
                response.exception = e.getMessage();
                return response;
            }

            response.message = "Node " + nodeId + " task " + taskName + " started";
            response.success = true;
            response.object  = true;
            return response;

        }
        catch (Exception e) {
            response.message   = "Node " + nodeId + " task " + taskName + " has not been started: exception upon task launch";
            response.exception = e.getMessage();
            return response;
        }

    }


    /**
     * Run specific task by task name
     *
     * @param  taskName - task name which will be executed
     * @return DTOShellResponse
     * @noinspection Duplicates
     */
    private DTOShellResponse runTask(String taskName) {

        DTOShellResponse response = new DTOShellResponse();
        ArrayList<Map<String, String>> task;

        try {

            /* Stop executing if Scheduler is not started */
            if (!CronSingleton.getInstance().isStarted()) {
                response.message = "Scheduler is not started. Please first start scheduler, then launch task";
                response.success = true;
                response.object  = false;
                return response;
            }

            /*
             * Getting tasks
             * Data example:
             * [[
             * "scheduleId"   => null,
             * "taskName"     => ..,
             * "put"          => ..,
             * "table"        => ..,
             * ]]
             */
            Map<String, String> params = new HashMap<>();
            params.put("task_name", taskName);

            ApiRequest taskRequest = new ApiRequest(this.coordinates)
                    .setRequestMethod(ApiRequestMethods.GET)
                    .setApiMethod("v1/core/get-task")
                    .setParams(params);

            ApiResponse taskResponse = ApiCaller.request(taskRequest);

            if(!taskResponse.success) {
                /*
                 * Log record
                 * Cannot get task
                 */
                this.logSystemBadResponse("ERROR", "SCHEDULER RUN TASK", "Can't get task from API.", taskResponse);
                response.message   = "Task has not been started: can't get task from API";
                return response;
            }

            String taskJson = taskResponse.response;

            Type tasksType = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();

            try {
                task = this.gson.fromJson(taskJson, tasksType);
            }
            catch(Exception e) {
                this.logSystemException("ERROR", "SCHEDULER RUN TASK", "Can't parse task list from json.", e);
                response.message   = "Task has not been started: can't parse task data from json";
                response.exception = e.getMessage();
                return response;
            }

            /* Check if API returned task */
            if (task.isEmpty()) {
                this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't find task with name " + taskName);
                response.message   = "Task has not been started: can't find task with name " + taskName;
                return response;
            }

            // TODO: rework. we don't need loop here
            for(Map<String, String> currentTask : task) {

                String curTaskName = currentTask.get("taskName");
                String curTaskType = currentTask.get("taskType");
                String curPut      = currentTask.get("put");
                String curTable    = currentTask.get("table");

                /*
                 * Task data validation
                 */
                if(curTaskName == null || curTaskName.length() == 0) {
                    this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add task. Task name is empty.");
                    response.message   = "Task has not been started: task name is empty";
                    return response;
                }
                if(curTaskType == null || curTaskType.length() == 0) {
                    this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add task. Task type is empty.");
                    response.message   = "Task has not been started: task type is empty";
                    return response;
                }
                if((curPut != null && curPut.equals("db")) && (curTable ==null || curTable.length() == 0)) {
                    this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add task. Task db table is empty.");
                    response.message   = "Task has not been started: db table is empty";
                    return response;
                }

                Map<String, String> currentCoordinates = new HashMap<>();
                currentCoordinates.putAll(currentTask);
                currentCoordinates.putAll(this.coordinates);

                CronTask taskObject = new CronTask(this.settings, currentCoordinates);

                try {
                    CronSingleton.getInstance().getScheduler().launch(taskObject);
                }
                catch (Exception e) {
                    this.logSystemMessage("ERROR", "SCHEDULER RUN TASK", "Can't add task. " + e.getMessage());
                    response.message   = "Task has not been started: exception upon task launch.";
                    response.exception = e.getMessage();
                    return response;
                }

            }

            response.message = "Task " + taskName +" started";
            response.success = true;
            response.object  = true;
            return response;

        }
        catch (Exception e) {
            response.message   = "Task has not been started: exception upon task launch";
            response.exception = e.getMessage();
            return response;
        }

    }


    /**
     * Show scheduler status
     *
     * @return DTOShellResponse
     */
    private DTOShellResponse status() {

        DTOShellResponse response = new DTOShellResponse();

        try {
            response.message = (CronSingleton.getInstance().isStarted()) ?  "Scheduler is running" : "Scheduler is not running";
            response.object  = CronSingleton.getInstance().isStarted();
            response.success = true;
            return response;
        }
        catch (Exception e) {
            this.logSystemMessage("ERROR", "SCHEDULER STATUS", "Can't get scheduler status.");
            response.message   = "Can't get scheduler status";
            response.exception = e.getMessage();
            return response;
        }

    }


    /**
     * Get setting from API
     *
     * @throws Exception if a error occurs.
     */
    private void init() throws Exception {

        /*
         * Getting API address and token
         */
        Properties config = new Properties();

        try {

            this.coordinates.put("scheme", this.scheme);
            this.coordinates.put("site", this.site);
            this.coordinates.put("token", this.token);

            /*
             * API address-token verification
             */
            if (this.coordinates.get("scheme") == null || this.coordinates.get("scheme").length() == 0) {
                throw new Exception("Can't get API protocol scheme (http/https) from config file application.properties.");
            }
            if (!(this.coordinates.get("scheme").equals("http")) && !(this.coordinates.get("scheme").equals("https"))) {
                throw new Exception("Unknown API protocol scheme.");
            }
            if (this.coordinates.get("site") == null || this.coordinates.get("site").length() == 0) {
                throw new Exception("Can't read API address from config file application.properties.");
            }
            if (this.coordinates.get("token") == null || this.coordinates.get("token").length() == 0) {
                throw new Exception("Can't get API auth token from config file application.properties.");
            }

            /*
             * Getting system settings:
             * - dataPath
             * - threadCount
             * - snmpTimeout
             * - snmpRetries
             * - telnetTimeout
             * - telnetBeforeSendDelay
             * - sshTimeout
             * - sshBeforeSendDelay
             * - systemLogLevel
             */
            ApiRequest settingsRequest = new ApiRequest(this.coordinates)
                    .setRequestMethod(ApiRequestMethods.GET)
                    .setApiMethod("v1/core/get-config");

            ApiResponse settingsResponse = ApiCaller.request(settingsRequest);

            if (!settingsResponse.success) {
                /*
                 * Log record
                 * Cannot get settings
                 */
                this.logSystemBadResponse("ERROR", "SCHEDULER INIT", "Can't get settings from API.", settingsResponse);
                throw new Exception("Can't get settings from API.");
            }

            String settingsJson = settingsResponse.response;

            Type settingsType = new TypeToken<HashMap<String, String>>() {}.getType();

            try {
                this.settings = this.gson.fromJson(settingsJson, settingsType);
            } catch (Exception e) {
                this.logSystemException("ERROR", "SCHEDULER INIT", "Can't parse settings from json.", e);
                throw new Exception("Can't parse settings from json.", e);
            }

            /*
             * Settings verification
             */
            if (this.settings.get("dataPath") == null || this.settings.get("dataPath").length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER INIT", "Can't get file save path from API.");
                throw new Exception("Can't get file save path from API.");
            }
        }
        catch (Exception e) {
            throw new Exception(e.getMessage());
        }

    }


    /**
     * Add tasks to schedule
     *
     * @throws Exception if a error occurs.
     */
    private void createTaskSchedule() throws Exception {

        ArrayList<Map<String, String>> tasks;

        /*
         * Getting tasks
         * Data example:
         * [[
         * "scheduleId"   => ..,
         * "taskName"     => ..,
         * "scheduleCron" => ..,
         * "put"          => ..,
         * "table"        => ..,
         * ],..]
         */
        ApiRequest tasksRequest = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/get-tasks");

        ApiResponse tasksResponse = ApiCaller.request(tasksRequest);

        if(!tasksResponse.success) {
                /*
                 * Log record
                 * Cannot get tasks
                 */
            this.logSystemBadResponse("ERROR", "SCHEDULER TASK INIT", "Can't get task list from API.", tasksResponse);
            throw new Exception("Can't get task list from API.");
        }

        String tasksJson = tasksResponse.response;

        Type tasksType = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();

        try {
            tasks = this.gson.fromJson(tasksJson, tasksType);
        }
        catch(Exception e) {
            this.logSystemException("ERROR", "SCHEDULER TASK INIT", "Can't parse task list from json.", e);
            throw new Exception("Can't parse task list from json.", e);
        }

        /*
         * Task list verification
         * Creating this.coordinates for every task
         * Adding tasks to scheduler
         *
         * this.coordinates keys:
         * - scheduleId
         * - taskName
         * - scheduleCron
         * - nodeId
         * - nodeIp
         * - nodeVendor
         * - nodeModel
         * - workerId
         * - put
         * - table
         * - scheme
         * - site
         * - token
         */
        for(Map<String, String> currentTask : tasks) {

            String curTaskName     = currentTask.get("taskName");
            String curScheduleId   = currentTask.get("scheduleId");
            String curScheduleCron = currentTask.get("scheduleCron");
            String curTaskType     = currentTask.get("taskType");
            String curPut          = currentTask.get("put");
            String curTable        = currentTask.get("table");

            /*
             * Task data validation
             */
            if(curTaskName == null || curTaskName.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER TASK INIT", "Can't add task. Task name is empty.");
                continue;
            }
            if(curScheduleId == null || curScheduleId.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER TASK INIT", "Can't add task. Schedule ID is empty.");
                continue;
            }
            if(curScheduleCron == null || curScheduleCron.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER TASK INIT", "Can't add task. Cron string is empty.");
                continue;
            }
            if(curTaskType == null || curTaskType.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER TASK INIT", "Can't add task. Task type is empty.");
                continue;
            }
            if((curPut != null && curPut.equals("db")) && (curTable ==null || curTable.length() == 0)) {
                this.logSystemMessage("ERROR", "SCHEDULER TASK INIT", "Can't add task. Task db table is empty.");
                continue;
            }

            /*
             * Creating coordinates for every task
             */
            Map<String, String> currentCoordinates = new HashMap<>();
            currentCoordinates.putAll(currentTask);
            currentCoordinates.putAll(this.coordinates);

            CronTask taskObject = new CronTask(this.settings, currentCoordinates);

            try {
                String taskScheduleId = CronSingleton.getInstance().scheduleJob(curScheduleCron, taskObject);
                this.scheduleIds.add(taskScheduleId);
            }
            catch (Exception e) {
                this.logSystemMessage("ERROR", "SCHEDULER TASK INIT", "Can't add task " + curTaskName + ": malformed cron string.");
            }

        }

    }


    /**
     * Add mailer tasks to schedule
     *
     * @throws Exception if a error occurs.
     */
    private void createMailSchedule() throws Exception {

        ArrayList<Map<String, String>> events;

        /*
         * Getting mail tasks
         * Data example:
         * [[
         * "scheduleId"   => ..,
         * "eventName"    => ..,
         * "scheduleCron" => ..,
         * ],..]
         */
        ApiRequest eventsRequest = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/get-mailer-events");

        ApiResponse eventsResponse = ApiCaller.request(eventsRequest);

        if(!eventsResponse.success) {
            /*
             * Log record
             * Cannot get mail tasks
             */
            this.logSystemBadResponse("ERROR", "SCHEDULER MAILER INIT", "Can't get events list from API.", eventsResponse);
            throw new Exception("Can't get events list from API.");
        }

        String eventsJson = eventsResponse.response;

        Type eventsType = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();

        try {
            events = this.gson.fromJson(eventsJson, eventsType);
        }
        catch(Exception e) {
            this.logSystemException("ERROR", "SCHEDULER MAILER INIT", "Can't parse events list from json.", e);
            throw new Exception("Can't parse events list from json.", e);
        }

        for(Map<String, String> currentEvent : events) {

            String curEventName         = currentEvent.get("eventName");
            String curEventScheduleId   = currentEvent.get("scheduleId");
            String curEventScheduleCron = currentEvent.get("scheduleCron");

            if(curEventName == null || curEventName.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER MAILER INIT", "Can't add event. Event name is empty.");
                continue;
            }
            if(curEventScheduleId == null || curEventScheduleId.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER MAILER INIT", "Can't add event. Schedule ID is empty.");
                continue;
            }
            if(curEventScheduleCron == null || curEventScheduleCron.length() == 0) {
                this.logSystemMessage("ERROR", "SCHEDULER MAILER INIT", "Can't add event. Cron string is empty.");
                continue;
            }

            /*
             * Creating coordinates for every event
             */
            Map<String, String> currentEventCoordinates = new HashMap<>();
            currentEventCoordinates.putAll(currentEvent);
            currentEventCoordinates.putAll(this.coordinates);

            CronMailer eventObject = new CronMailer(this.settings, currentEventCoordinates);

            try {
                String mailScheduleId = CronSingleton.getInstance().scheduleJob(curEventScheduleCron, eventObject);
                this.scheduleIds.add(mailScheduleId);
            }
            catch (Exception e) {
                this.logSystemMessage("ERROR", "SCHEDULER MAILER INIT", "Can't add event " + curEventName + ": malformed cron string.");
            }

        }

    }


    /**
     * Clear all schedules, stop scheduler
     */
    private void clearAndStopScheduler() {

        if (CronSingleton.getInstance().isStarted()) {
            CronSingleton.getInstance().stopScheduler();
        }

        /* Deschedule all tasks */
        for (String ids : this.scheduleIds){
            CronSingleton.getInstance().getScheduler().deschedule(ids);
        }

        /* Clear scheduleIds ArrayList */
        this.scheduleIds.clear();
    }

}
