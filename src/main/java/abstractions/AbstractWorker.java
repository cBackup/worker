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

import api.ApiCaller;
import api.ApiRequest;
import api.ApiRequestMethods;
import api.ApiResponse;

/*
 * gson
 */
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/*
 * md5
 */
import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.*;
import java.util.concurrent.Callable;


/**
 * Abstract class for all workers
 * @noinspection WeakerAccess
 */
public abstract class AbstractWorker extends AbstractCoreUnit implements Callable<Boolean> {

    protected Map<String, String> credentials;
    protected Map<String, Map<String, String>> jobs;
    protected Map<String, DTOVariableConvertResult> variables = new HashMap<>();
    protected Gson gson = new Gson();

    /*
     * Worker result DTO
     */
    protected DTOWorkerResult workerResult = new DTOWorkerResult();


    /**
     * Main worker thread-callable method
     *
     * @return Boolean - returns true|false for Task stats
     */
    public Boolean call()
    {

        /*if(!this.isActionRequired()) {
            return true;
        }*/ // todo implement (for save)

        // checking data, critical for factories
        if(!this.checkFactoryData()) {
            return false;
        }

        // Getting credentials
        if(!this.getCredentials()) {
            return false;
        }

        // Getting Job list
        if(!this.processJobs()) {
            return false;
        }

        // Executing jobs
        if(!this.executeJobs()) {
            return false;
        }

        // Check for data saving required
        if(!this.isSetResultRequired()) {
            return true;
        }

        // Setting worker result
        return this.setResult();

    }


    /**
     * Checking data, critical for factories
     *
     * @return Boolean
     */
    protected Boolean checkFactoryData()
    {

        String nodeVendor = this.coordinates.get("nodeVendor");
        String nodeModel = this.coordinates.get("nodeModel");

        if(nodeVendor == null || nodeVendor.length() == 0) {
            String noVendorMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": empty device vendor.";
            this.logMessage("WARNING", "NODE GET CREDENTIALS", noVendorMessage);
            return false;
        }

        if(nodeModel == null || nodeModel.length() == 0) {
            String noModelMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": empty device vendor.";
            this.logMessage("WARNING", "NODE GET CREDENTIALS", noModelMessage);
            return false;
        }

        return true;
    }


