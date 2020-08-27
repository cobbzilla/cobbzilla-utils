package org.cobbzilla.util.io.multi;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.SimpleDaemon;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;

@Slf4j
public class MultiUnderflowHandlerMonitor extends SimpleDaemon {

    public static final MultiUnderflowHandlerMonitor DEFAULT_UNDERFLOW_MONITOR = new MultiUnderflowHandlerMonitor();

    private final Map<String, MultiUnderflowHandler> handlers = new ConcurrentHashMap<>();
    private final Map<Integer, MultiUnderflowHandler> threads = new ConcurrentHashMap<>();

    @Getter @Setter private long checkInterval = SECONDS.toMillis(20);
    @Getter @Setter private long terminateTimeout = SECONDS.toMillis(2);

    @Getter @Setter private Function<Thread, Boolean> terminateThreadFunc = null;

    @Override protected long getSleepTime() { return checkInterval; }

    @Override protected void process() {
        final String prefix = "process("+handlers.size()+" handlers): ";
        if (log.isTraceEnabled()) log.trace(prefix+"examining underflow handlers");
        for (Iterator<MultiUnderflowHandler> iter = handlers.values().iterator(); iter.hasNext(); ) {
            final MultiUnderflowHandler underflow = iter.next();
            if (underflow.closed()) {
                if (log.isDebugEnabled()) log.debug(prefix+"removing closed handler: name="+underflow.getHandlerName()+" thread="+underflow.getThread());
                iter.remove();

            } else if (underflow.getLastRead() > 0 && !underflow.getThread().isAlive()) {
                if (log.isDebugEnabled()) log.debug(prefix+"removing dead thread: name="+underflow.getHandlerName()+" thread="+underflow.getThread());
                iter.remove();

            } else if (now() - underflow.getLastRead() > underflow.getUnderflowTimeout()) {
                iter.remove();
                if (terminateThreadFunc == null || terminateThreadFunc.apply(underflow.getThread())) {
                    if (log.isErrorEnabled()) log.error(prefix+"underflow timed out, terminating: name=" + underflow.getHandlerName() + " thread=" + underflow.getThread());
                    terminateQuietly(underflow.getThread(), terminateTimeout, terminateThreadFunc);
                } else {
                    if (log.isErrorEnabled()) log.error(prefix+"underflow timed out, removing but NOT terminating: name=" + underflow.getHandlerName() + " thread=" + underflow.getThread());
                }
            }
        }
    }

    public void register(MultiUnderflowHandler underflow) {
        final int threadId = System.identityHashCode(underflow.getThread());
        final MultiUnderflowHandler existingHandlerForThread = threads.get(threadId);
        if (existingHandlerForThread != null) {
            if (log.isWarnEnabled()) log.warn("register: detected thread reuse ("+underflow.getThread()+") overwriting existing handler: "+existingHandlerForThread.getHandlerName());
            handlers.remove(existingHandlerForThread.getId());
        }
        threads.put(threadId, underflow);
        handlers.put(underflow.getId(), underflow);
    }

    public void unregister(MultiUnderflowHandler underflow) { handlers.remove(underflow.getId()); }

}
