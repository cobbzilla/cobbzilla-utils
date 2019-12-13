package org.cobbzilla.util.handlebars.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PdfMergeOptions extends BaseMainOptions {

    public static final String USAGE_INFILE = "Input file. Default is stdin";
    public static final String OPT_INFILE = "-i";
    public static final String LONGOPT_INFILE= "--infile";
    @Option(name=OPT_INFILE, aliases=LONGOPT_INFILE, usage=USAGE_INFILE)
    @Getter @Setter private File infile;

    public InputStream getInputStream() { return inStream(getInfile()); }

    public static final String USAGE_CTXFILE = "Context file, must be a JSON map of String->Object";
    public static final String OPT_CTXFILE = "-c";
    public static final String LONGOPT_CTXFILE= "--context";
    @Option(name=OPT_CTXFILE, aliases=LONGOPT_CTXFILE, usage=USAGE_CTXFILE)
    @Getter @Setter private File contextFile;

    public Map<String, Object> getContext() throws Exception {
        if (contextFile == null) return new HashMap<>();
        return JsonUtil.fromJson(contextFile, Map.class);
    }

    public static final String USAGE_OUTFILE = "Output file. Default is a random temp file";
    public static final String OPT_OUTFILE = "-o";
    public static final String LONGOPT_OUTFILE= "--outfile";
    @Option(name=OPT_OUTFILE, aliases=LONGOPT_OUTFILE, usage=USAGE_OUTFILE)
    @Getter @Setter private File outfile;
    public boolean hasOutfile () { return outfile != null; }

}
