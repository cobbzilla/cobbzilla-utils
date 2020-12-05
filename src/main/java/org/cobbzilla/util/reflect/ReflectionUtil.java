package org.cobbzilla.util.reflect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.cobbzilla.util.collection.ExpirationEvictionPolicy;
import org.cobbzilla.util.collection.ExpirationMap;
import org.cobbzilla.util.string.StringUtil;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;
import static org.cobbzilla.util.collection.ArrayUtil.arrayToString;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.string.StringUtil.uncapitalize;

/**
 * Handy tools for working quickly with reflection APIs, which tend to be verbose.
 */
@Slf4j
public class ReflectionUtil {

    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    public static final Class<?>[] SINGLE_STRING_ARG = {String.class};

    public static Boolean toBoolean(Object object) {
        if (object == null) return null;
        if (object instanceof Boolean) return (Boolean) object;
        if (object instanceof String) return Boolean.valueOf(object.toString());
        return null;
    }

    public static Boolean toBoolean(Object object, String field, boolean defaultValue) {
        final Boolean val = toBoolean(get(object, field));
        return val == null ? defaultValue : val;
    }

    public static Long toLong(Object object) {
        if (object == null) return null;
        if (object instanceof Number) return ((Number) object).longValue();
        if (object instanceof String) return Long.valueOf(object.toString());
        return null;
    }

    public static Integer toInteger(Object object) {
        if (object == null) return null;
        if (object instanceof Number) return ((Number) object).intValue();
        if (object instanceof String) return Integer.valueOf(object.toString());
        return null;
    }

    public static Integer toIntegerOrNull(Object object) {
        if (object == null) return null;
        if (object instanceof Number) return ((Number) object).intValue();
        if (object instanceof String) {
            try {
                return Integer.valueOf(object.toString());
            } catch (Exception e) {
                log.info("toIntegerOrNull("+object+"): "+e);
                return null;
            }
        }
        return null;
    }

    public static Short toShort(Object object) {
        if (object == null) return null;
        if (object instanceof Number) return ((Number) object).shortValue();
        if (object instanceof String) return Short.valueOf(object.toString());
        return null;
    }

    public static Float toFloat(Object object) {
        if (object == null) return null;
        if (object instanceof Number) return ((Number) object).floatValue();
        if (object instanceof String) return Float.valueOf(object.toString());
        return null;
    }

    public static Double toDouble(Object object) {
        if (object == null) return null;
        if (object instanceof Number) return ((Number) object).doubleValue();
        if (object instanceof String) return Double.valueOf(object.toString());
        return null;
    }

    public static BigDecimal toBigDecimal(Object object) {
        if (object == null) return null;
        if (object instanceof Double) return big((Double) object);
        if (object instanceof Float) return big((Float) object);
        if (object instanceof Number) return big(((Number) object).longValue());
        if (object instanceof String) return big(object.toString());
        return null;
    }

    private static final Map<String, Class> forNameCache = new ExpirationMap<>(100, MINUTES.toMillis(20), ExpirationEvictionPolicy.atime);

    /**
     * Do a Class.forName and only throw unchecked exceptions.
     * @param clazz full class name. May end in [] to indicate array class
     * @param <T> The class type
     * @return A Class&lt;clazz&gt; object
     */
    public static <T> Class<? extends T> forName(String clazz) {
        Class<? extends T> cached = forNameCache.get(clazz);
        if (cached == null) {
            if (empty(clazz)) return (Class<? extends T>) Object.class;
            if (clazz.endsWith("[]")) return arrayClass(forName(clazz.substring(0, clazz.length()-2)));
            try {
                cached = (Class<? extends T>) Class.forName(clazz);
                forNameCache.put(clazz, cached);

            } catch (ClassNotFoundException e) {
                switch (clazz) {
                    case "boolean": return (Class<? extends T>) boolean.class;
                    case "byte":    return (Class<? extends T>) byte.class;
                    case "short":   return (Class<? extends T>) short.class;
                    case "char":    return (Class<? extends T>) char.class;
                    case "int":     return (Class<? extends T>) int.class;
                    case "long":    return (Class<? extends T>) long.class;
                    case "float":   return (Class<? extends T>) float.class;
                    case "double":  return (Class<? extends T>) double.class;
                }
                return die("Class.forName("+clazz+") error: "+shortError(e));

            } catch (Exception e) {
                return die("Class.forName("+clazz+") error: "+shortError(e));
            }
        }
        return cached;
    }

    public static Collection<Class> forNames(String[] classNames) {
        final List<Class> list = new ArrayList<>();
        if (!empty(classNames)) for (String c : classNames) list.add(forName(c));
        return list;
    }

    public static <T> Class<? extends T> arrayClass (Class clazz) { return forName("[L"+clazz.getName()+";"); }

