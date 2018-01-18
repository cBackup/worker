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
 * Api request dto
 * @noinspection WeakerAccess
 */
public class ApiRequest {

    public String apiMethod     = null;
    public String postJson      = null;

    // Request coordinates - schedule, task, node, etc...
    public Map<String, String> coordinates = new HashMap<>();

    // GET params
    public Map<String, String> params = new HashMap<>();

    // Request method(GET, POST)
    public ApiRequestMethods requestMethod = null;

    /**
     * @param coordinates  - process coordinates - schedule, task, node..
     */
    public ApiRequest(Map<String, String> coordinates)
    {
        this.coordinates.putAll(coordinates);
    }

    /**
     * @param requestMethod  Request method setter(GET,POST)
     * @return               Returns this context
     */
    public ApiRequest setRequestMethod(ApiRequestMethods requestMethod)
    {
        this.requestMethod = requestMethod;
        return this;
    }

    /**
     * @param apiMethod Api method setter
     * @return          Returns this context
     */
    public ApiRequest setApiMethod(String apiMethod)
    {
        this.apiMethod = apiMethod;
        return this;
    }

    /**
     * @param postJson Api post json setter
     * @return         Returns this context
     */
    public ApiRequest setPostJson(String postJson)
    {
        this.postJson = postJson;
        return this;
    }

    /**
     * @param params Api request GET params HashMap setter
     * @return       Returns this context
     */
    public ApiRequest setParams(Map<String, String> params)
    {
        this.params.putAll(params);
        return this;
    }

    /**
     * @return boolean
     */
    public boolean validate()
    {
        boolean toReturn = true;

        if(this.requestMethod == null || this.coordinates.get("site") == null ||  this.apiMethod == null || this.coordinates.get("token") == null) {
            toReturn = false;
        }
        else {
            if (this.requestMethod.toString().equals("POST") && this.postJson == null) {
                toReturn = false;
            }
        }

        return toReturn;
    }

}
