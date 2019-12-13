package org.cobbzilla.util.cron.quartz;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.cron.CronCommand;
import org.cobbzilla.util.cron.CronDaemon;
import org.cobbzilla.util.cron.CronJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzMaster implements CronDaemon {

    private static final String CAL_SUFFIX = "_calendar";
    private static final String JOB_SUFFIX = "_jobDetail";
    private static final String TRIGGER_SUFFIX = "_trigger";

    private Scheduler scheduler;
    @Getter @Setter private TimeZone timeZone;

    @Getter @Setter private List<? extends CronJob> jobs;

    public void start () throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();

        if (jobs != null) {
            for (final CronJob job : jobs) {
                addJob(job);
            }
        }
    }

    public void addJob(final CronJob job) throws SchedulerException {
        String id = job.getId();

        Job specialJob = new Job () {
            @Override
            public void execute(JobExecutionContext context) throws JobExecutionException {
                Map<String, Object> map = context.getMergedJobDataMap();
                try {
                    CronCommand command = job.getCommandInstance();
                    command.init(job.getProperties());
                    command.exec(map);
                } catch (Exception e) {
                    throw new JobExecutionException(e);
                }
            }
        };

        final JobDetail jobDetail = newJob(specialJob.getClass()).withIdentity(id+JOB_SUFFIX).build();

        TriggerBuilder<Trigger> builder = newTrigger().withIdentity(id+TRIGGER_SUFFIX);
        if (job.isStartNow()) builder = builder.startNow();
        final CronScheduleBuilder cronSchedule = cronSchedule(job.getCronTimeString());
        final Trigger trigger = builder.withSchedule(timeZone != null ? cronSchedule.inTimeZone(timeZone) : cronSchedule).build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    @Override public void removeJob(final String id) throws Exception {
        scheduler.deleteJob(new JobKey(id+JOB_SUFFIX));
    }

    public void stop () throws Exception { scheduler.shutdown(); }

}
