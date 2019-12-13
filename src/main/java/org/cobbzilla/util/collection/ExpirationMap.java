package org.cobbzilla.util.collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public class ExpirationMap<K, V> implements Map<K, V> {

    private final Map<K, ExpirationMapEntry<V>> map;
    private long expiration = TimeUnit.HOURS.toMillis(1);
    private long cleanInterval = TimeUnit.HOURS.toMillis(4);
    private long lastCleaned = 0;

    public ExpirationMap() {
        this.map = new ConcurrentHashMap<>();
    }

    public ExpirationMap(long expiration) {
        this.map = new ConcurrentHashMap<>();
        this.expiration = expiration;
    }

    public ExpirationMap(long expiration, long cleanInterval) {
        this(expiration);
        this.cleanInterval = cleanInterval;
    }

    public ExpirationMap(long expiration, long cleanInterval, int initialCapacity) {
        this.map = new ConcurrentHashMap<>(initialCapacity);
        this.expiration = expiration;
        this.cleanInterval = cleanInterval;
    }

    public ExpirationMap(long expiration, long cleanInterval, int initialCapacity, float loadFactor) {
        this.map = new ConcurrentHashMap<>(initialCapacity, loadFactor);
        this.expiration = expiration;
        this.cleanInterval = cleanInterval;
    }

    public ExpirationMap(long expiration, long cleanInterval, int initialCapacity, float loadFactor, int concurrencyLevel) {
        this.map = new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel);
        this.expiration = expiration;
        this.cleanInterval = cleanInterval;
    }

    @Accessors(chain=true)
    private class ExpirationMapEntry<VAL> {
        public final VAL value;
        public volatile long atime = now();
        public ExpirationMapEntry(VAL value) { this.value = value; }

        public VAL touch() { atime = now(); return value; }
        public boolean expired() { return now() > atime+expiration; }
    }

    @Override public int size() { return map.size(); }

    @Override public boolean isEmpty() { return map.isEmpty(); }

    @Override public boolean containsKey(Object key) { return map.containsKey(key); }

    @Override public boolean containsValue(Object value) {
        for (ExpirationMapEntry<V> val : map.values()) {
            if (val.value == value) return true;
        }
        return false;
    }

    @Override public V get(Object key) {
        final ExpirationMapEntry<V> value = map.get(key);
        return value == null ? null : value.touch();
    }

    @Override public V put(K key, V value) {
        if (lastCleaned+cleanInterval > now()) cleanExpired();
        final ExpirationMapEntry<V> previous = map.put(key, new ExpirationMapEntry<>(value));
        return previous == null ? null : previous.value;
    }

    @Override public V remove(Object key) {
        final ExpirationMapEntry<V> previous = map.remove(key);
        return previous == null ? null : previous.value;
    }

    @Override public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override public void clear() { map.clear(); }

    @Override public Set<K> keySet() { return map.keySet(); }

    @Override public Collection<V> values() {
        return map.values().stream().map(v -> v.value).collect(Collectors.toList());
    }

    @AllArgsConstructor
    private static class EMEntry<K, V> implements Entry<K, V> {
        @Getter private K key;
        @Getter private V value;
        @Override public V setValue(V value) { return notSupported("setValue"); }
    }

    @Override public Set<Entry<K, V>> entrySet() {
        return map.entrySet().stream().map(e -> new EMEntry<>(e.getKey(), e.getValue().value)).collect(Collectors.toSet());
    }

    private synchronized void cleanExpired () {
        if (lastCleaned+cleanInterval < now()) return;
        lastCleaned = now();
        final Set<K> toRemove = new HashSet<>();
        for (Map.Entry<K, ExpirationMapEntry<V>> entry : map.entrySet()) {
            if (entry.getValue().expired()) toRemove.add(entry.getKey());
        }
        for (K k : toRemove) map.remove(k);
    }
}
