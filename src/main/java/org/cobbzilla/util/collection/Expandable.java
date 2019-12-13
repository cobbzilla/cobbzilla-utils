package org.cobbzilla.util.collection;

import java.util.List;
import java.util.Map;

public interface Expandable<T> {

    List<T> expand(Map<String, Object> context);

}
