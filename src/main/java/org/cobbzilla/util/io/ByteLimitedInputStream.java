package org.cobbzilla.util.io;

import lombok.Delegate;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;

public class ByteLimitedInputStream extends InputStream {

    @Delegate(excludes=BLISDelegateExcludes.class) private InputStream delegate;

    private interface BLISDelegateExcludes {
        int read(byte[] b) throws IOException;
        int read(byte[] b, int off, int len) throws IOException;
        int read() throws IOException;
    }

    @Getter private long count = 0;
    @Getter private long limit;

    public double getPercentDone () { return ((double) count) / ((double) limit); }

    public ByteLimitedInputStream (InputStream in, long limit) {
        this.delegate = in;
        this.limit = limit;
    }

    @Override public int read(byte[] b) throws IOException {
        if (count >= limit) return -1;
        final int read = delegate.read(b);
        if (read != -1) count += read;
        return read;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        if (count >= limit) return -1;
        final int read = delegate.read(b, off, len);
        if (read != -1) count += read;
        return read;
    }

    @Override public int read() throws IOException {
        if (count >= limit) return -1;
        final int read = delegate.read();
        if (read != -1) count++;
        return read;
    }

}
