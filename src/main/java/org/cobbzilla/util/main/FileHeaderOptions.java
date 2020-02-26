package org.cobbzilla.util.main;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.FULL_MAPPER_ALLOW_COMMENTS;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;

public class FileHeaderOptions extends BaseMainOptions {

    public static final String USAGE_DIR = "Directory to process";
    public static final String OPT_DIR = "-d";
    public static final String LONGOPT_DIR = "--dir";
    @Option(name=OPT_DIR, aliases=LONGOPT_DIR, usage=USAGE_DIR, required=true)
    @Getter @Setter private File dir;

    public static final String USAGE_SHALLOW = "Do not recurse into subdirectories";
    public static final String OPT_SHALLOW = "-S";
    public static final String LONGOPT_SHALLOW = "--shallow";
    @Option(name=OPT_SHALLOW, aliases=LONGOPT_SHALLOW, usage=USAGE_SHALLOW)
    @Getter @Setter private boolean shallow = false;

    public static final String USAGE_HEADERS_JSON = "JSON file with header info, or - to read from stdin";
    public static final String OPT_HEADERS_JSON = "-H";
    public static final String LONGOPT_HEADERS_JSON = "--headers";
    @Option(name=OPT_HEADERS_JSON, aliases=LONGOPT_HEADERS_JSON, usage=USAGE_HEADERS_JSON, required=true)
    @Getter @Setter private String headersJson;

    public Map<String, FileHeader> getHeaders() throws IOException {
        final InputStream input;
        if (headersJson.equals("-")) {
            input = inStream(null);
        } else {
            input = inStream(new File(headersJson));
        }
        final FileHeader[] headers = json(IOUtils.toString(input, UTF8cs), FileHeader[].class, FULL_MAPPER_ALLOW_COMMENTS);
        return Arrays.stream(headers).collect(Collectors.toMap(FileHeader::getExt, identity()));
    }

    public static final String USAGE_EXCLUDE_PATHS = "Paths (or path parts) to exclude, separated by commas";
    public static final String OPT_EXCLUDE_PATHS = "-e";
    public static final String LONGOPT_EXCLUDE_PATHS = "--exclude-paths";
    @Option(name=OPT_EXCLUDE_PATHS, aliases=LONGOPT_EXCLUDE_PATHS, usage=USAGE_EXCLUDE_PATHS)
    @Getter @Setter private String excludePaths;

    @Getter(lazy=true) private final List<String> excludePathPatterns = initExcludePatterns();

    private List<String> initExcludePatterns() {
        if (empty(excludePaths)) return Collections.emptyList();
        final String[] parts = excludePaths.split(",");
        return Arrays.stream(parts).collect(Collectors.toList());
    }

    public boolean exclude(File f) { return getExcludePathPatterns().stream().anyMatch(p -> abs(f).contains(p)); }

}
