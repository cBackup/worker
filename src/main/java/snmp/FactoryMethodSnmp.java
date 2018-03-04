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
package snmp;

import java.util.Map;
import abstractions.DTOVariableConvertResult;
import api.ApiLogHelper;
import java.lang.reflect.Constructor;
import java.util.HashMap;


/**
 * Factory class to return snmp protocol classes
 * Usage:
 * 1) How to add main vendor class
 * - add 'put("VENDOR", null);' to static map $models
 * - create class _VENDOR_Snmp(extends GeneralSnmp)
 * 2) How to add model realization class
 * - create vendor models map
 * example:
 * private static final Map<String, String> MAP_VENDOR_MODELS = new HashMap<String, String>() {{
 * put("RB750", "RB750"); // this one for model RB750 protocol realization
 * put("RB960",   null);  // this one will use GeneralSnmp class
 * }};
 * - create class _VENDOR_RB750_Snmp(extends GeneralSnmp)
 * - add 'put("VENDOR", MAP_VENDOR_MODELS);' to static map $models
 *
 */
public class FactoryMethodSnmp {

    private static final Map<String, Map<String, String>> models = new HashMap<String, Map<String, String>>() {{
        put("Dlink", null);
        put("Zyxel", null);
    }};

    /**
     * Factory
     *
     * @param coordinates   - schedule, task, node, etc..
     * @param settings      - app settings
     * @param credentials   - credentials
     * @param jobs          - sorted jobs
     * @param variables     - variable list
     * @return GeneralSnmp|null
     */
    public GeneralSnmp getProtocolObject(Map<String, String> coordinates, Map<String, String> settings, Map<String, String> credentials, Map<String, Map<String, String>> jobs, Map<String, DTOVariableConvertResult> variables) {

        //noinspection UnusedAssignment
        GeneralSnmp toReturn = null;
        String className     = null;

        String vendor = coordinates.get("nodeVendor").replaceAll("-", "__");
        String model  = coordinates.get("nodeModel").replaceAll("-", "__");

        if(models.get(vendor) == null) {
            if(models.containsKey(vendor)) {
                className = "_" + vendor + "_Snmp";
            }
        }
        else {
            Map<String, String> currentVendor = models.get(vendor);

            if(currentVendor.get(model) == null) {
                if(!currentVendor.containsKey(model)) {
                    className = "_" + vendor + "_Snmp";
                }
            }
            else {
                className = "_" + models.get(vendor) + "_" + currentVendor.get(model) + "_Snmp";
            }
        }

        if(className == null) {
            toReturn = new GeneralSnmp(coordinates, settings, credentials, jobs, variables);
        }
        else {

            try {
                Class currentSnmpClass      = Class.forName("snmp."+className);
                //noinspection unchecked
                Constructor snmpConstructor = currentSnmpClass.getConstructor(Map.class, Map.class, Map.class, Map.class, Map.class);
                Object snmpObject           = snmpConstructor.newInstance(coordinates, settings, credentials, jobs, variables );
                toReturn                    = (GeneralSnmp)snmpObject;
            }
            catch(Exception e) {
                String portParseMessage = "Task " + coordinates.get("taskName") + ", node " + coordinates.get("nodeId") +
                        ": SNMP factory exception. Can't create object " + className + ".";
                ApiLogHelper.setLogException("ERROR", "WORKER PROTOCOL CLASS INIT", portParseMessage, coordinates, e);
                return null;
            }
        }

        return toReturn;
    }

}
