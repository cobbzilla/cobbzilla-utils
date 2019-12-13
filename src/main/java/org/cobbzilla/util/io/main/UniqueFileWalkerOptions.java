package org.cobbzilla.util.io.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;

public class UniqueFileWalkerOptions extends BaseMainOptions {

    public static final String USAGE_DIR = "Add a directory to the search";
    public static final String OPT_DIR = "-d";
    public static final String LONGOPT_DIR= "--dir";
    @Option(name=OPT_DIR, aliases=LONGOPT_DIR, usage=USAGE_DIR, required=true)
    @Getter @Setter private File[] dirs;

    public static final String USAGE_TIMEOUT = "Timeout duration. For example 10m for ten minutes. use h for hours, d for days.";
    public static final String OPT_TIMEOUT = "-t";
    public static final String LONGOPT_TIMEOUT= "--timeout";
    @Option(name=OPT_TIMEOUT, aliases=LONGOPT_TIMEOUT, usage=USAGE_TIMEOUT)
    @Getter @Setter private String timeoutDuration = "1d";

    public static final String USAGE_SIZE = "Rough guess to number of files to visit.";
    public static final String OPT_SIZE = "-s";
    public static final String LONGOPT_SIZE= "--size";
    @Option(name=OPT_SIZE, aliases=LONGOPT_SIZE, usage=USAGE_SIZE)
    @Getter @Setter private int size = 1_000_000;

    public static final String USAGE_THREADS = "Degree of parallelism";
    public static final String OPT_THREADS = "-p";
    public static final String LONGOPT_THREADS= "--parallel";
    @Option(name=OPT_THREADS, aliases=LONGOPT_THREADS, usage=USAGE_THREADS)
    @Getter @Setter private int threads = 5;

    public static final String USAGE_OUTFILE = "Output file";
    public static final String OPT_OUTFILE = "-o";
    public static final String LONGOPT_OUTFILE= "--outfile";
    @Option(name=OPT_OUTFILE, aliases=LONGOPT_OUTFILE, usage=USAGE_OUTFILE)
    @Getter @Setter private File outfile;
    public boolean hasOutfile () { return outfile != null; }

}
