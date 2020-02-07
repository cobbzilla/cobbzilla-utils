package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.nixxcode.jvmbrotli.common.BrotliLoader;
import com.nixxcode.jvmbrotli.dec.BrotliInputStream;
import com.nixxcode.jvmbrotli.enc.BrotliOutputStream;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.io.FilterInputStreamViaOutputStream;

import java.io.*;
import java.util.zip.*;

@AllArgsConstructor
public enum HttpContentEncodingType {

    none (BufferedInputStream::new, BufferedOutputStream::new, BufferedOutputStream.class),

    gzip (GZIPInputStream::new, GZIPOutputStream::new, GZIPOutputStream.class),

    deflate (in -> new InflaterInputStream(in, new Inflater(true)),
             out -> new DeflaterOutputStream(out, new Deflater(7, true)),
             in -> new FilterInputStreamViaOutputStream(in, out -> new InflaterOutputStream(out, new Inflater(true)))),

    br (BrotliInputStream::new, BrotliOutputStream::new, BrotliOutputStream.class),
    bro (BrotliInputStream::new, BrotliOutputStream::new, BrotliOutputStream.class);

    static {
        BrotliLoader.isBrotliAvailable();
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

    public InputStream wrapInput(InputStream in) throws IOException { return inputWrapper.wrap(in); }

    public OutputStream wrapOutput(OutputStream out) throws IOException { return outputWrapper.wrap(out); }

    public FilterInputStreamViaOutputStream wrapInputAsOutput(InputStream in) throws IOException { return inputAsOutputWrapper.wrap(in); }

    public interface HttpContentEncodingInputWrapper {
        InputStream wrap(InputStream in) throws IOException;
    }

    public interface HttpContentEncodingOutputWrapper {
        OutputStream wrap(OutputStream out) throws IOException;
    }

    public interface HttpContentEncodingInputAsOutputWrapper {
        FilterInputStreamViaOutputStream wrap(InputStream in) throws IOException;
    }

}
