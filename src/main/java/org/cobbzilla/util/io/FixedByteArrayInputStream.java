package org.cobbzilla.util.io;

import lombok.Getter;

import java.io.ByteArrayInputStream;

public class FixedByteArrayInputStream extends ByteArrayInputStream implements FixedSizeInputStream {

    @Getter private final int size;

    public FixedByteArrayInputStream(byte[] buf) {
        super(buf);
        size = buf.length;
    }

    public FixedByteArrayInputStream(byte[] buf, int offset, int length) {
        super(buf, offset, length);
        size = length;
    }
}
