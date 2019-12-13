package org.cobbzilla.util.collection;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

public class Sorter {

    public static <E> Collection<E> sort (Collection<E> things, Comparator sorter) {
        return sort(things, new TreeSet<>(sorter));
    }

    public static <C extends Collection> C sort (Collection things, C rval) {
        rval.addAll(things);
        return rval;
    }

}
