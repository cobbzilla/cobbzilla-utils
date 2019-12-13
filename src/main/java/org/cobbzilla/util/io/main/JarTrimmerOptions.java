package org.cobbzilla.util.io.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.JarTrimmerConfig;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.json.JsonUtil.json;

public class JarTrimmerOptions extends BaseMainOptions {

    public static final String USAGE_CONFIG = "JSON configuration file to drive JarTrimmer";
    public static final String OPT_CONFIG = "-c";
    public static final String LONGOPT_CONFIG= "--config";
    @Option(name=OPT_CONFIG, aliases=LONGOPT_CONFIG, usage=USAGE_CONFIG)
    @Getter @Setter private File configFile;

    public JarTrimmerConfig getConfig() { return json(FileUtil.toStringOrDie(getConfigFile()), JarTrimmerConfig.class); }

}
