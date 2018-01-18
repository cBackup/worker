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

import api.ApiRequest;
import api.ApiResponse;
import api.ApiCaller;
import api.ApiRequestMethods;
import abstractions.AbstractCoreUnit;
import abstractions.DTOVariableConvertResult;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Google gson
 */
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/*
 * Threads executor
 */
import java.lang.reflect.Type;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Future;

/*
 * Networks
 */
import org.apache.commons.net.util.SubnetUtils;


/**
 * Task class. Spawns threads pool of workers.
 */
@SuppressWarnings("FieldCanBeLocal")
public class Task extends AbstractCoreUnit implements Runnable
{

    private int threadCount = 10;

    // Worker's success-fail counters for output
    private int success = 0;
    private int failed  = 0;

    private Gson gson = new Gson();
    private Map<String, Map<String, String>> nodes;

    private Map<String, DTOVariableConvertResult> variables = new HashMap<>();


    /**
     * Constructor
     *
     * @param coordinates  - schedule, task, node, etc..
     * @param settings     - app settings
     */
    Task(Map<String, String> coordinates, Map<String, String> settings)
    {
        this.coordinates.putAll(coordinates);
        this.settings.putAll(settings);
    }


    @Override
    public void run() {

        /*
         * Task start log
         */
        this.logMessage("INFO", "TASK START", "Task " + this.coordinates.get("taskName") + " started.");

        /*
         * Settings verification
         */
        if (this.settings.get("systemLogLevel") == null || this.settings.get("systemLogLevel").length() == 0) {
            this.settings.put("systemLogLevel", "INFO");
            String logLevelNotSetMessage = "Task " + this.coordinates.get("taskName") + ": log level is not set. Using default log level: INFO.";
            this.logMessage("WARNING", "TASK EXECUTE", logLevelNotSetMessage);
        }

        /*
         * Set thread count
         */
        try {
            this.threadCount = Integer.parseInt(settings.get("threadCount"));
        } catch (NumberFormatException e) {
            this.logException("ERROR", "TASK INIT", "Task " + this.coordinates.get("taskName") + " can't read thread number from settings.", e);
        }

        /*
         * Detecting task type
         */
        switch (coordinates.get("taskType")) {
            case "system_task":
                this.runSystemTask();
                break;
            case "discovery":
                this.runDiscovery();
                break;
            case "node_task":
                this.runNodeTask();
                break;
            case "yii_console_task":
                this.runYiiConsoleTask();
                break;
            default:
                String unknownTaskMessage = "Task " + this.coordinates.get("taskName") + " failed. Unknown task type: ." + coordinates.get("taskType") + ".";
                this.logMessage("ERROR", "TASK EXECUTE", unknownTaskMessage);
        }

    }


    /*
     * ------------------------
     * Executing system tasks
     * ------------------------
     */
    private void runSystemTask(){

        String systemTaskApiMethod = coordinates.get("taskName").replace("_", "-");

        Boolean systemTaskSuccess = false;

        ApiRequest systemTaskRequest = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/" + systemTaskApiMethod);

        ApiResponse systemTaskResponse = ApiCaller.request(systemTaskRequest);

        if (!systemTaskResponse.success) {
            /*
             * Log record
             * Can't get system task result
             */
            this.logSystemBadResponse("ERROR", "TASK EXECUTE", "Can't get task result response.", systemTaskResponse);
        } else {

            String systemTaskJson = systemTaskResponse.response;

            Type settingsType = new TypeToken<Boolean>() {
            }.getType();

            try {
                systemTaskSuccess = gson.fromJson(systemTaskJson, settingsType);
            } catch (Exception e) {
                this.logSystemException("ERROR", "TASK EXECUTE", "Can't parse system task response to boolean.", e);
            }
        }

        if (systemTaskSuccess) {
            String finalMessage = "Task " + this.coordinates.get("taskName") + " has been finished successfully.";
            this.logMessage("INFO", "TASK FINISH", finalMessage);
        } else {
            String finalMessage = "Task " + this.coordinates.get("taskName") + " failed.";
            this.logMessage("ERROR", "TASK FINISH", finalMessage);
        }

    }


