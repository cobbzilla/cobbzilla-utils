package org.cobbzilla.util.daemon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.SystemUtils;
import org.cobbzilla.util.collection.ToStringTransformer;
import org.cobbzilla.util.error.ExceptionHandler;
import org.cobbzilla.util.error.GeneralErrorHandler;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.string.StringUtil;
import org.slf4j.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.Long.toHexString;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.LongStream.range;
import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.cobbzilla.util.error.ExceptionHandler.DEFAULT_EX_RUNNABLE;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.list;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.util.string.StringUtil.truncate;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;

/**
 * the Zilla doesn't mess around.
 */
@Slf4j
public class ZillaRuntime {

    public static final String CLASSPATH_PREFIX = "classpath:";

    public static String getJava() { return System.getProperty("java.home") + "/bin/java"; }

    public static boolean terminate(Thread thread, long timeout) {
        if (thread == null || !thread.isAlive()) return true;
        thread.interrupt();
        final long start = realNow();
        while (thread.isAlive() && realNow() - start < timeout) {
            sleep(100, "terminate: waiting for thread to die: "+thread);
        }
        if (thread.isAlive()) {
            log.warn("terminate: thread did not die voluntarily, killing it: "+thread);
            thread.stop();
        }
        return false;
    }

    public static boolean bool(Boolean b) { return b != null && b; }
    public static boolean bool(Boolean b, boolean val) { return b != null ? b : val; }

    public static Thread background (Runnable r) { return background(r, DEFAULT_EX_RUNNABLE); }

    public static Thread background (Runnable r, ExceptionHandler ex) {
        final Thread t = new Thread(() -> {
            try {
                r.run();
            } catch (Exception e) {
                ex.handle(e);
            }
        });
        t.start();
        return t;
    }

    public static final Function<Integer, Long> DEFAULT_RETRY_BACKOFF = SECONDS::toMillis;

    public static <T> T retry (Callable<T> func, int tries) {
        return retry(func, tries, DEFAULT_RETRY_BACKOFF, DEFAULT_EX_RUNNABLE);
    }

    public static <T> T retry (Callable<T> func, int tries, Function<Integer, Long> backoff) {
        return retry(func, tries, backoff, DEFAULT_EX_RUNNABLE);
    }

    public static <T> T retry (Callable<T> func,
                               int tries,
                               Logger logger) {
        return retry(func, tries, DEFAULT_RETRY_BACKOFF, e -> logger.error("Error: "+e));
    }

    public static <T> T retry (Callable<T> func,
                               int tries,
                               Function<Integer, Long> backoff,
                               Logger logger) {
        return retry(func, tries, backoff, e -> logger.error("Error: "+e));
    }

