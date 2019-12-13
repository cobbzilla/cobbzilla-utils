package org.cobbzilla.util.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.util.json.JsonUtil.FULL_MAPPER_ALLOW_COMMENTS;

/**
 * Facilitates editing JSON files.
 *
 * Notes:
 *  - only one read operation can be specified. the 'edit' method will return the value of the first read operation processed.
 *  - if you write a node that does not exist, it will be created (if it can be)
 */
@Accessors(chain=true)
public class JsonEdit {

    public static final ObjectMapper JSON = FULL_MAPPER_ALLOW_COMMENTS;

    @Getter @Setter private Object jsonData;
    @Getter @Setter private List<JsonEditOperation> operations = new ArrayList<>();

    public JsonEdit addOperation (JsonEditOperation operation) { operations.add(operation); return this; }

    public String edit () throws Exception {
        JsonNode root = readJson();
        for (JsonEditOperation operation : operations) {
            if (operation.isRead()) return JsonUtil.toString(findNode(root, operation.getPath()));
            root = apply(root, operation);
        }
        return JsonUtil.toString(JSON.treeToValue(root, Object.class));
    }

    private JsonNode readJson() throws IOException {
        if (jsonData instanceof JsonNode) return (JsonNode) jsonData;
        if (jsonData instanceof InputStream) return JSON.readTree((InputStream) jsonData);
        if (jsonData instanceof Reader) return JSON.readTree((Reader) jsonData);
        if (jsonData instanceof String) return JSON.readTree((String) jsonData);
        if (jsonData instanceof File) return JSON.readTree((File) jsonData);
        if (jsonData instanceof URL) return JSON.readTree((URL) jsonData);
        throw new IllegalArgumentException("jsonData is not a JsonNode, InputStream, Reader, String, File or URL");
    }

    private JsonNode apply(JsonNode root, JsonEditOperation operation) throws IOException {
        final List<JsonNode> path = findNodePath(root, operation.getPath());

        switch (operation.getType()) {
            case write:
                root = write(root, path, operation);
                break;

            case delete:
                delete(path, operation);
                break;

            case sort:
                root = sort(root, path, operation);
                break;

            default: throw new IllegalArgumentException("unsupported operation: "+operation.getType());
        }
        return root;
    }

    private JsonNode write(JsonNode root, List<JsonNode> path, JsonEditOperation operation) throws IOException {

        JsonNode current = path.get(path.size()-1);
        final JsonNode parent = path.size() > 1 ? path.get(path.size()-2) : null;
        final JsonNode data = operation.getNode();

        if (current == MISSING) {
            // add a new node to parent
            return addToParent(root, operation, path);
        }

        if (current instanceof ObjectNode) {
            final ObjectNode node = (ObjectNode) current;
            if (data instanceof ObjectNode) {
                final Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> entry = fields.next();
                    node.set(entry.getKey(), entry.getValue());
                }
            } else {
                return addToParent(root, operation, path);
            }

        } else if (current instanceof ArrayNode) {
            final ArrayNode node = (ArrayNode) current;
            if (operation.hasIndex()) {
                node.set(operation.getIndex(), operation.getNode());
            } else {
                node.add(operation.getNode());
            }

        } else if (current instanceof ValueNode) {
            if (parent == null) return newObjectNode().set(operation.getName(), data);

            // overwrite value node at location
            addToParent(root, operation, path);

        } else {
            throw new IllegalArgumentException("Cannot append to node (is a "+current.getClass().getName()+"): "+current);
        }

        return root;
    }

    private JsonNode addToParent(JsonNode root, JsonEditOperation operation, List<JsonNode> path) throws IOException {

        JsonNode parent = path.size() > 1 ? path.get(path.size()-2) : null;
        final JsonNode data = operation.getNode();

        if (parent == null) return newObjectNode().set(operation.getName(), data);

        if (parent instanceof ObjectNode) {
            // more than one missing node?
            while (path.size() <= operation.getNumPathSegments()) {
                final String childName = operation.getName(path.size()-2);
                final ObjectNode newNode = newObjectNode();
                ((ObjectNode) parent).set(childName, newNode);

                // re-generate path now that we've created one missing parent
                path = findNodePath(root, operation.getPath());
                parent = newNode;
            }

            // creating a new array node under parent?
            if (operation.isEmptyBrackets()) {
                final ArrayNode newArrayNode = newArrayNode();
                ((ObjectNode) parent).set(operation.getName(), newArrayNode);
                newArrayNode.add(data);

            } else {
                // otherwise, just set a field on the parent object
                ((ObjectNode) parent).set(operation.getName(), data);
            }

        } else if (parent instanceof ArrayNode) {
            if (operation.isEmptyBrackets()) {
                ((ArrayNode) parent).add(data);
            } else {
                ((ArrayNode) parent).set(operation.getIndex(), data);
            }
        } else {
            throw new IllegalArgumentException("Cannot append to node (is a "+parent.getClass().getName()+"): "+parent);
        }

        return root;
    }

    private void delete(List<JsonNode> path, JsonEditOperation operation) {
        if (path.size() < 2) throw new IllegalArgumentException("Cannot delete root");
        final JsonNode parent = path.get(path.size()-2);

        if (parent instanceof ArrayNode) {
            ((ArrayNode) parent).remove(operation.getIndex());

        } else if (parent instanceof ObjectNode) {
            ((ObjectNode) parent).remove(operation.getName());

        } else {
            throw new IllegalArgumentException("Cannot remove node (parent is a "+parent.getClass().getName()+")");
        }
    }

    private JsonNode sort(JsonNode root, List<JsonNode> path, JsonEditOperation operation) {
        final JsonNode array = path.get(path.size()-1);
        final JsonNode parent = path.size() == 1 ? array : path.get(path.size()-2);
        if (!array.isArray()) return die("sort: "+operation.getPath()+" is not an array: "+json(array));

        // sort array
        final SortedSet<JsonNode> sorted = new TreeSet<>(new JsonNodeComparator(operation.getJson()));
        final Iterator<JsonNode> iter = array.iterator();
        while (iter.hasNext()) sorted.add(iter.next());

        // create a new ArrayNode for the sorted array
        final ArrayNode newArray = newArrayNode();
        for (JsonNode n : sorted) newArray.add(n);

        // if we are sorting a root-level array, just return it now
        if (parent == array) return newArray;

        // find previous array node, remove it and add new one
        boolean replaced = false;
        if (parent.isArray()) {
            final ArrayNode parentArray = (ArrayNode) parent;
            for (int i=0; i<parentArray.size(); i++) {
                if (parentArray.get(i) == array) {
                    parentArray.remove(i);
                    parentArray.insert(i, newArray);
                    replaced = true;
                    break;
                }
            }
        } else if (parent.isObject()) {
            final ObjectNode parentObject = (ObjectNode) parent;
            final Iterator<String> fieldNames = parentObject.fieldNames();
            while (fieldNames.hasNext()) {
                final String fieldName = fieldNames.next();
                final JsonNode n = parentObject.get(fieldName);
                if (n == array) {
                    parentObject.remove(fieldName);
                    parentObject.set(fieldName, newArray);
                    replaced = true;
                    break;
                }
            }
        }
        if (!replaced) return die("sort: error replacing original array with sorted array");
        return root;
    }

}
