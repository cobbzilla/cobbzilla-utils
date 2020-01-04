package org.cobbzilla.util.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.function.Function;

@Slf4j
public class BackgroundRefreshable<T> extends BackgroundRefreshingReference<T> {

    @Getter private final String name;
    @Getter private final long timeout;
    @Getter private final Callable<T> refresher;
    @Getter private final Function<Exception, T> errorRefreshing;

    public BackgroundRefreshable(String name, long timeout, Callable<T> refresher) { this(name, timeout, refresher, e -> null); }

    public BackgroundRefreshable(String name, long timeout, Callable<T> refresher, Function<Exception, T> errorRefreshing) {
        this.name = name;
        this.timeout = timeout;
        this.refresher = refresher;
        this.errorRefreshing = errorRefreshing;
    }

    @Override public T refresh() { return Refreshable.refresh(name, refresher, errorRefreshing); }

}
