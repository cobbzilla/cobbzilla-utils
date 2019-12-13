package org.cobbzilla.util.io.main;

import lombok.Cleanup;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.io.regex.RegexFilterReader;
import org.cobbzilla.util.io.regex.RegexStreamFilter;
import org.cobbzilla.util.main.BaseMain;

import java.io.OutputStreamWriter;

public class RegexFilterMain extends BaseMain<RegexFilterOptions> {

    public static void main (String[] args) { main(RegexFilterMain.class, args); }

    @Override protected void run() throws Exception {

        final RegexFilterOptions options = getOptions();

        final RegexStreamFilter filter = getDriver(options);
        filter.configure(options.getConfig());

        @Cleanup final RegexFilterReader reader = new RegexFilterReader(System.in, options.getBufferSize(), filter);
        @Cleanup final OutputStreamWriter out = new OutputStreamWriter(System.out);

        IOUtils.copyLarge(reader, out);
    }

    protected RegexStreamFilter getDriver(RegexFilterOptions options) { return options.getDriver(); }

}
