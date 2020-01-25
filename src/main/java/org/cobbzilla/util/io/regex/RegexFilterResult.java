package org.cobbzilla.util.io.regex;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RegexFilterResult {

    public StringBuilder buffer;
    public int remainder;
    public int matchCount;

}
