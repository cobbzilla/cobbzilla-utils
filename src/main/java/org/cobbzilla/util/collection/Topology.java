package org.cobbzilla.util.collection;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

// adapted from: https://en.wikipedia.org/wiki/Topological_sorting
@NoArgsConstructor
public class Topology<T> {

    private final List<Node<T>> nodes = new ArrayList<>();

    public void addNode(T thing, Collection<T> refs) {
        final Node<T> existingNode = nodes.stream()
                .filter(n -> n.thing.equals(thing))
                .findFirst().orElse(null);
        final Node<T> node = existingNode != null ? existingNode : new Node<>(thing);

        // add refs as edges
        refs.stream()
                .filter(ref -> !ref.equals(thing))  // skip self-references
                .forEach(ref -> {
                    final Node<T> existingEdgeNode = nodes.stream()
                            .filter(n -> n.thing.equals(ref))
                            .findFirst()
                            .orElse(null);
                    if (existingEdgeNode != null) {
                        node.addEdge(existingEdgeNode);
                    } else {
                        final Node<T> newEdgeNode = new Node<>(ref);
                        nodes.add(newEdgeNode);
                        node.addEdge(newEdgeNode);
                    }
                });
        if (existingNode == null) nodes.add(node);
    }

    static class Node<T> {
        public final T thing;
        public final HashSet<Edge<T>> inEdges;
        public final HashSet<Edge<T>> outEdges;

        public Node(T thing) {
            this.thing = thing;
            inEdges = new HashSet<>();
            outEdges = new HashSet<>();
        }
        public Node addEdge(Node<T> node){
            final Edge<T> e = new Edge<>(this, node);
            outEdges.add(e);
            node.inEdges.add(e);
            return this;
        }
        public String toString() { return thing.toString(); }
    }

    @AllArgsConstructor
    static class Edge<T> {
        public final Node<T> from;
        public final Node<T> to;
        @Override public boolean equals(Object obj) {
            final Edge<T> e = (Edge<T>) obj;
            return e.from == from && e.to == to;
        }
    }

    public List<T> sort() {

        // L <- Empty list that will contain the sorted elements
        final ArrayList<Node<T>> L = new ArrayList<>();

        // S <- Set of all nodes with no incoming edges
        final HashSet<Node<T>> S = new HashSet<>();
        nodes.stream().filter(n -> n.inEdges.isEmpty()).forEach(S::add);

        // while S is non-empty do
        while (!S.isEmpty()) {
            // remove a node n from S
            final Node<T> n = S.iterator().next();
            S.remove(n);

            // insert n into L
            L.add(n);

            // for each node m with an edge e from n to m do
            for (Iterator<Edge<T>> it = n.outEdges.iterator(); it.hasNext();) {
                // remove edge e from the graph
                final Edge<T> e = it.next();
                final Node<T> m = e.to;
                it.remove();  // Remove edge from n
                m.inEdges.remove(e);  // Remove edge from m

                // if m has no other incoming edges then insert m into S
                if (m.inEdges.isEmpty()) S.add(m);
            }
        }
        // Check to see if all edges are removed
        for (Node<T> n : nodes) {
            if (!n.inEdges.isEmpty()) {
                return die("Cycle present, topological sort not possible: "+n.thing.toString()+" <- ["+n.inEdges.stream().map(e->e.from.thing.toString()).collect(Collectors.joining(", "))+"]");
            }
        }
        return L.stream().map(n -> n.thing).collect(Collectors.toList());
    }

    public List<T> sortReversed() {
        final List<T> sorted = sort();
        Collections.reverse(sorted);
        return sorted;
    }

}
