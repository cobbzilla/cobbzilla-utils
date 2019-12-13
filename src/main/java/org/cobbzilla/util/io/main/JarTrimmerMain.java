package org.cobbzilla.util.io.main;

import org.cobbzilla.util.io.JarTrimmer;
import org.cobbzilla.util.main.BaseMain;

public class JarTrimmerMain extends BaseMain<JarTrimmerOptions> {

    public static void main (String[] args) { main(JarTrimmerMain.class, args); }

    @Override protected void run() throws Exception {
        final JarTrimmerOptions opts = getOptions();
        final JarTrimmer trimmer = new JarTrimmer();
        trimmer.trim(opts.getConfig());
    }

}
