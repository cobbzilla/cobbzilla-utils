package org.cobbzilla.util.io.main;

import org.cobbzilla.util.main.BaseMain;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.Decompressors.unroll;

public class UnrollMain extends BaseMain<UnrollOptions> {

    public static void main (String[] args) { main(UnrollMain.class, args); }

    @Override protected void run() throws Exception {
        out(abs(unroll(getOptions().getFile())));
    }

}
