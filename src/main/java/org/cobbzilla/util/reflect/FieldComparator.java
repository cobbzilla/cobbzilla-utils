package org.cobbzilla.util.reflect;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Comparator;

@AllArgsConstructor
public class FieldComparator<T, F extends Comparable<F>> implements Comparator<T> {

    @Getter private final String field;
    @Getter private final boolean reverse = false;

    @Override public int compare(T o1, T o2) {
        final F v1 = (F) ReflectionUtil.get(o1, field);
        final F v2 = (F) ReflectionUtil.get(o2, field);
        return reverse
                ? (v1 == null ? (v2 == null ? 0 : 1) : (v2 == null ? 1 : v2.compareTo(v1)))
                : (v1 == null ? (v2 == null ? 0 : -1) : (v2 == null ? -1 : v1.compareTo(v2)));
    }

}
