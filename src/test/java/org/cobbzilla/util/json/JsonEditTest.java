package org.cobbzilla.util.json;

import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.json.data.TestData;
import org.cobbzilla.util.string.StringUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonEditTest {

    public static final String TEST_JSON = StringUtil.getPackagePath(JsonEditTest.class)+"/test.json";
    private final Random random = new Random();

    @Test public void testEditJson() throws Exception {

        JsonEdit jsonEdit;
        String result;

        // replace a node (overwrite an object with an integer)
        final Integer toReplace = random.nextInt();
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("thing.field2")
                        .setJson(toReplace.toString()));
        result = jsonEdit.edit();
        assertEquals(toReplace, JsonUtil.fromJson(result, "thing.field2", Integer.class));

        // replace a node within an array
        final Integer toReplace2 = random.nextInt();
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("thing.field1[2]")
                        .setJson(toReplace2.toString()));
        result = jsonEdit.edit();
        assertEquals(toReplace2, JsonUtil.fromJson(result, "thing.field1[2]", Integer.class));

        // add a node
        final String rand0 = randomAlphanumeric(10);
        final String toAdd0 = "{\"sub1\": true, \"sub2\": \"" + rand0 + "\"}";
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("thing.field3")
                        .setJson(toAdd0));
        result = jsonEdit.edit();
        assertEquals(rand0, JsonUtil.fromJson(result, "thing.field3.sub2", String.class));

        // add a node
        final String rand = randomAlphanumeric(10);
        final String toAdd = "{\"subC\": \""+ rand + "\"}";
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("thing.field2")
                        .setJson(toAdd));
        result = jsonEdit.edit();
        assertEquals(rand, JsonUtil.fromJson(result, "thing.field2.subC", String.class));

        // add a node at the root
        final String rand2 = randomAlphanumeric(10);
        final String rootAdd = "{\"rootfoo\": \""+ rand2 + "\"}";
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setJson(rootAdd));
        result = jsonEdit.edit();
        assertEquals(rand2, JsonUtil.fromJson(result, "rootfoo", String.class));

        // add a numeric node at the root
        final Integer rootAdd2 = random.nextInt();
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("newguy")
                        .setJson(rootAdd2.toString()));
        result = jsonEdit.edit();
        assertEquals(rootAdd2, JsonUtil.fromJson(result, "newguy", Integer.class));

        // replace something that doesn't exist -- should add it
        final String rand3 = randomAlphanumeric(10);
        final String toReplace3 = "\""+rand3+"\"";
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("thing.field3")
                        .setJson(toReplace3));
        result = jsonEdit.edit();
        assertEquals(rand3, JsonUtil.fromJson(result, "thing.field3", String.class));

        // append to a list
        final String rand4 = randomAlphanumeric(10);
        final String toReplace4 = "\""+rand4+"\"";
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath("thing.field1[]")
                        .setJson(toReplace4));
        result = jsonEdit.edit();
        assertEquals(rand4, JsonUtil.fromJson(result, "thing.field1[3]", String.class));
        assertEquals(4, JsonUtil.fromJson(result, "thing.field1", String[].class).length);

        // write a new, deep node at the root level of the tree.
        // ObjectNodes should be created along the way for missing nodes.
        final Integer deepNodeValue = random.nextInt();
        final String deepNodePath = "newRootField."+randomAlphabetic(4)+"."+randomAlphabetic(4);
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath(deepNodePath)
                        .setJson(deepNodeValue.toString()));
        result = jsonEdit.edit();
        assertEquals(deepNodeValue, JsonUtil.fromJson(result, deepNodePath, Integer.class));

        // write a new, deep node within the tree.
        // ObjectNodes should be created along the way for missing nodes.
        final Integer deepNodeValue2 = random.nextInt();
        final String deepNodePath2 = "thing.newField."+randomAlphabetic(4)+"."+randomAlphabetic(4);
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.write)
                        .setPath(deepNodePath2)
                        .setJson(deepNodeValue2.toString()));
        result = jsonEdit.edit();
        assertEquals(deepNodeValue2, JsonUtil.fromJson(result, deepNodePath2, Integer.class));

        // delete a node
        jsonEdit = new JsonEdit()
                .setJsonData(testJson())
                .addOperation(new JsonEditOperation()
                        .setType(JsonEditOperationType.delete)
                        .setPath("thing.field2"));
        result = jsonEdit.edit();
        assertNull(JsonUtil.fromJson(result, TestData.class).thing.field2);
    }

    private InputStream testJson() throws IOException {
        return StreamUtil.loadResourceAsStream(TEST_JSON);
    }

}
