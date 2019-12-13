package org.cobbzilla.util.system;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sun.jna.Platform;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public enum OsType {

    windows, macosx, linux;

    @JsonCreator public static OsType fromString (String val) { return valueOf(val.toLowerCase()); }

    public static final OsType CURRENT_OS = initCurrentOs();

    private static OsType initCurrentOs() {
        if (Platform.isWindows()) return windows;
        if (Platform.isMac()) return macosx;
        if (Platform.isLinux()) return linux;
        return die("could not determine operating system: "+System.getProperty("os.name"));
    }

    public static final boolean IS_ADMIN = initIsAdmin();

    private static boolean initIsAdmin() {
        switch (CURRENT_OS) {
            case macosx:  case linux: return System.getProperty("user.name").equals("root");
            case windows:             return WindowsAdminUtil.isUserWindowsAdmin();
            default:                  return false;
        }
    }

    public static final String ADMIN_USERNAME = initAdminUsername();

    private static String initAdminUsername() {
        switch (CURRENT_OS) {
            case macosx:  case linux: return "root";
            case windows:             return "Administrator";
            default:                  return die("initAdminUsername: invalid OS: "+CURRENT_OS);
        }
    }

}
