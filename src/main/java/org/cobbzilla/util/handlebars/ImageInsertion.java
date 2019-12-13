package org.cobbzilla.util.handlebars;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@NoArgsConstructor @Accessors(chain=true)
public abstract class ImageInsertion {

    @Getter @Setter private String name = null;
    @Getter @Setter private int page = 0;
    @Getter @Setter private float x;
    @Getter @Setter private float y;
    @Getter @Setter private float width = 0;
    @Getter @Setter private float height = 0;
    @Getter @Setter private String format = "png";
    @Getter @Setter private boolean watermark = false;

    @JsonIgnore public abstract File getImageFile() throws IOException;

    public ImageInsertion(ImageInsertion other) { copy(this, other); }

    public ImageInsertion(String spec) {
        for (String part : StringUtil.split(spec, ", ")) {
            final int eqPos = part.indexOf("=");
            if (eqPos == -1) die("invalid image insertion (missing '='): "+spec);
            if (eqPos == part.length()-1) die("invalid image insertion (no value): "+spec);
            final String key = part.substring(0, eqPos).trim();
            final String value = part.substring(eqPos+1).trim();
            setField(key, value);
        }
    }

    public void init (Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            setField(entry.getKey(), entry.getValue().toString());
        }
    }

    protected void setField(String key, String value) {
        switch (key) {
            case "name":   this.name   = value; break;
            case "page":   this.page   = Integer.parseInt(value); break;
            case "x":      this.x      = Float.parseFloat(value); break;
            case "y":      this.y      = Float.parseFloat(value); break;
            case "width":  this.width  = Float.parseFloat(value); break;
            case "height": this.height = Float.parseFloat(value); break;
            case "format": this.format = value; break;
            default: die("invalid parameter: "+key);
        }

    }
}
