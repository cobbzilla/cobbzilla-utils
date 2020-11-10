package org.cobbzilla.util.system;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.apache.commons.io.output.TeeOutputStream;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.io.FileUtil;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.*;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.getDefaultTempDir;
import static org.cobbzilla.util.string.StringUtil.ellipsis;
import static org.cobbzilla.util.string.StringUtil.trimQuotes;

@Slf4j
public class CommandShell {

    protected static final String EXPORT_PREFIX = "export ";

    public static final String CHMOD = "chmod";
    public static final String CHGRP = "chgrp";
    public static final String CHOWN = "chown";

    private static final int[] DEFAULT_EXIT_VALUES = {0};
    public static final SystemInfo SYSTEM_INFO = new SystemInfo();
    public static final HardwareAbstractionLayer HARDWARE = SYSTEM_INFO.getHardware();

    public static Map<String, String> loadShellExports (String path) throws IOException {
        if (!path.startsWith("/")) {
            final File file = userFile(path);
            if (file.exists()) return loadShellExports(file);
        }
        return loadShellExports(new File(path));
    }

    public static File userFile(String path) {
        return new File(System.getProperty("user.home") + File.separator + path);
    }

    public static Map<String, String> loadShellExports (File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            return loadShellExports(in);
        }
    }

    public static Map<String, String> loadShellExports (InputStream in) throws IOException {
        final Map<String, String> map = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line, key, value;
            int eqPos;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) continue;
                if (line.startsWith(EXPORT_PREFIX)) {
                    line = line.substring(EXPORT_PREFIX.length()).trim();
                    eqPos = line.indexOf('=');
                    if (eqPos != -1) {
                        key = line.substring(0, eqPos).trim();
                        value = line.substring(eqPos+1).trim();
                        value = trimQuotes(value);
                        map.put(key, value);
                    }
                }
            }
        }
        return map;
    }

    public static Map<String, String> loadShellExportsOrDie (String f) {
        try { return loadShellExports(f); } catch (Exception e) {
            return die("loadShellExportsOrDie: "+e);
        }
    }

    public static Map<String, String> loadShellExportsOrDie (File f) {
        try { return loadShellExports(f); } catch (Exception e) {
            return die("loadShellExportsOrDie: "+e);
        }
    }

    public static void replaceShellExport (String f, String name, String value) throws IOException {
        replaceShellExports(new File(f), MapBuilder.build(name, value));
    }

    public static void replaceShellExport (File f, String name, String value) throws IOException {
        replaceShellExports(f, MapBuilder.build(name, value));
    }

    public static void replaceShellExports (String f, Map<String, String> exports) throws IOException {
        replaceShellExports(new File(f), exports);
    }

    public static void replaceShellExports (File f, Map<String, String> exports) throws IOException {

        // validate -- no quote chars allowed for security reasons
        for (String key : exports.keySet()) {
            if (key.contains("\"") || key.contains("\'")) throw new IllegalArgumentException("replaceShellExports: name cannot contain a quote character: "+key);
            String value = exports.get(key);
            if (value.contains("\"") || value.contains("\'")) throw new IllegalArgumentException("replaceShellExports: value for "+key+" cannot contain a quote character: "+value);
        }

        // read entire file as a string
        final String contents = FileUtil.toString(f);

        // walk file line by line and look for replacements to make, overwrite file.
        final Set<String> replaced = new HashSet<>(exports.size());
        try (Writer w = new FileWriter(f)) {
            for (String line : contents.split("\n")) {
                line = line.trim();
                boolean found = false;
                for (String key : exports.keySet()) {
                    if (!line.startsWith("#") && line.matches("^\\s*export\\s+" + key + "\\s*=.*")) {
                        w.write("export " + key + "=\"" + exports.get(key) + "\"");
                        replaced.add(key);
                        found = true;
                        break;
                    }
                }
                if (!found) w.write(line);
                w.write("\n");
            }

            for (String key : exports.keySet()) {
                if (!replaced.contains(key)) {
                    w.write("export "+key+"=\""+exports.get(key)+"\"\n");
                }
            }
        }
    }

    public static MultiCommandResult exec (Collection<String> commands) throws IOException {
        final MultiCommandResult result = new MultiCommandResult();
        for (String c : commands) {
            Command command = new Command(c);
            result.add(command, exec(c));
            if (result.hasException()) return result;
        }
        return result;
    }

    public static CommandResult exec (String command) throws IOException {
        return exec(CommandLine.parse(command));
    }

    public static CommandResult exec (CommandLine command) throws IOException {
        return exec(new Command(command));
    }

    public static CommandResult exec (Command command) throws IOException {

        final DefaultExecutor executor = new DefaultExecutor();

        final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        OutputStream out = command.hasOut() ? new TeeOutputStream(outBuffer, command.getOut()) : outBuffer;
        if (command.isCopyToStandard()) out = new TeeOutputStream(out, System.out);

        final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        OutputStream err = command.hasErr() ? new TeeOutputStream(errBuffer, command.getErr()) : errBuffer;
        if (command.isCopyToStandard()) err = new TeeOutputStream(err, System.err);

        final ExecuteStreamHandler handler = new PumpStreamHandler(out, err, command.getInputStream());
        executor.setStreamHandler(handler);

        if (command.hasDir()) executor.setWorkingDirectory(command.getDir());
        executor.setExitValues(command.getExitValues());

        try {
            final int exitValue = executor.execute(command.getCommandLine(), command.getEnv());
            return new CommandResult(exitValue, outBuffer, errBuffer);

        } catch (Exception e) {
            final String stdout = outBuffer.toString().trim();
            final String stderr = errBuffer.toString().trim();
            log.error("exec("+command.getCommandLine()+"): " + e
                    + (stdout.length() > 0 ? "\nstdout="+ellipsis(stdout, 1000) : "")
                    + (stderr.length() > 0 ? "\nstderr="+ellipsis(stderr, 1000) : ""));
            return new CommandResult(e, outBuffer, errBuffer);
        }
    }

    public static int chmod (File file, String perms) {
        return chmod(abs(file), perms, false);
    }

    public static int chmod (File file, String perms, boolean recursive) {
        return chmod(abs(file), perms, recursive);
    }

    public static int chmod (String file, String perms) {
        return chmod(file, perms, false);
    }

    public static int chmod (String file, String perms, boolean recursive) {
        final CommandLine commandLine = new CommandLine(CHMOD);
        if (recursive) commandLine.addArgument("-R");
        commandLine.addArgument(perms);
        commandLine.addArgument(abs(file), false);
        final Executor executor = new DefaultExecutor();
        try {
            return executor.execute(commandLine);
        } catch (Exception e) {
            throw new CommandShellException(commandLine.toString(), e);
        }
    }

    public static int chgrp(String group, File path) {
        return chgrp(group, path, false);
    }

    public static int chgrp(String group, File path, boolean recursive) {
        return chgrp(group, abs(path), recursive);
    }

    public static int chgrp(String group, String path) {
        return chgrp(group, path, false);
    }

    public static int chgrp(String group, String path, boolean recursive) {
        return runChCmd(group, path, recursive, CHGRP);
    }

    private static int runChCmd(String subject, String path, boolean recursive, String cmd) {
        final Executor executor = new DefaultExecutor();
        final CommandLine command = new CommandLine(cmd);
        if (recursive) command.addArgument("-R");
        command.addArgument(subject).addArgument(path);
        try {
            return executor.execute(command);
        } catch (Exception e) {
            throw new CommandShellException(command.toString(), e);
        }
    }

    public static int chown(String owner, File path) { return chown(owner, path, false); }

    public static int chown(String owner, File path, boolean recursive) {
        return chown(owner, abs(path), recursive);
    }

    public static int chown(String owner, String path) { return chown(owner, path, false); }

    public static int chown(String owner, String path, boolean recursive) {
        return runChCmd(owner, path, recursive, CHOWN);
    }

    public static String toString(String command) {
        try {
            return exec(command).getStdout().trim();
        } catch (IOException e) {
            throw new CommandShellException(command, e);
        }
    }

    public static long totalSystemMemory() { return HARDWARE.getMemory().getTotal(); }

    public static String hostname () { return toString("hostname"); }
    public static String domainname() { return toString("hostname -d"); }
    public static String hostname_short() { return toString("hostname -s"); }
    public static String whoami() { return toString("whoami"); }
    public static boolean isRoot() { return "root".equals(whoami()); }

    public static String locale () {
        return execScript("locale | grep LANG= | tr '=.' ' ' | awk '{print $2}'").trim();
    }

    public static String lang () {
        return execScript("locale | grep LANG= | tr '=_' ' ' | awk '{print $2}'").trim();
    }

    public static File tempScript (String contents) {
        contents = "#!/bin/bash\n\n"+contents;
        try {
            final File temp = File.createTempFile("tempScript", ".sh", getDefaultTempDir());
            FileUtil.toFile(temp, contents);
            chmod(temp, "700");
            return temp;

        } catch (Exception e) {
            throw new CommandShellException(contents, e);
        }
    }

    public static String execScript (String contents) { return execScript(contents, null); }

    public static String execScript (String contents, Map<String, String> env) { return execScript(contents, env, null); }

    public static String execScript (String contents, Map<String, String> env, List<Integer> exitValues) {
        final CommandResult result = scriptResult(contents, env, null, exitValues);
        if (!result.isZeroExitStatus() && (exitValues == null || !exitValues.contains(result.getExitStatus()))) {
            throw new CommandShellException(contents, result);
        }
        return result.getStdout();
    }

    public static CommandResult scriptResult (String contents) { return scriptResult(contents, null, null, null); }

    public static CommandResult scriptResult (String contents, Map<String, String> env) {
        return scriptResult(contents, env, null, null);
    }

    public static CommandResult scriptResult (String contents, String input) {
        return scriptResult(contents, null, input, null);
    }

    public static CommandResult scriptResult (String contents, Map<String, String> env, String input, List<Integer> exitValues) {
        try {
            @Cleanup("delete") final File script = tempScript(contents);
            final Command command = new Command(new CommandLine(script)).setEnv(env).setInput(input);
            if (!empty(exitValues)) command.setExitValues(exitValues);
            return exec(command);
        } catch (Exception e) {
            throw new CommandShellException(contents, e);
        }
    }

    public static CommandResult okResult(CommandResult result) {
        if (result == null || !result.isZeroExitStatus()) throw new CommandShellException(result);
        return result;
    }

    public static File home(String user) {
        String path = execScript("cd ~" + user + " && pwd");
        if (empty(path)) die("home("+user+"): no home found for user "+user);
        final File f = new File(path);
        if (!f.exists()) die("home("+user+"): home does not exist "+path);
        return f;
    }

    public static File pwd () { return new File(System.getProperty("user.dir")); }

}
