package org.cobbzilla.util.collection;

import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class CaseInsensitiveStringKeyMap<V> extends ConcurrentHashMap<String, V> {

    private ConcurrentHashMap<String, String> origKeys = new ConcurrentHashMap<>();

    public String key(Object key) { return key == null ? null : key.toString().toLowerCase(); }

    @Override public KeySetView<String, V> keySet() { return super.keySet(); }

    @Override public Enumeration<String> keys() { return Collections.enumeration(origKeys.values()); }

    @Override public V get(Object key) { return super.get(key(key)); }

    @Override public boolean containsKey(Object key) { return super.containsKey(key(key)); }

    @Override public V put(String key, V value) {
        final String ciKey = key(key);
        origKeys.put(ciKey, key);
        return super.put(ciKey, value);
    }

    @Override public V putIfAbsent(String key, V value) {
        final String ciKey = key(key);
        origKeys.putIfAbsent(ciKey, key);
        return super.putIfAbsent(ciKey, value);
    }

    @Override public V remove(Object key) {
        final String ciKey = key(key);
        origKeys.remove(ciKey);
        return super.remove(ciKey);
    }

    @Override public boolean remove(Object key, Object value) {
        final String ciKey = key(key);
        origKeys.remove(ciKey, value);
        return super.remove(ciKey, value);
    }

    @Override public boolean replace(String key, V oldValue, V newValue) {
        final String ciKey = key(key);
        return super.replace(ciKey, oldValue, newValue);
    }

    @Override public V replace(String key, V value) {
        final String ciKey = key(key);
        return super.replace(ciKey, value);
    }

}
