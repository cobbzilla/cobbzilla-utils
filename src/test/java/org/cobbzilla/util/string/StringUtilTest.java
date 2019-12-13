package org.cobbzilla.util.string;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class StringUtilTest {

    public static final String[][] TESTS = new String[][] {
            {"simple", "simple"},
            {"two terms", "two", "terms"},
            {"multiple   spaces", "multiple", "spaces"},
            {"first \"something in quotes\" last", "first", "something in quotes", "last"},
            {"first \"something in quotes    with spaces\" last", "first", "something in quotes with spaces", "last"},
            {"first e:\"exact phrase\" last", "first", "e:", "exact phrase", "last"},
            {"R:first \"exact phrase\" last", "R:first", "exact phrase", "last"}
    };

    @Test public void testSplitIntoTerms () throws Exception {
        for (String[] test : TESTS) {
            final List<String> terms = StringUtil.splitIntoTerms(test[0]);
            assertEquals("wrong # terms: "+test[0], test.length-1, terms.size());
            for (int i=1; i<test.length-1; i++) {
                assertEquals("wrong term at index: "+i+" for test "+test[0], test[i], terms.get(i-1));
            }
        }
    }

}
