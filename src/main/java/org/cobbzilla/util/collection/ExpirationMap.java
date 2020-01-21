package org.cobbzilla.util.collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@Accessors(chain=true)
public class ExpirationMap<K, V> implements Map<K, V> {

    private final Map<K, ExpirationMapEntry<V>> map;

    @Getter @Setter private long expiration = TimeUnit.HOURS.toMillis(1);
    @Getter @Setter private long maxExpiration = TimeUnit.HOURS.toMillis(2);
    @Getter @Setter private long cleanInterval = TimeUnit.HOURS.toMillis(4);

    public ExpirationMap<K, V> setExpirations(long val) {
        this.expiration = this.maxExpiration = this.cleanInterval = val;
        return this;
    }

    private long lastCleaned = 0;

    public ExpirationMap() {
        this.map = new ConcurrentHashMap<>();
    }
    public ExpirationMap(long val) { this(); setExpirations(val); }

    @Accessors(chain=true)
    private class ExpirationMapEntry<VAL> {
        public final VAL value;
        public volatile long ctime = now();
        public volatile long atime = now();
        public ExpirationMapEntry(VAL value) { this.value = value; }

        public ExpirationMapEntry<VAL> touch() { atime = now(); return this; }
        public boolean expired() { return now() > ctime+maxExpiration || now() > atime+expiration; }
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
        if (lastCleaned+cleanInterval > now()) cleanExpired();
        final ExpirationMapEntry<V> value = map.get(key);
        return value == null || value.expired() ? null : value.touch().value;
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

    @Override public V putIfAbsent(K key, V value) {
        if (lastCleaned+cleanInterval > now()) cleanExpired();
        final ExpirationMapEntry<V> val = map.putIfAbsent(key, new ExpirationMapEntry<>(value));
        return val == null ? null : val.value;
    }

    @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (lastCleaned+cleanInterval > now()) cleanExpired();
        return map.computeIfAbsent(key, k -> new ExpirationMapEntry<>(mappingFunction.apply(k))).value;
    }

    @Override public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (lastCleaned+cleanInterval > now()) cleanExpired();
        final ExpirationMapEntry<V> found = map.computeIfPresent(key, (k, vExpirationMapEntry) -> new ExpirationMapEntry<>(remappingFunction.apply(k, vExpirationMapEntry.value)));
        return found == null ? null : found.value;
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
