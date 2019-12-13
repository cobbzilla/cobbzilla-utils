package org.cobbzilla.util.handlebars;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

@NoArgsConstructor @Accessors(chain=true)
public class ESignInsertion extends ImageInsertion {

    public static final ESignInsertion[] NO_ESIGN_INSERTIONS = new ESignInsertion[0];

    @Getter @Setter private String role = null;

    public ESignInsertion(ESignInsertion other) { super(other); }

    public ESignInsertion(String spec) { super(spec); }

    @Override protected void setField(String key, String value) {
        switch (key) {
            case "role": role = value; break;
            default: super.setField(key, value);
        }
    }

    @Override public File getImageFile() {
        return notSupported("getImageFile not supported for " + this.getClass().getName());
    }

}
