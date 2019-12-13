package org.cobbzilla.util.io.regex;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;

@NoArgsConstructor @Accessors(chain=true)
public class RegexInsertionFilter implements RegexStreamFilter {

    @Getter @Setter private String pattern;
    @Getter @Setter private int flags = CASE_INSENSITIVE | MULTILINE;
    @Getter @Setter private int group = 1;
    @Getter @Setter private String before;
    @Getter @Setter private String after;

    @Getter(lazy=true) private final Pattern _pattern = Pattern.compile(pattern, flags);

    @Override public void configure(JsonNode config) {

        this.pattern = config.get("pattern").textValue();

        final JsonNode groupNode = config.get("group");
        this.group = groupNode == null ? 1 : groupNode.intValue();

        final JsonNode replacementNode = config.get("replacement");
        this.after = replacementNode == null ? "" : replacementNode.textValue();

        final JsonNode afterReplacementNode = config.get("after");
        this.after = afterReplacementNode == null ? "" : afterReplacementNode.textValue();

        final JsonNode beforeReplacementNode = config.get("before");
        this.before = beforeReplacementNode == null ? "" : beforeReplacementNode.textValue();
    }

    public RegexFilterResult apply(StringBuilder buffer, boolean eof) {
        final StringBuilder result = new StringBuilder(buffer.length());
        int start = 0;
        final Matcher matcher = get_pattern().matcher(buffer.toString());
        while (matcher.find(start)) {

            // add everything before the first match
            result.append(buffer.subSequence(start, matcher.start()));

            // add the before stuff
            if (before != null) result.append(before);

            // add the group match itself
            result.append(buffer.subSequence(matcher.start(group), matcher.end(group)));

            // add the after stuff
            if (after != null) result.append(after);

            // add everything after the group match
            result.append(buffer.subSequence(matcher.end(group), matcher.end()));

            // advance start pointer and track last match end
            start = matcher.end();
        }

        if (eof) {
            // we are at the end, include everything else, no remainder
            result.append(buffer.subSequence(start, buffer.length()));
            return new RegexFilterResult(result, 0);
        }

        // nothing matched
        // leave 1k remaining to reprocess, we might see our pattern again.
        final int totalRemainder = buffer.length() - start;
        if (totalRemainder > 1024) {
            result.append(buffer.subSequence(start, buffer.length()-1024));
            return new RegexFilterResult(result, 1024);
        } else {
            // leave the entire remainder, we can't be sure
            return new RegexFilterResult(result, totalRemainder);
        }
    }

}