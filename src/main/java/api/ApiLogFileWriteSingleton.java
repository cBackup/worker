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

import java.io.FileWriter;
import java.io.File;


/**
 *  Multithread log filewrite class
 *  Implemented using the singleton pattern.
 */
class ApiLogFileWriteSingleton {

    private final static ApiLogFileWriteSingleton _instance = new ApiLogFileWriteSingleton();

    private ApiLogFileWriteSingleton() {
        super();
    }

    /**
     * Synhronized file write method
     *
     * @param log String log message
     */
    synchronized void writeToFile(String log) {

        FileWriter fw = null;

        try
        {

            /*
             * Getting file path
             */
            File logPath = new File(ApiLogFileWriteSingleton.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            String propertiesPath = logPath.getParentFile().getParentFile().getAbsolutePath();
            propertiesPath = propertiesPath + File.separator + "runtime" + File.separator + "logs" + File.separator + "javacore.log";

            /*
             * Writing
             */
            fw = new FileWriter(propertiesPath,true);
            fw.write(log + System.getProperty( "line.separator" ));

        }
        catch(Exception e) {
            // we can't do something here
            // file logging is our LAST hope
        }
        finally {
            // File close
            // noinspection EmptyCatchBlock
            try {
                if (fw != null) {
                    fw.close();
                }
            }
            catch ( Exception e) {}
        }

    }

    static ApiLogFileWriteSingleton getInstance() {
        return _instance;
    }
}
