package org.cobbzilla.util.http;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;

@Slf4j
public class HtmlScreenCapture extends PhantomUtil {

    private static final long TIMEOUT = SECONDS.toMillis(60);

    public static final String SCRIPT = loadResourceAsStringOrDie(getPackagePath(HtmlScreenCapture.class)+"/html_screen_capture.js");

    public synchronized void capture (String url, File file) { capture(url, file, TIMEOUT); }

    public synchronized void capture (String url, File file, long timeout) {
        final String script = SCRIPT.replace("@@URL@@", url).replace("@@FILE@@", abs(file));
        try {
            @Cleanup final PhantomJSHandle handle = execJs(script);
            long start = now();
            while (file.length() == 0 && now() - start < timeout) sleep(200);
            if (file.length() == 0 && now() - start >= timeout) {
                sleep(5000);
                if (file.length() == 0) die("capture: after " + formatDuration(timeout) + " file was never written to: " + abs(file)+", handle="+handle);
            }
        } catch (Exception e) {
            die("capture: unexpected exception: "+e, e);
        }
    }

    public void capture (File in, File out) {
        try {
            capture(in.toURI().toString(), out);
        } catch (Exception e) {
            die("capture("+abs(in)+"): "+e, e);
        }
    }

}
