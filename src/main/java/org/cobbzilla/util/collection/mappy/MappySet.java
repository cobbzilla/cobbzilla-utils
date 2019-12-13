package org.cobbzilla.util.collection.mappy;

import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
public class MappySet<K, V> extends Mappy<K, V, Set<V>> {

    public MappySet (int size) { super(size); }

    @Override protected Set<V> newCollection() { return new HashSet<>(); }

}

