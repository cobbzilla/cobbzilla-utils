package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.io.FilterInputStreamViaOutputStream;
import org.cobbzilla.util.system.Bytes;

import java.io.*;
import java.util.zip.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@AllArgsConstructor
public enum HttpContentEncodingType {

    identity (BufferedInputStream::new, BufferedOutputStream::new, BufferedOutputStream.class),

    gzip (GZIPInputStream::new, GZIPOutputStream::new, GZIPOutputStream.class),

    deflate ((in, bufsiz) -> new InflaterInputStream(in, new Inflater(true), bufsiz),
             out -> new DeflaterOutputStream(out, new Deflater(7, true)),
             in -> new FilterInputStreamViaOutputStream(in, out -> new InflaterOutputStream(out, new Inflater(true)))),

    br (HttpContentEncodingType::wrapBrotliInput, BrotliOutputStream::new, BrotliOutputStream.class),
    bro (HttpContentEncodingType::wrapBrotliInput, BrotliOutputStream::new, BrotliOutputStream.class);

    public static final int DEFAULT_IN_BUFSIZ = (int) (8 * Bytes.KB);

    static {
        if (!BrotliLoader.isBrotliAvailable()) die("BrotliLoader.isBrotliAvailable() returned false");
    }

    public interface HttpContentEncodingInputWrapper {
        InputStream wrap(InputStream in, int bufsiz) throws IOException;
    }

    public interface HttpContentEncodingOutputWrapper {
        OutputStream wrap(OutputStream out) throws IOException;
    }

    public interface HttpContentEncodingInputAsOutputWrapper {
        FilterInputStreamViaOutputStream wrap(InputStream in) throws IOException;
    }

    private final HttpContentEncodingInputWrapper inputWrapper;
    private final HttpContentEncodingOutputWrapper outputWrapper;
    private final HttpContentEncodingInputAsOutputWrapper inputAsOutputWrapper;

    HttpContentEncodingType (HttpContentEncodingInputWrapper inWrap,
                             HttpContentEncodingOutputWrapper outWrap,
                             Class<? extends OutputStream> inAsOutClass) {
        this.inputWrapper = inWrap;
        this.outputWrapper = outWrap;
        this.inputAsOutputWrapper = in -> new FilterInputStreamViaOutputStream(in, inAsOutClass);
    }

    @JsonCreator public static HttpContentEncodingType fromString (String v) { return valueOf(v.toLowerCase()); }

    private static InputStream wrapBrotliInput(InputStream in, int bufsiz) throws IOException {
        final BrotliInputStream brIn = new BrotliInputStream(in, bufsiz);
        brIn.enableEagerOutput();
        return brIn;
    }

    public InputStream wrapInput(InputStream in) throws IOException { return inputWrapper.wrap(in, DEFAULT_IN_BUFSIZ); }
    public InputStream wrapInput(InputStream in, int bufsiz) throws IOException { return inputWrapper.wrap(in, bufsiz); }

    public OutputStream wrapOutput(OutputStream out) throws IOException { return outputWrapper.wrap(out); }

    public FilterInputStreamViaOutputStream wrapInputAsOutput(InputStream in) throws IOException { return inputAsOutputWrapper.wrap(in); }

}
