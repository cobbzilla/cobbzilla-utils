package org.cobbzilla.util.cron;

import java.util.Properties;

public abstract class CronCommandBase implements CronCommand {

    protected Properties properties;

    @Override
    public void init(Properties properties) { this.properties = properties; }

}
