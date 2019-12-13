package org.cobbzilla.util.collection.mappy;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.reflect.ReflectionUtil.getTypeParam;

/**
 * Mappy is a map of keys to collections of values. The collection type is configurable and there are several
 * subclasses available. See MappyList, MappySet, MappySortedSet, and MappyConcurrentSortedSet
 *
 * It can be viewed either as a mapping of K->V or as K->C->V
 *
 * Mappy objects are meant to be short-lived. While methods are generally thread-safe, the getter will create a new empty
 * collection every time a key is not found. So it makes a horrible cache. Mappy instances are best suited to be value
 * objects of limited scope.
 *
 * @param <K> key class
 * @param <V> value class
 * @param <C> collection class
 */
@Accessors(chain=true)
public abstract class Mappy<K, V, C extends Collection<V>> implements Map<K, V> {

    private final ConcurrentHashMap<K, C> map;

    @Getter(lazy=true) private final Class<C> valueClass = initValueClass();
    private Class<C> initValueClass() { return getTypeParam(getClass(), 2); }

    public Mappy ()         { map = new ConcurrentHashMap<>(); }
    public Mappy (int size) { map = new ConcurrentHashMap<>(size); }

    public Mappy(Map<K, Collection<V>> other) {
        this();
        for (Map.Entry<K, Collection<V>> entry : other.entrySet()) {
            putAll(entry.getKey(), entry.getValue());
        }
    }

    /**
     * For subclasses to override and provide their own collection types
     * @return A new (empty) instance of the collection type
     */
    protected abstract C newCollection();

    /**
     * @return the number of key mappings
     */
    @Override public int size() { return map.size(); }

    /**
     * @return the total number of values (may be higher than # of keys)
     */
    public int totalSize () {
        int count = 0;
        for (Collection<V> c : allValues()) count += c.size();
        return count;
    }

    /**
     * @return true if this Mappy contains no values. It may contain keys whose collections have no values.
     */
    @Override public boolean isEmpty() { return flatten().isEmpty(); }

    @Override public boolean containsKey(Object key) { return map.containsKey(key); }

    /**
     * @param value the value to check
     * @return true if the Mappy contains any collection that contains the value, which should be of type V
     */
    @Override public boolean containsValue(Object value) {
        for (C collection : allValues()) {
            //noinspection SuspiciousMethodCalls
            if (collection.contains(value)) return true;
        }
        return false;
    }

    /**
     * @param key the key to find
     * @return the first value in the collection for they key, or null if the collection is empty
     */
    @Override public V get(Object key) {
        final C collection = getAll((K) key);
        return collection.isEmpty() ? null : firstInCollection(collection);
    }

    protected V firstInCollection(C collection) { return collection.iterator().next(); }

    /**
     * Get the collection of values for a key. This method never returns null.
     * @param key the key to find
     * @return the collection of values for the key, which may be empty
     */
    public C getAll (K key) {
        C collection = map.get(key);
        if (collection == null) {
            collection = newCollection();
            map.put(key, collection);
        }
        return collection;
    }

    /**
     * Add a mapping.
     * @param key the key to add
     * @param value the value to add
     * @return the value passed in, if the map already contained the item. null otherwise.
     */
    @Override public V put(K key, V value) {
        V rval = null;
        synchronized (map) {
            C group = map.get(key);
            if (group == null) {
                group = newCollection();
                map.put(key, group);
            } else {
                rval = group.contains(value) ? value : null;
            }
            group.add(value);
        }
        return rval;
    }

    /**
     * Remove a key
     * @param key the key to remove
     * @return The first value in the collection that was referenced by the key
     */
    @Override public V remove(Object key) {
        final C group = map.remove(key);
        if (group == null || group.isEmpty()) return null; // empty case should never happen, but just in case
        return group.iterator().next();
    }

    /**
     * Put a bunch of stuff into the map
     * @param m mappings to add
     */
    @Override public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Put a bunch of stuff into the map
     * @param key the key to add
     * @param values the values to add to the key's collection
     */
    public void putAll(K key, Collection<V> values) {
        synchronized (map) {
            C collection = getAll(key);
            if (collection == null) collection = newCollection();
            collection.addAll(values);
            map.put(key, collection);
        }
    }

    /**
     * Erase the entire map.
     */
    @Override public void clear() { map.clear(); }

    @Override public Set<K> keySet() { return map.keySet(); }

    @Override public Collection<V> values() {
        final List<V> vals = new ArrayList<>();
        for (C collection : map.values()) vals.addAll(collection);
        return vals;
    }
    @Override public Set<Entry<K, V>> entrySet() {
        final Set<Entry<K, V>> entries = new HashSet<>();
        for (Entry<K, C> entry : map.entrySet()) {
            for (V item : entry.getValue()) {
                entries.add(new AbstractMap.SimpleEntry<K, V>(entry.getKey(), item));
            }
        }
        return entries;
    }

    public Collection<C> allValues() { return map.values(); }
    public Set<Entry<K, C>> allEntrySets() { return map.entrySet(); }

    public List<V> flatten() {
        final List<V> values = new ArrayList<>();
        for (C collection : allValues()) values.addAll(collection);
        return values;
    }

    public List<V> flatten(Collection<V> values) {
        for (C collection : allValues()) values.addAll(collection);
        return new ArrayList<>(values);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Mappy other = (Mappy) o;

        if (totalSize() != other.totalSize()) return false;

        for (K key : keySet()) {
            if (!other.containsKey(key)) return false;
            final Collection otherValues = other.getAll(key);
            final Collection thisValues = getAll(key);
            if (otherValues.size() != thisValues.size()) return false;
            for (Object value : thisValues) {
                if (!otherValues.contains(value)) return false;
            }
        }
        return true;
    }

    @Override public int hashCode() {
        int result = Integer.valueOf(totalSize()).hashCode();
        result = 31 * result + (valueClass != null ? valueClass.hashCode() : 0);
        for (K key : keySet()) {
            result = 31 * result + (key.hashCode() + 13);
            for (V value : getAll(key)) {
                result = 31 * result + (value == null ? 0 : value.hashCode());
            }
        }
        return result;
    }

    @Override public String toString() {
        final StringBuilder b = new StringBuilder();
        for (K key : keySet()) {
            if (b.length() > 0) b.append(" | ");
            b.append(key).append("->(").append(StringUtil.toString(getAll(key), ", ")).append(")");
        }
        return "{"+b.toString()+"}";
    }

    public Map<K, C> toMap() {
        final HashMap<K, C> m = new HashMap<>();
        for (K key : keySet()) m.put(key, getAll(key));
        return m;
    }
}
