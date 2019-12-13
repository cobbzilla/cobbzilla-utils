package org.cobbzilla.util.system;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.lang3.ArrayUtils;
import org.cobbzilla.util.collection.SingletonList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;

@NoArgsConstructor @Accessors(chain=true)
public class Command {

    public static final List<Integer> DEFAULT_EXIT_VALUES = new SingletonList<>(0);
    public static final int[] DEFAULT_EXIT_VALUES_INT = { 0 };

    @Getter @Setter private CommandLine commandLine;
    @Getter @Setter private String input;
    @Getter @Setter private byte[] rawInput;
    @Getter @Setter private InputStream stdin;
    @Getter @Setter private File dir;
    @Getter @Setter private Map<String, String> env;
    private List<Integer> exitValues = DEFAULT_EXIT_VALUES;

    @Getter @Setter private boolean copyToStandard = false;

    @Getter @Setter private OutputStream out;
    public boolean hasOut () { return out != null; }

    @Getter @Setter private OutputStream err;
    public boolean hasErr () { return err != null; }

    public Command(CommandLine commandLine) { this.commandLine = commandLine; }
    public Command(String command) { this(CommandLine.parse(command)); }
    public Command(File executable) { this(abs(executable)); }

    public boolean hasDir () { return dir != null; }
    public boolean hasInput () { return !empty(input) || !empty(rawInput) || stdin != null; }
    public InputStream getInputStream () {
        if (!hasInput()) return null;
        if (stdin != null) return stdin;
        if (rawInput != null) return new ByteArrayInputStream(rawInput);
        return new ByteArrayInputStream(input.getBytes(UTF8cs));
    }

    public int[] getExitValues () {
        return exitValues == DEFAULT_EXIT_VALUES
                ? DEFAULT_EXIT_VALUES_INT
                : ArrayUtils.toPrimitive(exitValues.toArray(new Integer[exitValues.size()]));
    }

    public Command setExitValues (List<Integer> values) { this.exitValues = values; return this; }

    public Command setExitValues (int[] values) {
        exitValues = new ArrayList<>(values.length);
        for (int v : values) exitValues.add(v);
        return this;
    }

}
