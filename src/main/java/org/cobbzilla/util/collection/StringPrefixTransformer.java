package org.cobbzilla.util.collection;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.Transformer;

@AllArgsConstructor
public class StringPrefixTransformer implements Transformer {

    @Getter private String prefix;

    @Override public Object transform(Object input) { return prefix + input.toString(); }

}
