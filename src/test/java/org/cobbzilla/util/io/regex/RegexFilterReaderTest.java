package org.cobbzilla.util.io.regex;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.io.BlockedInputStream;
import org.cobbzilla.util.io.multi.MultiReader;
import org.cobbzilla.util.io.multi.MultiStream;
import org.junit.Test;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.background;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.multi.MultiUnderflowHandlerMonitor.DEFAULT_UNDERFLOW_MONITOR;
import static org.cobbzilla.util.io.regex.RegexReplacementFilter.DEFAULT_PREFIX_REPLACEMENT_WITH_MATCH;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.junit.Assert.*;

@Slf4j
public class RegexFilterReaderTest {

    public static final String TEST_STRING_1 = "this is a string\nand another string with a lone a near the end\nfoo.";
    public static final String EXPECTED_STRING = "this is X string\nand another string with X lone X near the end\nfoo.";

    @Test public void testSimpleRegexReader() throws Exception {
        final Reader reader = new StringReader(TEST_STRING_1);
        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" a ", " X ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(reader, 1024, regexStreamFilter);
        final StringWriter result = new StringWriter();
        IOUtils.copyLarge(regexFilterReader, result);
        assertEquals("multi reader failed to get expected output", EXPECTED_STRING, result.toString());
    }

    @Test public void testSmallBufferRegexReader() throws Exception {
        final Reader reader = new StringReader(TEST_STRING_1);
        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" a ", " X ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(reader, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();
        IOUtils.copyLarge(regexFilterReader, result);
        assertEquals("multi reader failed to get expected output", EXPECTED_STRING, result.toString());
    }

    public static final String TEST_STRING_INCLUDE_MATCH = "<!DOCTYPE html>\n<html dir=\"ltr\" lang=\"en\">\n<meta charset=\"utf-8\">something</html>\n";
    public static final String EXPECTED_STRING_INCLUDE_MATCH = "<!DOCTYPE html>\n<html dir=\"ltr\" lang=\"en\">INSERTED_DATA\n<meta charset=\"utf-8\">something</html>\n";

    @Test public void testRegexReaderIncludeMatch() throws Exception {
        final Reader reader = new StringReader(TEST_STRING_INCLUDE_MATCH);
        final RegexStreamFilter regexStreamFilter
                = new RegexReplacementFilter("<html\\s+[^>]*>", DEFAULT_PREFIX_REPLACEMENT_WITH_MATCH+"INSERTED_DATA");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(reader, 1024, regexStreamFilter);
        final StringWriter result = new StringWriter();
        IOUtils.copyLarge(regexFilterReader, result);
        assertEquals("multi reader failed to get expected output", EXPECTED_STRING_INCLUDE_MATCH, result.toString());
    }

    public static final String TEST_STRING_INCLUDE_MATCH_MIDDLE = "<!DOCTYPE html>\n<html dir=\"ltr\" lang=\"en\">\n<meta charset=\"utf-8\">something</html>\n";
    public static final String EXPECTED_STRING_INCLUDE_MATCH_MIDDLE = "<!DOCTYPE html>\nINSERTED_<html dir=\"ltr\" lang=\"en\">DATA\n<meta charset=\"utf-8\">something</html>\n";

    @Test public void testRegexReaderIncludeMatchInMiddle() throws Exception {
        final Reader reader = new StringReader(TEST_STRING_INCLUDE_MATCH_MIDDLE);
        final RegexStreamFilter regexStreamFilter
                = new RegexReplacementFilter("<html\\s+[^>]*>", "INSERTED_"+DEFAULT_PREFIX_REPLACEMENT_WITH_MATCH+"DATA");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(reader, 1024, regexStreamFilter);
        final StringWriter result = new StringWriter();
        IOUtils.copyLarge(regexFilterReader, result);
        assertEquals("multi reader failed to get expected output", EXPECTED_STRING_INCLUDE_MATCH_MIDDLE, result.toString());
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

        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" a ", " X ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(multiReader, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();

        final Thread t = background(() -> {
            try {
                IOUtils.copyLarge(regexFilterReader, result);
            } catch (IOException e) {
                die("Error copying in background: "+e, e);
            }
        }, "RegexFilterReaderTest.testMultiStreamRegexReader");

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

        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" barbecue", " BBQ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(multiReader, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();

        final Thread t = background(() -> {
            try {
                IOUtils.copyLarge(regexFilterReader, result);
            } catch (IOException e) {
                die("Error copying in background: "+e, e);
            }
        }, "RegexFilterReaderTest.testMultiStreamRegexReaderWithRegexAcrossBoundary");

        sleep(500);
        multiReader.addReader(reader2);
        sleep(500);
        multiReader.addReader(reader3);
        sleep(500);
        multiReader.addLastReader(reader4);

        t.join(1000);
        assertFalse("Expected copy thread to finish", t.isAlive());
        assertEquals("multi reader failed to get expected output", EXPECTED_MULTI_RESULT2, result.toString());
    }

