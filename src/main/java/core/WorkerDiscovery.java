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

/*
 * SNMP
 */
import api.ApiCaller;
import api.ApiRequest;
import api.ApiRequestMethods;
import api.ApiResponse;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

import java.util.*;
import java.util.concurrent.Callable;

/*
 * gson
 */
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;


/**
 * Device SNMP discovery class
 */
public class WorkerDiscovery extends AbstractCoreUnit implements Callable<Boolean>
{

    private Gson gson = new Gson();

    private Integer snmpVer;
    private Integer snmpPort;
    private String  snmpRead;

    private Integer snmpRetries;
    private Integer snmpTimeout;

    private String networkId;
    private Boolean allResponsesEmpty = true;

    /*
     * SNMP objects
     */
    private Snmp snmp;
    private CommunityTarget target;
    private PDU requestPDU;

    private static final Map<String, String> discoveryOids = createMap();

    /*
     * SNMP discovery info OIDs
     */
    private static Map<String, String> createMap() {
        Map<String, String> result = new HashMap<>();
        result.put("1.3.6.1.2.1.1.2.0", "sysobject_id");     // sysObjectID
        result.put("1.3.6.1.2.1.16.19.3.0", "hw");           // probeHardwareRev
        result.put("1.3.6.1.2.1.1.1.0", "sys_description");  // sysDescr
        result.put("1.3.6.1.2.1.1.5.0", "hostname");         // sysName
        result.put("1.3.6.1.2.1.1.6.0", "location");         // sysLocation
        result.put("1.3.6.1.2.1.1.4.0", "contact");          // sysContact
        result.put("1.3.6.1.2.1.17.1.1.0", "mac");           // dot1dBaseBridgeAddress
        result.put("1.3.6.1.2.1.47.1.1.1.1.11.1", "serial"); // entPhysicalSerialNum
        return Collections.unmodifiableMap(result);
    }

    // ipAdEntAddr
    private static final String ipInterfacesOidString = "1.3.6.1.2.1.4.20.1.1";
    private OID ipInterfacesOid = null;

    /*
     * Result map
     */
    private HashMap<String, String> result = new HashMap<String, String>(){{
        put("sysobject_id", null);
        put("hw", null);
        put("sys_description", null);
        put("hostname", null);
        put("location", null);
        put("contact", null);
        put("mac", null);
        put("serial", null);
        put("ip_interfaces", null);
    }};

    /*
     * List of device interfaces
     */
    private List<String> ips = new ArrayList<>();


    /**
     * Constructor
     *
     * @param coordinates  - schedule, task, node, etc..
     * @param settings     - app settings
     */
    WorkerDiscovery(Map<String, String> coordinates, Map<String, String> settings, String networkId, Integer snmpVer, String snmpRead, Integer snmpPort)
    {
        this.coordinates.putAll(coordinates);
        this.settings.putAll(settings);

        this.snmpVer   = snmpVer;
        this.snmpPort  = snmpPort;
        this.snmpRead  = snmpRead;
        this.networkId = networkId;
    }


    /**
     * Main worker thread-callable method
     *
     * @return Boolean - returns true|false for Discovery stats
     */
    public Boolean call()
    {
        // parse snmpRetries, snmpTimeout
        if(!this.extractSettings()) {
            return false;
        }

        // executing
        if(!this.getDiscovery()) {
            this.closeSnmp();
            return false;
        }

        // success
        this.closeSnmp();
        return true;
    }


    /**
     *
     * @return extract setting result
     */
    private Boolean extractSettings()
    {
        String retries = this.settings.get("snmpRetries");
        String timeout = this.settings.get("snmpTimeout");

        /*
         * Set snmp retries
         */
        if(retries == null || retries.length() == 0) {
            String retriesNotSetMessage = "Task " + this.coordinates.get("taskName") + ": SNMP retries not set.";
            this.logMessage("ERROR", "DISCOVERY PARSE SETTINGS", retriesNotSetMessage);
            return false;
        }
        else {
            /*
             * Parse snmp retries
             */
            try {
                this.snmpRetries  = Integer.parseInt(retries);
            }
            catch(NumberFormatException e) {

                String snmpRetriesParseMessage = "Task " + this.coordinates.get("taskName") + ": can't parse SNMP retries setting to integer.";
                this.logException("ERROR", "DISCOVERY PARSE SETTINGS", snmpRetriesParseMessage, e);
                return false;
            }
        }

        /*
         * Validating retries count
         */
        if(this.snmpRetries < 1 || this.snmpRetries > 10) {

            String snmpRetriesCountFailMessage = "Task " + this.coordinates.get("taskName") + ": SNMP retries count must be between 1 and 10.";
            this.logMessage("ERROR", "DISCOVERY PARSE SETTINGS", snmpRetriesCountFailMessage);

            return false;
        }

        /*
         * Set snmp timeout
         */
        if(timeout == null || timeout.length() == 0) {
            this.snmpTimeout = 500;
        }
        else {
            /*
             * Parse snmp timeout
             */
            try {
                this.snmpTimeout  = Integer.parseInt(timeout);
            }
            catch(NumberFormatException e) {
                String snmpTimeoutParseMessage = "Task " + this.coordinates.get("taskName") + ": can't parse SNMP timeout setting to integer.";
                this.logException("ERROR", "DISCOVERY PARSE SETTINGS", snmpTimeoutParseMessage, e);
                return false;
            }
        }

        return true;
    }


