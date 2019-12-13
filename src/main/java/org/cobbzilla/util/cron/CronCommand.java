package org.cobbzilla.util.cron;

import java.util.Map;
import java.util.Properties;

public interface CronCommand {

    void init (Properties properties);

    void exec (Map<String, Object> context) throws Exception;

}
