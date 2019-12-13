package org.cobbzilla.util.handlebars;

import org.cobbzilla.util.string.StringUtil;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class SimpleJurisdictionResolver implements JurisdictionResolver {

    public static final SimpleJurisdictionResolver instance = new SimpleJurisdictionResolver();

    @Override public String usState(String value) {
        return empty(value) || value.length() != 2 ? die("usState: invalid: " + value) : value.toUpperCase();
    }

    @Override public String usZip(String value) {
        return empty(value) || value.length() != 5 || StringUtil.onlyDigits(value).length() != value.length()
                ? die("usZip: invalid: " + value)
                : value.toUpperCase();
    }

}
