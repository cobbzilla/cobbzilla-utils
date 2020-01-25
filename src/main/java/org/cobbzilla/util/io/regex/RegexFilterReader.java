package org.cobbzilla.util.io.regex;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.cobbzilla.util.system.Bytes;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.string.StringUtil.EMPTY_CHAR_ARRAY;

@Slf4j @Accessors(chain=true)
public class RegexFilterReader extends BufferedReader {

    public static final int DEFAULT_BUFFER_SIZE = (int) (64 * Bytes.KB);

    private final int bufsiz;
    private RegexStreamFilter filter;
    @Getter @Setter private Integer maxMatches;
    @Getter @Setter private String name; // for debugging/identifying which reader

    public RegexFilterReader(Reader in, RegexStreamFilter filter) {
        this(in, DEFAULT_BUFFER_SIZE, filter);
    }

    public RegexFilterReader(Reader in, int bufsiz, RegexStreamFilter filter) {
        super(in, DEFAULT_BUFFER_SIZE);
        this.bufsiz = bufsiz;
        this.filter = filter;
    }

    public RegexFilterReader(InputStream in, RegexStreamFilter filter) {
        this(in, DEFAULT_BUFFER_SIZE, filter);
    }

    public RegexFilterReader(InputStream in, int bufsiz, RegexStreamFilter filter) {
        super(new InputStreamReader(in), bufsiz);
        this.bufsiz = bufsiz;
        this.filter = filter;
    }

    @Getter(lazy=true) private final FilterResponse filterResponse = initFilterResponse();
    private FilterResponse initFilterResponse() { return new FilterResponse(bufsiz, filter, maxMatches); }

    @Override public int read() throws IOException {
        final char[] c = new char[1];
        if (read(c, 0, 1) == -1) return -1;
        return c[0];
    }

    @Override public int read(char[] cbuf, int off, int len) throws IOException {
        return fillBuffer(len).read(cbuf, off, len);
    }

    @Override public String readLine() throws IOException {
        return fillBuffer(-1).readLine();
    }

    private FilterResponse fillBuffer(int sz) throws IOException {
        return fillBuffer(sz, bufsiz);
    }

    private FilterResponse fillBuffer(int sz, int bz) throws IOException {

        // if the filterResponse still has enough data, we don't need to re-fill
        final FilterResponse f = getFilterResponse();
        final int readSize = Math.max(sz, bz);
        if (f.canRead(readSize)) return f;

        // we need more data
        final char[] buffer = new char[readSize];
        int len = 0;
        boolean eof = false;
        // fill the buffer with the underlying stream
        while (len != buffer.length) {
            final int val = super.read(buffer, len, buffer.length - len);
            if (val == -1) {
                eof = true;
                break;
            }
            len += val;
        }

        // if we read anything, filter the buffer and apply filters
        if (len > 0) {
            if (f.addData(buffer, len, eof) == 0) {
                if (2 * bz < (2 * Bytes.MB)) {
                    return fillBuffer(2 * bz, 2 * bz);
                } else {
                    // we don't want to run out of memory
                    return die("fillBuffer: is someone DOSing us?");
                }
            }
        }
        return f;
    }

    private static class FilterResponse {

        @Getter @Setter private int bufsiz;

        private int readPos = 0;
        private boolean eof = false;
        private RegexStreamFilter filter;
        private int matchCount = 0;
        private Integer maxMatches = null;

        public FilterResponse(int bufsiz, RegexStreamFilter filter, Integer maxMatches) {
            this.bufsiz = bufsiz;
            this.filter = filter;
            this.maxMatches = maxMatches;
        }

        private final AtomicReference<char[]> unprocessed = new AtomicReference<>();
        private final AtomicReference<char[]> processed = new AtomicReference<>();

        public int addData(char[] b, int len, boolean eof) {

            this.eof = eof;

            if (maxMatches != null && matchCount >= maxMatches) {
                // no more processing to do. add all unprocessed bytes to processed, and add new bytes to processed
                synchronized (unprocessed) {
                    synchronized (processed) {
                        char[] u = unprocessed.get();
                        if (u == null) u = EMPTY_CHAR_ARRAY;
                        char[] p = processed.get();
                        if (p == null) p = EMPTY_CHAR_ARRAY;
                        final int newLen = p.length + u.length + len;
                        final char[] update = new char[newLen];
                        System.arraycopy(p, 0, update, 0, p.length);
                        System.arraycopy(u, 0, update, p.length, u.length);
                        System.arraycopy(b, 0, update, p.length + u.length, len);
                        processed.set(update);
                        unprocessed.set(EMPTY_CHAR_ARRAY);
                        return u.length + len;
                    }
                }
            } else {

                synchronized (unprocessed) {
                    char[] input = unprocessed.get();
                    if (input == null) {
                        input = new char[len];
                        System.arraycopy(b, 0, input, 0, len);
                    } else {
                        char[] new_input = new char[input.length + len];
                        System.arraycopy(input, 0, new_input, 0, input.length);
                        System.arraycopy(b, 0, new_input, input.length, len);
                        input = new_input;
                    }
                    unprocessed.set(input);

                    // process buffer with filter, it may leave a remainder
                    final RegexFilterResult result = filter.apply(new StringBuilder().append(input), eof);
                    matchCount += result.matchCount;

                    // put unprocessed remainder chars back onto unprocessed array
                    if (result.remainder > 0) {
                        final char[] subarray = ArrayUtils.subarray(input, input.length - result.remainder, input.length);
                        unprocessed.set(subarray);
                    } else {
                        unprocessed.set(null);
                    }

                    // if it produced nothing, but gave us a remainder, return zero now, read more data
                    if (result.buffer.length() == 0 && !eof) return 0;

                    // remove processed chars from unprocessed array
                    synchronized (processed) {
                        final char[] newChars = result.buffer.toString().toCharArray();
                        if (newChars.length > 0) {
                            if (processed.get() == null) {
                                processed.set(newChars);
                            } else {
                                processed.set(addAll(processed.get(), newChars));
                            }
                        }
                    }
                    return result.buffer.length();
                }
            }
        }

        public boolean canRead(int sz) {
            synchronized (processed) {
                if (processed.get() == null) return false;
                return eof || readPos + sz < processed.get().length;
            }
        }

        public int read(char[] cbuf, int off, int len) {
            synchronized (processed) {
                final char[] buf = processed.get();

                // nothing to read
                if (buf == null || buf.length == 0) {
                    return eof ? -1 : 0;
                }

                // do we have enough to fill?
                if (readPos + len < buf.length) {
                    System.arraycopy(buf, readPos, cbuf, off, len);
                    readPos += len;
                    return len;
                } else {
                    // are we at EOF? if so, return the last bytes
                    if (eof) {
                        // are we really AT eof?
                        if (readPos == buf.length) return -1;
                    }
                    // return what we can, which may be nothing if we have a buffer under-run
                    final int remainingLen = buf.length - readPos;
                    System.arraycopy(buf, readPos, cbuf, off, remainingLen);
                    readPos = buf.length;
                    return remainingLen;
                }
            }
        }

        public String readLine() {
            synchronized (processed) {
                final StringBuilder buf = new StringBuilder(String.valueOf(processed.get()));
                int newline = buf.indexOf("\n", readPos);
                final String line;
                if (newline == -1 || newline == buf.length() - 1) {
                    line = buf.substring(readPos, buf.length());
                    readPos = buf.length();
                } else {
                    line = buf.substring(readPos, newline);
                    readPos = newline + 1;
                }
                return line;
            }
        }
    }

}

