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

import it.sauronsoftware.cron4j.Scheduler;


/**
 * Logic to interact with the Cron4j library.
 * Implemented using the singleton pattern.
 */
public class CronSingleton {

    // Singleton static instance for the class
    private static CronSingleton _instance = null;

    // Static instance for the shared scheduler. Initialized when singleton is created.
    private static Scheduler _scheduler = null;

    /**
     * Default constructor
     * Defined as protected to prevent instantiation
     */
    private CronSingleton() {
        _scheduler = new Scheduler();
    }

    /**
     * Get the instance of the singleton
     * @return instance of the singleton
     */
    static CronSingleton getInstance() {
        if (_instance == null) {
            _instance = new CronSingleton();
        }
        return _instance;
    }

    /**
     * Get the scheduler initialized based on the properties uploaded to JCS.
     * @noinspection WeakerAccess
     * @return initialized scheduler
     */
    public Scheduler getScheduler() {
        return _scheduler;
    }

    /**
     * Check scheduler status
     *
     * @return boolean
     */
    boolean isStarted() {
        return (_scheduler.isStarted());
    }

    /**
     * Starts the scheduler
     */
    void startScheduler() {
        _scheduler.start();
    }

    /**
     * Starts the scheduler
     */
    void stopScheduler() {
        _scheduler.stop();
    }

    /**
     * Schedule a job
     * @noinspection unused
     * @param cronSchedule the cron based schedule for the job
     * @param job the job to be scheduled
     */
    public String scheduleJob(String cronSchedule, Runnable job) {
       return _scheduler.schedule(cronSchedule, job);
    }

    /**
     * Schedule a task
     * @param cronSchedule the cron based schedule for the job
     * @param task the task to be scheduled
     */
    String scheduleJob(String cronSchedule, it.sauronsoftware.cron4j.Task task) {
        // Schedule a job based on the schedule provided.
        return _scheduler.schedule(cronSchedule, task);
    }

}
