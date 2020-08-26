package org.cobbzilla.util.io.multi;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.SimpleDaemon;
import org.cobbzilla.util.io.regex.MultiUnderflowException;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.daemon.ZillaRuntime.terminate;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j @Accessors(chain=true)
public class MultiUnderflowHandler extends SimpleDaemon {

    private final static Map<String, MultiUnderflowHandler> handlers = new ConcurrentHashMap<>();

    @Getter @Setter private static long checkInterval = SECONDS.toMillis(20);
    private static final long TERMINATE_TIMEOUT = SECONDS.toMillis(2);

    @Override protected long getSleepTime() { return checkInterval; }

    @Override protected void process() {
        if (log.isTraceEnabled()) log.trace("process: examining "+handlers.size()+" underflow handlers");
        for (Iterator<MultiUnderflowHandler> iter = handlers.values().iterator(); iter.hasNext(); ) {
            final MultiUnderflowHandler underflow = iter.next();
            if (underflow.closed()) {
                if (log.isDebugEnabled()) log.debug("process: removing closed handler: name="+underflow.getHandlerName()+" thread="+underflow.getThread());
                iter.remove();

            } else if (underflow.getLastRead() > 0 && !underflow.getThread().isAlive()) {
                if (log.isDebugEnabled()) log.debug("process: removing dead thread: name="+underflow.getHandlerName()+" thread="+underflow.getThread());
                iter.remove();

            } else if (now() - underflow.getLastRead() > underflow.getUnderflowTimeout()) {
                iter.remove();
                if (log.isErrorEnabled()) log.error("process: underflow timed out, terminating: name="+underflow.getHandlerName()+" thread="+underflow.getThread());
                terminate(underflow.getThread(), TERMINATE_TIMEOUT);
            }
        }
    }

    public MultiUnderflowHandler () { start(); }

    @Getter private final String id = randomUUID().toString();
    @Getter @Setter private String handlerName;
    @Getter private long minUnderflowSleep = 10;
    public MultiUnderflowHandler setMinUnderflowSleep(long s) { underflowSleep = minUnderflowSleep = s; return this; }

    @Getter @Setter private long maxUnderflowSleep = 500;

    @Getter @Setter private long lastRead = 0;
    @Getter @Setter private long firstUnderflow = 0;
    @Getter @Setter private long underflowTimeout = SECONDS.toMillis(60);
    private long underflowSleep = minUnderflowSleep;
    @Getter private Thread thread;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    public boolean closed () { return closed.get(); }

    public void handleUnderflow() throws IOException {
        if (thread == null) {
            thread = Thread.currentThread();
            lastRead = now();
            handlers.put(id, this);
        }
        if (firstUnderflow == 0) {
            if (log.isDebugEnabled()) log.debug(handlerName+": first data underflow");
            firstUnderflow = now();
        } else if (now() - firstUnderflow > underflowTimeout) {
            if (log.isErrorEnabled()) log.error(handlerName+": underflow timeout, throwing MultiUnderflowException");
            throw new MultiUnderflowException(handlerName);
        }
        if (log.isDebugEnabled()) log.debug(handlerName+": data underflow, sleeping for "+ underflowSleep);
        sleep(underflowSleep);
        underflowSleep *= 2;
        if (underflowSleep > maxUnderflowSleep) underflowSleep = maxUnderflowSleep;
    }

    public void handleSuccessfulRead() {
        lastRead = now();
        if (thread == null) {
            thread = Thread.currentThread();
            handlers.put(id, this);
        }
        firstUnderflow = 0;
        underflowSleep = minUnderflowSleep;
    }

    public void close() {
        if (!closed()) {
            closed.set(true);
            if (log.isDebugEnabled()) log.debug(handlerName + ": closing");
            handlers.remove(id);
        }
    }

}
