package org.cobbzilla.util.handlebars.main;

import lombok.Cleanup;
import org.cobbzilla.util.handlebars.PdfMerger;
import org.cobbzilla.util.main.BaseMain;

import java.io.OutputStream;

public class PdfConcatMain extends BaseMain<PdfConcatOptions> {

    public static void main (String[] args) { main(PdfConcatMain.class, args); }

    @Override protected void run() throws Exception {
        final PdfConcatOptions options = getOptions();
        @Cleanup final OutputStream out = options.getOut();
        PdfMerger.concatenate(options.getInfiles(), out, options.getMaxMemory(), options.getMaxDisk());
        out("success");
    }

}
