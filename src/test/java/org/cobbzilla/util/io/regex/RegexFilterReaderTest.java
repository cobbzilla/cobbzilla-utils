package org.cobbzilla.util.io.regex;

import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.io.MultiReader;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import static org.cobbzilla.util.daemon.ZillaRuntime.background;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class RegexFilterReaderTest {

    public static final String TEST_STRING_1 = "this is a string\nand another string with a lone a near the end\nfoo.";
    public static final String EXPECTED_STRING = "this is X string\nand another string with X lone X near the end\nfoo.";

    @Test public void testSimpleRegexReader() throws Exception {
        final Reader reader = new StringReader(TEST_STRING_1);
        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" a ", 0, " X ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(reader, 1024, regexStreamFilter);
        final StringWriter result = new StringWriter();
        IOUtils.copyLarge(regexFilterReader, result);
        assertEquals("multi reader failed to get expected output", EXPECTED_STRING, result.toString());
    }

    @Test public void testSmallBufferRegexReader() throws Exception {
        final Reader reader = new StringReader(TEST_STRING_1);
        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" a ", 0, " X ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(reader, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();
        IOUtils.copyLarge(regexFilterReader, result);
        assertEquals("multi reader failed to get expected output", EXPECTED_STRING, result.toString());
    }

    public static final String MULTI_TEST_STRING_1 = "this is a multi-stream test string\nthat should has a lot of stuff";
    public static final String MULTI_TEST_STRING_2 = "in it but why should that matter?\nit is a bad thing to have multiple streams?";
    public static final String MULTI_TEST_STRING_3 = "maybe some people think so\nbut a good person would never say that";

    public static final String EXPECTED_MULTI_RESULT
            = "this is X multi-stream test string\nthat should has X lot of stuff"
            + "in it but why should that matter?\nit is X bad thing to have multiple streams?"
            + "maybe some people think so\nbut X good person would never say that";

    @Test public void testMultiStreamRegexReader() throws Exception {
        final Reader reader1 = new StringReader(MULTI_TEST_STRING_1);
        final Reader reader2 = new StringReader(MULTI_TEST_STRING_2);
        final Reader reader3 = new StringReader(MULTI_TEST_STRING_3);
        final MultiReader multiReader = new MultiReader(reader1);

        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" a ", 0, " X ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(multiReader, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();

        final Thread t = background(() -> {
            try {
                IOUtils.copyLarge(regexFilterReader, result);
            } catch (IOException e) {
                die("Error copying in background: "+e, e);
            }
        });

        sleep(500);
        multiReader.addReader(reader2);
        sleep(500);
        multiReader.addLastReader(reader3);

        t.join(1000);
        assertFalse("Expected copy thread to finish", t.isAlive());
        assertEquals("multi reader failed to get expected output", EXPECTED_MULTI_RESULT, result.toString());
    }

    public static final String MULTI2_TEST_STRING_1 = "this is a multi-stream barbecue string\nthat should has a lot of barbe";
    public static final String MULTI2_TEST_STRING_2 = "cue stuff in it but why should that matter?\nit is a bad thing to barbecue multiple barbecues?";
    public static final String MULTI2_TEST_STRING_3 = "maybe some people think so\nbut a good person would never say that about ba";
    public static final String MULTI2_TEST_STRING_4 = "rbecues because barbecues are so totally awesome";

    public static final String EXPECTED_MULTI_RESULT2
            = "this is a multi-stream BBQ string\nthat should has a lot of BBQ "
            + "stuff in it but why should that matter?\nit is a bad thing to BBQ multiple BBQs?"
            + "maybe some people think so\nbut a good person would never say that about BBQs "
            + "because BBQs are so totally awesome";

    @Test public void testMultiStreamRegexReaderWithRegexAcrossBoundary() throws Exception {
        final Reader reader1 = new StringReader(MULTI2_TEST_STRING_1);
        final Reader reader2 = new StringReader(MULTI2_TEST_STRING_2);
        final Reader reader3 = new StringReader(MULTI2_TEST_STRING_3);
        final Reader reader4 = new StringReader(MULTI2_TEST_STRING_4);
        final MultiReader multiReader = new MultiReader(reader1);

        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" barbecue", 0, " BBQ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(multiReader, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();

        final Thread t = background(() -> {
            try {
                IOUtils.copyLarge(regexFilterReader, result);
            } catch (IOException e) {
                die("Error copying in background: "+e, e);
            }
        });

        sleep(500);
        multiReader.addReader(reader2);
        sleep(500);
        multiReader.addReader(reader3);
        sleep(500);
        multiReader.addLastReader(reader4);

        t.join(1000);
        assertFalse("Expected copy thread to finish", t.isAlive());
        assertEquals("multi reader failed to get expected output", EXPECTED_MULTI_RESULT2, result.toString());
    }}