package org.cobbzilla.util.graphics;

import lombok.Getter;
import lombok.Setter;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class ImageTransformConfig {

    @Getter @Setter private int height;
    @Getter @Setter private int width;

    public ImageTransformConfig(String config) {
        final int xpos = config.indexOf('x');
        try {
            width = Integer.parseInt(config.substring(xpos + 1));
            height = Integer.parseInt(config.substring(0, xpos));
        } catch (Exception e) {
            die("invalid config (expected WxH): " + config + ": " + e, e);
        }
    }

}
