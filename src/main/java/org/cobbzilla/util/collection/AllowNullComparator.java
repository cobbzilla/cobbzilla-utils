package org.cobbzilla.util.collection;

import java.util.Comparator;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class AllowNullComparator<E> implements Comparator<E> {

    public static final AllowNullComparator<String>  STRING = new AllowNullComparator<>();
    public static final AllowNullComparator<Integer> INT    = new AllowNullComparator<>();
    public static final AllowNullComparator<Long>    LONG   = new AllowNullComparator<>();

    @Override public int compare(E o1, E o2) {
        if (o1 == null) return o2 == null ? 0 : -1;
        if (o2 == null) return 1;
        if (o1 instanceof Comparable && o2 instanceof Comparable) return ((Comparable) o1).compareTo(o2);
        return die("compare: incomparable objects: "+o1+", "+o2);
    }

}