    /**
     * Get node credentials
     *
     * @return Boolean - result of get-parse node credentials
     */
    protected Boolean getCredentials()
    {
        /*
         * Getting credentials Json
         */
        Map<String, String> params = new HashMap<>();
        params.put("node_id", this.coordinates.get("nodeId"));

        ApiRequest getCredentials = new ApiRequest(coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/get-node-credentials")
                .setParams(params);

        ApiResponse credentialsResponse = ApiCaller.request(getCredentials);

        if(!credentialsResponse.success) {

            /*
             * Log record
             * Can't get node credentials
             */
            String getCredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't get credentials from API.";
            this.logBadResponse("WARNING", "NODE GET CREDENTIALS", getCredMessage, credentialsResponse);
            return false;

        }


        /*
         * Parsing credentials Json to map
         * Writing parse result
         */
        String credentialsJson = credentialsResponse.response;

        Type credentialsType = new TypeToken<HashMap<String, String>>(){}.getType();

        try {

            this.credentials = gson.fromJson(credentialsJson, credentialsType);

        }
        catch(JsonSyntaxException e) {
            String parseCredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse credentials JSON.";
            this.logException("WARNING", "NODE PARSE CREDENTIALS", parseCredMessage, e);
            return false;
        }

        return true;
    }


    /**
     * Get job list
     * Set job list to this.jobs
     *
     * @return Boolean - job list get result
     */
    protected Boolean processJobs()
    {
        /*
         * Getting credentials Json
         */
        Map<String, String> params = new HashMap<>();
        params.put("worker_id", this.coordinates.get("workerId"));

        ApiRequest getCredentials = new ApiRequest(coordinates)
                .setRequestMethod(ApiRequestMethods.GET)
                .setApiMethod("v1/core/get-jobs")
                .setParams(params);

        ApiResponse jobsResponse = ApiCaller.request(getCredentials);

        if(!jobsResponse.success) {

            /*
             * Log record
             * Can't get node credentials
             */
            String getCredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't get jobs from api.";
            this.logBadResponse("WARNING", "NODE GET JOBS", getCredMessage, jobsResponse);
            return false;

        }

        /*
         * Parsing credentials Json to map
         * Writing parse result
         */
        String jobsJson = jobsResponse.response;

        Type jobsType = new TypeToken<TreeMap<String, HashMap<String, String>>>(){}.getType();

        try {

            this.jobs = gson.fromJson(jobsJson, jobsType);

        }
        catch(JsonSyntaxException e) {
            String parseCredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse jobs JSON.";
            this.logException("WARNING", "NODE PARSE JOBS", parseCredMessage, e);
            return false;

        }

        /*
         * Empty job list
         */
        if(this.jobs.size() == 0) {
            String jobsCountMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": empty jobs list.";
            this.logMessage("WARNING", "NODE GET JOBS", jobsCountMessage);
            return false;
        }

        return true;
    }


    /**
     * Check for data saving required
     * Getting old data hash, comparing with new hash
     *
     * @return Boolean
     */
    protected Boolean isSetResultRequired()
    {
        // noinspection UnusedAssignment
        Boolean required = true;
        String oldHash;
        String allData   = "";
        // noinspection UnusedAssignment
        String newHash   = "";

        if(this.coordinates.get("put") == null) {
            required = false;
        }
        else {

            /*
             * Getting old hash
             */
            Map<String, String> params = new HashMap<>();
            params.put("task_name", this.coordinates.get("taskName"));
            params.put("node_id", this.coordinates.get("nodeId"));

            ApiRequest getCredentials = new ApiRequest(coordinates)
                    .setRequestMethod(ApiRequestMethods.GET)
                    .setApiMethod("v1/core/get-hash")
                    .setParams(params);

            ApiResponse hashResponse = ApiCaller.request(getCredentials);

            if (!hashResponse.success) {

                /*
                 * Log record
                 * Can't get old hash
                 */
                String getCredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't get old hash from API.";
                this.logBadResponse("ERROR", "WORKER SEND RESULT", getCredMessage, hashResponse);
                return false;
            }


            /*
             * Parsing old hash Json to string
             * Writing parse result
             */
            String hashJson = hashResponse.response;

            Type hashType = new TypeToken<String>() {
            }.getType();

            try {

                oldHash = gson.fromJson(hashJson, hashType);

            } catch (JsonSyntaxException e) {
                String parseCredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't parse old hash JSON.";
                this.logException("WARNING", "WORKER SEND RESULT", parseCredMessage, e);
                return false;
            }

            // Calculating new hash
            // Get a set of the entries
            Set resultDataSet = this.workerResult.data.entrySet();

            // Get an iterator
            Iterator it = resultDataSet.iterator();

            // Concatenating all new data
            // noinspection WhileLoopReplaceableByForEach
            while (it.hasNext()) {
                Map.Entry result = (Map.Entry) it.next();
                allData += result.getValue();
            }

            // Getting new hash
            try {

                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(allData.getBytes()); // todo test getBytes("UTF-8")
                byte[] digest = md.digest();
                newHash = DatatypeConverter.printHexBinary(digest).toUpperCase();

            } catch (NoSuchAlgorithmException e) {
                String parseCredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't get new md5 hash from result.";
                this.logException("ERROR", "WORKER SEND RESULT", parseCredMessage, e);
                return false;
            }

            this.workerResult.hash = newHash;

            required = oldHash == null || !oldHash.equals(newHash);

        }

        if(required) {
            return true;
        }
        else {
            String saveNotRequiredMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId")
                    + ": worker success. Result saving is not required.";
            this.logMessage("INFO","WORKER SEND RESULT", saveNotRequiredMessage);
            return false;
        }
    }


    /**
     * Sending worker result to API
     *
     * @return Boolean
     */
    protected Boolean setResult()
    {

        String message;

        /*
         * No result save required
         */
        if(this.coordinates.get("put") == null) {
            return true;
        }

        /*
         * Target database table is not set
         */
        if(this.coordinates.get("put").equals("db") && (this.coordinates.get("table") == null || this.coordinates.get("table").length() == 0)) {
            message = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": destination db table is not set.";
            this.logMessage("WARNING","WORKER SEND RESULT", message);
            return false;
        }

        /*
         * Sending worker result POST
         */
        ApiRequest log = new ApiRequest(this.coordinates)
                .setRequestMethod(ApiRequestMethods.POST)
                .setApiMethod("v1/core/set-worker-result")
                .setPostJson(this.gson.toJson(this.workerResult));

        ApiResponse setResultResponse = ApiCaller.request(log);

        /*
         * Logging
         */
        if(!setResultResponse.success) {
            message = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": set result via API is failed.";
            this.logBadResponse("ERROR", "WORKER SEND RESULT", message, setResultResponse);
            return false;
        }
        else {
            message = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": worker success. Result is successfully saved.";
            this.logMessage("INFO","WORKER SEND RESULT", message);
            return true;
        }

    }


    /**
     * Executing all commands(this.jobs).
     * Setting execution results to this.workerResult
     * Returning success.
     *
     * @return Boolean - success of jobs execution
     */
    protected abstract Boolean executeJobs();


}
