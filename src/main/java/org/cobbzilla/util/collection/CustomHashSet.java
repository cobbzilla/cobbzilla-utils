package org.cobbzilla.util.collection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor @Accessors(chain=true)
public class CustomHashSet<E> implements Set<E> {

    public interface Hasher<E> { String hash (E thing); }

    @Getter @Setter private Class<E> elementClass;
    @Getter @Setter private Hasher hasher;

    private Map<String, E> map = new ConcurrentHashMap<>();

    public CustomHashSet(Class<E> clazz, Hasher<E> hasher, Collection<E> collection) {
        this(clazz, hasher);
        addAll(collection);
    }

    public CustomHashSet(Class<E> elementClass, Hasher<E> hasher) {
        this.elementClass = elementClass;
        this.hasher = hasher;
    }

    @Override public int size() { return map.size(); }

    @Override public boolean isEmpty() { return map.isEmpty(); }

    @Override public boolean contains(Object o) {
        if (o == null) return false;
        if (getElementClass().isAssignableFrom(o.getClass())) {
            return map.containsKey(hasher.hash(o));

        } else if (o instanceof String) {
            return map.containsKey(o);
        }
        return false;
    }

    @Override public Iterator<E> iterator() { return map.values().iterator(); }

    @Override public Object[] toArray() { return map.values().toArray(); }

    @Override public <T> T[] toArray(T[] a) { return (T[]) map.values().toArray(); }

    @Override public boolean add(E e) { return map.put(hasher.hash(e), e) == null; }

    public E find(E e) { return map.get(hasher.hash(e)); }

    @Override public boolean remove(Object o) {
        if (getElementClass().isAssignableFrom(o.getClass())) {
            return map.remove(hasher.hash(o)) != null;

        } else if (o instanceof String) {
            return map.remove(o) != null;
        }
        return false;
    }

    @Override public boolean containsAll(Collection<?> c) {
        for (Object o : c) if (!contains(o)) return false;
        return true;
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        boolean anyAdded = false;
        for (E o : c) if (!add(o)) anyAdded = true;
        return anyAdded;
    }

    @Override public boolean retainAll(Collection<?> c) {
        final Set<String> toRemove = new HashSet<>();
        for (Map.Entry<String, E> entry : map.entrySet()) {
            if (!c.contains(entry.getValue())) toRemove.add(entry.getKey());
        }
        for (String k : toRemove) remove(k);
        return !toRemove.isEmpty();
    }

    @Override public boolean removeAll(Collection<?> c) {
        boolean anyRemoved = false;
        for (Object o : c) if (map.remove(o) != null) anyRemoved = true;
        return anyRemoved;
    }

    @Override public void clear() { map.clear(); }

}
