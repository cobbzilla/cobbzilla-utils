package org.cobbzilla.util.io.main;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.regex.RegexStreamFilter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.cobbzilla.util.system.Bytes;
import org.kohsuke.args4j.Option;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public class RegexFilterOptions extends BaseMainOptions {

    public static final String USAGE_BUFSIZ = "Buffer size (default 16K)";
    public static final String OPT_BUFSIZ = "-b";
    public static final String LONGOPT_BUFSIZ= "--buffer-size";
    @Option(name=OPT_BUFSIZ, aliases=LONGOPT_BUFSIZ, usage=USAGE_BUFSIZ)
    @Getter @Setter private int bufferSize = (int) (16 * Bytes.KB);

    public static final String USAGE_DRIVER = "Driver class";
    public static final String OPT_DRIVER = "-d";
    public static final String LONGOPT_DRIVER= "--driver";
    @Option(name=OPT_DRIVER, aliases=LONGOPT_DRIVER, usage=USAGE_DRIVER, required=true)
    @Getter @Setter private String driverClass;

    public RegexStreamFilter getDriver() {
        try {
            return instantiate(getDriverClass());
        } catch (Exception e) {
            return die("getDriver: "+e, e);
        }
    }

    public static final String USAGE_CONFIG = "Configuration. If first char is @ then this is a file path. Otherwise it is literal JSON";
    public static final String OPT_CONFIG = "-c";
    public static final String LONGOPT_CONFIG= "--config";
    @Option(name=OPT_CONFIG, aliases=LONGOPT_CONFIG, usage=USAGE_CONFIG)
    @Getter @Setter private String configJson;

    public JsonNode getConfig () {
        if (configJson == null) return null;
        if (configJson.startsWith("@")) {
            return json(FileUtil.toStringOrDie(configJson.substring(1)), JsonNode.class);
        }
        return json(configJson, JsonNode.class);
    }

}
