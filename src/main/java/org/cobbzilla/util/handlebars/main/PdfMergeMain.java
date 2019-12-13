package org.cobbzilla.util.handlebars.main;

import com.github.jknack.handlebars.Handlebars;
import lombok.Cleanup;
import lombok.Getter;
import org.cobbzilla.util.error.GeneralErrorHandler;
import org.cobbzilla.util.handlebars.PdfMerger;
import org.cobbzilla.util.main.BaseMain;
import org.cobbzilla.util.string.StringUtil;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;

public class PdfMergeMain extends BaseMain<PdfMergeOptions> {

    public static void main (String[] args) { main(PdfMergeMain.class, args); }

    @Getter protected Handlebars handlebars;

    @Override protected void run() throws Exception {
        final PdfMergeOptions options = getOptions();

        final List<String> errors = new ArrayList<>();
        PdfMerger.setErrorHandler(new GeneralErrorHandler() {
            @Override public <T> T handleError(String message) { errors.add(message); return null; }
            @Override public <T> T handleError(String message, Exception e) { return handleError(message+": "+e.getClass().getSimpleName()+": "+e.getMessage()); }
            @Override public <T> T handleError(List<String> validationErrors) { errors.addAll(validationErrors); return null; }
        });
        @Cleanup final InputStream in = options.getInputStream();
        try {
            if (options.hasOutfile()) {
                final File outfile = options.getOutfile();
                PdfMerger.merge(in, outfile, options.getContext(), getHandlebars());
                out(abs(outfile));

            } else {
                final File output = PdfMerger.merge(in, options.getContext(), getHandlebars());
                out(abs(output));
            }
        } catch (Exception e) {
            err("Unexpected exception merging PDF: "+e.getClass().getSimpleName()+": "+e.getMessage());
        }
        if (!empty(errors)) {
            err(errors.size()+" error"+(errors.size() > 1 ? "s" : "")+" found when merging PDF:\n"+ StringUtil.toString(errors, "\n"));
        }
    }

}
