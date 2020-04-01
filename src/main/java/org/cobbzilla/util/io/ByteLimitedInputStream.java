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
    @Getter private boolean eos = false;

    public double getPercentDone () { return ((double) count) / ((double) limit); }

    public ByteLimitedInputStream (InputStream in, long limit) {
        this.delegate = in;
        this.limit = limit;
    }

    @Override public int read(byte[] b) throws IOException {
        if (eosOrLimitExceeded() == -1) return -1;
        final int read = delegate.read(b);
        incr(read);
        return read;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        if (eosOrLimitExceeded() == -1) return -1;
        final int read = delegate.read(b, off, len);
        incr(read);
        return read;
    }

    @Override public int read() throws IOException {
        if (eosOrLimitExceeded() == -1) return -1;
        final int read = delegate.read();
        incr(read);
        return read;
    }

    public int eosOrLimitExceeded() throws IOException{
        if (count >= limit) {
            if (!eos) throw new IOException("cannot continue reading: stream is larger than " + limit + " bytes");
            return -1;
        }
        return 0;
    }

    public void incr(int read) {
        if (read == -1) {
            eos = true;
        } else {
            count += read;
        }
    }

}
