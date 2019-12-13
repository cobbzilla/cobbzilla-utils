package org.cobbzilla.util.collection;

import java.util.Collection;
import java.util.HashSet;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

public class SingletonSet<E> extends HashSet<E> {

    public SingletonSet (E element) { super.add(element); }

    @Override public boolean add(E e) { return notSupported(); }
    @Override public boolean remove(Object o) { return notSupported();  }
    @Override public void clear() { notSupported();  }
    @Override public boolean addAll(Collection<? extends E> c) { return notSupported(); }
    @Override public boolean retainAll(Collection<?> c) { return notSupported(); }

}
