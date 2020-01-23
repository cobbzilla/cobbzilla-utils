package org.cobbzilla.util.io;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.system.Bytes;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@Slf4j
public class FilterInputStreamViaOutputStreamTest {

    @Test public void testGzipFilterInputStream () throws Exception {

        final String testData = RandomStringUtils.random((int) (256 * Bytes.KB));
        log.info("testData has "+testData.getBytes().length+ " bytes");

        // compress testData to byte array the normal way, using a GZIPOutputStream
        final ByteArrayOutputStream expected = new ByteArrayOutputStream();
        try (OutputStream gzout = new GZIPOutputStream(expected)) {
            IOUtils.copyLarge(new ByteArrayInputStream(testData.getBytes()), gzout);
        }
        log.info("expected byte array has "+expected.toByteArray().length+" bytes");

        // sanity check that standard decompression gets us back to where we started
        final ByteArrayOutputStream check = new ByteArrayOutputStream();
        try (InputStream checkIn = new GZIPInputStream(new ByteArrayInputStream(expected.toByteArray()))) {
            IOUtils.copyLarge(checkIn, check);
        }
        assertArrayEquals("uncompressed data did not match testData", check.toByteArray(), testData.getBytes());
        log.info("(check) uncompressed data has "+check.toByteArray().length+" bytes");

        // Read uncompressed data using FilterInputStreamViaOutputStream, should yield same compressed bytes as the buffer
        final FilterInputStreamViaOutputStream filter = new FilterInputStreamViaOutputStream(new ByteArrayInputStream(testData.getBytes()), GZIPOutputStream.class);
        final ByteArrayOutputStream actual = new ByteArrayOutputStream();
        final long copied = IOUtils.copyLarge(filter, actual);
        log.info("copied "+copied+" bytes, actual has "+actual.toByteArray().length+" bytes");

        // Decompress what we just read, we should end up back at testData
        final ByteArrayOutputStream finalCheck = new ByteArrayOutputStream();
        try (InputStream finalCheckIn = new GZIPInputStream(new ByteArrayInputStream(actual.toByteArray()))) {
            IOUtils.copyLarge(finalCheckIn, finalCheck);
        }

        assertEquals("testData was not preserved", testData, new String(finalCheck.toByteArray()));
    }

}
