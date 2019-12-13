package org.cobbzilla.util.collection;

import java.math.BigDecimal;
import java.util.Comparator;

// adapted from: https://stackoverflow.com/a/2683388/1251543
public class NumberComparator implements Comparator<Number> {

    public static final NumberComparator INSTANCE = new NumberComparator();

    public int compare(Number a, Number b){
        return new BigDecimal(a.toString()).compareTo(new BigDecimal(b.toString()));
    }

}
