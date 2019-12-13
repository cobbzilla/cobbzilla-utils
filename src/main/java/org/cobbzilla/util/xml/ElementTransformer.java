package org.cobbzilla.util.xml;

import org.w3c.dom.Element;

public interface ElementTransformer<T> {

    T transform (Element e);

}
