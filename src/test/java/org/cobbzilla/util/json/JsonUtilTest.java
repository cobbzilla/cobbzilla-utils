package org.cobbzilla.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.data.TestData;
import org.cobbzilla.util.string.StringUtil;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.cobbzilla.util.io.FileUtil.getDefaultTempDir;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonUtilTest {

    public static final String PREFIX = StringUtil.getPackagePath(JsonUtilTest.class);
    private static final String TEST_JSON = PREFIX +"/test.json";

    private static final Object[][] TESTS = new Object[][] {
        new Object[] {
                "id", new ReplacementValue() { @Override public String getValue(TestData testData) { return testData.id; } }
        },
        new Object[] {
                "thing.field1[1]", new ReplacementValue() { @Override public String getValue(TestData testData) { return testData.thing.field1[1]; } }
        },
        new Object[] {
                "another_thing.field1", new ReplacementValue() { @Override public String getValue(TestData testData) { return testData.another_thing.field1; } }
        },
    };

    @Test
    public void testReplaceJsonValue () throws Exception {

        final String testJson = StreamUtil.loadResourceAsString(TEST_JSON);
        final String replacement = RandomStringUtils.randomAlphanumeric(10);

        for (Object[] test : TESTS) {
            assertReplacementMade(testJson, replacement, (String) test[0], (ReplacementValue) test[1]);
        }
    }

    public void assertReplacementMade(String testJson, String replacement, String path, ReplacementValue value) throws Exception {
        final ObjectNode doc = JsonUtil.replaceNode(testJson, path, replacement);
        final File temp = File.createTempFile("JsonUtilTest", ".json", getDefaultTempDir());
        FileUtil.toFile(temp, toJson(doc));
        final TestData data = JsonUtil.fromJson(temp, TestData.class);
        assertEquals(replacement, value.getValue(data));
    }

    public interface ReplacementValue {
        String getValue(TestData testData);
    }

    @Test public void testMerge () throws Exception {
        final String orig = StreamUtil.stream2string(PREFIX + "/merge/test1_orig.json");
        final String request = StreamUtil.stream2string(PREFIX + "/merge/test1_request.json");
        final String expected = StreamUtil.stream2string(PREFIX + "/merge/test1_expected.json");
        assertTrue(jsonEquals(expected.replaceAll("\\p{javaSpaceChar}+", ""), JsonUtil.mergeJson(orig, request).replaceAll("\\p{javaSpaceChar}+", "")));
    }

    private boolean jsonEquals(String j1, String j2) {
        if (j1 == null) return j2 == null;
        if (j2 == null) return false;

        final JsonNode n1 = json(j1, JsonNode.class);
        final JsonNode n2 = json(j2, JsonNode.class);
        return jsonEquals(n1, n2);
    }

    private boolean jsonEquals(JsonNode n1, JsonNode n2) {
        if (!n1.getNodeType().equals(n2.getNodeType())) return false;

        switch (n1.getNodeType()) {
            case ARRAY:
                return jsonArrayEquals((ArrayNode) n1, (ArrayNode) n2);
            case OBJECT:
                return jsonObjectEquals((ObjectNode) n1, (ObjectNode) n2);
            case NULL:
                return n1.isNull() == n2.isNull();
            default:
                return n1.textValue().equals(n2.textValue());
        }
    }

    private boolean jsonObjectEquals(ObjectNode n1, ObjectNode n2) {
        final Map<String, JsonNode> n1fields = toMap(n1);
        final Map<String, JsonNode> n2fields = toMap(n2);
        if (n1fields.size() != n2fields.size()) return false;
        for (Map.Entry<String, JsonNode> n1entry : n1fields.entrySet()) {
            if (!jsonEquals(n1entry.getValue(), n2fields.get(n1entry.getKey()))) return false;
        }
        return true;
    }

    public static Map<String, JsonNode> toMap(ObjectNode node) {
        final Map<String, JsonNode> map = new HashMap<>();
        for (Iterator<String> iter = node.fieldNames(); iter.hasNext(); ) {
            final String name = iter.next();
            map.put(name, node.get(name));
        }
        return map;
    }

    public boolean jsonArrayEquals(ArrayNode n1, ArrayNode n2) {
        if (n1.size() != n2.size()) return false;
        if (n1.size() == 0) return true;

        final List<JsonNode> n1elements = new ArrayList<>();
        final List<JsonNode> n2elements = new ArrayList<>();
        for (int i=0; i<n1.size(); i++) {
            n1elements.add(n1.get(i));
            n2elements.add(n2.get(i));
        }

        boolean found = false;
        for (JsonNode n1element : n1elements) {
            for (JsonNode n2element : n2elements) {
                if (jsonEquals(n1element, n2element)) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }
}