    /**
     * Create an instance of a class, only throwing unchecked exceptions. The class must have a default constructor.
     * @param clazz we will instantiate an object of this type
     * @param <T> The class type
     * @return An Object that is an instance of Class&lt;clazz&gt; object
     */
    public static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return die("Error instantiating "+clazz+": "+e, e);
        }
    }

    /**
     * Create an instance of a class based on a class name, only throwing unchecked exceptions. The class must have a default constructor.
     * @param clazz full class name
     * @param <T> The class type
     * @return An Object that is an instance of Class&lt;clazz&gt; object
     */
    public static <T> T instantiate(String clazz) {
        try {
            return (T) instantiate(forName(clazz));
        } catch (Exception e) {
            return die("instantiate("+clazz+"): "+e, e);
        }
    }

    private static final Map<Class, Map<Object, Enum>> enumCache = new ConcurrentHashMap<>(1000);

    /**
     * Create an instance of a class using the supplied argument to a matching single-argument constructor.
     * @param clazz The class to instantiate
     * @param argument The object that will be passed to a matching single-argument constructor
     * @param <T> Could be anything
     * @return A new instance of clazz, created using a constructor that matched argument's class.
     */
    public static <T> T instantiate(Class<T> clazz, Object argument) {
        Constructor<T> constructor = null;
        Class<?> tryClass = argument.getClass();
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean": return (T) Boolean.valueOf(argument.toString());
                case "byte":    return (T) Byte.valueOf(argument.toString());
                case "short":   return (T) Short.valueOf(argument.toString());
                case "char":    return (T) Character.valueOf(empty(argument) ? 0 : argument.toString().charAt(0));
                case "int":     return (T) Integer.valueOf(argument.toString());
                case "long":    return (T) Long.valueOf(argument.toString());
                case "float":   return (T) Float.valueOf(argument.toString());
                case "double":  return (T) Double.valueOf(argument.toString());
                default: return die("instantiate: unrecognized primitive type: "+clazz.getName());
            }
        }
        if (clazz.isEnum()) {
            return argument == null ? null : (T) enumCache
                    .computeIfAbsent(clazz, c -> new ConcurrentHashMap<>(50))
                    .computeIfAbsent(argument, e -> {
                        try {
                            final Method valueOf = clazz.getMethod("valueOf", SINGLE_STRING_ARG);
                            return (Enum) valueOf.invoke(null, new Object[]{argument.toString()});
                        } catch (Exception ex) {
                            return die("instantiate: error instantiating enum "+clazz.getName()+": "+e);
                        }
                    });
        }
        while (constructor == null) {
            try {
                constructor = clazz.getConstructor(tryClass);
            } catch (NoSuchMethodException e) {
                if (tryClass.equals(Object.class)) {
                    // try interfaces
                    for (Class<?> iface : argument.getClass().getInterfaces()) {
                        try {
                            constructor = clazz.getConstructor(iface);
                        } catch (NoSuchMethodException e2) {
                            // noop
                        }
                    }
                    break;
                } else {
                    tryClass = tryClass.getSuperclass();
                }
            }
        }
        if (constructor == null) {
            die("instantiate: no constructor could be found for class "+clazz.getName()+", argument type "+argument.getClass().getName());
        }
        try {
            return constructor.newInstance(argument);
        } catch (Exception e) {
            return die("instantiate("+clazz.getName()+", "+argument+"): "+e, e);
        }
    }

    /**
     * Create an instance of a class using the supplied argument to a matching single-argument constructor.
     * @param clazz The class to instantiate
     * @param arguments The objects that will be passed to a matching constructor
     * @param <T> Could be anything
     * @return A new instance of clazz, created using a constructor that matched argument's class.
     */
    public static <T> T instantiate(Class<T> clazz, Object... arguments) {
        try {
            for (Constructor constructor : clazz.getConstructors()) {
                final Class<?>[] cParams = constructor.getParameterTypes();
                if (cParams.length == arguments.length) {
                    boolean match = true;
                    for (int i=0; i<cParams.length; i++) {
                        if (!cParams[i].isAssignableFrom(arguments[i].getClass())) {
                            match = false; break;
                        }
                    }
                    if (match) return (T) constructor.newInstance(arguments);
                }
            }
            log.warn("instantiate("+clazz.getName()+"): no matching constructor found, trying with exact match (will probably fail), args="+ArrayUtils.toString(arguments));

            final Class<?>[] parameterTypes = new Class[arguments.length];
            for (int i=0; i<arguments.length; i++) {
                parameterTypes[i] = getSimpleClass(arguments[i]);
            }
            return clazz.getConstructor(parameterTypes).newInstance(arguments);

        } catch (Exception e) {
            return die("instantiate("+clazz.getName()+", "+Arrays.toString(arguments)+"): "+e, e);
        }
    }

    public static Class<?> getSimpleClass(Object argument) {
        Class<?> argClass = argument.getClass();
        final int enhancePos = argClass.getName().indexOf("$$Enhance");
        if (enhancePos != -1) {
            argClass = forName(argClass.getName().substring(0, enhancePos));
        }
        return argClass;
    }

    public static String getSimpleClassName(Object argument) { return getSimpleClass(argument).getClass().getSimpleName(); }

    /**
     * Make a copy of the object, assuming its class has a copy constructor
     * @param thing The thing to copy
     * @param <T> Whatevs
     * @return A copy of the object, created using the thing's copy constructor
     */
    public static <T> T copy(T thing) { return (T) instantiate(thing.getClass(), thing); }

    /**
     * Mirror the object. Create a new instance and copy all fields
     * @param thing The thing to copy
     * @param <T> Whatevs
     * @return A mirror of the object, created using the thing's default constructor and copying all fields with 'copy'
     */
    public static <T> T mirror(T thing) {
        T copy = (T) instantiate(thing.getClass());
        copy(copy, thing);
        return copy;
    }

    public static Object invokeStatic(Method m, Object... values) {
        try {
            return m.invoke(null, values);
        } catch (Exception e) {
            return die("invokeStatic: "+m.getClass().getSimpleName()+"."+m.getName()+"("+arrayToString(values, ", ")+"): "+e, e);
        }
    }

    public static Field getDeclaredField(Class<?> clazz, String field) {
        try {
            return clazz.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            if (clazz.equals(Object.class)) {
                log.info("getDeclaredField: field not found "+clazz.getName()+"/"+field);
                return null;
            }
        }
        return getDeclaredField(clazz.getSuperclass(), field);
    }

    public static Field getField(Class<?> clazz, String field) {
        try {
            return clazz.getField(field);
        } catch (NoSuchFieldException e) {
            if (clazz.equals(Object.class)) {
                log.info("getField: field not found "+clazz.getName()+"/"+field);
                return null;
            }
        }
        return getDeclaredField(clazz.getSuperclass(), field);
    }

    public static <T> Method factoryMethod(Class<T> clazz, Object value) {
        // find a static method that takes the value and returns an instance of the class
        for (Method m : clazz.getMethods()) {
            if (m.getReturnType().equals(clazz)) {
                final Class<?>[] parameterTypes = m.getParameterTypes();
                if (parameterTypes != null && parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(value.getClass())) {
                    return m;
                }
            }
        }
        log.warn("factoryMethod: class "+clazz.getName()+" does not have static factory method that takes a String, returning null");
        return null;
    }

    public static <T> T callFactoryMethod(Class<T> clazz, Object value) {
        final Method m = factoryMethod(clazz, value);
        return m != null ? (T) invokeStatic(m, value) : null;
    }

    public static Object scrubStrings(Object thing, String[] fields) {
        if (empty(thing)) return thing;
        if (thing.getClass().isPrimitive()
                || thing instanceof String
                || thing instanceof Number
                || thing instanceof Enum) return thing;

        if (thing instanceof JsonNode) {
            if (thing instanceof ObjectNode) {
                for (String field : fields) {
                    if (((ObjectNode) thing).has(field)) {
                        ((ObjectNode) thing).remove(field);
                    }
                }
            } else if (thing instanceof ArrayNode) {
                ArrayNode arrayNode = (ArrayNode) thing;
                for (int i = 0; i < arrayNode.size(); i++) {
                    scrubStrings(arrayNode.get(i), fields);
                }
            }
        } else if (thing instanceof Map) {
            final Map map = (Map) thing;
            final Set toRemove = new HashSet();
            for (Object e : map.entrySet()) {
                Map.Entry entry = (Map.Entry) e;
                if (ArrayUtils.contains(fields, entry.getKey().toString())) {
                    toRemove.add(entry.getKey());
                } else {
                    scrubStrings(entry.getValue(), fields);
                }
            }
            for (Object key : toRemove) map.remove(key);

        } else if (Object[].class.isAssignableFrom(thing.getClass())) {
            if ( !((Object[]) thing)[0].getClass().isPrimitive() ) {
                for (Object obj : ((Object[]) thing)) {
                    scrubStrings(obj, fields);
                }
            }
        } else if (thing instanceof Collection) {
            for (Object obj : ((Collection) thing)) {
                scrubStrings(obj, fields);
            }
        } else {
            for (String field : ReflectionUtil.toMap(thing).keySet()) {
                final Object val = get(thing, field, null);
                if (val != null) {
                    if (ArrayUtils.contains(fields, field)) {
                        setNull(thing, field, String.class);
                    } else {
                        scrubStrings(val, fields);
                    }
                }
            }
        }
        return thing;
    }

    public static <T extends Annotation> List<String> fieldNamesWithAnnotation(String className, Class<T> aClass) {
        return fieldsWithAnnotation(className, aClass).stream().map(Field::getName).collect(Collectors.toList());
    }

    public static <T extends Annotation> List<String> fieldNamesWithAnnotation(Class clazz, Class<T> aClass) {
        return fieldsWithAnnotation(clazz, aClass).stream().map(Field::getName).collect(Collectors.toList());
    }

    public static <T extends Annotation> List<String> fieldNamesMatching(Class clazz, Function<Field, Boolean> matcher) {
        return fieldsMatching(clazz, matcher).stream().map(Field::getName).collect(Collectors.toList());
    }

    public static <T extends Annotation> List<Field> fieldsWithAnnotation(final Class clazz, final Class<T> aClass) {
        return fieldsMatching(clazz, f -> f.getAnnotation(aClass) != null);
    }

    public static <T extends Annotation> List<Field> fieldsWithAnnotation(String className, Class<T> aClass) {
        return fieldsWithAnnotation(forName(className), aClass);
    }

    private static final Map<String, List<Field>> _fwaCache = new ExpirationMap<>();
    public static <T extends Annotation> List<Field> fieldsMatching(final Class clazz, Function<Field, Boolean> matcher) {
        final String className = clazz.getName();
        return _fwaCache.computeIfAbsent(className+":"+matcher.hashCode(), k -> {
            final Set<Field> matches = new LinkedHashSet<>();
            Class c = clazz;
            while (!c.equals(Object.class)) {
                for (Field f : getAllFields(c)) {
                    if (matcher.apply(f)) matches.add(f);
                }
                c = c.getSuperclass();
            }
            return new ArrayList<>(matches);
        });
    }

    public static List<String> fieldNamesWithAnnotations(final Class clazz, Class<? extends Annotation> ...aClasses) {
        return fieldNamesMatching(clazz, f -> Arrays.stream(aClasses).anyMatch(c -> f.getAnnotation(c) != null));
    }

    private enum Accessor { get, set }

    /**
     * Copies fields from src to dest. Code is easier to read if this method is understdood to be like an assignment statement, dest = src
     *
     * We consider only 'getter' methods that meet the following criteria:
     *   (1) starts with "get"
     *   (2) takes zero arguments
     *   (3) has a return value
     *   (4) does not carry any annotation whose simple class name is "Transient"
     *
     * The value returned from the source getter will be copied to the destination (via setter), if a setter exists, and:
     * (1) No getter exists on the destination, or (2) the destination's getter returns a different value (.equals returns false)
     *
     * Getters that return null values on the source object will not be copied.
     *
     * @param dest destination object
     * @param src source object
     * @param <T> objects must share a type
     * @return count of fields copied
     */
    public static <T> int copy (T dest, T src) {
        return copy(dest, src, null, null);
    }

    /**
     * Same as copy(dest, src) but only named fields are copied
     * @param dest destination object
     * @param src source object
     * @param fields only fields with these names will be considered for copying
     * @param <T> objects must share a type
     * @return count of fields copied
     */
    public static <T> int copy (T dest, T src, String[] fields) {
        int copyCount = 0;
        if (fields != null) {
            for (String field : fields) {
                try {
                    final Object value = get(src, field, null);
                    if (value != null) {
                        set(dest, field, value);
                        copyCount++;
                    }
                } catch (Exception e) {
                    log.debug("copy: field=" + field + ": " + e);
                }
            }
        }
        return copyCount;
    }

    /**
     * Same as copy(dest, src) but only named fields are copied
     * @param dest destination object, or a Map<String, Object>
     * @param src source object
     * @param fields only fields with these names will be considered for copying
     * @param exclude fields with these names will NOT be considered for copying
     * @param <T> objects must share a type
     * @return count of fields copied
     */
    public static <T> int copy (T dest, T src, String[] fields, String[] exclude) {
        int copyCount = 0;
        final boolean isMap = dest instanceof Map;
        try {
            if (src instanceof Map) copyFromMap(dest, (Map<String, Object>) src, exclude);

            checkGetter:
            for (Method getter : src.getClass().getMethods()) {
                // only look for getters on the source object (methods with no arguments that have a return value)
                final Class<?>[] types = getter.getParameterTypes();
                if (types.length != 0) continue;
                if (getter.getReturnType().equals(Void.class)) continue;;

                // and it must be named appropriately
                final String fieldName = fieldName(getter.getName());
                if (fieldName == null || ArrayUtils.contains(exclude, fieldName)) continue;

                // if specific fields were given, it must be one of those
                if (fields != null && !ArrayUtils.contains(fields, fieldName)) continue;

                // getter must not be marked @Transient
                if (isIgnored(src, fieldName, getter)) continue;

                // what would the setter be called?
                final String setterName = setterForGetter(getter.getName());
                if (setterName == null) continue;

                // get the setter method on the destination object
                Method setter = null;
                if (!isMap) {
                    try {
                        setter = dest.getClass().getMethod(setterName, getter.getReturnType());
                    } catch (Exception e) {
                        log.debug("copy: setter not found: "+dest.getClass().getName()+"."+setterName);
                        continue;
                    }
                }

                // do not copy null fields (should this be configurable?)
                Object srcValue = null;
                try {
                    srcValue = getter.invoke(src);
                } catch (InvocationTargetException ite) {
                    log.debug("copy: error invoking getter for "+src.getClass().getName()+"."+fieldName+": "+shortError(ite));
                }
                if (srcValue == null) continue;

                // does the dest have a getter? if so grab the current value
                Object destValue = null;
                try {
                    if (isMap) {
                        destValue = ((Map) dest).get(fieldName);
                    } else {
                        destValue = getter.invoke(dest);
                    }
                } catch (Exception e) {
                    log.debug("copy: error calling getter "+dest.getClass().getName()+"."+fieldName+" on dest: "+e);
                }

                // copy the value from src to dest, if it's different
                if (!srcValue.equals(destValue)) {
                    if (isMap) {
                        ((Map) dest).put(fieldName, srcValue);
                    } else {
                        setter.invoke(dest, srcValue);
                    }
                    copyCount++;
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error copying "+dest.getClass().getSimpleName()+" from src="+src+": "+e, e);
        }
        return copyCount;
    }

    private static <T> boolean isIgnored(T o, String fieldName, Method getter) {
        Field field = null;
        try {
            field = o.getClass().getDeclaredField(fieldName);
        } catch (NoSuchFieldException ignored) {}
        return isIgnored(getter.getAnnotations()) || (field != null && isIgnored(field.getAnnotations()));
    }

    private static boolean isIgnored(Annotation[] annotations) {
        if (annotations != null) {
            for (Annotation a : annotations) {
                final Class<?>[] interfaces = a.getClass().getInterfaces();
                if (interfaces != null) {
                    for (Class<?> i : interfaces) {
                        if (i.getSimpleName().equals("Transient")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static String fieldName(String method) {
        if (method.startsWith("get")) return uncapitalize(method.substring(3));
        if (method.startsWith("set")) return uncapitalize(method.substring(3));
        if (method.startsWith("is")) return uncapitalize(method.substring(2));
        return null;
    }

    public static String setterForGetter(String getter) {
        if (getter.startsWith("get")) return "set"+getter.substring(3);
        if (getter.startsWith("is")) return "set"+getter.substring(2);
        return null;
    }

    /**
     * Call setters on an object based on keys and values in a Map
     * @param dest destination object
     * @param src map of field name -> value
     * @param <T> type of object
     * @return the destination object
     */
    public static <T> T copyFromMap (T dest, Map<String, Object> src) {
        return copyFromMap(dest, src, null);
    }

    public static <T> T copyFromMap (T dest, Map<String, Object> src, String[] exclude) {
        for (Map.Entry<String, Object> entry : src.entrySet()) {
            final String key = entry.getKey();
            if (exclude != null && ArrayUtils.contains(exclude, key)) continue;
            final Object value = entry.getValue();
            if (value != null && Map.class.isAssignableFrom(value.getClass())) {
                if (hasGetter(dest, key)) {
                    Map m = (Map) value;
                    if (m.isEmpty()) continue;
                    if (m.keySet().iterator().next().getClass().equals(String.class)) {
                        copyFromMap(get(dest, key), (Map<String, Object>) m);
                    } else {
                        log.info("copyFromMap: not recursively copying Map (has non-String keys): " + key);
                    }
                }
            } else {
                if (Map.class.isAssignableFrom(dest.getClass())) {// || dest.getClass().getName().equals(HashMap.class.getName())) {
                    ((Map) dest).put(key, value);
                } else {
                    if (hasSetter(dest, key, value.getClass())) {
                        set(dest, key, value);
                    } else {
                        final Class pc = getPrimitiveClass(value.getClass());
                        if (pc != null && hasSetter(dest, key, pc)) {
                            set(dest, key, value);
                        } else {
                            log.info("copyFromMap: skipping uncopyable property: "+key);
                        }
                    }
                }
            }
        }
        return dest;
    }

    public static Class getPrimitiveClass(Class<?> clazz) {
        if (clazz.isArray()) return arrayClass(getPrimitiveClass(clazz.getComponentType()));
        switch (clazz.getSimpleName()) {
            case "Long": return long.class;
            case "Integer": return int.class;
            case "Short": return short.class;
            case "Double": return double.class;
            case "Float": return float.class;
            case "Boolean": return boolean.class;
            case "Character": return char.class;
            default: return null;
        }
    }

    public static final String[] TO_MAP_STANDARD_EXCLUDES = {"declaringClass", "class"};

    /**
     * Make a copy of the object, assuming its class has a copy constructor
     * @param thing The thing to copy
     * @return A copy of the object, created using the thing's copy constructor
     */
    public static Map<String, Object> toMap(Object thing) { return toMap(thing, null, TO_MAP_STANDARD_EXCLUDES); }

    public static Map<String, Object> toMap(Object thing, String[] fields) { return toMap(thing, fields, TO_MAP_STANDARD_EXCLUDES); }

    public static Map<String, Object> toMap(Object thing, String[] fields, String[] exclude) {
        final Map<String, Object> map = new HashMap<>();
        copy(map, thing, fields, exclude);
        return map;
    }

    /**
     * Find the concrete class for the first declared parameterized class variable
     * @param clazz The class to search for parameterized types
     * @return The first concrete class for a parameterized type found in clazz
     */
    public static <T> Class<T> getFirstTypeParam(Class clazz) { return getTypeParam(clazz, 0); }

    private static final Map<String, Class> typeParamCache = new ExpirationMap<>(ExpirationEvictionPolicy.atime);

    public static <T> Class<T> getTypeParam(Class clazz, int index) {
        return (Class<T>) typeParamCache.computeIfAbsent(clazz.getName()+":"+index, k -> {
            Class check = clazz;
            while (check.getGenericSuperclass() == null || !(check.getGenericSuperclass() instanceof ParameterizedType)) {
                check = check.getSuperclass();
                if (check.equals(Object.class)) die("getTypeParam("+clazz.getName()+"): no type parameters found");
            }
            final ParameterizedType parameterizedType = (ParameterizedType) check.getGenericSuperclass();
            final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (index >= actualTypeArguments.length) die("getTypeParam("+clazz.getName()+"): "+actualTypeArguments.length+" type parameters found, index "+index+" out of bounds");
            if (actualTypeArguments[index] instanceof Class) return (Class) actualTypeArguments[index];
            if (actualTypeArguments[index] instanceof ParameterizedType) return (Class) ((ParameterizedType) actualTypeArguments[index]).getRawType();
            return ((Type) actualTypeArguments[index]).getClass();
        });
    }

    /**
     * Find the concrete class for a parameterized class variable.
     * @param clazz The class to start searching. Search will continue up through superclasses
     * @param impl The type (or a supertype) of the parameterized class variable
     * @return The first concrete class found that is assignable to an instance of impl
     */
    public static <T> Class<T> getFirstTypeParam(Class clazz, Class impl) {
        // todo: add a cache on this thing... could do wonders
        Class check = clazz;
        while (check != null && !check.equals(Object.class)) {
            Class superCheck = check;
            Type superType = superCheck.getGenericSuperclass();
            while (superType != null && !superType.equals(Object.class)) {
                if (superType instanceof ParameterizedType) {
                    final ParameterizedType ptype = (ParameterizedType) superType;
                    final Class<?> rawType = (Class<?>) ptype.getRawType();
                    if (impl.isAssignableFrom(rawType)) {
                        return (Class<T>) rawType;
                    }
                    for (Type t : ptype.getActualTypeArguments()) {
                        if (impl.isAssignableFrom((Class<?>) t)) {
                            return (Class<T>) t;
                        }
                    }

                } else if (superType instanceof Class) {
                    superType = ((Class) superType).getGenericSuperclass();
                }
            }
            check = check.getSuperclass();
        }
        return null;
    }

    /**
     * Call a getter. getXXX and isXXX will both be checked.
     * @param object the object to call get(field) on
     * @param field the field name
     * @return the value of the field
     * @throws IllegalArgumentException If no getter for the field exists
     */
    public static Object get(Object object, String field) {
        Object target = object;
        for (String token : field.split("\\.")) {
            if (target == null) return null;
            target = invoke_get(target, token);
        }
        return target;
    }

    public static <T> T get(Object object, String field, T defaultValue) {
        try {
            final Object val = get(object, field);
            return val == null ? defaultValue : (T) val;
        } catch (Exception e) {
            log.warn("get: "+e);
            return defaultValue;
        }
    }

    public static boolean isGetter(Method method) {
        return method.getName().startsWith("get") || method.getName().startsWith("is") && method.getParameters().length == 0;
    }

    public static boolean hasGetter(Object object, String field) {
        Object target = object;
        try {
            for (String token : field.split("\\.")) {
                final String methodName = getAccessorMethodName(Accessor.get, token);
                target = MethodUtils.invokeExactMethod(target, methodName, null);
            }
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static Class getterType(Object object, String field) {
        try {
            final Object o = get(object, field);
            if (o == null) return die("getterType: cannot determine field type, value was null");
            return o.getClass();

        } catch (Exception e) {
            return die("getterType: simple get failed: "+e, e);
        }
    }

    /**
     * Call a setter
     * @param object the object to call set(field) on
     * @param field the field name
     * @param value the value to set
     */
    public static void set(Object object, String field, Object value) {
        set(object, field, value, value == null ? null : value.getClass());
    }

    /**
     * Call a setter with a hint as to what the type should be
     * @param object the object to call set(field) on
     * @param field the field name
     * @param value the value to set
     * @param type type of the field
     */
    public static void set(Object object, String field, Object value, Class<?> type) {
        if (type != null) {
            if (value == null) {
                setNull(object, field, type);
                return;
            } else if (!type.isAssignableFrom(value.getClass())) {
                // if value is not assignable to type, then the type class should have a constructor for the value class
                value = instantiate(type, value);
            }
        }
        final String[] tokens = field.split("\\.");
        Object target = getTarget(object, tokens);
        if (target != null) invoke_set(target, tokens[tokens.length - 1], value);
    }

    public static void setNull(Object object, String field, Class type) {
        final String[] tokens = field.split("\\.");
        Object target = getTarget(object, tokens);
        if (target != null) invoke_set_null(target, tokens[tokens.length - 1], type);
    }

    private static Object getTarget(Object object, String[] tokens) {
        Object target = object;
        for (int i=0; i<tokens.length-1; i++) {
            target = invoke_get(target, tokens[i]);
            if (target == null) {
                log.warn("getTarget("+object+", "+Arrays.toString(tokens)+"): exiting early, null object found at token="+tokens[i]);
                return null;
            }
        }
        return target;
    }

    public static boolean hasSetter(Object object, String field, Class type) {
        Object target = object;
        final String[] tokens = field.split("\\.");
        try {
            for (int i=0; i<tokens.length-1; i++) {
                target = MethodUtils.invokeExactMethod(target, tokens[i], null);
            }

            target.getClass().getMethod(getAccessorMethodName(Accessor.set, tokens[tokens.length-1]), type);

        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static String getAccessorMethodName(Accessor accessor, String token) {
        return token.length() == 1 ? accessor.name() +token.toUpperCase() : accessor.name() + token.substring(0, 1).toUpperCase() + token.substring(1);
    }

    private static Object invoke_get(Object target, String token) {
        final String methodName = getAccessorMethodName(Accessor.get, token);
        try {
            target = MethodUtils.invokeMethod(target, methodName, null);
        } catch (Exception e) {
            final String isMethod = methodName.replaceFirst("get", "is");
            try {
                target = MethodUtils.invokeMethod(target, isMethod, null);
            } catch (Exception e2) {
                if (target instanceof Map) return ((Map) target).get(token);
                if (target instanceof ObjectNode) return ((ObjectNode) target).get(token);
                throw new IllegalArgumentException("Error calling "+methodName+" and "+isMethod+": "+e+", "+e2);
            }
        }
        return target;
    }

    private static final Map<String, Method> setterCache = new ConcurrentHashMap<>(5000);
    private static final Map<Class, Object[]> nullArgCache = new ConcurrentHashMap<>(5000);

    private static void invoke_set(Object target, String token, Object value) {
        final String cacheKey = target.getClass().getName()+"."+token+"."+(value == null ? "null" : value.getClass().getName());
        final Method method = setterCache.computeIfAbsent(cacheKey, s -> {
            final String methodName = getAccessorMethodName(Accessor.set, token);
            Method found = null;
            if (value == null) {
                // try to find a single-arg method named methodName...
                for (Method m : target.getClass().getMethods()) {
                    if (m.getName().equals(methodName) && m.getParameterTypes().length == 1) {
                        if (found != null) {
                            return die("invoke_set: value was null and multiple single-arg methods named " + methodName + " exist");
                        } else {
                            found = m;
                        }
                    }
                }
            } else {
                try {
                    found = MethodUtils.getMatchingAccessibleMethod(target.getClass(), methodName, new Class<?>[]{value.getClass()});
                } catch (Exception e) {
                    return die("Error calling " + methodName + ": " + e);
                }
            }
            return found != null ? found : die("invoke_set: no method " + methodName + " found on target: " + target);
        });
        if (value == null) {
            try {
                final Object[] nullArg = nullArgCache.computeIfAbsent(method.getParameterTypes()[0], type -> new Object[] {getNullArgument(type)});
                method.invoke(target, nullArg);
            } catch (Exception e) {
                die("Error calling " + method.getName() + " on target: " + target + " - " + e);
            }
        } else {
            try {
                MethodUtils.invokeMethod(target, method.getName(), value);
            } catch (Exception e) {
                die("Error calling " + method.getName() + ": " + e);
            }
        }
    }

    private static void invoke_set_null(Object target, String token, Class type) {
        final String methodName = getAccessorMethodName(Accessor.set, token);
        try {
            MethodUtils.invokeMethod(target, methodName, new Object[] {getNullArgument(type)}, new Class[] { type });
        } catch (Exception e) {
            die("Error calling "+methodName+": "+e);
        }
    }

    private static Object getNullArgument(Class clazz) {
        if (clazz.isPrimitive()) {
            switch (clazz.getName()) {
                case "boolean": return false;
                case "byte":    return (byte) 0;
                case "short":   return (short) 0;
                case "char":    return (char) 0;
                case "int":     return (int) 0;
                case "long":    return (long) 0;
                case "float":   return (float) 0;
                case "double":  return (double) 0;
                default: return die("getNullArgument: unrecognized primitive type: "+clazz.getName());
            }
        }
        return null;
    }

    // methods below forked from dropwizard-- https://github.com/codahale/dropwizard

    /**
     * Finds the type parameter for the given class.
     *
     * @param klass    a parameterized class
     * @return the class's type parameter
     */
    public static Class<?> getTypeParameter(Class<?> klass) {
        return getTypeParameter(klass, Object.class);
    }

    /**
     * Finds the type parameter for the given class which is assignable to the bound class.
     *
     * @param klass    a parameterized class
     * @param bound    the type bound
     * @param <T>      the type bound
     * @return the class's type parameter
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getTypeParameter(Class<?> klass, Class<? super T> bound) {
        Type t = checkNotNull(klass);
        while (t instanceof Class<?>) {
            t = ((Class<?>) t).getGenericSuperclass();
        }
        /* This is not guaranteed to work for all cases with convoluted piping
         * of type parameters: but it can at least resolve straight-forward
         * extension with single type parameter (as per [Issue-89]).
         * And when it fails to do that, will indicate with specific exception.
         */
        if (t instanceof ParameterizedType) {
            // should typically have one of type parameters (first one) that matches:
            for (Type param : ((ParameterizedType) t).getActualTypeArguments()) {
                if (param instanceof Class<?>) {
                    final Class<T> cls = determineClass(bound, param);
                    if (cls != null) { return cls; }
                }
                else if (param instanceof TypeVariable) {
                    for (Type paramBound : ((TypeVariable<?>) param).getBounds()) {
                        if (paramBound instanceof Class<?>) {
                            final Class<T> cls = determineClass(bound, paramBound);
                            if (cls != null) { return cls; }
                        }
                    }
                }
            }
        }
        return die("Cannot figure out type parameterization for " + klass.getName());
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> determineClass(Class<? super T> bound, Type candidate) {
        if (candidate instanceof Class<?>) {
            final Class<?> cls = (Class<?>) candidate;
            if (bound.isAssignableFrom(cls)) {
                return (Class<T>) cls;
            }
        }
        return null;
    }

    public static void close(Object o) throws Exception {
        if (o == null) return;
        if (o instanceof Closeable) {
            ((Closeable) o).close();

        } else {
            final Method closeMethod = o.getClass().getMethod("close", (Class<?>[]) null);
            if (closeMethod == null) die("no close method found on " + o.getClass().getName());
            closeMethod.invoke(o);
        }
    }

    public static void closeQuietly(Object o) {
        if (o == null) return;
        try {
            close(o);
        } catch (Exception e) {
            log.warn("close: error closing: "+e);
        }
    }

    @NoArgsConstructor @AllArgsConstructor
    public static class Setter<T> {
        @Getter protected String field;
        @Getter protected String value;
        public void set(T data) { ReflectionUtil.set(data, field, value); }
        @Override public String toString() { return getClass().getName() + '{' + field + ", " + value + '}'; }
    }

    // adapted from https://stackoverflow.com/a/2924426/1251543
    private static class CallerInspector extends SecurityManager {
        public String getCallerClassName() { return getClassContext()[2].getName(); }
        public String getCallerClassName(int depth) { return getClassContext()[depth].getName(); }
    }
    private final static CallerInspector callerInspector = new CallerInspector();

    public static String callerClassName() { return callerInspector.getCallerClassName(); }
    public static String callerClassName(int depth) { return callerInspector.getCallerClassName(depth); }
    public static String callerClassName(String match) {
        final StackTraceElement s = callerFrame(match);
        return s == null ? "callerClassName: no match: "+match : s.getMethodName();
    }

    public static String callerMethodName() { return new Throwable().getStackTrace()[2].getMethodName(); }
    public static String callerMethodName(int depth) { return new Throwable().getStackTrace()[depth].getMethodName(); }
    public static String callerMethodName(String match) {
        final StackTraceElement s = callerFrame(match);
        return s == null ? "callerMethodName: no match: "+match : s.getMethodName();
    }

    public static String caller () {
        final StackTraceElement[] t = new Throwable().getStackTrace();
        if (t == null || t.length == 0) return "caller: NO STACK TRACE!";
        return caller(t[Math.max(t.length-1, 2)]);
    }

    public static String caller (int depth) {
        final StackTraceElement[] t = new Throwable().getStackTrace();
        if (t == null || t.length == 0) return "caller: NO STACK TRACE!";
        return caller(t[Math.min(depth, t.length-1)]);
    }

    public static String caller(String match) {
        final StackTraceElement s = callerFrame(match);
        return s == null ? "caller: no match: "+match : caller(s);
    }

    public static StackTraceElement callerFrame(String match) {
        final StackTraceElement[] t = new Throwable().getStackTrace();
        if (t == null || t.length == 0) return null;
        for (StackTraceElement s : t) if (caller(s).contains(match)) return s;
        return null;
    }

    public static String caller(StackTraceElement s) { return s.getClassName() + "." + s.getMethodName() + ":" + s.getLineNumber(); }

    /**
     * Replace any string values with their transformed values
     * @param map a map of things
     * @param transformer a transformer
     * @return the same map, but if any value was a string, the transformer has been applied to it.
     */
    public static Map transformStrings(Map map, Transformer transformer) {
        if (empty(map)) return map;
        final Map setOps = new HashMap();
        for (Object entry : map.entrySet()) {
            final Map.Entry e = (Map.Entry) entry;
            if (e.getValue() instanceof String) {
                setOps.put(e.getKey(), transformer.transform(e.getValue()).toString());
            } else if (e.getValue() instanceof Map) {
                setOps.put(e.getKey(), transformStrings((Map) e.getValue(), transformer));
            }
        }
        for (Object entry : setOps.entrySet()) {
            final Map.Entry e = (Map.Entry) entry;
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    public static boolean isStaticFinalString(Field f) { return isStaticFinal(f, String.class, StringUtil.EMPTY); }

    public static boolean isStaticFinalString(Field f, String prefix) { return isStaticFinal(f, String.class, prefix); }

    public static boolean isStaticFinal(Field f, Class type) { return isStaticFinal(f, type, StringUtil.EMPTY); }

    public static boolean isStaticFinal(Field f, Class type, String prefix) {
        final int mods = f.getModifiers();
        return isStatic(mods) && isFinal(mods) && type.isAssignableFrom(f.getType()) && f.getName().startsWith(prefix);
    }

    public static <T> T constValue(Field f) {
        try {
            return (T) f.get(null);
        } catch (Exception e) {
            return die("constValue("+f+"): "+shortError(e));
        }
    }

    public static <T> T safeConstValue(Field f) {
        try {
            return constValue(f);
        } catch (Exception e) {
            log.warn("safeConstValue("+f+"): error (returning null): "+shortError(e));
            return null;
        }
    }

    public static <T> T constValue(Class type, String fieldName) {
        final Field field;
        try {
            field = type.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (Exception e) {
            return die("constValue("+type.getName()+", "+fieldName+"): "+shortError(e));
        }
        return constValue(field);
    }

    public static <T> T safeConstValue(Class type, String fieldName) {
        try {
            return constValue(type, fieldName);
        } catch (Exception e) {
            log.warn("safeConstValue("+type.getName()+", "+fieldName+"): error (returning null): "+shortError(e));
            return null;
        }
    }

}
