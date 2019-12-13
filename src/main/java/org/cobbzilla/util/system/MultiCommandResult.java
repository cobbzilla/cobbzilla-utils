package org.cobbzilla.util.system;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

public class MultiCommandResult {

    @Getter private final Map<Command, CommandResult> results = new LinkedHashMap<>();

    public boolean hasException () {
        for (CommandResult result : results.values()) {
            if (result.hasException()) return true;
        }
        return false;
    }

    public void add(Command command, CommandResult commandResult) { results.put(command, commandResult); }

    @Override public String toString() {
        return "MultiCommandResult{" +
                "results=" + results +
                ", exception=" + hasException() +
                '}';
    }
}