    /**
     * SNMP4J init
     * Send all snmp requests
     *
     * @return perform jobs success
     */
    private Boolean getDiscovery() {

        /*
         * SNMP object init
         */
        try {

            this.snmp = new Snmp(new DefaultUdpTransportMapping());
            this.snmp.listen();

            Address address = new UdpAddress(this.coordinates.get("nodeIp") + "/" + this.snmpPort.toString());

            this.target = new CommunityTarget();
            this.target.setAddress(address);
            this.target.setTimeout(this.snmpTimeout);
            this.target.setRetries(this.snmpRetries);
            this.target.setCommunity(new OctetString(this.snmpRead));

            this.requestPDU = new PDU();
            this.requestPDU.setType(PDU.GET);
            switch (this.snmpVer) {
                case 0:
                    target.setVersion(SnmpConstants.version1);
                    break;
                case 1:
                    target.setVersion(SnmpConstants.version2c);
                    break;
                // ver 3 is not supported yet
                default:
                    String credentialsMessage = "Task " + this.coordinates.get("taskName") + ": wrong SNMP version.";
                    this.logMessage("ERROR", "DISCOVERY", credentialsMessage);
                    return false;
            }

        } catch (Exception e) {
            String snmpObjectInitMessage = "Task " + this.coordinates.get("taskName") + ": can't create SNMP object.";
            this.logException("ERROR", "DISCOVERY", snmpObjectInitMessage, e);
            return false;
        }

        /*
         * Add GET-oids to PDU
         */
        for(Map.Entry<String, String> entry : discoveryOids.entrySet()) {

            String currentOID = entry.getKey();

            try {
                OID oid = new OID(currentOID);
                this.requestPDU.add(new VariableBinding(oid));
            }
            catch (Exception e) {
                String addGetToPduMessage = "Task " + this.coordinates.get("taskName") +
                        ": can't add SNMP OID to PDU. Please check discovery SNMP OIDs.";
                this.logException("ERROR", "DISCOVERY", addGetToPduMessage, e);
                return false;
            }

            /*
             * Discovery GET request
             */
            if(!this.sendRequest()) {
                return false;
            }

            this.requestPDU.clear();

        }


        /*
         * No NULL's in result
         */
        for(Map.Entry<String, String> entry : result.entrySet()) {
            if(entry.getValue() == null) {
                entry.setValue("");
            }
            else {
                this.allResponsesEmpty = false;
            }
        }


        /*
         * No data collected
         * Only when ALL responses are Error 2 - no such OID(noSuchName)
         * Is it possible? Who knows..
         */
        if(this.allResponsesEmpty) {
            return false;
        }


        /*
         * Clearing PDU before IP interface walk
         */
        this.requestPDU.clear();


        /*
         * Adding ipAdEntAddr OID to PDU
         */
        try {
            this.ipInterfacesOid = new OID(ipInterfacesOidString);
        }
        catch (Exception e) {
            String addGetToPduMessage = "Task " + this.coordinates.get("taskName") +
                    ": can't add SNMP ip interface OID to PDU. Please check discovery SNMP ipAdEntAddr OID.";
            this.logException("ERROR", "DISCOVERY", addGetToPduMessage, e);
            return false;
        }

        /*
         * Get all ip interfaces from walk
         */
        this.sendWalk();

        this.result.put("ip_interfaces", this.gson.toJson(this.ips));
        this.result.put("ip", this.coordinates.get("nodeIp"));
        this.result.put("network_id", this.networkId);

        Type resultType = new TypeToken<HashMap<String, String>>(){}.getType();

        /*
         * Sending worker result POST
         */
        ApiRequest discoveryPost = new ApiRequest(this.coordinates)
            .setRequestMethod(ApiRequestMethods.POST)
            .setApiMethod("v1/core/set-discovery-result")
            .setPostJson(this.gson.toJson(this.result, resultType));

        ApiResponse setResultResponse = ApiCaller.request(discoveryPost);

        /*
         * Logging
         */
        if(!setResultResponse.success) {
            String setDiscoveryMessage = "Task " + this.coordinates.get("taskName") + ": failed to set result via API.";
            this.logBadResponse("ERROR", "DISCOVERY", setDiscoveryMessage, setResultResponse);
            return false;
        }

        return true;
    }


