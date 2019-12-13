package org.cobbzilla.util.json.data;

import com.fasterxml.jackson.databind.JsonNode;

public class TestData {

    public String id;
    public Thing thing;
    public AnotherThing another_thing;

    public static class Thing {
        public String[] field1;
        public Field2 field2;
    }

    public static class Field2 {
        public String subfieldA;
        public String subB;
    }

    public static class AnotherThing {
        public String field1;
        public JsonNode fieldZ;
    }

}
