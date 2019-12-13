package org.cobbzilla.util.handlebars;

import com.github.jknack.handlebars.Handlebars;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.converter.xhtml.XHTMLConverter;
import org.apache.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.cobbzilla.util.http.HtmlScreenCapture;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.xml.TidyHandlebarsSpanMerger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import static org.cobbzilla.util.io.FileUtil.temp;
import static org.cobbzilla.util.xml.TidyUtil.tidy;

@Slf4j
public class WordDocxMerger {

    public static File merge(InputStream in,
                             Map<String, Object> context,
                             HtmlScreenCapture capture,
                             Handlebars handlebars) throws Exception {

        // convert to HTML
        final XWPFDocument document = new XWPFDocument(in);
        final File mergedHtml = temp(".html");
        try (OutputStream out = new FileOutputStream(mergedHtml)) {
            final XHTMLOptions options = XHTMLOptions.create().setIgnoreStylesIfUnused(true);
            XHTMLConverter.getInstance().convert(document, out, options);
        }

        // - tidy HTML file
        // - merge consecutive <span> tags (which might occur in the middle of a {{variable}})
        // - replace HTML-entities encoded within handlebars templates (for example, convert &lsquo; and &rsquo; to single-quote char)
        // - apply Handlebars
        String tidyHtml = tidy(mergedHtml, TidyHandlebarsSpanMerger.instance);
        tidyHtml = TidyHandlebarsSpanMerger.scrubHandlebars(tidyHtml);
        FileUtil.toFile(mergedHtml, HandlebarsUtil.apply(handlebars, tidyHtml, context));

        // convert HTML -> PDF
        final File pdfOutput = temp(".pdf");
        capture.capture(mergedHtml, pdfOutput);

        return pdfOutput;
    }

}
