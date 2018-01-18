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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import it.sauronsoftware.cron4j.Scheduler;
import java.util.Date;


/**
 * Cbackup application starter class
 * @noinspection WeakerAccess, SpringFacetCodeInspection
 */
@SpringBootApplication
public class Cbackup implements CommandLineRunner {

    private Scheduler pulseScheduler = new Scheduler();
    public static Date startTime;
    public static String packageVersion = "unknown";

    /**
     * @param args input arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(Cbackup.class, args);
    }


    /**
     * App start method
     */
    public void run(String... args) throws Exception {
        this.runMainSchedule();
    }


    /**
     * Run main application schedule
     * App pulse task, running every hour
     */
    private void runMainSchedule() {

        Runnable pulseTask = () -> {/* Run any 'i am alive' code here */};

        this.pulseScheduler.schedule("0 * * * *", pulseTask);
        this.pulseScheduler.start();

        /* Save start date */
        Cbackup.startTime = new Date();

        /* Get version only if app is in package!!! */
        Package aPackage = Cbackup.class.getPackage();
        if(aPackage.getImplementationVersion() != null) {
            packageVersion = aPackage.getImplementationVersion();
        }

    }
}
