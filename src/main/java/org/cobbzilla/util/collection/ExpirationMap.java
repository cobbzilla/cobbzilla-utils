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
import static org.cobbzilla.util.time.TimeUtil.isTimestampInFuture;
import static org.cobbzilla.util.time.TimeUtil.isTimestampInPast;

@Accessors(chain=true)
public class ExpirationMap<K, V> implements Map<K, V> {

    private final Map<K, ExpirationMapEntry<V>> map;

    @Getter @Setter private long expiration = TimeUnit.HOURS.toMillis(1);
    @Getter @Setter private long maxExpiration = TimeUnit.HOURS.toMillis(2);
    @Getter @Setter private long cleanInterval = TimeUnit.HOURS.toMillis(4);

    public ExpirationMap<K, V> setExpirations(long val) {
        final var isNewExpirationShorter = val < this.expiration;
        this.expiration = this.maxExpiration = this.cleanInterval = val;
        if (isNewExpirationShorter) {
            final var updatedNextCleaningTime = now() + this.expiration;
            // the following calculation of nextCleaningTime is not really correct, but it doesn't really influence
            // anything much:
            if (this.nextCleaningTime > updatedNextCleaningTime) this.nextCleaningTime = updatedNextCleaningTime;
        }
        return this;
    }

    @Getter @Setter private ExpirationEvictionPolicy evictionPolicy = ExpirationEvictionPolicy.ctime_or_atime;
    private long nextCleaningTime = now();

    public ExpirationMap() { this.map = new ConcurrentHashMap<>(); }

    public ExpirationMap(long val) { this(); setExpirations(val); }

    public ExpirationMap(long val, ExpirationEvictionPolicy evictionPolicy) {
        this(val);
        this.evictionPolicy = evictionPolicy;
    }

    public ExpirationMap(ExpirationEvictionPolicy evictionPolicy) {
        this();
        this.evictionPolicy = evictionPolicy;
    }

    @Accessors(chain=true)
    private class ExpirationMapEntry<VAL> {
        public final VAL value;
        public volatile long ctime = now();
        public volatile long atime = now();
        public ExpirationMapEntry(VAL value) { this.value = value; }

        public ExpirationMapEntry<VAL> touch() { atime = now(); return this; }
        public boolean expired() {
            switch (evictionPolicy) {
                case ctime_or_atime: default: return now() > ctime+maxExpiration || now() > atime+expiration;
                case atime: return now() > atime+expiration;
                case ctime: return now() > ctime+expiration;
            }
        }
    }

    @Override public int size() {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        return map.size();
    }

    @Override public boolean isEmpty() {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        return map.isEmpty();
    }

    @Override public boolean containsKey(Object key) {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        return map.containsKey(key);
    }

    @Override public boolean containsValue(Object value) {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        for (ExpirationMapEntry<V> val : map.values()) {
            if (val.value == value) return true;
        }
        return false;
    }

    @Override public V get(Object key) {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        final ExpirationMapEntry<V> value = map.get(key);
        return value == null || value.expired() ? null : value.touch().value;
    }

    @Override public V put(K key, V value) {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
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

    @Override public Set<K> keySet() {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        return map.keySet();
    }

    @Override public Collection<V> values() {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        return map.values().stream().map(v -> v.value).collect(Collectors.toList());
    }

    @Override public V putIfAbsent(K key, V value) {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        final ExpirationMapEntry<V> val = map.putIfAbsent(key, new ExpirationMapEntry<>(value));
        return val == null ? null : val.value;
    }

    @Override public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        return map.computeIfAbsent(key, k -> new ExpirationMapEntry<>(mappingFunction.apply(k))).value;
    }

    @Override public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
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
        if (isTimestampInPast(nextCleaningTime)) cleanExpired();
        return map.entrySet().stream().map(e -> new EMEntry<>(e.getKey(), e.getValue().value)).collect(Collectors.toSet());
    }

    private synchronized void cleanExpired () {
        if (isTimestampInFuture(nextCleaningTime)) return;
        nextCleaningTime = now() + cleanInterval;
        final Set<K> toRemove = new HashSet<>();
        for (Map.Entry<K, ExpirationMapEntry<V>> entry : map.entrySet()) {
            if (entry.getValue().expired()) toRemove.add(entry.getKey());
        }
        for (K k : toRemove) map.remove(k);
    }
}
