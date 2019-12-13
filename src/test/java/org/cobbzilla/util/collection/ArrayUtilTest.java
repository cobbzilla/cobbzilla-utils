package org.cobbzilla.util.collection;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class ArrayUtilTest {

    @Test public void testSlice () throws Exception {

        assertTrue(Arrays.deepEquals(ArrayUtil.slice(
                new String[]{"one", "two", "three"}, 0, 2),
                new String[]{"one", "two"}));

        assertTrue(Arrays.deepEquals(ArrayUtil.slice(
                new String[]{"one", "two", "three"}, 0, 0),
                new String[0]));

        assertTrue(Arrays.deepEquals(ArrayUtil.slice(
                new String[]{"one", "two", "three"}, 2, 2),
                new String[0]));

        assertTrue(Arrays.deepEquals(ArrayUtil.slice(
                new String[]{"one", "two", "three"}, 3, 3),
                new String[0]));

        assertTrue(Arrays.deepEquals(ArrayUtil.slice(
                new String[]{"one", "two", "three"}, 0, 3),
                new String[]{"one", "two", "three"}));

        assertTrue(Arrays.deepEquals(ArrayUtil.slice(
                new String[]{"one", "two", "three"}, 1, 3),
                new String[]{"two", "three"}));

        assertTrue(Arrays.deepEquals(ArrayUtil.slice(
                new String[]{"one", "two", "three"}, 2, 3),
                new String[]{"three"}));

    }
}
