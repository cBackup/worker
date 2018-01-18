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

import java.util.Map;

import abstractions.AbstractWorker;
import abstractions.AbstractProtocol;
import abstractions.DTOProtocolResult;
import abstractions.DTOVariableConvertResult;
import snmp.FactoryMethodSnmp;


/**
 * Worker class for SNMP protocol
 */
public final class WorkerSnmp extends AbstractWorker
{

    /**
     * Constructor
     *
     * @noinspection WeakerAccess
     * @param coordinates - schedule, task, node, etc..
     * @param settings    - app settings
     * @param variables   - app variables
     */
    public WorkerSnmp(Map<String, String> coordinates, Map<String, String> settings, Map<String, DTOVariableConvertResult> variables)
    {
        this.coordinates.putAll(coordinates);
        this.settings.putAll(settings);
        this.variables.putAll(variables);

        /*
         * Setting first data for result map
         */
        this.workerResult.put       = this.coordinates.get("put");
        this.workerResult.table     = this.coordinates.get("table");
        this.workerResult.taskName  = this.coordinates.get("taskName");
        this.workerResult.nodeId    = this.coordinates.get("nodeId");
        this.workerResult.dataPath  = this.settings.get("dataPath");

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean executeJobs()
    {

        /*
         * Using factory to get vendor-model protocol class or main protocol class
         */
        FactoryMethodSnmp factory = new FactoryMethodSnmp();
        AbstractProtocol snmpExecutor = factory.getProtocolObject(this.coordinates, this.settings, this.credentials, this.jobs, this.variables);

        if(snmpExecutor !=null) {

            DTOProtocolResult protocolResult = snmpExecutor.execute();

            if(!protocolResult.success) {
                return false;
            }
            else {
                this.workerResult.data.putAll(protocolResult.data);
                return true;
            }
        }
        else {

            /*
             * Log record
             * Cannot get executor class from factory
             */
            String getExecutorMessage = "Task " + this.coordinates.get("taskName") + ", node " + this.coordinates.get("nodeId") + ": can't get SNMP executor class from factory.";
            this.logMessage("ERROR", "NODE GET EXECUTOR", getExecutorMessage);
            return false;
        }
    }

}