    /**
     * ----------------------
     * Executing discovery
     * ----------------------
     */
    private void runDiscovery(){

        HashMap<String, HashMap<String, String>> networks;
        List<String> exclusions;

        /*
         * Get networks
         */
        ApiRequest networksRequest = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/get-networks");

        ApiResponse networksResponse = ApiCaller.request(networksRequest);

        if (!networksResponse.success) {

            /*
             * Log record
             * Can't get node list
             */
            this.logBadResponse("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't get discovery network list.", networksResponse);
            return;
        }

        String networksJson = networksResponse.response;

        Type networksType = new TypeToken<HashMap<String, HashMap<String, String>>>() {
        }.getType();

        try {
            networks = gson.fromJson(networksJson, networksType);

        } catch (JsonSyntaxException e) {
            this.logException("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't parse discovery network list from json.", e);
            return;
        }


        /*
         * Get exclusions IP's
         */
        ApiRequest exclusionsRequest = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/get-exclusions");

        ApiResponse exclusionsResponse = ApiCaller.request(exclusionsRequest);

        if (!exclusionsResponse.success) {

            /*
             * Log record
             * Can't get node list
             */
            this.logBadResponse("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't get exclusions ip list.", exclusionsResponse);
            return;
        }

        String exclusionsJson = exclusionsResponse.response;

        Type exclusionsType = new TypeToken<ArrayList<String>>(){}.getType();

        try {
            exclusions = gson.fromJson(exclusionsJson, exclusionsType);
        } catch (JsonSyntaxException e) {
            this.logException("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't parse exclusions ip list from json.", e);
            return;
        }


        /*
         * Thread executor init
         */
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount); // number of threads

        /*
         * Futures for workers results return
         */
        List<Future<Boolean>> results = new ArrayList<>();

        // Adding discovery workers to executor
        // noinspection Java8MapForEach
        networks.entrySet().forEach(node -> {

            Boolean dataValid = true;

            String[] allIps = {};
            List<String> allExclusions = new ArrayList<>();

            String snmpRead  = node.getValue().get("snmp_read");
            String version   = node.getValue().get("snmp_version");
            String port      = node.getValue().get("port_snmp");
            String networkId = node.getValue().get("id");

            Integer snmpVer  = 1;
            Integer snmpPort = 161;

            if(snmpRead == null || snmpRead.length() == 0) {
                dataValid = false;
                String unknownTaskMessage = "Task " + this.coordinates.get("taskName") + ". Network " + node.getKey() +
                        ": empty SNMP read community.";
                this.logMessage("ERROR", "TASK EXECUTE", unknownTaskMessage);
            }
            if(version == null || version.length() == 0) {
                dataValid = false;
                String unknownTaskMessage = "Task " + this.coordinates.get("taskName") + ". Network " + node.getKey() +
                        ": empty SNMP version.";
                this.logMessage("ERROR", "TASK EXECUTE", unknownTaskMessage);
            }
            if(port == null || port.length() == 0) {
                dataValid = false;
                String unknownTaskMessage = "Task " + this.coordinates.get("taskName") + ". Network " + node.getKey() +
                        ": empty SNMP port.";
                this.logMessage("ERROR", "TASK EXECUTE", unknownTaskMessage);
            }

            if(dataValid) {
                /*
                 * Set SNMP version
                 */
                try {
                    snmpVer = Integer.parseInt(version);
                } catch (NumberFormatException e) {
                    dataValid = false;
                    String parseVersionMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse SNMP version of network";
                    this.logException("ERROR", "TASK EXECUTE", parseVersionMessage, e);
                }

                /*
                 * Set SNMP port
                 */
                try {
                    snmpPort = Integer.parseInt(port);
                } catch (NumberFormatException e) {
                    dataValid = false;
                    String parsePortMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse SNMP port to integer.";
                    this.logException("WARNING", "TASK EXECUTE", parsePortMessage, e);
                }
            }


            /*
             * Calculating all IPs of current subnet
             */
            if(dataValid) {
                try {
                    SubnetUtils subnet = new SubnetUtils(node.getKey());

                    /*
                     * If exclusions ip is in subnet range, add it to exclusuins list
                     */
                    for (String exclusionIp : exclusions) {
                        try {
                            if (subnet.getInfo().isInRange(exclusionIp)) {
                                allExclusions.add(exclusionIp);
                            }
                        } catch(IllegalArgumentException e){
                            dataValid = false;
                            String validateExclusionsMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": wrong exclusion ip " + exclusionIp;
                            this.logException("WARNING", "TASK EXECUTE", validateExclusionsMessage, e);
                        }
                    }

                    allIps = subnet.getInfo().getAllAddresses();
                } catch (Exception e) {
                    dataValid = false;
                    String extractIpsMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't extract IPs from subnet.";
                    this.logException("WARNING", "TASK EXECUTE", extractIpsMessage, e);
                }
            }

            if(dataValid) {
                for (String ip : allIps) {
                    if(!allExclusions.contains(ip)) {
                        Map<String, String> currentCoord = new HashMap<>();
                        currentCoord.putAll(this.coordinates);
                        currentCoord.put("nodeIp", ip);
                        results.add(executor.submit(new WorkerDiscovery(currentCoord, this.settings, networkId, snmpVer, snmpRead, snmpPort)));
                    }
                }
            }
        });

