package org.cobbzilla.util.io.main;

import lombok.Cleanup;
import org.apache.commons.io.output.TeeOutputStream;
import org.cobbzilla.util.daemon.AwaitResult;
import org.cobbzilla.util.io.FilesystemWalker;
import org.cobbzilla.util.io.UniqueFileFsWalker;
import org.cobbzilla.util.main.BaseMain;
import org.cobbzilla.util.string.StringUtil;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class UniqueFileWalkerMain extends BaseMain<UniqueFileWalkerOptions> {

    public static void main (String[] args) { main(UniqueFileWalkerMain.class, args); }

    @Override protected void run() throws Exception {
        final UniqueFileWalkerOptions options = getOptions();

        final UniqueFileFsWalker visitor = new UniqueFileFsWalker(options.getSize());
        final AwaitResult result = new FilesystemWalker()
                .setSize(options.getSize())
                .setThreads(options.getThreads())
                .withDirs(options.getDirs())
                .withTimeoutDuration(options.getTimeoutDuration())
                .withVisitor(visitor)
                .walk();

        if (!result.allSucceeded()) {
            if (result.numFails() > 0) {
                out(">>>>> "+result.getFailures().values().size()+" failures:");
                out(StringUtil.toString(result.getFailures().values(), "\n-----"));
            }
            if (result.numTimeouts() > 0) {
                out(">>>>> "+result.getTimeouts().size()+" timeouts");
            }
        }
        int i=1;
        OutputStream out;
        if (options.hasOutfile()) {
            out = new TeeOutputStream(new FileOutputStream(options.getOutfile()), System.out);
        } else {
            out = System.out;
        }
        @Cleanup final Writer w = new OutputStreamWriter(out);
        for (Set<String> dup : visitor.getHash().values().stream().filter(v -> v.size() > 1).collect(toList())) {
            w.write("\n----- dup#" + (i++) + ": \n");
            w.write(StringUtil.toString(dup, "\n"));
        }
    }

}
