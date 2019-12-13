package org.cobbzilla.util.collection;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

/**
 * A handy utility for creating and initializing Maps in a single statement.
 * @author Jonathan Cobb.
 */
public class MapBuilder {

    /**
     * Most common create/init case. Usage:
     *
     *   Map<String, Boolean> myPremadeMap = MapBuilder.build(new Object[][]{
     *     { "a", true }, { "b", false }, { "c", true }, { "d", true },
     *     { "e", "yes, still dangerous but at least it's not an anonymous class" }
     *   });
     *
     * If your keys and values are of the same type, it will even be typesafe:
     *   Map<String, String> someProperties = MapBuilder.build(new String[][]{
     *       {"propA", "valueA" }, { "propB", "valueB" }
     *   });
     *
     * @param values [x][2] array. items at [x][0] are keys and [x][1] are values.
     * @return a LinkedHashMap (to preserve order of declaration) with the "values" mappings
     */
    public static <K,V> Map<K,V> build(Object[][] values) {
        return build((Map<K,V>) new LinkedHashMap<>(), values);
    }

    /**
     * Usage:
     *  Map<K,V> myMap = MapBuilder.build(new MyMapClass(options),
     *                                    new Object[][]{ {k,v}, {k,v}, ... });
     * @param map add key/value pairs to this map
     * @return the map passed in, now containing new "values" mappings
     */
    public static <K,V> Map<K,V> build(Map<K,V> map, Object[][] values) {
        for (Object[] value : values) {
            map.put((K) value[0], (V) value[1]);
        }
        return map;
    }

    /** Same as above, for single-value maps */
    public static <K,V> Map<K,V> build(Map<K,V> map, K key, V value) {
        return build(map, new Object[][]{{key,value}});
    }

    /**
     * Usage:
     *  Map<K,V> myMap = MapBuilder.build(MyMapClass.class, new Object[][]{ {k,v}, {k,v}, ... });
     * @param mapClass a Class that implements Map
     * @return the map passed in, now containing new "values" mappings
     */
    public static <K,V> Map<K,V> build(Class<? extends Map<K,V>> mapClass, Object[][] values) {
        return build(instantiate(mapClass), values);
    }

    /** Usage: Map<K,V> myMap = MapBuilder.build(key, value); */
    public static <K,V> Map<K, V> build(K key, V value) {
        Map<K,V> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

}