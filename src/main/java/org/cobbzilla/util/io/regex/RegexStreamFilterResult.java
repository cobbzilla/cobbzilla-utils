package org.cobbzilla.util.io.regex;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
class RegexStreamFilterResult {
    @Getter private final StringBuilder result;
    @Getter private final int lastMatchEnd;
}
