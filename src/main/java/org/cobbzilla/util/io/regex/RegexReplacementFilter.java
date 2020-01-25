package org.cobbzilla.util.io.regex;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public class RegexReplacementFilter implements RegexStreamFilter {

    @Getter @Setter private Pattern pattern;
    @Getter @Setter private int group;
    @Getter @Setter private String replacement;

    public RegexReplacementFilter(String regex, int group, String replacement) {
        this.pattern = Pattern.compile(regex);
        this.group = group;
        this.replacement = replacement;
    }

    @Override public void configure(JsonNode config) {
        this.pattern = Pattern.compile(config.get("pattern").textValue());

        final JsonNode groupNode = config.get("group");
        this.group = groupNode == null ? 1 : groupNode.intValue();

        final JsonNode replacementNode = config.get("replacement");
        this.replacement = replacementNode == null ? "" : replacementNode.textValue();
    }

    public RegexFilterResult apply(StringBuilder buffer, boolean eof) {
        final StringBuilder result = new StringBuilder(buffer.length());
        int start = 0;
        final Matcher matcher = pattern.matcher(buffer.toString());
        int matchCount = 0;
        while (matcher.find(start)) {
            // add everything before the first match
            result.append(buffer.subSequence(start, matcher.start()));

            // add everything before the group match
            result.append(buffer.subSequence(matcher.start(), matcher.start(group)));

            // add the replacement
            result.append(replacement);

            // add everything after the group match
            result.append(buffer.subSequence(matcher.end(group), matcher.end()));

            // advance start pointer and track last match end
            start = matcher.end();

            matchCount++;
        }
        if (eof) {
            result.append(buffer.subSequence(start, buffer.length()));
            return new RegexFilterResult(result, 0, matchCount);
        }
        return new RegexFilterResult(result, buffer.length() - start, matchCount);
    }

}