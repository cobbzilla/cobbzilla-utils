package org.cobbzilla.util.collection;

import org.apache.commons.collections.Transformer;

public class ToStringTransformer implements Transformer {

    public static final ToStringTransformer instance = new ToStringTransformer();

    @Override public Object transform(Object o) { return o == null ? "null" : o.toString(); }

}
