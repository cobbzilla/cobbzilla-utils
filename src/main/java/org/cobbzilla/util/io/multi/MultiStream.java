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

    public int pendingStreamCount () { return streams.size() - streamIndex; }

    public MultiStream (InputStream r) { this(r, false); }

    @Override public String toString () {
        return "MultiStream{"+streams.size()+" streams, index="+streamIndex+", EOS="+endOfStreams+"}";
    }

    public void addStream (InputStream in) {
        if (endOfStreams) {
            log.warn("addStream: endOfStreams is true, not adding InputStream");
        } else {
            streams.add(in);
            if (log.isTraceEnabled()) log.trace("addStream: added stream ("+in.getClass().getSimpleName()+"). this="+this);
        }
    }

    public void addLastStream (InputStream in) {
        addStream(in);
        endOfStreams = true;
        if (log.isTraceEnabled()) log.trace("addLastStream: added last stream ("+in.getClass().getSimpleName()+"). this="+this);
    }

    @Override public int read() throws IOException {
        final int val = currentStream.read();
        if (val == -1) {
            if (streamIndex == streams.size()-1) {
                if (log.isTraceEnabled()) log.trace("read(byte): end of all streams? this="+this);
                return endOfStreams ? -1 : 0;
            }
            currentStream.close();
            streamIndex++;
            currentStream = streams.get(streamIndex);
            if (log.isTraceEnabled()) log.trace("read(byte): end of all stream, advanced to next stream ("+currentStream.getClass().getSimpleName()+"). this="+this);
            return read();

        } else {
            if (log.isTraceEnabled()) log.trace("read(byte): one byte read. this="+this);
        }
        return val;
    }

    @Override public int read(byte[] buf, int off, int len) throws IOException {
        if (log.isTraceEnabled()) log.trace("read(byte[]): trying to read "+len+" bytes. this="+this);
        final int count = currentStream.read(buf, off, len);
        if (count == -1) {
            if (streamIndex == streams.size()-1) {
                if (log.isTraceEnabled()) log.trace("read(byte[]): end of all streams? this="+this);
                return endOfStreams ? -1 : 0;
            }
            currentStream.close();
            streamIndex++;
            currentStream = streams.get(streamIndex);
            if (log.isTraceEnabled()) log.trace("read(byte[]): end of all stream, advanced to next stream ("+currentStream.getClass().getSimpleName()+"). this="+this);
            return read(buf, off, len);

        } else {
            if (log.isTraceEnabled()) log.trace("read(byte[]): "+count+" bytes read. this="+this);
        }
        return count;
    }

    @Override public void close() throws IOException {
        if (log.isTraceEnabled()) log.trace("close: closing current stream ("+(currentStream == null ? "null" : currentStream.getClass().getSimpleName())+"). this="+this);
        if (currentStream != null) currentStream.close();
    }

}
