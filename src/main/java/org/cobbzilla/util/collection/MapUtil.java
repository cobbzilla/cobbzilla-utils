package org.cobbzilla.util.collection;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;

public class MapUtil {

    public static final TypeReference<HashMap<String, Object>> JSON_STRING_OBJECT_MAP = new TypeReference<HashMap<String, Object>>() {};
    public static final TypeReference<HashMap<String, String>> JSON_STRING_STRING_MAP = new TypeReference<HashMap<String, String>>() {};

    public static Map<String, String> toMap (Properties props) {
        if (props == null || props.isEmpty()) return Collections.emptyMap();
        final Map<String, String> map = new LinkedHashMap<>(props.size());
        for (String name : props.stringPropertyNames()) map.put(name, props.getProperty(name));
        return map;
    }

    public static <K, V> boolean deepEquals (Map<K, V>  m1, Map<K, V>  m2) {
        if (m1 == null) return m2 == null;
        if (m2 == null) return false;
        if (m1.size() != m2.size()) return false;
        final Set<Map.Entry<K, V>> set = m1.entrySet();
        for (Map.Entry<K, V>  e : set) {
            V m1v = e.getValue();
            V m2v = m2.get(e.getKey());
            if (m2v == null) return false;
            if ((m1v instanceof Map && !deepEquals((Map<K,V>) m1v, (Map<K,V>) m2v)) || !m1v.equals(m2v)) {
                return false;
            }
        }
        return true;
    }

    public static <K, V> int deepHash(Map<K, V> m) {
        int hash = 0;
        for (Map.Entry<K, V>  e : m.entrySet()) {
            hash = (31 * hash) + e.getKey().hashCode() + (31 * e.getValue().hashCode());
        }
        return hash;
    }
}
