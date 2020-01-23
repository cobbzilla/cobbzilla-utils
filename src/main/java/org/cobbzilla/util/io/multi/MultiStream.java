package org.cobbzilla.util.io.multi;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MultiStream extends InputStream {

    private final List<InputStream> streams = new ArrayList<>();
    private InputStream currentStream;
    private int streamIndex = 0;
    private boolean endOfStreams = false;

    public MultiStream (InputStream r, boolean last) {
        if (last) {
            addLastStream(r);
        } else {
            addStream(r);
        }
        currentStream = r;
    }

    public MultiStream (InputStream r) { this(r, false); }

    protected int getEndOfStreamMarker() { return 0; }

    public void addStream (InputStream in) {
        if (endOfStreams) {
            log.warn("addStream: endOfStreams is true, not adding InputStream");
        } else {
            streams.add(in);
        }
    }

    public void addLastStream (InputStream in) {
        addStream(in);
        endOfStreams = true;
    }

    @Override public int read() throws IOException {
        int val = currentStream.read();
        if (val == -1) {
            if (streamIndex == streams.size()-1) {
                return endOfStreams ? -1 : getEndOfStreamMarker();
            }
            currentStream.close();
            streamIndex++;
            currentStream = streams.get(streamIndex);
            return read();
        }
        return val;
    }

    @Override public int read(byte[] buf, int off, int len) throws IOException {
        int count = currentStream.read(buf, off, len);
        if (count == -1) {
            if (streamIndex == streams.size()-1) {
                return endOfStreams ? -1 : getEndOfStreamMarker();
            }
            currentStream.close();
            streamIndex++;
            currentStream = streams.get(streamIndex);
            return read(buf, off, len);
        }
        return count;
    }

    @Override public void close() throws IOException {
        if (currentStream != null) currentStream.close();
    }

}
