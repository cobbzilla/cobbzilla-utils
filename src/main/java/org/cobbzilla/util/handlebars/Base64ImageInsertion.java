package org.cobbzilla.util.handlebars;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64InputStream;
import org.cobbzilla.util.io.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.temp;

@NoArgsConstructor @Accessors(chain=true)
public class Base64ImageInsertion extends ImageInsertion {

    public static final Base64ImageInsertion[] NO_IMAGE_INSERTIONS = new Base64ImageInsertion[0];

    public Base64ImageInsertion(Base64ImageInsertion other) { super(other); }

    public Base64ImageInsertion(String spec) { super(spec); }

    @Getter @Setter private String image; // base64-encoded image data

    @Override public File getImageFile() throws IOException {
        if (empty(getImage())) return null;
        final File temp = temp("."+getFormat());
        final Base64InputStream stream = new Base64InputStream(new ByteArrayInputStream(image.getBytes()));
        FileUtil.toFile(temp, stream);
        return temp;
    }

    @Override protected void setField(String key, String value) {
        switch (key) {
            case "image": this.image = value; break;
            default: super.setField(key, value);
        }
    }

}
