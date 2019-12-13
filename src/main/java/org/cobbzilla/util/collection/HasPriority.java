package org.cobbzilla.util.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public interface HasPriority {

    Integer getPriority ();

    default boolean hasPriority () { return getPriority() != null; }

    Comparator<HasPriority> SORT_PRIORITY = (r1, r2) -> {
        if (!r2.hasPriority()) return r1.hasPriority() ? -1 : 0;
        if (!r1.hasPriority()) return 1;
        return r1.getPriority().compareTo(r2.getPriority());
    };

    static int compare(Object o1, Object o2) {
        return o1 instanceof HasPriority && o2 instanceof HasPriority ? SORT_PRIORITY.compare((HasPriority) o1, (HasPriority) o2) : 0;
    }

    static <T extends HasPriority> Collection<T> priorityAsc (Collection<T> c) {
        final Collection<T> sorted = new ArrayList<>(c);
        ((ArrayList<T>) sorted).sort(SORT_PRIORITY);
        return sorted;
    }

    static <T extends HasPriority> Collection<T> priorityDesc (Collection<T> c) {
        final Collection<T> sorted = new ArrayList<>(c);
        ((ArrayList<T>) sorted).sort(SORT_PRIORITY.reversed());
        return sorted;
    }

}
