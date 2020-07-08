package org.cobbzilla.util.io;

import java.io.InputStream;

public class NullInputStream extends InputStream implements FixedSizeInputStream {

    public static final NullInputStream NULL_STREAM = new NullInputStream();

    @Override public int read() { return -1; }
    @Override public int read(byte[] b) { return -1; }
    @Override public int read(byte[] b, int off, int len) { return -1; }

    @Override public long size() { return 0; }

}
