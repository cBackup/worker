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

import org.apache.http.client.utils.URIBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Api request caller (http GET|POST)
 */
public final class ApiCaller {

    /*
     * Instances not allowed
     */
    private ApiCaller() {}

    /**
     * Http request GET-POST router
     *
     * @param request Class ApiRequest instance
     * @return        Class ApiResponse instance
     */
    public static ApiResponse request(ApiRequest request)
    {

        ApiResponse response = null;

        if(request.validate()) {

            switch (request.requestMethod) {

                case GET:
                    response = ApiCaller.getRequest(request);
                    break;

                case POST:
                    response = ApiCaller.postRequest(request);
                    break;
            }
        }
        else {

            response = new ApiResponse(request);
            response.setSuccess(false);
            response.setResponse("JAVA CORE - API REQUEST FAILED. The required request parameters are missing.");
            response.setResponseCode(400);

        }

        return response;

    }


    /**
     * GET http request
     *
     * @param request Class ApiRequest instance
     * @return        Class ApiResponse instance
     */
    private static ApiResponse getRequest(ApiRequest request) {

        ApiResponse response = new ApiResponse(request);

        //noinspection Duplicates
        try {

            /*
             * Create URI
             */
            URIBuilder uribuilder = new URIBuilder().setScheme(request.coordinates.get("scheme")).setHost(request.coordinates.get("site")).addParameter("r", request.apiMethod);

            /*
             * Add GET params
             */
            if(request.params != null) {
                // noinspection Java8MapForEach, CodeBlock2Expr
                request.params.entrySet().forEach(entry -> {
                    uribuilder.addParameter(entry.getKey(), entry.getValue());
                });
            }

            /*
             * Get url from URI builder
             */
            URL url = uribuilder.build().toURL();

            HttpURLConnection connect = (HttpURLConnection)url.openConnection();

            connect.setRequestMethod("GET");
            connect.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            connect.setRequestProperty("Accept", "text/html,application/json");//old one
            connect.setRequestProperty("Authorization", "Bearer " + request.coordinates.get("token"));

            response.setResponseCode(connect.getResponseCode());

            BufferedReader responseBuffer;

            if (response.responseCode >= 400) {
                responseBuffer = new BufferedReader(new InputStreamReader(connect.getErrorStream(), "UTF-8"));
            }
            else {
                responseBuffer = new BufferedReader(new InputStreamReader(connect.getInputStream(), "UTF-8"));
            }

            StringBuilder sb = new StringBuilder();

            for (int c; (c = responseBuffer.read()) >= 0; ) {
                sb.append((char) c);
            }

            responseBuffer.close();

            response.setResponse(sb.toString());

            if(response.responseCode == 200) {
                response.setSuccess(true);

            }

        }
        catch(Exception e) {

            StringBuilder sb = new StringBuilder();

            /*
             * Stack trace to string
             */
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString());
                sb.append("\n");
            }

            response.setException(e.getClass().getSimpleName() + ". Message: " +e.getMessage());
            response.setStackTrace(sb.toString());

            ApiCaller.setFileLog(request, response);

        }

        return response;
    }


    /**
     * POST http request
     *
     * @param request Class ApiRequest instance
     * @return        Class ApiResponse instance
     */
    private static ApiResponse postRequest(ApiRequest request)
    {

        ApiResponse response = new ApiResponse(request);

        //noinspection Duplicates
        try {

            /*
             * Create URI
             */
            URIBuilder uribuilder = new URIBuilder().setScheme(request.coordinates.get("scheme")).setHost(request.coordinates.get("site")).addParameter("r", request.apiMethod);

            /*
             * Get url from URI builder
             */
            URL url = uribuilder.build().toURL();

            byte[] postDataBytes = request.postJson.getBytes("UTF-8");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "text/html,application/json");
            conn.setRequestProperty("Authorization", "Bearer " + request.coordinates.get("token"));
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);

            response.setResponseCode(conn.getResponseCode());

            if (response.responseCode == 201) {

                response.setSuccess(true);
            }
            else {
                /*
                 * Writing response body only in case of an error
                 * We don't use success responses and they are too big. Memory economy.
                 */
                BufferedReader responseBuffer;

                if (response.responseCode >= 400) {
                    responseBuffer = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
                }
                else {
                    responseBuffer = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                }

                StringBuilder sb = new StringBuilder();

                for (int c; (c = responseBuffer.read()) >= 0; ) {
                    sb.append((char) c);
                }

                responseBuffer.close();

                response.setResponse(sb.toString());
            }

        }
        catch(Exception e) {

            StringBuilder sb = new StringBuilder();

            /*
             * Stack trace to string
             */
            for (StackTraceElement element : e.getStackTrace()) {
                sb.append(element.toString());
                sb.append("\n");
            }

            response.setException(e.getClass().getSimpleName() + ". Message: " +e.getMessage());
            response.setStackTrace(sb.toString());

            ApiCaller.setFileLog(request, response);

        }

        return response;
    }


    /**
     * Writing log to file
     *
     * @noinspection UnusedReturnValue
     * @param request  Class ApiRequest instance
     * @param response Class ApiResponse instance
     * @return         Returns lof-file write result - true|false
     */
    private static boolean setFileLog(ApiRequest request, ApiResponse response) {

        boolean toReturn = true;

        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
            String date = dateFormat.format(new Date());

            String logMessage = date + " severity: ERROR." + System.getProperty( "line.separator" )
                    + "action: " + "V1 API "+ request.requestMethod.toString() + System.getProperty( "line.separator" )
                    + "message: " + ApiLogHelper.getMessage("JAVA CORE - API REQUEST FAILED.", response.coordinates, response);

            ApiLogFileWriteSingleton.getInstance().writeToFile(logMessage);
        }
        catch (Exception e) {
            toReturn = false;
        }

        return toReturn;
    }


}
