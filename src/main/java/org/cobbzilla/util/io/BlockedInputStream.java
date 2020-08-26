package org.cobbzilla.util.io;

import java.io.IOException;
import java.io.InputStream;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.cobbzilla.util.system.Sleep.sleep;

public class BlockedInputStream extends InputStream {

    @Override public int read() throws IOException {
        sleep(DAYS.toMillis(100), "blocking");
        return -1;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        sleep(DAYS.toMillis(100), "blocking");
        return super.read(b, off, len);
    }

}
