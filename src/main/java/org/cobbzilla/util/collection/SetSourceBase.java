package org.cobbzilla.util.collection;

import lombok.ToString;
import org.cobbzilla.util.string.StringUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

public class SetSourceBase<T> implements CollectionSource<T> {

    private final AtomicReference<Collection<T>> values = new AtomicReference(new HashSet<T>());

    @Override public Collection<T> getValues() {
        synchronized (values) { return new HashSet<>(values.get()); }
    }

    @Override public void addValue (T val) {
        synchronized (values) { values.get().add(val); }
    }

    @Override public void addValues (Collection<T> vals) {
        synchronized (values) { values.get().addAll(vals); }
    }

    @Override public String toString () { return StringUtil.toString(values.get(), ", "); }

}
