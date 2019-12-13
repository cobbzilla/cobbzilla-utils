package org.cobbzilla.util.collection;

import java.util.*;

public class InspectCollection {

    public static boolean containsCircularReference(String start, Map<String, List<String>> graph) {
        return containsCircularReference(new HashSet<String>(), start, graph);
    }

    public static boolean containsCircularReference(Set<String> found, String start, Map<String, List<String>> graph) {
        final List<String> descendents = graph.get(start);
        if (descendents == null) return false; // special case: our starting point is outside the graph.
        for (String target : descendents)  {
            if (found.contains(target)) {
                // we've seen this target already, we have a circular reference
                return true;
            }
            if (graph.containsKey(target)) {
                // this target is also a member of the graph -- add to found and recurse
                found.add(target);
                if (containsCircularReference(new HashSet<>(found), target, graph)) return true;
            }
            // no "else" clause here: we don't care about anything not in the graph, it can't create a circular reference.
        }
        return false;
    }

    public static boolean isLargerThan (Collection c, int size) {
        int count = 0;
        final Iterator i = c.iterator();
        while (i.hasNext() && count <= size) {
            i.next();
            count++;
        }
        return count > size;
    }

}
