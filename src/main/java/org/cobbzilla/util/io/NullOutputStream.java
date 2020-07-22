package org.cobbzilla.util.io;

import java.io.OutputStream;

public class NullOutputStream extends OutputStream {

    public static final NullOutputStream NULL_OUTPUT_STREAM = new NullOutputStream();

    @Override public void write(int b) {}
    @Override public void write(byte[] b) {}
    @Override public void write(byte[] b, int off, int len) {}
    @Override public void flush() {}
    @Override public void close() {}

}
