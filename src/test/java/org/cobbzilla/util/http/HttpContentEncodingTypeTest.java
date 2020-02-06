package org.cobbzilla.util.http;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.system.Bytes;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertArrayEquals;

public class HttpContentEncodingTypeTest {

    @Test public void testEncodingTypes () throws Exception {
        for (HttpContentEncodingType encoding : HttpContentEncodingType.values()) {
            testEncodingType(encoding);
        }
    }

    private void testEncodingType(HttpContentEncodingType encoding) throws IOException {
        final byte[] expectedData = RandomUtils.nextBytes((int) (65*Bytes.KB));
        final ByteArrayInputStream in = new ByteArrayInputStream(expectedData);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final OutputStream encOut = encoding.wrapOutput(out);
        IOUtils.copyLarge(in, encOut);
        in.close();
        encOut.close();

        final InputStream decIn = encoding.wrapInput(new ByteArrayInputStream(out.toByteArray()));
        final ByteArrayOutputStream actualData = new ByteArrayOutputStream();
        IOUtils.copyLarge(decIn, actualData);
        assertArrayEquals("data was not the same", expectedData, actualData.toByteArray());
    }
}
