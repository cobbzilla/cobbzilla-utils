package org.cobbzilla.util.collection.mappy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

@NoArgsConstructor @AllArgsConstructor
public class MappyConcurrentSortedSet<K, V> extends Mappy<K, V, ConcurrentSkipListSet<V>> {

    public MappyConcurrentSortedSet(int size) { super(size); }

    @Getter @Setter private Comparator<? super V> comparator;

    @Override protected ConcurrentSkipListSet<V> newCollection() {
        return comparator == null ? new ConcurrentSkipListSet<V>() : new ConcurrentSkipListSet<>(comparator);
    }

    @Override protected V firstInCollection(ConcurrentSkipListSet<V> collection) { return collection.first(); }

}
