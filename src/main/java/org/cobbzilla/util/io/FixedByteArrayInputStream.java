package org.cobbzilla.util.io;

import java.io.ByteArrayInputStream;

public class FixedByteArrayInputStream extends ByteArrayInputStream implements FixedSizeInputStream {

    private final int size;

    @Override public long size() { return size; }

    public FixedByteArrayInputStream(byte[] buf) {
        super(buf);
        size = buf.length;
    }

    public FixedByteArrayInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
        size = length;
    }
}
