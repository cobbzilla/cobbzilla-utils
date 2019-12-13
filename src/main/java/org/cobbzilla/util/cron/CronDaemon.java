package org.cobbzilla.util.cron;

public interface CronDaemon {

    void start () throws Exception;

    void stop() throws Exception;

    void addJob(final CronJob job) throws Exception;

    void removeJob(final String id) throws Exception;

}