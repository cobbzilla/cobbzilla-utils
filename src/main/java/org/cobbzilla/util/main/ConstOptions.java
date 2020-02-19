package org.cobbzilla.util.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Argument;

public class ConstOptions extends BaseMainOptions {

    public static final String USAGE_CLASS_AND_MEMBER = "Specify a class.member constant";
    @Argument(usage=USAGE_CLASS_AND_MEMBER, required=true)
    @Getter @Setter private String classAndMember;

}