    @Test public void testMultiReaderUnderflow() throws Exception {
        final StringReader reader1 = new StringReader("some test data1 ".repeat(1000));
        final StringReader reader2 = new StringReader("some test data2 ".repeat(1000));
        final StringReader reader3 = new StringReader("some test data3 ".repeat(1000));

        final MultiReader multiReader = new MultiReader(reader1);
        multiReader.getUnderflow()
                .setMinUnderflowSleep(1000)
                .setMaxUnderflowSleep(1000)
                .setUnderflowTimeout(5000);

        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" test ", " bogus ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(multiReader, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();

        final AtomicReference<Exception> exRef = new AtomicReference<>(null);
        final Thread t = background(() -> {
            try {
                IOUtils.copyLarge(regexFilterReader, result);
            } catch (IOException e) {
                exRef.set(e);
            }
        }, "RegexFilterReaderTest.testMultiReaderUnderflow");

        sleep(multiReader.getUnderflow().getMaxUnderflowSleep());
        log.info("adding reader2...");
        multiReader.addReader(reader2);
        log.info("added reader2...");
        sleep(multiReader.getUnderflow().getUnderflowTimeout()*2);
        log.info("adding reader3...");
        multiReader.addReader(reader3);
        log.info("added reader3...");

        t.join(multiReader.getUnderflow().getUnderflowTimeout()+100);
        assertFalse("Expected copy thread to finish", t.isAlive());
        assertNotNull("Expected copy thread to have an exception", exRef.get());
        assertTrue("expected multi reader failed to get data1 output", result.toString().contains(" bogus data1 "));
        assertTrue("expected multi reader failed to get data2 output", result.toString().contains(" bogus data2 "));
        assertFalse("expected multi reader failed to NOT get data3 output", result.toString().contains(" bogus data3 "));
    }

    @Test public void testMultiStreamUnderflow() throws Exception {
        final InputStream stream1 = new ByteArrayInputStream("some test data1 ".repeat(1000).getBytes());
        final InputStream stream2 = new ByteArrayInputStream("some test data2 ".repeat(1000).getBytes());
        final InputStream stream3 = new BlockedInputStream();

        DEFAULT_UNDERFLOW_MONITOR.setCheckInterval(1000);
        final MultiStream multiStream = new MultiStream(stream1);
        multiStream.getUnderflow()
                .setMinUnderflowSleep(1000)
                .setMaxUnderflowSleep(1000)
                .setUnderflowTimeout(5000);
        multiStream.addStream(stream2);
        multiStream.addStream(stream3);

        final RegexStreamFilter regexStreamFilter = new RegexReplacementFilter(" test ", " bogus ");
        final RegexFilterReader regexFilterReader = new RegexFilterReader(multiStream, 8, regexStreamFilter);
        final StringWriter result = new StringWriter();

        final AtomicReference<Exception> exRef = new AtomicReference<>(null);
        final Thread t = background(() -> {
            try {
                IOUtils.copyLarge(regexFilterReader, result);
            } catch (Exception e) {
                exRef.set(e);
            }
        }, "RegexFilterReaderTest.testMultiStreamUnderflow");

        sleep(multiStream.getUnderflow().getUnderflowTimeout()*2);

        t.join(multiStream.getUnderflow().getUnderflowTimeout()+100);
        assertFalse("Expected copy thread to finish", t.isAlive());
        assertNotNull("Expected copy thread to have an exception", exRef.get());
        assertTrue("expected multi stream failed to get data1 output", result.toString().contains(" bogus data1 "));
        assertTrue("expected multi stream failed to get data2 output", result.toString().contains(" bogus data2 "));
    }

}
