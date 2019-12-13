package org.cobbzilla.util.chef;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.security.ShaUtil;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain=true)
public class VendorDatabag {

    public static final VendorDatabag NULL = new VendorDatabag();

    @Getter @Setter private String service_key_endpoint;
    @Getter @Setter private String ssl_key_sha;
    @Getter @Setter private List<VendorDatabagSetting> settings = new ArrayList<>();

    public VendorDatabag addSetting (VendorDatabagSetting setting) { settings.add(setting); return this; }

    public VendorDatabagSetting getSetting(String path) {
        for (VendorDatabagSetting s : settings) {
            if (s.getPath().equals(path)) return s;
        }
        return null;
    }

    public boolean containsSetting (String path) { return getSetting(path) != null; }

    public boolean isDefault (String path, String value) {
        final VendorDatabagSetting setting = getSetting(path);
        if (setting == null) return false;

        final String shasum = setting.getShasum();
        return shasum != null && ShaUtil.sha256_hex(value).equals(shasum);

    }
}
