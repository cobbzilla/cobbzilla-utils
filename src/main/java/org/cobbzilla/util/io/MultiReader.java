package org.cobbzilla.util.io;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MultiReader extends Reader {

    private final List<Reader> readers = new ArrayList<>();
    private Reader currentReader;
    private int readerIndex = 0;
    private boolean endOfReaders = false;

    public MultiReader (Reader r, boolean last) {
        if (last) {
            addLastReader(r);
        } else {
            addReader(r);
        }
        currentReader = r;
    }

    public MultiReader (Reader r) { this(r, false); }

    public MultiReader (InputStream in) { this(new InputStreamReader(in), false); }

    public MultiReader (InputStream in, boolean last) { this(new InputStreamReader(in), last); }

    public void addReader (Reader r) {
        if (endOfReaders) {
            log.warn("addReader: endOfReaders is true, not adding reader");
        } else {
            readers.add(r);
        }
    }

    public void addLastReader (Reader r) {
        addReader(r);
        endOfReaders = true;
    }

    public void addStream (InputStream in) { addReader(new InputStreamReader(in)); }

    public void addLastStream (InputStream in) { addLastReader(new InputStreamReader(in)); }

    @Override public int read(char[] buf, int off, int len) throws IOException {
        int count = currentReader.read(buf, off, len);
        if (count == -1) {
            if (readerIndex == readers.size()-1) {
                return endOfReaders ? -1 : 0;
            }
            currentReader.close();
            readerIndex++;
            currentReader = readers.get(readerIndex);
            return read(buf, off, len);
        }
        return count;
    }

    @Override public void close() throws IOException {
        if (currentReader != null) currentReader.close();
    }
}
