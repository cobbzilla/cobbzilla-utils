package org.cobbzilla.util.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Function;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;

@Slf4j
public class Refreshable<T> extends AutoRefreshingReference<T> {

    @Getter private final String name;
    @Getter private final long timeout;
    @Getter private final Callable<T> refresher;
    @Getter private final Function<Exception, T> errorRefreshing;

    public Refreshable(String name, long timeout, Callable<T> refresher) { this(name, timeout, refresher, e -> null); }

    public Refreshable(String name, long timeout, Callable<T> refresher, Function<Exception, T> errorRefreshing) {
        this.name = name;
        this.timeout = timeout;
        this.refresher = refresher;
        this.errorRefreshing = errorRefreshing;
    }

    @Override public T refresh() { return refresh(name, refresher, errorRefreshing); }

    public static <T> T refresh(String name, Callable<T> refresher, Function<Exception,T> errorRefreshing) {
        try {
            return refresher.call();
        } catch (Exception e) {
            final String msg = "refresh(" + name + "): " + shortError(e);
            log.error(msg);
            try {
                return errorRefreshing.apply(e);
            } catch (Exception ex) {
                return die("refresh("+name+") failed with: "+shortError(e)+", and then errorRefreshing failed with "+shortError(ex));
            }
        }
    }

}
