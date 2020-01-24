package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.io.FilterInputStreamViaOutputStream;
import org.meteogroup.jbrotli.io.BrotliInputStream;
import org.meteogroup.jbrotli.io.BrotliOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.*;

@AllArgsConstructor
public enum HttpContentEncodingType {

    gzip (GZIPInputStream::new, GZIPOutputStream::new, GZIPOutputStream.class),

    deflate (in -> new InflaterInputStream(in, new Inflater(true)),
             out -> new DeflaterOutputStream(out, new Deflater(7, true)),
             in -> new FilterInputStreamViaOutputStream(in, out -> new InflaterOutputStream(out, new Inflater(true)))),

    br (BrotliInputStream::new, BrotliOutputStream::new, BrotliOutputStream.class),
    bro (BrotliInputStream::new, BrotliOutputStream::new, BrotliOutputStream.class);

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
