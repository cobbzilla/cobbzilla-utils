package org.cobbzilla.util.chef;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class VendorDatabagSetting {

    @Getter @Setter private String path;
    @Getter @Setter private String shasum;
    @Getter @Setter private boolean block_ssh = false;

    public VendorDatabagSetting(String path, String shasum) {
        setPath(path);
        setShasum(shasum);
    }

}
