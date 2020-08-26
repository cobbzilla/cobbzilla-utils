package org.cobbzilla.util.io.multi;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.regex.MultiUnderflowException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j @Accessors(chain=true)
public class MultiUnderflowHandler {

    private final MultiUnderflowHandlerMonitor monitor;

    public MultiUnderflowHandler (MultiUnderflowHandlerMonitor m) {
        this.monitor = m;
        m.start();
    }

    public MultiUnderflowHandler () { this(MultiUnderflowHandlerMonitor.DEFAULT_UNDERFLOW_MONITOR); }

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
            monitor.register(this);
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
            monitor.register(this);
        }
        firstUnderflow = 0;
        underflowSleep = minUnderflowSleep;
    }

    public void close() {
        if (!closed()) {
            closed.set(true);
            if (log.isDebugEnabled()) log.debug(handlerName + ": closing");
            monitor.unregister(this);
        }
    }

}
