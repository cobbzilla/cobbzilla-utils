package org.cobbzilla.util.collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.cobbzilla.util.collection.ArrayUtil.EMPTY_OBJECT_ARRAY;

@AllArgsConstructor
public class FieldTransformer implements Transformer {

    public static final FieldTransformer TO_NAME = new FieldTransformer("name");
    public static final FieldTransformer TO_ID = new FieldTransformer("id");
    public static final FieldTransformer TO_UUID = new FieldTransformer("uuid");

    @Getter private final String field;

    @Override public Object transform(Object o) { return ReflectionUtil.get(o, field); }

    public <E> List<E> collect (Collection c) { return c == null ? null : (List<E>) CollectionUtils.collect(c, this); }
    public <E> Set<E> collectSet (Collection c) { return c == null ? null : new HashSet<>(CollectionUtils.collect(c, this)); }

    public <E> E[] array (Collection c) {
        if (c == null) return null;
        if (c.isEmpty()) return (E[]) EMPTY_OBJECT_ARRAY;
        final List<E> collect = (List<E>) CollectionUtils.collect(c, this);
        final Class<E> elementType = (Class<E>) ReflectionUtil.getterType(c.iterator().next(), field);
        return collect.toArray((E[]) Array.newInstance(elementType, collect.size()));
    }

}