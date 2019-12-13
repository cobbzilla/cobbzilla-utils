package org.cobbzilla.util.collection.mappy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Comparator;
import java.util.TreeSet;

@NoArgsConstructor @AllArgsConstructor
public class MappySortedSet<K, V> extends Mappy<K, V, TreeSet<V>> {

    public MappySortedSet(int size) { super(size); }

    @Getter @Setter private Comparator<? super V> comparator;

    @Override protected TreeSet<V> newCollection() { return comparator == null ? new TreeSet<V>() : new TreeSet<>(comparator); }

    @Override protected V firstInCollection(TreeSet<V> collection) { return collection.first(); }

}
