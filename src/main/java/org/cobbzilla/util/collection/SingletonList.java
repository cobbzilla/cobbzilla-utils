package org.cobbzilla.util.collection;

import java.util.ArrayList;
import java.util.Collection;

public class SingletonList<E> extends ArrayList<E> {

    public SingletonList (E element) { super.add(element); }

    @Override public E set(int index, E element) { throw unsupported(); }
    @Override public boolean add(E e) { throw unsupported(); }
    @Override public void add(int index, E element) { throw unsupported(); }
    @Override public E remove(int index) { throw unsupported(); }
    @Override public boolean remove(Object o) { throw unsupported(); }
    @Override public void clear() { throw unsupported(); }
    @Override public boolean addAll(Collection<? extends E> c) { throw unsupported(); }
    @Override public boolean addAll(int index, Collection<? extends E> c) { throw unsupported(); }
    @Override protected void removeRange(int fromIndex, int toIndex) { throw unsupported(); }
    @Override public boolean removeAll(Collection<?> c) { throw unsupported(); }
    @Override public boolean retainAll(Collection<?> c) { throw unsupported(); }

    private UnsupportedOperationException unsupported () { return new UnsupportedOperationException("singleton list is immutable"); }

}
