package org.cobbzilla.util.collection;

import org.cobbzilla.util.string.StringUtil;

import java.lang.reflect.Array;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;

public class ArrayUtil {

    public static final Object[] SINGLE_NULL_OBJECT = new Object[]{null};
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public static <T> T[] append (T[] array, T... elements) {
        if (array == null || array.length == 0) {
            if (elements.length == 0) return (T[]) new Object[]{}; // punt, it's empty anyway
            final T[] newArray = (T[]) Array.newInstance(elements[0].getClass(), elements.length);
            System.arraycopy(elements, 0, newArray, 0, elements.length);
            return newArray;
        } else {
            if (elements.length == 0) return Arrays.copyOf(array, array.length);
            final T[] copy = Arrays.copyOf(array, array.length + elements.length);
            System.arraycopy(elements, 0, copy, array.length, elements.length);
            return copy;
        }
    }

    public static <T> T[] concat (T[]... arrays) {
        int size = 0;
        for (T[] array : arrays) {
            size += array == null ? 0 : array.length;
        }
        final Class<?> componentType = arrays.getClass().getComponentType().getComponentType();
        final T[] newArray = (T[]) Array.newInstance(componentType, size);
        int destPos = 0;
        for (T[] array : arrays) {
            System.arraycopy(array, 0, newArray, destPos, array.length);
            destPos += array.length;
        }
        return newArray;
    }

    public static <T> T[] remove(T[] array, int indexToRemove) {
        if (array == null) throw new NullPointerException("remove: array was null");
        if (indexToRemove >= array.length || indexToRemove < 0) throw new IndexOutOfBoundsException("remove: cannot remove element "+indexToRemove+" from array of length "+array.length);
        final List<T> list = new ArrayList<>(Arrays.asList(array));
        list.remove(indexToRemove);
        final T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length-1);
        return list.toArray(newArray);
    }

    /**
     * Return a slice of an array. If from == to then an empty array will be returned.
     * @param array the source array
     * @param from the start index, inclusive. If less than zero or greater than the length of the array, an Exception is thrown
     * @param to the end index, NOT inclusive. If less than zero or greater than the length of the array, an Exception is thrown
     * @param <T> the of the array
     * @return A slice of the array. The source array is not modified.
     */
    public static <T> T[] slice(T[] array, int from, int to) {

        if (array == null) throw new NullPointerException("slice: array was null");
        if (from < 0 || from > array.length) die("slice: invalid 'from' index ("+from+") for array of size "+array.length);
        if (to < 0 || to < from || to > array.length) die("slice: invalid 'to' index ("+to+") for array of size "+array.length);

        final T[] newArray = (T[]) Array.newInstance(array.getClass().getComponentType(), to-from);
        if (to == from) return newArray;
        System.arraycopy(array, from, newArray, 0, to-from);
        return newArray;
    }

    public static <T> List<T> merge(Collection<T>... collections) {
        if (empty(collections)) return Collections.emptyList();
        final Set<T> result = new HashSet<>();
        for (Collection<T> c : collections) result.addAll(c);
        return new ArrayList<>(result);
    }

    /**
     * Produce a delimited string from an array. Null values will appear as "null"
     * @param array the array to consider
     * @param delim the delimiter to put in between each element
     * @return the result of calling .toString on each array element, or "null" for null elements, separated by the given delimiter.
     */
    public static String arrayToString(Object[] array, String delim) {
        return arrayToString(array, delim, "null");
    }

    /**
     * Produce a delimited string from an array.
     * @param array the array to consider
     * @param delim the delimiter to put in between each element
     * @param nullValue the value to write if an array entry is null. if this parameter is null, then null array entries will not be included in the output.
     * @return a string that starts with [ and ends with ] and within is the result of calling .toString on each non-null element (and printing nullValue for each null element, unless nulValue == null in which case null elements are omitted), with 'delim' in between each entry.
     */
    public static String arrayToString(Object[] array, String delim, String nullValue) {
        return arrayToString(array, delim, nullValue, true);
    }

    /**
     * Produce a delimited string from an array.
     * @param array the array to consider
     * @param delim the delimiter to put in between each element
     * @param nullValue the value to write if an array entry is null. if this parameter is null, then null array entries will not be included in the output.
     * @param includeBrackets if false, the return value will not start/end with []
     * @return a string that starts with [ and ends with ] and within is the result of calling .toString on each non-null element (and printing nullValue for each null element, unless nulValue == null in which case null elements are omitted), with 'delim' in between each entry.
     */
    public static String arrayToString(Object[] array, String delim, String nullValue, boolean includeBrackets) {
        if (array == null) return "null";
        final StringBuilder b = new StringBuilder();
        for (Object o : array) {
            if (b.length() > 0) b.append(delim);
            if (o == null) {
                if (nullValue == null) continue;
                b.append(nullValue);
            } else if (o.getClass().isArray()) {
                b.append(arrayToString((Object[]) o, delim, nullValue));
            } else if (o instanceof Map) {
                b.append(StringUtil.toString((Map) o));
            } else {
                b.append(o.toString());
            }
        }
        return includeBrackets ? b.insert(0, "[").append("]").toString() : b.toString();
    }

    public static <T> T[] shift(T[] args) {
        if (args == null) return null;
        if (args.length == 0) return args;
        final T[] newArgs = (T[]) Array.newInstance(args[0].getClass(), args.length-1);
        System.arraycopy(args, 1, newArgs, 0, args.length-1);
        return newArgs;
    }

    public static <T> T[] singletonArray (T thing) { return singletonArray(thing, (Class<T>) thing.getClass()); }

    public static <T> T[] singletonArray (T thing, Class<T> clazz) {
        final T[] array = (T[]) Array.newInstance(arrayClass(clazz), 1);
        array[0] = thing;
        return array;
    }
}