        for (Future<Boolean> result : results) {

            Boolean currentResult;

            try {
                currentResult = result.get();

                if (currentResult) {
                    this.success++;
                } else {
                    this.failed++;
                }

            } catch (Exception e) {
                this.logException("ERROR", "TASK GET WORKER RESPONSE", "Task " + this.coordinates.get("taskName") + " was interrupted while waiting for discovery worker result.", e);
                return;
            }
        }

        executor.shutdown();

        /*
         * Task finish log
         */
        String finalMessage = "Task " + this.coordinates.get("taskName") + " has been finished. " +
                " Success: " + this.success + ". Failed or offline: " + this.failed + ".";
        this.logMessage("INFO", "TASK FINISH", finalMessage);

    }


    /*
     * ---------------------
     * Executing node tasks
     * ---------------------
     */
    private void runNodeTask(){

        /* Single node id */
        String runOnNode = this.coordinates.get("runOnNode");

        /*
         * Get custom user variables
         */
        ApiRequest variablesRequest = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/get-variables");

        ApiResponse variablesResponse = ApiCaller.request(variablesRequest);

        if(!variablesResponse.success) {
            /*
             * Log record
             * Can't get variables
             */
            this.logSystemBadResponse("ERROR", "TASK GET CUSTOM VARIABLES", "Can't get task variables from API.", variablesResponse);
            return;
        }

        String variablesJson = variablesResponse.response;

        Type variablesType = new TypeToken<HashMap<String, String>>(){}.getType();
        Map<String, String> customVariables;

        try {
            customVariables = gson.fromJson(variablesJson, variablesType);

            /*
             * Setting hashMap of custom user variables
             */
            for (Map.Entry<String, String> curVar : customVariables.entrySet()) {
                DTOVariableConvertResult curVariableObject = new DTOVariableConvertResult();
                curVariableObject.setAction("process");
                curVariableObject.setStatus("success");
                curVariableObject.setVariableName(curVar.getKey());
                curVariableObject.setVariableValue(curVar.getValue());
                curVariableObject.setResult(curVar.getValue());
                this.variables.put(curVar.getKey(), curVariableObject);
            }

        }
        catch(Exception e) {
            this.logSystemException("ERROR", "TASK GET CUSTOM VARIABLES", "Can't parse variables list from json.", e);
            return;
        }

        /*
         * Add Date variable to variables hashMap
         */
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String date = dateFormat.format(new Date());

            DTOVariableConvertResult dateVariableObject = new DTOVariableConvertResult();
            dateVariableObject.setAction("process");
            dateVariableObject.setStatus("success");
            dateVariableObject.setVariableName("%%DATE%%");
            dateVariableObject.setVariableValue(date);
            dateVariableObject.setResult(date);
            this.variables.put("%%DATE%%", dateVariableObject);
        }
        catch(Exception e) {
            this.logSystemException("ERROR", "TASK GET CUSTOM VARIABLES", "Can't set date variable.", e);
            return;
        }

        /*
         * Run node task on nodes scope
         */
        if(runOnNode == null) {

            /*
             * Get nodes with workers by task
             */
            Map<String, String> params = new HashMap<>();
            params.put("schedule_id", this.coordinates.get("scheduleId"));
            params.put("task_name", this.coordinates.get("taskName"));

            ApiRequest request = new ApiRequest(this.coordinates)
                    .setRequestMethod(ApiRequestMethods.GET)
                    .setApiMethod("v1/core/get-nodes-workers-by-task")
                    .setParams(params);

            ApiResponse nodesResponse = ApiCaller.request(request);

            if (!nodesResponse.success) {
                /*
                 * Log record
                 * Can't get node list
                 */
                this.logBadResponse("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't get node list from API.", nodesResponse);
                return;
            }

            String nodesJson = nodesResponse.response;

            Type nodesType = new TypeToken<HashMap<String, HashMap<String, String>>>() {
            }.getType();

            try {

                this.nodes = gson.fromJson(nodesJson, nodesType);

            } catch (JsonSyntaxException e) {
                this.logException("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't parse nodes list from json.", e);
                return;
            }
        }
        else {
            /*  Run node task on single node (on demand) */

            /*
             * Get worker by node id
             */
            Map<String, String> params = new HashMap<>();
            params.put("node_id", runOnNode);
            params.put("task_name", this.coordinates.get("taskName"));

            ApiRequest nodeRequest = new ApiRequest(this.coordinates)
                    .setRequestMethod(ApiRequestMethods.GET)
                    .setApiMethod("v1/core/get-worker-by-node-id")
                    .setParams(params);

            ApiResponse nodeResponse = ApiCaller.request(nodeRequest);

            if (!nodeResponse.success) {
                /*
                 * Log record
                 * Can't get node list
                 */
                this.logBadResponse("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't get node list from API.", nodeResponse);
                return;
            }

            String nodeJson = nodeResponse.response;

            Type nodeType = new TypeToken<HashMap<String, HashMap<String, String>>>(){}.getType();

            try {

                this.nodes = gson.fromJson(nodeJson, nodeType);

            } catch (JsonSyntaxException e) {
                this.logException("ERROR", "TASK GET NODES", "Task " + this.coordinates.get("taskName") + " can't parse nodes list from json.", e);
                return;
            }
        }

        /*
         * Thread executor init
         */
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount); // number of threads

        /*
         * Futures for workers results return
         */
        List<Future<Boolean>> results = new ArrayList<>();

        // Add Workers to Executor
        // noinspection Java8MapForEach
        this.nodes.entrySet().forEach(node -> {
            Map<String, String> currentCoord = new HashMap<>();
            currentCoord.putAll(this.coordinates);
            currentCoord.put("nodeId", node.getKey());
            currentCoord.put("workerId", node.getValue().get("id"));
            currentCoord.put("nodeIp", node.getValue().get("ip"));

            currentCoord.put("nodeVendor", node.getValue().get("vendor"));
            currentCoord.put("nodeModel", node.getValue().get("model"));

            switch (node.getValue().get("get")) {
                case "snmp":
                    results.add(executor.submit(new WorkerSnmp(currentCoord, this.settings, this.variables)));
                    break;
                case "telnet":
                    results.add(executor.submit(new WorkerTelnet(currentCoord, this.settings, this.variables)));
                    break;
                case "ssh":
                    results.add(executor.submit(new WorkerSsh(currentCoord, this.settings, this.variables)));
                    break;
                default:
                    String unknownProtocol = "Task " + this.coordinates.get("taskName") + " has unknown protocol " + node.getValue().get("get") +
                            ". Node id: " + node.getKey();
                    this.logMessage("ERROR", "WORKER SPAWN", unknownProtocol);
            }
        });

        for (Future<Boolean> result : results) {

            Boolean currentResult;

            try {
                currentResult = result.get();

                if (currentResult) {
                    this.success++;
                } else {
                    this.failed++;
                }

            } catch (Exception e) {
                this.logException("ERROR", "TASK GET WORKER RESPONSE", "Task " + this.coordinates.get("taskName") + " was interrupted while waiting for worker result.", e);
                return;
            }
        }

        executor.shutdown();

        /*
         * Task finish log
         */
        String finalMessage = "Task " + this.coordinates.get("taskName") + " has been finished. " +
                "Nodes: " + this.nodes.size() + ". Success: " + this.success + ". Failed: " + this.failed + ".";
        this.logMessage("INFO", "TASK FINISH", finalMessage);

    }


    /**
     * Executing yii command task
     */
    private void runYiiConsoleTask(){

        Map<String, String> params = new HashMap<>();
        params.put("schedule_id", this.coordinates.get("scheduleId"));
        params.put("task_name", this.coordinates.get("taskName"));

        ApiRequest runYiiCommandRequest = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/run-console-command")
                .setParams(params);

        ApiResponse runYiiCommandResponse = ApiCaller.request(runYiiCommandRequest);

        if(!runYiiCommandResponse.success) {
            /*
             * Log record
             * Can't run yii command
             */
            this.logBadResponse("ERROR", "TASK EXECUTE", "Task " + this.coordinates.get("taskName") + " can't run yii command task. API response error.", runYiiCommandResponse);
        }
        else {
            this.logMessage("INFO", "TASK EXECUTE", "Task " + this.coordinates.get("taskName") + ". Yii console command successfully started.");
        }

    }

}
