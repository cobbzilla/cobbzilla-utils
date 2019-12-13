package org.cobbzilla.util.io.regex;

import com.fasterxml.jackson.databind.JsonNode;

public interface RegexStreamFilter {

    default void configure (JsonNode config) {}

    RegexFilterResult apply(StringBuilder buffer, boolean eof);

}
