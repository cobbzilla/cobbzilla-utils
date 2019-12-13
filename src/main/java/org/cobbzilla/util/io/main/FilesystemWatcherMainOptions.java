package org.cobbzilla.util.io.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.SingletonSet;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.util.Collection;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class FilesystemWatcherMainOptions extends BaseMainOptions {

    public static final String USAGE_COMMAND = "Command to run when something changes, must be executable. Default is to print the changes detected.";
    public static final String OPT_COMMAND = "-c";
    public static final String LONGOPT_COMMAND = "--command";
    @Option(name=OPT_COMMAND, aliases=LONGOPT_COMMAND, usage=USAGE_COMMAND, required=false)
    @Getter @Setter private String command = null;
    public boolean hasCommand () { return !empty(command); }

    public static final int DEFAULT_TIMEOUT = 600;
    public static final String USAGE_TIMEOUT = "Command will be run after this timeout (in seconds), regardless of any changes. Default is "+DEFAULT_TIMEOUT+" seconds ("+DEFAULT_TIMEOUT/60+" minutes).";
    public static final String OPT_TIMEOUT = "-t";
    public static final String LONGOPT_TIMEOUT = "--timeout";
    @Option(name=OPT_TIMEOUT, aliases=LONGOPT_TIMEOUT, usage=USAGE_TIMEOUT, required=false)
    @Getter @Setter private int timeout = DEFAULT_TIMEOUT;

    public static final int DEFAULT_MAXEVENTS = 100;
    public static final String USAGE_MAXEVENTS = "Command will be run after this many events have occurred. Default is " + DEFAULT_MAXEVENTS;
    public static final String OPT_MAXEVENTS = "-m";
    public static final String LONGOPT_MAXEVENTS = "--max-events";
    @Option(name=OPT_MAXEVENTS, aliases=LONGOPT_MAXEVENTS, usage=USAGE_MAXEVENTS, required=false)
    @Getter @Setter private int maxEvents = DEFAULT_MAXEVENTS;

    public static final String USAGE_DAMPER = "Command will never be run until there have been no events for this many seconds. Default is 0 (disabled). Takes precedence over "+OPT_TIMEOUT+"/"+LONGOPT_TIMEOUT;
    public static final String OPT_DAMPER = "-d";
    public static final String LONGOPT_DAMPER = "--damper";
    @Option(name=OPT_DAMPER, aliases=LONGOPT_DAMPER, usage=USAGE_DAMPER, required=false)
    @Getter @Setter private int damper = 0;
    public long getDamperMillis () { return damper * 1000; }

    public static final String USAGE_PATHS = "Paths to watch for changes. Default is the current directory";
    @Argument(usage=USAGE_PATHS)
    @Getter @Setter private List<String> paths = null;

    public boolean hasPaths () { return !empty(paths); }
    public Collection getWatchPaths() { return hasPaths() ? paths : new SingletonSet(System.getProperty("user.dir")); }
}
