package org.cobbzilla.util.handlebars;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public interface JurisdictionResolver {

    String usState (String value);

    String usZip (String value);

    default boolean isValidUsStateAbbreviation(String a) { return !empty(a) && usState(a) != null; }

}
