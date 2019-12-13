package org.cobbzilla.util.system;

import java.text.DecimalFormat;

import static org.apache.commons.lang3.StringUtils.chop;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.string.StringUtil.removeWhitespace;

public class Bytes {

    public static final long KB = 1024;
    public static final long MB = 1024 * KB;
    public static final long GB = 1024 * MB;
    public static final long TB = 1024 * GB;
    public static final long PB = 1024 * TB;
    public static final long EB = 1024 * PB;

    public static final long KiB = 1000;
    public static final long MiB = 1000 * KiB;
    public static final long GiB = 1000 * MiB;
    public static final long TiB = 1000 * GiB;
    public static final long PiB = 1000 * TiB;
    public static final long EiB = 1000 * PiB;

    public static long parse(String value) {
        String val = removeWhitespace(value).toLowerCase();
        if (val.endsWith("bytes")) return Long.parseLong(val.substring(0, val.length()-"bytes".length()));
        if (val.endsWith("b")) return Long.parseLong(chop(val));
        final char suffix = val.charAt(val.length());
        final long size = Long.parseLong(val.substring(0, val.length() - 1));
        switch (suffix) {
            case 'k': return KB * size;
            case 'm': return MB * size;
            case 'g': return GB * size;
            case 't': return TB * size;
            case 'p': return PB * size;
            case 'e': return EB * size;
            default: return die("parse: Unrecognized suffix '"+suffix+"' in string "+value);
        }
    }

    public static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat();
    static {
        DEFAULT_FORMAT.setMaximumFractionDigits(2);
    }

    public static String format(Long count) {
        if (count == null) return "0 bytes";
        if (count >= EB) return DEFAULT_FORMAT.format(count.doubleValue() / ((double) EB)) + " EB";
        if (count >= PB) return DEFAULT_FORMAT.format(count.doubleValue() / ((double) PB)) + " PB";
        if (count >= TB) return DEFAULT_FORMAT.format(count.doubleValue() / ((double) TB)) + " TB";
        if (count >= GB) return DEFAULT_FORMAT.format(count.doubleValue() / ((double) GB)) + " GB";
        if (count >= MB) return DEFAULT_FORMAT.format(count.doubleValue() / ((double) MB)) + " MB";
        if (count >= KB) return DEFAULT_FORMAT.format(count.doubleValue() / ((double) KB)) + " KB";
        return count + " bytes";
    }

    public static String formatBrief(Long count) {
        final String s = format(count);
        return s.endsWith(" bytes") ? s.split("\\w+")[0]+"b" : removeWhitespace(s.toLowerCase());
    }

}
