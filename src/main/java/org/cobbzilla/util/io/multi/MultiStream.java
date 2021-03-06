package org.cobbzilla.util.io.multi;

import lombok.Getter;
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

    private Integer markedStreamIndex = null;
    private int markReadLimit = 0;

    @Getter private final MultiUnderflowHandler underflow = new MultiUnderflowHandler();

    public MultiStream (InputStream r, boolean last) { this(r, last, "no-name"); }

    public MultiStream (InputStream r, boolean last, String name) {
        underflow.setHandlerName(name);
        if (last) {
            addLastStream(r);
        } else {
            addStream(r);
        }
        currentStream = r;
        if (log.isInfoEnabled()) log.info(logPrefix()+"created with initial stream="+r+", last="+last);
    }

    public MultiStream (InputStream r, String name) { this(r, false, name); }

    public MultiStream (InputStream r) { this(r, false); }

    @Override public boolean markSupported() { return currentStream.markSupported(); }

    @Override public synchronized void mark(int readlimit) {
        this.markReadLimit = readlimit;
        currentStream.mark(readlimit);
        markedStreamIndex = streamIndex;
    }

    @Override public synchronized void reset() throws IOException {
        if (markedStreamIndex == null) throw new IOException("cannot reset stream that was never marked");
        int marked = streamIndex;
        while (marked >= markedStreamIndex) {
            streams.get(marked).reset();
            marked--;
        }
        streamIndex = markedStreamIndex;
        currentStream = streams.get(streamIndex);
        markedStreamIndex = null;
    }

    public int pendingStreamCount () { return streams.size() - streamIndex; }

    public MultiStream setUnderflowTimeout(long timeout) { getUnderflow().setUnderflowTimeout(timeout); return this; }

    @Override public String toString () {
        return "MultiStream{name="+underflow.getHandlerName()+", "+streams.size()+" streams, index="+streamIndex+", EOS="+endOfStreams+"}";
    }

    private String logPrefix () { return this + ": "; }

    public void addStream (InputStream in) {
        if (endOfStreams) {
            if (log.isWarnEnabled()) log.warn(logPrefix()+"addStream: endOfStreams is true, not adding InputStream");
        } else {
            streams.add(in);
            if (log.isTraceEnabled()) log.trace(logPrefix()+"addStream: added stream ("+in.getClass().getSimpleName()+")");
        }
    }

    public void addLastStream (InputStream in) {
        addStream(in);
        endOfStreams = true;
        if (log.isTraceEnabled()) log.trace(logPrefix()+"addLastStream: added last stream ("+in.getClass().getSimpleName()+")");
    }

    @Override public int read() throws IOException {
        final int val = currentStream.read();
        if (val == -1) {
            if (streamIndex == streams.size()-1) {
                if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte): end of all streams? this="+this);
                if (endOfStreams) return -1;
                underflow.handleUnderflow();
                return 0;
            }
            if (markedStreamIndex == null) {
                currentStream.close();
            }
            streamIndex++;
            currentStream = streams.get(streamIndex);
            if (markedStreamIndex != null) currentStream.mark(markReadLimit);
            if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte): end of all stream, advanced to next stream ("+currentStream.getClass().getSimpleName()+")");
            return read();

        } else {
            if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte): one byte read");
        }
        underflow.handleSuccessfulRead();
        return val;
    }

    @Override public int read(byte[] buf, int off, int len) throws IOException {
        if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte[]): trying to read "+len+" bytes");
        final int count = currentStream.read(buf, off, len);
        if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte[]): trying to read "+count+" bytes");
        if (count == -1) {
            if (streamIndex == streams.size()-1) {
                if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte[]): end of all streams?");
                if (endOfStreams) return -1;
                underflow.handleUnderflow();
                return 0;
            }
            if (markedStreamIndex == null) {
                currentStream.close();
            }
            streamIndex++;
            currentStream = streams.get(streamIndex);
            if (markedStreamIndex != null) currentStream.mark(markReadLimit);
            if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte[]): end of all stream, advanced to next stream ("+currentStream.getClass().getSimpleName()+")");
            return read(buf, off, len);

        } else {
            if (log.isTraceEnabled()) log.trace(logPrefix()+"read(byte[]): "+count+" bytes read");
        }
        underflow.handleSuccessfulRead();
        return count;
    }

    @Override public void close() throws IOException {
        if (log.isInfoEnabled()) log.info(logPrefix()+"close: closing current stream ("+(currentStream == null ? "null" : currentStream.getClass().getSimpleName())+"). name="+underflow.getHandlerName());
        else if (log.isTraceEnabled()) log.trace(logPrefix()+"close: closing current stream ("+(currentStream == null ? "null" : currentStream.getClass().getSimpleName())+"). name="+underflow.getHandlerName());
        if (currentStream != null) currentStream.close();
        underflow.close();
    }

}
