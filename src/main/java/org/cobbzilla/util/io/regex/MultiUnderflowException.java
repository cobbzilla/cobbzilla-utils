package org.cobbzilla.util.io.regex;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;

@AllArgsConstructor
public class MultiUnderflowException extends IOException {

    @Getter private final String name;

}
