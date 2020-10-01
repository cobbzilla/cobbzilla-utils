package org.cobbzilla.util.main;

import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.FilesystemWalker;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;
import static org.cobbzilla.util.io.FileUtil.*;

public class FileHeaderMain extends BaseMain<FileHeaderOptions> {

    public static final String FILE_HEADER_IGNORE = ".file_header_ignore";

    public static void main (String[] args) { main(FileHeaderMain.class, args); }

    @Override protected void run() throws Exception {
        final FileHeaderOptions opts = getOptions();
        final Map<String, FileHeader> headers = opts.getHeaders();
        if (opts.isShallow()) {
            Arrays.stream(Objects.requireNonNull(opts.getDir().listFiles()))
                    .forEach(file -> processFile(opts, headers, file));
        } else {
            new FilesystemWalker()
                    .withDir(opts.getDir())
                    .withVisitor(file -> processFile(opts, headers, file))
                    .walk();
        }
    }

    private void processFile(FileHeaderOptions opts, Map<String, FileHeader> headers, File file) {
        if (opts.exclude(file)) {
            err("excluding: "+abs(file));
            return;
        }
        if (!file.isFile()) {
            err("skipping non-file: "+abs(file));
            return;
        }
        if (isIgnored(file)) {
            err("skipping ignored file: "+abs(file));
            return;
        }
        final String ext = FileUtil.extension(file);
        final FileHeader header = headers.get(ext.length() > 0 ? ext.substring(1) : ext);
        if (header != null) {
            String contents = toStringOrDie(file);
            if (contents == null) contents = "";
            final String prefix;
            if (header.hasPrefix()) {
                final Matcher prefixMatcher = header.getPrefixPattern().matcher(contents);
                if (!prefixMatcher.find()) {
                    err("prefix not found ("+header.getPrefix().replace("\n", "\\n")+") in file: "+abs(file));
                    prefix = "";
                } else {
                    prefix = contents.substring(0, prefixMatcher.start())
                            + contents.substring(prefixMatcher.start(), prefixMatcher.end());
                    contents = contents.substring(prefixMatcher.end());
                }
            } else {
                prefix = "";
            }
            final Matcher matcher = header.getPattern().matcher(contents);
            if (matcher.find()) {
                contents = prefix + contents.substring(0, matcher.start())
                        + header.getHeader() + "\n" + contents.substring(matcher.end());
            } else {
                contents = prefix + header.getHeader() + "\n" + contents;
            }
            out(abs(file));
            toFileOrDie(file, contents);
        }
    }

    private final Map<String, List<Pattern>> ignoreFileCache = new ConcurrentHashMap<>(100);

    private boolean isIgnored(File file) {
        final File dir = file.getParentFile();
        final File ignoreFile = new File(dir, FILE_HEADER_IGNORE);
        if (ignoreFile.exists()) {
            final List<Pattern> patterns = ignoreFileCache.computeIfAbsent(abs(dir), k -> {
                final List<String> regexes;
                try {
                    regexes = FileUtil.toStringList(ignoreFile);
                } catch (Exception e) {
                    err("isIgnored: error reading "+abs(ignoreFile)+" (all files will be ignored): "+shortError(e));
                    return null;
                }
                final List<Pattern> list = new ArrayList<>();
                for (String regex : regexes) {
                    regex = regex.trim();
                    if (empty(regex)) continue;
                    try {
                        list.add(Pattern.compile(regex));
                    } catch (Exception e) {
                        err("isIgnored: skipping invalid regex in "+abs(ignoreFile)+": '"+regex+"': "+shortError(e));
                    }
                }
                return list;
            });
            final String name = file.getName();
            return patterns == null || patterns.isEmpty() || patterns.stream().anyMatch(p -> p.matcher(name).matches());
        }
        return false;
    }

}
