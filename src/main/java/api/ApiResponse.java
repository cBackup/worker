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

import java.util.HashMap;
import java.util.Map;


/**
 * Api response dto
 * @noinspection WeakerAccess
 */
public class ApiResponse {

    public String apiMethod     = null;
    public String requestMethod = null;

    public boolean success      = false;
    public int responseCode     = 0;
    public String response      = "";
    public String exception     = "";
    public String stackTrace    = "";

    // Request coordinates - schedule, task, node, etc...
    public Map<String, String> coordinates = new HashMap<>();

    public ApiResponse(ApiRequest request) {

        this.coordinates.putAll(request.coordinates);
        this.apiMethod      = request.apiMethod;
        this.requestMethod  = request.requestMethod.toString();
    }

    /**
     * @param success  Request success bool setter
     * @return         Returns this context
     */
    public ApiResponse setSuccess(boolean success)
    {
        this.success = success;
        return this;
    }

    /**
     * @noinspection UnusedReturnValue
     * @param responseCode  Response code setter
     * @return              Returns this context
     */
    public ApiResponse setResponseCode(int responseCode)
    {
        this.responseCode = responseCode;
        return this;
    }

    /**
     * @param response  Response code setter
     * @return          Returns this context
     */
    public ApiResponse setResponse(String response)
    {
        this.response = response;
        return this;
    }

    /**
     * @param exception Exception name setter
     * @return          Returns this context
     */
    public ApiResponse setException(String exception)
    {
        this.exception = exception;
        return this;
    }

    /**
     * @noinspection UnusedReturnValue
     * @param stackTrace  Stack trace setter
     * @return            Returns this context
     */
    public ApiResponse setStackTrace(String stackTrace)
    {
        this.stackTrace = stackTrace;
        return this;
    }

}
