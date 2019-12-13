package org.cobbzilla.util.graphics;

import org.apache.commons.lang3.RandomUtils;

import java.awt.*;
import java.util.Collection;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.getHexValue;

public class ColorUtil {

    public static final String ANSI_RESET = "\\033[0m";

    public static int parseRgb(String colorString) { return parseRgb(colorString, null); }

    public static int parseRgb(String colorString, Integer defaultRgb) {
        try {
            if (empty(colorString)) return defaultRgb;
            if (colorString.startsWith("0x")) return Integer.parseInt(colorString.substring(2), 16);
            if (colorString.startsWith("#")) return Integer.parseInt(colorString.substring(1), 16);
            return Integer.parseInt(colorString, 16);

        } catch (Exception e) {
            if (defaultRgb == null) {
                return die("parseRgb: '' was unparseable and no default value provided: "+e.getClass().getSimpleName()+": "+e.getMessage(), e);
            }
            return defaultRgb;
        }
    }

    public static int rgb2ansi(int color) { return rgb2ansi(new Color(color)); }

    public static int rgb2ansi(Color c) {
        return 16 + (36 * (c.getRed() / 51)) + (6 * (c.getGreen() / 51)) + c.getBlue() / 51;
    }

    public static String rgb2hex(int color) {
        final Color c = new Color(color);
        return getHexValue((byte) c.getRed())
                + getHexValue((byte) c.getGreen())
                + getHexValue((byte) c.getBlue());
    }

    public static int randomColor() { return randomColor(null, ColorMode.rgb); }
    public static int randomColor(ColorMode mode) { return randomColor(null, mode); }
    public static int randomColor(Collection<Integer> usedColors) { return randomColor(usedColors, ColorMode.rgb); }

    public static int randomColor(Collection<Integer> usedColors, ColorMode mode) {
        int val;
        do {
            val = RandomUtils.nextInt(0x000000, 0xffffff);
        } while (usedColors != null && usedColors.contains(val));
        return mode == ColorMode.rgb ? val : rgb2ansi(val);
    }

    public static String ansiColor(int fg) { return ansiColor(fg, null); }

    public static String ansiColor(int fg, Integer bg) {
        final StringBuilder b = new StringBuilder();
        b.append("\\033[38;5;")
                .append(rgb2ansi(fg))
                .append(bg == null ? "" : ";48;5;"+rgb2ansi(bg))
                .append("m");
        return b.toString();
    }
}
