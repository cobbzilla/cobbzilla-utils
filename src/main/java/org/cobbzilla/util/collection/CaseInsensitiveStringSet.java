package org.cobbzilla.util.collection;

import java.util.Collection;
import java.util.TreeSet;

public class CaseInsensitiveStringSet extends TreeSet<String> {

    public CaseInsensitiveStringSet() { super(String.CASE_INSENSITIVE_ORDER); }

    public CaseInsensitiveStringSet(Collection<String> c) { addAll(c); }

}
