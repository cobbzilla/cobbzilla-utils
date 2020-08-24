package org.cobbzilla.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.cobbzilla.util.io.FileSuffixFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.FilenameSuffixFilter;
import org.cobbzilla.util.io.StreamUtil;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;

public class JsonUtil {

    public static final String EMPTY_JSON = "{}";
    public static final String EMPTY_JSON_ARRAY = "[]";

    public static final ThreadLocal<Boolean> verboseErrors = new ThreadLocal<>();
    public static boolean verboseErrors() { return verboseErrors.get() != null && verboseErrors.get(); }

    public static final JsonNode MISSING = MissingNode.getInstance();

    public static final FileFilter JSON_FILES = new FileSuffixFilter(".json");
    public static final FilenameFilter JSON_FILENAMES = new FilenameSuffixFilter(".json");

    public static final ObjectMapper COMPACT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final ObjectMapper FULL_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    public static final ObjectWriter FULL_WRITER = FULL_MAPPER.writer();

    public static final ObjectMapper FULL_MAPPER_ALLOW_COMMENTS = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    static {
        FULL_MAPPER_ALLOW_COMMENTS.getFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);
    }

    public static final ObjectMapper FULL_MAPPER_ALLOW_COMMENTS_AND_UNKNOWN_FIELDS = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    static {
        FULL_MAPPER_ALLOW_COMMENTS_AND_UNKNOWN_FIELDS.getFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);
    }

    public static final ObjectMapper FULL_MAPPER_ALLOW_UNKNOWN_FIELDS = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    static {
        FULL_MAPPER_ALLOW_UNKNOWN_FIELDS.getFactory().enable(JsonParser.Feature.ALLOW_COMMENTS);
    }

    public static final ObjectMapper NOTNULL_MAPPER = FULL_MAPPER
            .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final ObjectMapper NOTNULL_MAPPER_ALLOW_EMPTY = FULL_MAPPER
            .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public static final ObjectMapper PUBLIC_MAPPER = buildMapper();

    public static final ObjectWriter PUBLIC_WRITER = buildWriter(PUBLIC_MAPPER, PublicView.class);

    public static ObjectMapper buildMapper() {
        return new ObjectMapper()
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static ObjectWriter buildWriter(Class<? extends PublicView> view) {
        return buildMapper().writerWithView(view);
    }
    public static ObjectWriter buildWriter(ObjectMapper mapper, Class<? extends PublicView> view) {
        return mapper.writerWithView(view);
    }

    public static ArrayNode newArrayNode() { return new ArrayNode(FULL_MAPPER.getNodeFactory()); }
    public static ObjectNode newObjectNode() { return new ObjectNode(FULL_MAPPER.getNodeFactory()); }

    public static String find(JsonNode array, String name, String value, String returnValue) {
        if (array instanceof ArrayNode) {
            for (int i=0; i<array.size(); i++) {
                final JsonNode n = array.get(i).get(name);
                if (n != null && n.textValue().equals(value)) {
                    final JsonNode valNode = array.get(i).get(returnValue);
                    return valNode == null ? null : valNode.textValue();
                }
            }
        }
        return null;
    }

    public static String prettyJson(String json) {
        try {
            return FULL_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json(json, JsonNode.class));
        } catch (Exception e) {
            return die("prettyPrint: "+e);
        }
    }

    public static String json_html(Object value) { return json_html(value, null); }

    public static String json_html(Object value, ObjectMapper m) {
        return (m == null ? json(value) : json(value, m)).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace(" ", "&nbsp;").replace("\n", "<br/>");
    }

    public static class PublicView {}

    public static String toJson (Object o) throws Exception { return toJson(o, NOTNULL_MAPPER); }

    public static String toJson (Object o, ObjectMapper m) throws Exception { return m.writeValueAsString(o); }

    public static String json (Object o) { return toJsonOrDie(o); }
    public static String json (Object o, ObjectMapper m) { return toJsonOrDie(o, m); }

    public static String toJsonOrDie (Object o) {
        try { return toJson(o); } catch (Exception e) {
            final String msg = "toJson: exception writing object (" + o + "): " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }

    public static String toJsonOrDie (Object o, ObjectMapper m) {
        try { return toJson(o, m); } catch (Exception e) {
            final String msg = "toJson: exception writing object (" + o + "): " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }

    public static String toJsonOrErr(Object o) {
        try { return toJson(o); } catch (Exception e) {
            return e.toString();
        }
    }

    private static Map<String, ObjectWriter> viewWriters = new ConcurrentHashMap<>();

    protected static ObjectWriter viewWriter(Class jsonView) {
        ObjectWriter w = viewWriters.get(jsonView.getName());
        if (w == null) {
            w = JsonUtil.NOTNULL_MAPPER.disable(MapperFeature.DEFAULT_VIEW_INCLUSION).writerWithView(jsonView);
            viewWriters.put(jsonView.getName(), w);
        }
        return w;
    }

    public static String toJson (Object o, Class jsonView) throws Exception {
        return viewWriter(jsonView).writeValueAsString(o);
    }

    public static String toJsonOrDie (Object o, Class jsonView) {
        try { return toJson(o, jsonView); } catch (Exception e) {
            final String msg = "toJson: exception writing object (" + o + "): " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }

    public static String toJsonOrErr(Object o, Class jsonView) {
        try { return toJson(o, jsonView); } catch (Exception e) {
            return e.toString();
        }
    }

    public static <T> T fromJson(InputStream json, Class<T> clazz) throws Exception {
        return fromJson(StreamUtil.toString(json), clazz);
    }

    public static <T> T fromJson(InputStream json, Class<T> clazz, ObjectMapper mapper) throws Exception {
        return fromJson(StreamUtil.toString(json), clazz, mapper);
    }

    public static <T> T fromJson(File json, Class<T> clazz) throws Exception {
        return fromJson(FileUtil.toString(json), clazz);
    }

    public static <T> T fromJson(File json, Class<T> clazz, ObjectMapper mapper) throws Exception {
        return fromJson(FileUtil.toString(json), clazz, mapper);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return fromJson(json, clazz, JsonUtil.FULL_MAPPER);
    }

    public static <T> T fromJson(String json, JavaType type) throws Exception {
        if (empty(json)) return null;
        return JsonUtil.FULL_MAPPER.readValue(json, type);
    }

    public static <T> T fromJson(String json, Class<T> clazz, ObjectMapper mapper) throws Exception {
        if (empty(json)) return null;
        if (clazz == String.class && !(json.startsWith("\"") && json.endsWith("\""))) {
            json = "\"" + json + "\"";
        }
        return mapper.readValue(json, clazz);
    }

    public static <T> T fromJsonOrDie(File json, Class<T> clazz) {
        return fromJsonOrDie(FileUtil.toStringOrDie(json), clazz);
    }

    public static <T> T json(String json, Class<T> clazz) { return fromJsonOrDie(json, clazz); }

    public static <T> T json(String json, Class<T> clazz, ObjectMapper mapper) { return fromJsonOrDie(json, clazz, mapper); }

    public static <T> T json(JsonNode json, Class<T> clazz) { return fromJsonOrDie(json, clazz); }

    public static <T> List<T> json(JsonNode[] json, Class<T> clazz) {
        final List<T> list = new ArrayList<>();
        for (JsonNode node : json) list.add(json(node, clazz));
        return list;
    }

    public static <T> T jsonWithComments(String json, Class<T> clazz) { return fromJsonOrDie(json, clazz, FULL_MAPPER_ALLOW_COMMENTS); }
    public static <T> T jsonWithComments(JsonNode json, Class<T> clazz) { return fromJsonOrDie(json(json), clazz, FULL_MAPPER_ALLOW_COMMENTS); }

    public static <T> T fromJsonOrDie(String json, Class<T> clazz) {
        return fromJsonOrDie(json, clazz, FULL_MAPPER);
    }
    public static <T> T fromJsonOrDie(String json, Class<T> clazz, ObjectMapper mapper) {
        if (empty(json)) return null;
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            final String msg = "fromJsonOrDie: exception while reading: " + json + ": " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }

    public static <T> T fromJson(String json, String path, Class<T> clazz) throws Exception {
        return fromJson(FULL_MAPPER.readTree(json), path, clazz);
    }

    public static <T> T fromJson(File json, String path, Class<T> clazz) throws Exception {
        return fromJson(FULL_MAPPER.readTree(json), path, clazz);
    }

    public static <T> T fromJson(JsonNode child, Class<? extends T> childClass) throws Exception {
        return fromJson(child, "", childClass);
    }

    public static <T> T fromJsonOrDie(JsonNode child, Class<? extends T> childClass) {
        return fromJsonOrDie(child, "", childClass);
    }

    public static <T> T fromJsonOrDie(JsonNode node, String path, Class<T> clazz) {
        return fromJsonOrDie(node, path, clazz, FULL_MAPPER);
    }

    public static <T> T fromJson(JsonNode node, String path, Class<T> clazz) throws Exception {
        return fromJson(node, path, clazz, FULL_MAPPER);
    }

    public static <T> T fromJsonOrDie(JsonNode node, String path, Class<T> clazz, ObjectMapper mapper) {
        try {
            return fromJson(node, path, clazz, mapper);
        } catch (Exception e) {
            final String msg = "fromJsonOrDie: exception while reading: " + node + ": " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }
    public static <T> T fromJson(JsonNode node, String path, Class<T> clazz, ObjectMapper mapper) throws Exception {
        node = findNode(node, path);
        return mapper.convertValue(node, clazz);
    }

    public static JsonNode findNode(JsonNode node, String path) throws IOException {
        if (node == null || path == null) return null;
        final List<JsonNode> nodePath = findNodePath(node, path);
        if (nodePath == null || nodePath.isEmpty()) return null;
        final JsonNode lastNode = nodePath.get(nodePath.size()-1);
        return lastNode == MISSING ? null : lastNode;
    }

    public static String toString(Object node) throws JsonProcessingException {
        return node == null ? null : FULL_MAPPER.writeValueAsString(node);
    }

    public static String nodeValue (JsonNode node, String path) throws IOException {
        return fromJsonOrDie(toString(findNode(node, path)), String.class);
    }

    public static List<JsonNode> findNodePath(JsonNode node, String path) throws IOException {

        final List<JsonNode> nodePath = new ArrayList<>();
        nodePath.add(node);
        if (empty(path)) return nodePath;
        final List<String> pathParts = tokenize(path);

        for (String pathPart : pathParts) {
            int index = -1;
            int bracketPos = pathPart.indexOf("[");
            int bracketClosePos = pathPart.indexOf("]");
            boolean isEmptyBrackets = false;
            if (bracketPos != -1 && bracketClosePos != -1 && bracketClosePos > bracketPos) {
                if (bracketClosePos == bracketPos+1) {
                    // ends with [], they mean to append
                    isEmptyBrackets = true;
                } else {
                    index = Integer.parseInt(pathPart.substring(bracketPos + 1, bracketClosePos));
                }
                pathPart = pathPart.substring(0, bracketPos);
            }
            if (!empty(pathPart)) {
                node = node.get(pathPart);
                if (node == null) {
                    nodePath.add(MISSING);
                    return nodePath;
                }
                nodePath.add(node);

            } else if (nodePath.size() > 1) {
                return die("findNodePath: invalid path: "+path);
            }
            if (index != -1) {
                node = node.get(index);
                nodePath.add(node);

            } else if (isEmptyBrackets) {
                nodePath.add(MISSING);
                return nodePath;
            }
        }
        return nodePath;
    }

    public static List<String> tokenize(String path) {
        final List<String> pathParts = new ArrayList<>();
        final StringTokenizer st = new StringTokenizer(path, ".'", true);
        boolean collectingQuotedToken = false;
        StringBuffer pathToken = new StringBuffer();
        while (st.hasMoreTokens()) {
            final String token = st.nextToken();
            if (token.equals("'")) {
                collectingQuotedToken = !collectingQuotedToken;

            } else if (collectingQuotedToken) {
                pathToken.append(token);

            } else if (token.equals(".") && pathToken.length() > 0) {
                pathParts.add(pathToken.toString());
                pathToken = new StringBuffer();

            } else {
                pathToken.append(token);
            }
        }
        if (collectingQuotedToken) throw new IllegalArgumentException("Unterminated single quote in: "+path);
        if (pathToken.length() > 0) pathParts.add(pathToken.toString());
        return pathParts;
    }

    public static ObjectNode replaceNode(File file, String path, String replacement) throws Exception {
        return replaceNode((ObjectNode) FULL_MAPPER.readTree(file), path, replacement);
    }

    public static ObjectNode replaceNode(String json, String path, String replacement) throws Exception {
        return replaceNode((ObjectNode) FULL_MAPPER.readTree(json), path, replacement);
    }

    public static ObjectNode replaceNode(ObjectNode document, String path, String replacement) throws Exception {

        final String simplePath = path.contains(".") ? path.substring(path.lastIndexOf(".")+1) : path;
        Integer index = null;
        if (simplePath.contains("[")) {
            index = Integer.parseInt(simplePath.substring(simplePath.indexOf("[")+1, simplePath.indexOf("]")));
        }
        final List<JsonNode> found = findNodePath(document, path);
        if (found == null || found.isEmpty() || found.get(found.size()-1).equals(MISSING)) {
            throw new IllegalArgumentException("path not found: "+path);
        }

        final JsonNode parent = found.size() > 1 ? found.get(found.size()-2) : document;
        if (index != null) {
            final JsonNode origNode = ((ArrayNode) parent).get(index);
            ((ArrayNode) parent).set(index, getValueNode(origNode, path, replacement));
        } else {
            // what is the original node type?
            final JsonNode origNode = parent.get(simplePath);
            ((ObjectNode) parent).set(simplePath, getValueNode(origNode, path, replacement));
        }
        return document;
    }

    public static JsonNode getValueNode(JsonNode node, String path, String replacement) {
        final String nodeClass = node.getClass().getName();
        if ( ! (node instanceof ValueNode) ) die("Path "+path+" does not refer to a value (it is a "+ nodeClass +")");
        if (node instanceof TextNode) return new TextNode(replacement);
        if (node instanceof BooleanNode) return BooleanNode.valueOf(Boolean.parseBoolean(replacement));
        if (node instanceof IntNode) return new IntNode(Integer.parseInt(replacement));
        if (node instanceof LongNode) return new LongNode(Long.parseLong(replacement));
        if (node instanceof DoubleNode) return new DoubleNode(Double.parseDouble(replacement));
        if (node instanceof DecimalNode) return new DecimalNode(big(replacement));
        if (node instanceof BigIntegerNode) return new BigIntegerNode(new BigInteger(replacement));
        return die("Path "+path+" refers to an unsupported ValueNode: "+ nodeClass);
    }

    public static Object getNodeAsJava(JsonNode node, String path) {

        if (node == null || node instanceof NullNode) return null;
        final String nodeClass = node.getClass().getName();

        if (node instanceof ArrayNode) {
            final Object[] array = new Object[node.size()];
            for (int i=0; i<node.size(); i++) {
                array[i] = getNodeAsJava(node.get(i), path+"["+i+"]");
            }
            return array;
        }

        if (node instanceof ObjectNode) {
            final Map<String, Object> map = new HashMap<>(node.size());
            for (Iterator<String> iter = node.fieldNames(); iter.hasNext(); ) {
                final String name = iter.next();
                map.put(name, getNodeAsJava(node.get(name), path+"."+name));
            }
            return map;
        }

        if ( ! (node instanceof ValueNode) ) return node; // return as-is...
        if (node instanceof TextNode) return node.textValue();
        if (node instanceof BooleanNode) return node.booleanValue();
        if (node instanceof IntNode) return node.intValue();
        if (node instanceof LongNode) return node.longValue();
        if (node instanceof DoubleNode) return node.doubleValue();
        if (node instanceof DecimalNode) return node.decimalValue();
        if (node instanceof BigIntegerNode) return node.bigIntegerValue();
        return die("Path "+path+" refers to an unsupported ValueNode: "+ nodeClass);
    }

    public static JsonNode getValueNode(Object data) {
        if (data == null) return NullNode.getInstance();
        if (data instanceof Integer) return new IntNode((Integer) data);
        if (data instanceof Boolean) return BooleanNode.valueOf((Boolean) data);
        if (data instanceof Long) return new LongNode((Long) data);
        if (data instanceof Float) return new DoubleNode((Float) data);
        if (data instanceof Double) return new DoubleNode((Double) data);
        if (data instanceof BigDecimal) return new DecimalNode((BigDecimal) data);
        if (data instanceof BigInteger) return new BigIntegerNode((BigInteger) data);
        return die("Cannot create value node from: "+data+" (type "+data.getClass().getName()+")");
    }

    public static JsonNode toNode (File f) { return fromJsonOrDie(FileUtil.toStringOrDie(f), JsonNode.class); }

    // adapted from: https://stackoverflow.com/a/11459962/1251543
    public static JsonNode mergeNodes(JsonNode mainNode, JsonNode updateNode) {

        final Iterator<String> fieldNames = updateNode.fieldNames();
        while (fieldNames.hasNext()) {
            final String fieldName = fieldNames.next();
            final JsonNode jsonNode = mainNode.get(fieldName);
            // if field exists and is an embedded object
            if (jsonNode != null && jsonNode.isObject()) {
                mergeNodes(jsonNode, updateNode.get(fieldName));
            } else {
                if (mainNode instanceof ObjectNode) {
                    // Overwrite field
                    final JsonNode value = updateNode.get(fieldName);
                    ((ObjectNode) mainNode).set(fieldName, value);
                }
            }
        }

        return mainNode;
    }

    public static String mergeJsonOrDie(String json, String request) {
        try {
            return mergeJson(json, request);
        } catch (Exception e) {
            final String msg = "mergeJsonOrDie: " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }
    public static String mergeJson(String json, String request) throws Exception {
        return mergeJson(json, fromJson(request, JsonNode.class));
    }

    public static String mergeJson(String json, Object request) throws Exception {
        return json(mergeJsonNodes(json, request));
    }

    public static JsonNode mergeJsonNodes(String json, Object request) throws Exception {
        if (request != null) {
            if (json != null) {
                final JsonNode current = fromJson(json, JsonNode.class);
                final JsonNode update;
                if (request instanceof JsonNode) {
                    update = (JsonNode) request;
                } else {
                    update = PUBLIC_MAPPER.valueToTree(request);
                }
                mergeNodes(current, update);
                return current;
            } else {
                return PUBLIC_MAPPER.valueToTree(request);
            }
        }
        return json(json, JsonNode.class);
    }

    public static JsonNode mergeJsonNodesOrDie(String json, Object request) {
        try {
            return mergeJsonNodes(json, request);
        } catch (Exception e) {
            final String msg = "mergeJsonNodesOrDie: " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }

    public static String mergeJsonOrDie(String json, Object request) {
        try {
            return mergeJson(json, request);
        } catch (Exception e) {
            final String msg = "mergeJsonOrDie: " + shortError(e);
            return verboseErrors() ? die(msg, e) : die(msg);
        }
    }

    public static JsonStringEncoder getJsonStringEncoder() { return JsonStringEncoder.getInstance(); }

    public static String jsonQuoteRegex (String val) {
        return val.replaceAll("([-/^$*+?.()|\\[\\]{}])", "\\\\$1");
    }

}
