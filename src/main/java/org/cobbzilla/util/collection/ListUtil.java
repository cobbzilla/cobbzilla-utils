package org.cobbzilla.util.collection;

import com.google.common.collect.Lists;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.util.*;

public class ListUtil {

    public static <T> List<T> concat(List<T> list1, List<T> list2) {
        if (list1 == null || list1.isEmpty()) return list2 == null ? null : new ArrayList<>(list2);
        if (list2 == null || list2.isEmpty()) return new ArrayList<>(list1);
        final List<T> newList = new ArrayList<>(list1.size() + list2.size());
        newList.addAll(list1);
        newList.addAll(list2);
        return newList;
    }

    // adapted from: https://stackoverflow.com/a/23870892/1251543

    /**
     * Combines several collections of elements and create permutations of all of them, taking one element from each
     * collection, and keeping the same order in resultant lists as the one in original list of collections.
     * <p/>
     * <ul>Example
     * <li>Input  = { {a,b,c} , {1,2,3,4} }</li>
     * <li>Output = { {a,1} , {a,2} , {a,3} , {a,4} , {b,1} , {b,2} , {b,3} , {b,4} , {c,1} , {c,2} , {c,3} , {c,4} }</li>
     * </ul>
     *
     * @param collections Original list of collections which elements have to be combined.
     * @return Resultant collection of lists with all permutations of original list.
     */
    public static <T> List<List<T>> permutations(List<List<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<List<T>> res = Lists.newLinkedList();
            permutationsImpl(collections, res, 0, new LinkedList<T>());
            return res;
        }
    }

    private static <T> void permutationsImpl(List<List<T>> ori, Collection<List<T>> res, int d, List<T> current) {
        // if depth equals number of original collections, final reached, add and return
        if (d == ori.size()) {
            res.add(current);
            return;
        }

        // iterate from current collection and copy 'current' element N times, one for each element
        Collection<T> currentCollection = ori.get(d);
        for (T element : currentCollection) {
            List<T> copy = Lists.newLinkedList(current);
            copy.add(element);
            permutationsImpl(ori, res, d + 1, copy);
        }
    }

    public static List<Object> expand(Object[] things, Map<String, Object> context) {
        final List<Object> results = new ArrayList<>();
        for (Object thing : things) {
            if (thing instanceof Expandable) {
                results.addAll(((Expandable) thing).expand(context));
            } else {
                results.add(thing);
            }
        }
        return results;
    }

    public static <T> List<T> deepCopy(List<T> list) {
        if (list == null) return null;
        final List<T> copy = new ArrayList<>();
        for (T item : list) copy.add(item == null ? null : ReflectionUtil.copy(item));
        return copy;
    }

}