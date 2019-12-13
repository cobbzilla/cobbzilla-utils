package org.cobbzilla.util.collection;

import java.util.Collection;

public interface CollectionSource<T> {

    void addValue (T val);
    void addValues (Collection<T> vals);

    Collection<T> getValues();

}
