package org.cobbzilla.util.handlebars.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

public class PdfConcatOptions extends BaseMainOptions {

    public static final String USAGE_OUTFILE = "Output file. Default is stdout.";
    public static final String OPT_OUTFILE = "-o";
    public static final String LONGOPT_OUTFILE= "--output";
    @Option(name=OPT_OUTFILE, aliases=LONGOPT_OUTFILE, usage=USAGE_OUTFILE)
    @Getter @Setter private File outfile;

    public OutputStream getOut () { return outStream(outfile); }

    public static final String USAGE_INFILES = "Show help for this command";
    @Argument(usage=USAGE_INFILES)
    @Getter @Setter private List<String> infiles;

    public static final String USAGE_MAX_MEMORY = "Max memory to use. Default is unlimited";
    public static final String OPT_MAX_MEMORY = "-m";
    public static final String LONGOPT_MAX_MEMORY= "--max-memory";
    @Option(name=OPT_MAX_MEMORY, aliases=LONGOPT_MAX_MEMORY, usage=USAGE_MAX_MEMORY)
    @Getter @Setter private long maxMemory = -1;

    public static final String USAGE_MAX_DISK = "Max disk to use. Default is unlimited";
    public static final String OPT_MAX_DISK = "-d";
    public static final String LONGOPT_MAX_DISK= "--max-disk";
    @Option(name=OPT_MAX_DISK, aliases=LONGOPT_MAX_DISK, usage=USAGE_MAX_DISK)
    @Getter @Setter private long maxDisk = -1;

}
