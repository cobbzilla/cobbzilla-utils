package org.cobbzilla.util.reflect;

import java.util.Map;

public interface ObjectFactory<T> {

    T create ();
    T create (Map<String, Object> ctx);

}
