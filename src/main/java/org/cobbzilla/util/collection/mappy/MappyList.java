package org.cobbzilla.util.collection.mappy;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class MappyList<K, V> extends Mappy<K, V, List<V>> {

    protected Integer subSize;

    public MappyList (int size) { super(size); }
    public MappyList (int size, int subSize) { super(size); this.subSize = subSize; }

    public MappyList(Map<K, Collection<V>> other, Integer subSize) {
        super(other);
        this.subSize = subSize;
    }

    public MappyList(Map<K, Collection<V>> other) { this(other, null); }

    @Override protected List<V> newCollection() { return subSize != null ? new ArrayList<V>(subSize) : new ArrayList<V>(); }

    @Override protected V firstInCollection(List<V> collection) { return collection.get(0); }

}