    public static <T> T retry (Callable<T> func,
                               int tries,
                               Function<Integer, Long> backoff,
                               ExceptionHandler ex) {
        Exception lastEx = null;
        try {
            for (int i = 0; i < tries; i++) {
                try {
                    final T rVal = func.call();
                    log.debug("retry: successful, returning: " + rVal);
                    return rVal;
                } catch (Exception e) {
                    lastEx = e;
                    log.debug("retry: failed (attempt " + (i + 1) + "/" + tries + "): " + e);
                    ex.handle(e);
                    sleep(backoff.apply(i), "waiting to retry " + func.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            return die("retry: fatal exception, exiting: "+e);
        }
        return die("retry: max tries ("+tries+") exceeded. last exception: "+lastEx);
    }

    public static Thread daemon (Runnable r) {
        final Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
        return t;
    }

    @Getter @Setter private static ErrorApi errorApi;

    public static <T> T die(String message)              { return _throw(new IllegalStateException(message, null)); }
    public static <T> T die(String message, Exception e) { return _throw(new IllegalStateException(message, e)); }
    public static <T> T die(Exception e)                 { return _throw(new IllegalStateException("(no message)", e)); }

    public static <T> T notSupported()               { return notSupported("not supported"); }
    public static <T> T notSupported(String message) { return _throw(new UnsupportedOperationException(message)); }

    private static <T> T _throw (RuntimeException e) {
        final String message = e.getMessage();
        final Throwable cause = e.getCause();
        if (errorApi != null) {
            if (cause instanceof Exception) errorApi.report(message, (Exception) cause);
            else errorApi.report(e);
        }
        if (cause != null) log.error("Inner exception: " + message, cause);
        throw e;
    }

    public static String shortError(Exception e) { return e.getClass().getName()+": "+e.getMessage(); }

    public static String errorString(Exception e) { return errorString(e, 1000); }

    public static String errorString(Exception e, int maxlen) {
        return truncate(shortError(e)+"\n"+ getStackTrace(e), maxlen);
    }

    public static boolean empty(String s) { return s == null || s.length() == 0; }

    /**
     * Determines if the parameter is "empty", by criteria described in @return
     * Tries to avoid throwing exceptions, handling just about any case in a true/false fashion.
     *
     * @param o anything
     * @return true if and only o is:
     *    * null
     *    * a collection, map, iterable or array that contains no objects
     *    * a file that does not exist or whose size is zero
     *    * a directory that does not exist or that contains no files
     *    * any object whose .toString method returns a zero-length string
     */
    public static boolean empty(Object o) {
        if (o == null) return true;
        if (o instanceof String) return o.toString().length() == 0;
        if (o instanceof Collection) return ((Collection)o).isEmpty();
        if (o instanceof Map) return ((Map)o).isEmpty();
        if (o instanceof JsonNode) {
            if (o instanceof ObjectNode) return ((ObjectNode) o).size() == 0;
            if (o instanceof ArrayNode) return ((ArrayNode) o).size() == 0;
            final String json = ((JsonNode) o).textValue();
            return json == null || json.length() == 0;
        }
        if (o instanceof Iterable) return !((Iterable)o).iterator().hasNext();
        if (o instanceof File) {
            final File f = (File) o;
            return !f.exists() || f.length() == 0 || (f.isDirectory() && list(f).length == 0);
        }
        if (o.getClass().isArray()) {
            if (o.getClass().getComponentType().isPrimitive()) {
                switch (o.getClass().getComponentType().getName()) {
                    case "boolean": return ((boolean[]) o).length == 0;
                    case "byte":    return ((byte[]) o).length == 0;
                    case "short":   return ((short[]) o).length == 0;
                    case "char":    return ((char[]) o).length == 0;
                    case "int":     return ((int[]) o).length == 0;
                    case "long":    return ((long[]) o).length == 0;
                    case "float":   return ((float[]) o).length == 0;
                    case "double":  return ((double[]) o).length == 0;
                    default: return o.toString().length() == 0;
                }
            } else {
                return ((Object[]) o).length == 0;
            }
        }
        return o.toString().length() == 0;
    }

    public static <T> T first (Iterable<T> o) { return (T) ((Iterable) o).iterator().next(); }
    public static <K, T> T first (Map<K, T> o) { return first(o.values()); }
    public static <T> T first (T[] o) { return o[0]; }

    public static <T> T sorted(T o) {
        if (empty(o)) return o;
        if (o.getClass().isArray()) {
            final Object[] copy = (Object[]) Array.newInstance(o.getClass().getComponentType(),
                                                               ((Object[])o).length);
            System.arraycopy(o, 0, copy, 0 , copy.length);
            Arrays.sort(copy);
            return (T) copy;
        }
        if (o instanceof Collection) {
            final List list = new ArrayList((Collection) o);
            Collections.sort(list);
            final Collection copy = (Collection) instantiate(o.getClass());
            copy.addAll(list);
            return (T) copy;
        }
        return die("sorted: cannot sort a "+o.getClass().getSimpleName()+", can only sort arrays and Collections");
    }
    public static <T> List toList(T o) {
        if (o == null) return null;
        if (o instanceof Collection) return new ArrayList((Collection) o);
        if (o instanceof Object[]) return Arrays.asList((Object[]) o);
        return die("sortedList: cannot sort a "+o.getClass().getSimpleName()+", can only sort arrays and Collections");
    }

    public static Boolean safeBoolean(String val, Boolean ifNull) { return empty(val) ? ifNull : Boolean.valueOf(val); }
    public static Boolean safeBoolean(String val) { return safeBoolean(val, null); }

    public static Integer safeInt(String val, Integer ifNull) { return empty(val) ? ifNull : Integer.valueOf(val); }
    public static Integer safeInt(String val) { return safeInt(val, null); }

    public static Long safeLong(String val, Long ifNull) { return empty(val) ? ifNull : Long.valueOf(val); }
    public static Long safeLong(String val) { return safeLong(val, null); }

    public static BigInteger bigint(long val) { return new BigInteger(String.valueOf(val)); }
    public static BigInteger bigint(int val) { return new BigInteger(String.valueOf(val)); }
    public static BigInteger bigint(byte val) { return new BigInteger(String.valueOf(val)); }

    public static BigDecimal big(String val) { return new BigDecimal(val); }
    public static BigDecimal big(double val) { return new BigDecimal(String.valueOf(val)); }
    public static BigDecimal big(float val) { return new BigDecimal(String.valueOf(val)); }
    public static BigDecimal big(long val) { return new BigDecimal(String.valueOf(val)); }
    public static BigDecimal big(int val) { return new BigDecimal(String.valueOf(val)); }
    public static BigDecimal big(byte val) { return new BigDecimal(String.valueOf(val)); }

    public static int percent(int value, double pct) { return percent(value, pct, RoundingMode.HALF_UP); }

    public static int percent(int value, double pct, RoundingMode rounding) {
        return big(value).multiply(big(pct)).setScale(0, rounding).intValue();
    }

    public static int percent(BigDecimal value, BigDecimal pct) {
        return percent(value.intValue(), pct.multiply(big(0.01)).doubleValue(), RoundingMode.HALF_UP);
    }

    public static String uuid() { return UUID.randomUUID().toString(); }

    private static AtomicLong systemTimeOffset = new AtomicLong(0L);
    public static long getSystemTimeOffset () { return systemTimeOffset.get(); }
    public static void setSystemTimeOffset (long t) { systemTimeOffset.set(t); }
    public static long incrementSystemTimeOffset(long t) { return systemTimeOffset.addAndGet(t); }
    public static long now() { return System.currentTimeMillis() + systemTimeOffset.get(); }
    public static String hexnow() { return toHexString(now()); }
    public static String hexnow(long now) { return toHexString(now); }
    public static long realNow() { return System.currentTimeMillis(); }

    public static <T> T pickRandom(T[] things) { return things[RandomUtils.nextInt(0, things.length)]; }
    public static <T> T pickRandom(List<T> things) { return things.get(RandomUtils.nextInt(0, things.size())); }

    public static BufferedReader stdin() { return new BufferedReader(new InputStreamReader(System.in)); }
    public static BufferedWriter stdout() { return new BufferedWriter(new OutputStreamWriter(System.out)); }

    public static String readStdin() { return StreamUtil.toStringOrDie(System.in); }

    public static int envInt (String name, int defaultValue) { return envInt(name, defaultValue, null, null); }
    public static int envInt (String name, int defaultValue, Integer maxValue) { return envInt(name, defaultValue, null, maxValue); }
    public static int envInt (String name, int defaultValue, Integer minValue, Integer maxValue) {
        return envInt(name, defaultValue, minValue, maxValue, System.getenv());
    }
    public static int envInt (String name, int defaultValue, Integer minValue, Integer maxValue, Map<String, String> env) {
        final String s = env.get(name);
        if (!empty(s)) {
            try {
                final int val = Integer.parseInt(s);
                if (val <= 0) {
                    log.warn("envInt: invalid value("+name+"): " +val+", returning "+defaultValue);
                    return defaultValue;
                } else if (maxValue != null && val > maxValue) {
                    log.warn("envInt: value too large ("+name+"): " +val+ ", returning " + maxValue);
                    return maxValue;
                } else if (minValue != null && val < minValue) {
                    log.warn("envInt: value too small ("+name+"): " +val+ ", returning " + minValue);
                    return minValue;
                }
                return val;
            } catch (Exception e) {
                log.warn("envInt: invalid value("+name+"): " +s+", returning "+defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static int processorCount() { return Runtime.getRuntime().availableProcessors(); }

    public static String hashOf (Object... things) {
        final StringBuilder b = new StringBuilder();
        for (Object thing : things) {
            if (b.length() > 0) b.append("\t");
            b.append(thing == null ? "null" : (thing instanceof Object[]) ? Arrays.deepHashCode((Object[]) thing) : thing.hashCode());
        }
        return sha256_hex(b.toString());
    }

    // from https://stackoverflow.com/a/8563667/1251543
    public static String hexToBase36(String hex) {
        return new BigInteger(hex, 16).toString(36);
    }

    public static Collection<String> stringRange(Number start, Number end) {
        return collect(range(start.longValue(), end.longValue()).boxed().iterator(), ToStringTransformer.instance);
    }

    public static String zcat() { return SystemUtils.IS_OS_MAC ? "gzcat" : "zcat"; }
    public static String zcat(File f) { return (SystemUtils.IS_OS_MAC ? "gzcat" : "zcat") + " " + abs(f); }

    public static final String[] JAVA_DEBUG_OPTIONS = {"-Xdebug", "-agentlib", "-Xrunjdwp"};

    public static boolean isDebugOption (String arg) {
        for (String opt : JAVA_DEBUG_OPTIONS) if (arg.startsWith(opt)) return true;
        return false;
    }

    public static String javaOptions() { return javaOptions(true); }

    public static String javaOptions(boolean excludeDebugOptions) {
        final List<String> opts = new ArrayList<>();
        for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (excludeDebugOptions && isDebugOption(arg)) continue;
            opts.add(arg);
        }
        return StringUtil.toString(opts, " ");
    }

    public static <T> T dcl (AtomicReference<T> target, Callable<T> init) {
        return dcl(target, init, null);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static <T> T dcl (AtomicReference<T> target, Callable<T> init, GeneralErrorHandler error) {
        if (target.get() == null) {
            synchronized (target) {
                if (target.get() == null) {
                    try {
                        target.set(init.call());
                    } catch (Exception e) {
                        if (error != null) {
                            error.handleError("dcl: error initializing: "+e, e);
                        } else {
                            log.warn("dcl: "+e);
                            return null;
                        }
                    }
                }
            }
        }
        return target.get();
    }

    public static String stacktrace() { return getStackTrace(new Exception()); }

    private static final AtomicLong selfDestructInitiated = new AtomicLong(-1);
    public static void setSelfDestruct (long t) { setSelfDestruct(t, 0); }
    public static void setSelfDestruct (long t, int status) {
        synchronized (selfDestructInitiated) {
            final long dieTime = selfDestructInitiated.get();
            if (dieTime == -1) {
                selfDestructInitiated.set(now()+t);
                daemon(() -> { sleep(t); System.exit(status); });
            } else {
                log.warn("setSelfDestruct: already set: self-destructing in "+formatDuration(dieTime-now()));
            }
        }
    }
}
