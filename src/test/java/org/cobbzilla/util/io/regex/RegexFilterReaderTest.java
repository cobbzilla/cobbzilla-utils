package org.cobbzilla.util.io.regex;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.cobbzilla.util.io.BlockedInputStream;
import org.cobbzilla.util.io.multi.MultiReader;
import org.cobbzilla.util.io.multi.MultiStream;
import org.cobbzilla.util.system.Bytes;
import org.junit.Test;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.background;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.multi.MultiUnderflowHandlerMonitor.DEFAULT_UNDERFLOW_MONITOR;
import static org.cobbzilla.util.io.regex.RegexReplacementFilter.DEFAULT_PREFIX_REPLACEMENT_WITH_MATCH;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;
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

    @Test public void testSimpleMultiStreamMark() throws Exception {
        final String data1 = "dat1\n".repeat(1024);
        final InputStream stream1 = new ByteArrayInputStream(data1.getBytes());
        final MultiStream multiStream = new MultiStream(stream1, true);

        multiStream.mark(data1.length());
        final byte[] buffer = new byte[(int) (Bytes.KB)];
        final String initialData = readStream(multiStream, buffer, buffer.length);
        assertTrue(data1.startsWith(initialData));

        multiStream.reset();
        final ByteArrayOutputStream out = new ByteArrayOutputStream(data1.length());
        IOUtils.copyLarge(multiStream, out);
        assertEquals("expected output == data1", data1, out.toString());
    }

    @Test public void testMultiStreamMark() throws Exception {
        final String data1 = "dt1\n".repeat(1024);
        final String data2 = "dt2\n".repeat(1024);
        final String allData = data1 + data2;
        final InputStream stream1 = new ByteArrayInputStream(data1.getBytes());
        final InputStream stream2 = new ByteArrayInputStream(data2.getBytes());

        final MultiStream multiStream = new MultiStream(stream1);
        multiStream.addLastStream(stream2);
        final byte[] buffer = new byte[(int) (2 * Bytes.KB)];

        // read 5K of data
        final int initialReadSize = (int) (5 * Bytes.KB);
        final String initialData = readStream(multiStream, buffer, initialReadSize);
        assertEquals(initialReadSize, initialData.length());
        assertTrue("expected initial read to start with dt1", initialData.startsWith(data1));
        assertTrue("expected initial read to contain some of dt2", initialData.contains("dt2\n"));

        // then mark
        multiStream.mark(allData.length());

        // then read some more
        final String moreData = readStream(multiStream, buffer, buffer.length);
        assertEquals(buffer.length, moreData.length());

        // verify what we read was the remainder of data2
        assertTrue("expected remainder read to contain dt2", moreData.contains("dt2\n"));
        assertFalse("expected remainder read to NOT contain dt1", moreData.contains("dt1"));

        // reset the stream
        multiStream.reset();

        // now read the remainder
        final ByteArrayOutputStream out = new ByteArrayOutputStream(allData.length());
        IOUtils.copyLarge(multiStream, out);
        final String remainderData = out.toString(UTF8cs);

        assertEquals( "expected initial + remainder == all", allData, initialData + remainderData);
    }

    @Test public void testMultiStreamExtendedMark() throws Exception {
        final String data1 = "dt1\n".repeat(1024);
        final String data2 = "dt2\n".repeat(1024);
        final String data3 = "dt3\n".repeat(1024);
        final String data4 = "dt4\n".repeat(1024);
        final String allData = data1 + data2 + data3 + data4;
        final InputStream stream1 = new ByteArrayInputStream(data1.getBytes());
        final InputStream stream2 = new ByteArrayInputStream(data2.getBytes());
        final InputStream stream3 = new ByteArrayInputStream(data3.getBytes());
        final InputStream stream4 = new ByteArrayInputStream(data4.getBytes());

        final MultiStream multiStream = new MultiStream(stream1);
        multiStream.addStream(stream2);
        multiStream.addStream(stream3);
        multiStream.addLastStream(stream4);
        final byte[] buffer = new byte[(int) (2 * Bytes.KB)];

        // read 5K of data
        final int initialReadSize = (int) (5 * Bytes.KB);
        final String initialData = readStream(multiStream, buffer, initialReadSize);
        assertEquals(initialReadSize, initialData.length());
        assertTrue("expected initial read to start with dt1", initialData.startsWith(data1));
        assertTrue("expected initial read to contain some of dt2", initialData.contains("dt2\n"));

        // then mark
        multiStream.mark(allData.length());

        // then read some more
        final String moreData = readStream(multiStream, buffer, data2.length());
        assertEquals(data2.length(), moreData.length());

        // verify what we read was the remainder of data2
        assertTrue("expected remainder read to contain dt2", moreData.contains("dt2\n"));
        assertTrue("expected remainder read to contain dt3", moreData.contains("dt3\n"));
        assertFalse("expected remainder read to NOT contain dt4", moreData.contains("dt4"));
        assertFalse("expected remainder read to NOT contain dt1", moreData.contains("dt1"));

        // reset the stream and re-mark
        multiStream.reset();
        multiStream.mark(allData.length());

        // do the same read again, should get the same data
        final String moreData2 = readStream(multiStream, buffer, data2.length());
        assertEquals(data2.length(), moreData.length());

        // verify what we read was the remainder of data2
        assertTrue("expected remainder read to contain dt2", moreData2.contains("dt2\n"));
        assertTrue("expected remainder read to contain dt3", moreData2.contains("dt3\n"));
        assertFalse("expected remainder read to NOT contain dt4", moreData2.contains("dt4"));
        assertFalse("expected remainder read to NOT contain dt1", moreData2.contains("dt1"));

        // verify reset read was the same
        assertEquals("expected to read the same data in moreData and moreData2", moreData, moreData2);

        // reset the stream again
        multiStream.reset();

        // now read the all the remainder
        final ByteArrayOutputStream out = new ByteArrayOutputStream(allData.length());
        IOUtils.copyLarge(multiStream, out);
        final String remainderData = out.toString(UTF8cs);

        assertEquals( "expected initial + remainder == all", allData, initialData + remainderData);
    }

    public String readStream(MultiStream multiStream, byte[] buffer, int size) throws IOException {
        final StringBuilder b = new StringBuilder(size);
        int bytesRead = 0;
        int count;
        while ((bytesRead < size
                && ((count = multiStream.read(buffer, 0, readSize(buffer, size, bytesRead))) != -1))) {
            bytesRead += count;
            b.append(new String(buffer, 0, count));
        }
        return b.toString();
    }

    private int readSize(byte[] buffer, int initialReadSize, int bytesRead) {
        if (bytesRead + buffer.length <= initialReadSize) return buffer.length;
        return buffer.length - (initialReadSize - bytesRead);
    }

}
