package org.cobbzilla.util.handlebars;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.temp;

@NoArgsConstructor @Accessors(chain=true)
public class TextImageInsertion extends ImageInsertion {

    public static final TextImageInsertion[] NO_TEXT_INSERTIONS = new TextImageInsertion[0];

    @Getter @Setter private String content;
    public void capitalizeContent() { content = content == null ? null : content.toUpperCase(); }

    @Getter @Setter private String fontFamily = "Arial";
    @Getter @Setter private String fontStyle = "plain";
    @Getter @Setter private String fontColor = "000000";
    @Getter @Setter private int fontSize = 14;
    @Getter @Setter private int alpha = 255;
    @Getter @Setter private int maxWidth = -1;
    @Getter @Setter private int widthPadding = 10;
    @Getter @Setter private int lineSpacing = 4;

    public TextImageInsertion(TextImageInsertion other) { super(other); }
    public TextImageInsertion(String spec) { super(spec); }

    @Override protected void setField(String key, String value) {
        switch (key) {
            case "content":      content      = value; break;
            case "fontFamily":   fontFamily   = value; break;
            case "fontStyle":    fontStyle    = value; break;
            case "fontColor":    fontColor    = value; break;
            case "fontSize":     fontSize     = Integer.parseInt(value); break;
            case "alpha":        alpha        = Integer.parseInt(value); break;
            case "maxWidth":     maxWidth     = Integer.parseInt(value); break;
            case "widthPadding": widthPadding = Integer.parseInt(value); break;
            case "lineSpacing":  lineSpacing  = Integer.parseInt(value); break;
            default: super.setField(key, value);
        }
    }

    @JsonIgnore private int getRed   () { return (int) (Long.parseLong(fontColor, 16) & 0xff0000) >> 16; }
    @JsonIgnore private int getGreen () { return (int) (Long.parseLong(fontColor, 16) & 0x00ff00) >> 8; }
    @JsonIgnore private int getBlue  () { return (int) (Long.parseLong(fontColor, 16) & 0x0000ff); }

    @JsonIgnore private Color getAwtFontColor() { return new Color(getRed(), getGreen(), getBlue(), getAlpha()); }

    @JsonIgnore private int getAwtFontStyle() {
        switch (fontStyle.toLowerCase()) {
            case "plain": return Font.PLAIN;
            case "bold": return Font.BOLD;
            case "italic": return Font.ITALIC;
            default: return Font.PLAIN;
        }
    }

    // adapted from: https://stackoverflow.com/a/18800845/1251543
    @Override public File getImageFile() {
        if (empty(getContent())) return null;

        Graphics2D g2d = getGraphics2D();

        final ParsedText txt = getParsedText(g2d);
        if (getWidth() == 0) setWidth(txt.width);
        if (getHeight() == 0) setHeight(txt.height);

        g2d.dispose();

        final BufferedImage img = new BufferedImage(txt.width, txt.height, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setFont(getFont());

        final FontMetrics fm = g2d.getFontMetrics();
        g2d.setColor(getAwtFontColor());
        for (int i=0; i<txt.lines.size(); i++) {
            final String line = txt.lines.get(i);
            int y = getLineY(fm, i);
            g2d.drawString(line, 0, y);
        }
        g2d.dispose();
        final File temp = temp("."+getFormat());
        try {
            ImageIO.write(img, getFormat(), temp);
            return temp;
        } catch (IOException e) {
            return die("getImageStream: "+e, e);
        }
    }

    protected ParsedText getParsedText() { return getParsedText(getGraphics2D()); }

    private Graphics2D getGraphics2D() {
        final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2d = img.createGraphics();
        final Font font = getFont();
        g2d.setFont(font);
        return g2d;
    }

    private Font getFont() {
        return new Font(getFontFamily(), getAwtFontStyle(), getFontSize());
    }

    protected ParsedText getParsedText(Graphics2D g2d) {
        FontMetrics fm = g2d.getFontMetrics();
        final ParsedText txt = new ParsedText();
        int widest = -1;
        final String[] inLines = getContent().trim().split("\n");
        if (getMaxWidth() == -1) {
            for (String inLine : inLines) {
                txt.lines.add(inLine);
                txt.width = fm.stringWidth(getContent()) + getWidthPadding();
                if (txt.width > widest) widest = txt.width;
            }

        } else {
            for (String inLine : inLines) {
                final String[] words = inLine.split("\\s+");
                StringBuilder b = new StringBuilder();
                for (String word : words) {
                    int stringWidth = fm.stringWidth(b.toString() + " " + word);
                    if (stringWidth + getWidthPadding() > getMaxWidth()) {
                        if (b.length() == 0) die("getImageFile: word too long for maxWidth=" + maxWidth + ": " + word);
                        txt.lines.add(b.toString());
                        b = new StringBuilder(word);
                    } else {
                        if (b.length() > 0) b.append(" ");
                        b.append(word);
                        if (stringWidth > widest) widest = stringWidth;
                    }
                }
                txt.lines.add(b.toString());
            }
        }
        txt.width = widest + getWidthPadding();
        txt.height = getLineY(fm, txt.lines.size());
        return txt;
    }

    protected int getLineY(FontMetrics fm, int i) { return (i+1) * (fm.getAscent() + getLineSpacing()); }

    public int determineHeight() { return getParsedText().height; }

    private class ParsedText {
        public java.util.List<String> lines = new ArrayList<>();
        public int width;
        public int height;
    }
}
