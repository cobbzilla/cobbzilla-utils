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
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.daemon.ZillaRuntime.terminate;

@Slf4j
public class MultiUnderflowHandlerMonitor extends SimpleDaemon {

    public static final MultiUnderflowHandlerMonitor DEFAULT_UNDERFLOW_MONITOR = new MultiUnderflowHandlerMonitor();

    private final Map<String, MultiUnderflowHandler> handlers = new ConcurrentHashMap<>();

    @Getter @Setter private long checkInterval = SECONDS.toMillis(20);
    private static final long TERMINATE_TIMEOUT = SECONDS.toMillis(2);

    @Getter @Setter private Function<Thread, Boolean> terminateThreadFunc = null;

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
                if (terminateThreadFunc == null || terminateThreadFunc.apply(underflow.getThread())) {
                    if (log.isErrorEnabled()) log.error("process: underflow timed out, terminating: name=" + underflow.getHandlerName() + " thread=" + underflow.getThread());
                    terminate(underflow.getThread(), TERMINATE_TIMEOUT);
                } else {
                    if (log.isErrorEnabled()) log.error("process: underflow timed out, removing but NOT terminating: name=" + underflow.getHandlerName() + " thread=" + underflow.getThread());
                }
            }
        }
    }

    public void register(MultiUnderflowHandler underflow) { handlers.put(underflow.getId(), underflow); }

    public void unregister(MultiUnderflowHandler underflow) { handlers.remove(underflow.getId()); }

}
