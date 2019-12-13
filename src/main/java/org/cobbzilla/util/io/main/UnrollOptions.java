package org.cobbzilla.util.io.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;

public class UnrollOptions extends BaseMainOptions {

    public static final String USAGE_FILE = "File to unroll";
    public static final String OPT_FILE = "-f";
    public static final String LONGOPT_FILE= "--file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE, required=true)
    @Getter @Setter private File file;

}
