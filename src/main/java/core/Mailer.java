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

import java.util.*;
import java.lang.reflect.Type;

/*
 * Google gson
 */
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Mailer class
 */
public class Mailer extends AbstractCoreUnit implements Runnable {

    private Gson gson = new Gson();

    /**
     * Constructor
     *
     * @param coordinates  - schedule, task, node, etc..
     * @param settings     - app settings
     */
    Mailer(Map<String, String> coordinates, Map<String, String> settings)
    {
        this.coordinates.putAll(coordinates);
        this.settings.putAll(settings);
    }

    /**
     * Main runner
     */
    @Override
    public void run() {

        this.logMailerMessage("INFO", "MAILER START", "Mail event " + this.coordinates.get("eventName") + " distribution started.");

        Boolean mailerEventSuccess = false;

        Map<String, String> params = new HashMap<>();
        params.put("event_name", this.coordinates.get("eventName"));
        params.put("schedule_id", this.coordinates.get("scheduleId"));

        ApiRequest mailerEventRequest = new ApiRequest(this.coordinates)
            .setRequestMethod(ApiRequestMethods.GET)
            .setApiMethod("v1/core/send-mail")
            .setParams(params);

        ApiResponse mailerEventResponse = ApiCaller.request(mailerEventRequest);

        if (!mailerEventResponse.success) {
                /*
                 * Log record
                 * Cannot get system task result
                 */
            this.logSystemBadResponse("ERROR", "MAILER EXECUTE", "Can't get mailer event result response.", mailerEventResponse);
        } else {

            String mailerEventJson = mailerEventResponse.response;

            Type settingsType = new TypeToken<Boolean>() {
            }.getType();

            try {
                mailerEventSuccess = gson.fromJson(mailerEventJson, settingsType);
            } catch (Exception e) {
                this.logSystemException("ERROR", "MAILER EXECUTE", "Can't parse mailer event response to boolean.", e);
            }
        }

        if (mailerEventSuccess) {
            String finalMessage = "Mail event " + this.coordinates.get("eventName") + " distribution finished. See Mailer log for detailed information.";
            this.logMailerMessage("INFO", "MAILER FINISH", finalMessage);
        } else {
            String finalMessage = "Mail event " + this.coordinates.get("eventName") + " distribution failed. See Mailer log for detailed information";
            this.logMailerMessage("ERROR", "MAILER FINISH", finalMessage);
        }

    }

}