    /**
     * Sending SNMP-WALK
     *
     * @noinspection UnusedReturnValue
     * @return  Boolean SNMP walk success
     */
    private Boolean sendWalk()
    {

        try {
            TreeUtils treeUtils = new TreeUtils(this.snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(this.target, this.ipInterfacesOid);

            if (events == null || events.size() == 0) {
                return false;
            }

            /*
             * Handle the snmpwalk result.
             */
            for (TreeEvent event : events) {
                if (event == null) {
                    continue;
                }
                if (event.isError()) {
                    // error in request
                    continue;
                }

                VariableBinding[] varBindings = event.getVariableBindings();

                if (varBindings == null || varBindings.length == 0) {
                    continue;
                }

                for (VariableBinding varBinding : varBindings) {
                    if (varBinding == null) {
                        continue;
                    }

                    String currentIp = varBinding.getVariable().toString();

                    if (currentIp.length() < 7) {
                        continue;
                    }

                    if (currentIp.equals("0.0.0.0")) {
                        continue;
                    }

                    if (currentIp.substring(0, 3).equals("127")) {
                        continue;
                    }

                    if (currentIp.length() > 7 && currentIp.substring(0, 7).equals("169.245")) {
                        continue;
                    }

                    this.ips.add(currentIp);
                }

            }
        }
        catch(Exception e) {
            String snmpWalkMessage = "Task " + this.coordinates.get("taskName") + ": can't perform snmpwalk() operation.";
            this.logException("ERROR", "DISCOVERY", snmpWalkMessage, e);
            return false;
        }

        return true;
    }


    /**
     * Sending get request
     *
     * @return Boolean SNMP GET send success
     */
    private Boolean sendRequest()
    {

        PDU responsePDU;
        ResponseEvent responseEvent;

        /*
         * Sending request
         */
        try {
            responseEvent = this.snmp.send(this.requestPDU, this.target);
        }
        catch (Exception e) {
            String snmpSendGetMessage = "Task " + this.coordinates.get("taskName") + ": can't send snmpget() request.";
            this.logException("WARNING", "DISCOVERY", snmpSendGetMessage, e);
            return false;
        }

        /*
         * Response processing
         * Agent timeout. Node offline or wrong community.
         */
        if (responseEvent == null) {
            return false;
        }

        responsePDU = responseEvent.getResponse();

        /*
         * Empty response PDU. Node offline or wrong community.
         */
        if (responsePDU == null) {
            return false;
        }

        Vector tempVector = responsePDU.getVariableBindings();

        // Checking errors
        Integer errorStatus = responsePDU.getErrorStatus();

        if (errorStatus != PDU.noError) {

            /*
             * Error 2 - noSuchName. Device have no such SNMP-OID.
             * Return true. Null value in result will be changed to empty string later.
             */
            if(errorStatus == 2) {
                return true;
            }
            // SNMP request error
            return false;
        }

        // Empty response PDU Vector. Node offline or wrong community.
        if (tempVector == null) {
            return false;
        }

        // Foreach SNMP response if !exception add response to result
        // noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < tempVector.size(); i++) {
            VariableBinding vb = (VariableBinding)tempVector.get(i);

            if (!vb.isException()) {
                /*
                 * Get snmp responses, set required values to result
                 */
                try {
                    String sOid = vb.getOid().toString();
                    String sVar = vb.getVariable().toString();
                    this.result.put(discoveryOids.get(sOid), sVar);
                }
                catch (Exception e) {
                    String responsePduVectorConvertMessage = "Task " + this.coordinates.get("taskName") +
                            ": "+ vb.getOid().toString() +" - can't convert SNMP response to string.";
                    this.logMessage("ERROR", "DISCOVERY", responsePduVectorConvertMessage);
                }
            }
        }

        return true;
    }


    /**
     * Trying to close snmp session
     */
    private void closeSnmp()
    {
        try {
            if(this.snmp != null) {
                this.snmp.close();
            }
        }
        catch (Exception e) {
            String snmpCloseMessage = "Task " + this.coordinates.get("taskName") + ": can't close SNMP session.";
            this.logException("ERROR", "DISCOVERY", snmpCloseMessage, e);
        }
    }
}
