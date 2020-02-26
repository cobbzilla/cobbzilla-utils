package org.cobbzilla.util.main;

import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.FilesystemWalker;

import java.util.Map;
import java.util.regex.Matcher;

import static org.cobbzilla.util.io.FileUtil.*;

public class FileHeaderMain extends BaseMain<FileHeaderOptions> {

    public static void main (String[] args) { main(FileHeaderMain.class, args); }

    @Override protected void run() throws Exception {
        final FileHeaderOptions opts = getOptions();
        final Map<String, FileHeader> headers = opts.getHeaders();
        new FilesystemWalker()
                .withDir(opts.getDir())
                .withVisitor(file -> {
                    final String ext = FileUtil.extension(file);
                    if (ext.startsWith(".")) {
                        final FileHeader header = headers.get(ext.substring(1));
                        if (header != null) {
                            String contents = toStringOrDie(file);
                            if (contents == null) contents = "";
                            final Matcher matcher = header.getPattern().matcher(contents);
                            if (matcher.find()) {
                                contents = contents.substring(0, matcher.start())
                                        + header.getHeader() + "\n" + contents.substring(matcher.end());
                            } else {
                                contents = header.getHeader() + "\n" + contents;
                            }
                            out(abs(file));
                            toFileOrDie(file, contents);
                        }
                    }
                }).walk();
    }

}
