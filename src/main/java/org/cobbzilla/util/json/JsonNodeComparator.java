package org.cobbzilla.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;

import java.util.Comparator;

import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

@AllArgsConstructor
public class JsonNodeComparator implements Comparator<JsonNode> {

    final String path;

    @Override public int compare(JsonNode n1, JsonNode n2) {
        final JsonNode v1 = fromJsonOrDie(n1, path, JsonNode.class);
        final JsonNode v2 = fromJsonOrDie(n2, path, JsonNode.class);
        if (v1 == null) return 1;
        if (v2 == null) return -1;
        return v1.textValue().compareTo(v2.textValue());
    }
}
