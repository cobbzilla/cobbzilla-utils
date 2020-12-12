package org.cobbzilla.util.system;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(of={"command", "result", "exception"})
public class CommandShellException extends RuntimeException {

    @Getter @Setter private String command;
    @Getter @Setter private CommandResult result;
    @Getter @Setter private Exception exception;

    @Override public String getMessage() { return toString(); }

    public CommandShellException (CommandResult result) { this.result = result; }

    public CommandShellException (Exception e) { this.exception = e; }

    public CommandShellException (String command, Exception e) {
        this(e);
        this.command = command;
    }

    public CommandShellException(String command, CommandResult result) {
        this(result);
        this.command = command;
    }

}
