package org.cobbzilla.util.io;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.Bytes;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.closeQuietly;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public class FilterInputStreamViaOutputStream extends PipedInputStream implements Runnable {

    private static final int DEFAULT_PIPE_BUFFER_SIZE = (int) (64 * Bytes.KB);
    private static final long THREAD_TERMINATE_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private InputStream in;
    private PipedOutputStream pipeOut;
    private OutputStream out;
    private Thread thread;

    public FilterInputStreamViaOutputStream(InputStream in, Class<? extends OutputStream> outStreamClass) {
        super(DEFAULT_PIPE_BUFFER_SIZE);
        this.in = in;
        try {
            this.pipeOut = new PipedOutputStream(this);
        } catch (Exception e) {
            die("FilterInputStreamViaOutputStream: error creating pipeOut: "+shortError(e));
        }
        this.out = instantiate(outStreamClass, this.pipeOut);
        start();
    }

    public FilterInputStreamViaOutputStream(InputStream in, Function<OutputStream, OutputStream> outFactory) {
        super(DEFAULT_PIPE_BUFFER_SIZE);
        this.in = in;
        try {
            this.pipeOut = new PipedOutputStream(this);
        } catch (Exception e) {
            die("FilterInputStreamViaOutputStream: error creating pipeOut: "+shortError(e));
        }
        try {
            this.out = outFactory.apply(this.pipeOut);
        } catch (Exception e) {
            die("FilterInputStreamViaOutputStream: error creating out: "+shortError(e));
        }
        start();
    }

    private void start() {
        this.thread = new Thread(this);
        this.thread.setName(getClass().getSimpleName()+"/"+System.identityHashCode(this));
        this.thread.setDaemon(true);
        this.thread.start();
    }

    @Override public void run() {
        try {
            int c;
            int counter = 0;
            while((c = in.read()) >= 0) {
                out.write(c);
                counter++;
            }
            out.flush();

        } catch (IOException e) {
            log.error("run: error copying bytes: "+shortError(e));
            throw new RuntimeException(e);

        } finally {
            closeQuietly(out);
        }
    }

    @Override public void close() {
        log.info("close called from "+stacktrace());
        try {
            super.close();
        } catch (Exception e) {
            log.warn("close: error closing pipeIn: "+shortError(e));
        }
        closeQuietly(in);
        closeQuietly(out);
        closeQuietly(pipeOut);
        if (this.thread.isAlive()) {
            background(() -> terminate(this.thread, THREAD_TERMINATE_TIMEOUT));
        }
    }
}
