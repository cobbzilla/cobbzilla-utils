package org.cobbzilla.util.io;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class MultiReader extends Reader {

    private final List<Reader> readers = new ArrayList<>();
    private Reader currentReader;
    private int readerIndex = 0;
    private boolean endOfReaders = false;

    public MultiReader (Reader r) {
        addReader(r);
        currentReader = r;
    }

    public void addReader (Reader r) {
        synchronized (readers) {
            readers.add(r);
        }
    }
    public void addLastReader (Reader r) {
        addReader(r);
        endOfReaders = true;
    }

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
