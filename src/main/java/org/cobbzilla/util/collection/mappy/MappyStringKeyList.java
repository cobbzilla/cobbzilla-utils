package org.cobbzilla.util.collection.mappy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;

@NoArgsConstructor
public class MappyStringKeyList<V> extends MappyList<String, V> {

    public MappyStringKeyList(int size) { super(size); }

    public MappyStringKeyList(int size, int subSize) { super(size, subSize); }

    public MappyStringKeyList(Map<String, Collection<V>> other, Integer subSize) {
        super(other);
        this.subSize = subSize;
    }

    public MappyStringKeyList(Map other) { this(other, null); }

    public MappyStringKeyList(String json) {
        final ObjectNode object = json(json, ObjectNode.class);
        final Class<?> arrayClass = arrayClass(getFirstTypeParam(getClass()));
        for (Iterator<Entry<String, JsonNode>> iter = object.fields(); iter.hasNext(); ) {
            final Map.Entry<String, JsonNode> entry = iter.next();
            putAll(entry.getKey(), asList((V[]) json(entry.getValue(), arrayClass)));
        }
    }

}
