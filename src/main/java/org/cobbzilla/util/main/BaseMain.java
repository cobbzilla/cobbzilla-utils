package org.cobbzilla.util.main;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.error.ExceptionHandler;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import static org.cobbzilla.util.daemon.ZillaRuntime.background;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public abstract class BaseMain<OPT extends BaseMainOptions> {

    @Getter private final OPT options = initOptions();
    protected OPT initOptions() { return instantiate(getFirstTypeParam(getClass())); }

    @Getter(value=AccessLevel.PROTECTED) private final CmdLineParser parser = new CmdLineParser(getOptions());

    protected abstract void run() throws Exception;

    @Getter private String[] args;
    public void setArgs(String[] args) throws CmdLineException {
        this.args = args;
        try {
            parser.parseArgument(args);
            if (options.isHelp()) {
                showHelpAndExit();
            }
        } catch (Exception e) {
            showHelpAndExit(e);
        }
    }

    protected void preRun() {}
    protected void postRun() {}

    public static void main(Class<? extends BaseMain> clazz, String[] args) {
        BaseMain m = null;
        int returnValue = 0;
        try {
            m = clazz.getDeclaredConstructor().newInstance();
            m.setArgs(args);
            m.preRun();
            m.run();
            m.postRun();

        } catch (Exception e) {
            if (m == null || m.getOptions() == null || m.getOptions().isVerboseFatalErrors()) {
                final String msg = "Unexpected error: " + e + (e.getCause() != null ? " (caused by " + e.getCause() + ")" : "");
                log.error(msg, e);
                ZillaRuntime.die("Unexpected error: " + e);
            } else {
                final String msg = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
                log.error(msg);
            }
            returnValue = -1;
        } finally {
            if (m != null) m.cleanup();
        }
        System.exit(returnValue);
    }

    public void cleanup () {}

    public void showHelpAndExit() {
        parser.printUsage(System.out);
        System.exit(0);
    }

    public void showHelpAndExit(String error) { showHelpAndExit(new IllegalArgumentException(error)); }

    public static final String ERR_LINE = "\n--------------------------------------------------------------------------------\n";

    public void showHelpAndExit(Exception e) {
        parser.printUsage(System.err);
        err(ERR_LINE + " >>> " + e.getMessage() + ERR_LINE);
        System.exit(1);
    }

    public static void out(String message) { System.out.println(message); }

    public static void err (String message) { System.err.println(message); }

    public <T> T die (String message) {
        if (options.isVerboseFatalErrors()) {
            log.error(message);
        }
        err(message);
        System.exit(1);
        return null;
    }

    public <T> T die (String message, Exception e) {
        if (options.isVerboseFatalErrors()) {
            log.error(message, e);
        }
        err(message + ": " + e.getClass().getName() + (!empty(e.getMessage()) ? ": "+e.getMessage(): ""));
        System.exit(1);
        return null;
    }

    public Thread runInBackground (ExceptionHandler errorHandler) {
        return background(new RunWithHandler(this, errorHandler), errorHandler);
    }

    @AllArgsConstructor
    private static class RunWithHandler implements Runnable {
        private final BaseMain runnable;
        private final ExceptionHandler errorHandler;
        @Override public void run() {
            try {
                runnable.run();
            } catch (Exception e) {
                errorHandler.handle(e);
            }
        }
    }
}
