package org.cobbzilla.util.reflect;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.junit.Test;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.junit.Assert.*;

public class ReflectionUtilTest {

    @AllArgsConstructor
    public static class Dummy {
        @Getter @Setter public Long id;

        @Getter public String name;

        public void setName (String name) {
            this.name = name;
        }
        public void setName (Dummy something) {
            die("should not get called!");
        }
    }

    private static final String ID = "id";
    public static final String NAME = "name";

    @Test public void testGetSet () throws Exception {

        Long testValue = now();
        Dummy dummy = new Dummy(testValue, NAME);
        assertEquals(ReflectionUtil.get(dummy, ID), testValue);

        ReflectionUtil.set(dummy, ID, null);
        assertNull(ReflectionUtil.get(dummy, ID));

        testValue += 10;
        ReflectionUtil.set(dummy, ID, testValue);
        assertEquals(ReflectionUtil.get(dummy, ID), testValue);

        ReflectionUtil.setNull(dummy, ID, Long.class);
        assertNull(ReflectionUtil.get(dummy, ID));

        ReflectionUtil.set(dummy, NAME, "a value");
        assertEquals(ReflectionUtil.get(dummy, NAME), "a value");

        try {
            ReflectionUtil.set(dummy, NAME, null);
            fail("should not have been able to set name field to null");
        } catch (Exception expected) {}

        ReflectionUtil.setNull(dummy, NAME, String.class);
        assertNull(ReflectionUtil.get(dummy, NAME));


    }
}
