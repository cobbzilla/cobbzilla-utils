package org.cobbzilla.util.main;

import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.FilesystemWalker;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import static org.cobbzilla.util.io.FileUtil.*;

public class FileHeaderMain extends BaseMain<FileHeaderOptions> {

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

}
