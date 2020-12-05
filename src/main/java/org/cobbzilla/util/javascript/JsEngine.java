package org.cobbzilla.util.javascript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import javax.script.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

@Accessors(chain=true) @Slf4j
public class JsEngine {

    private final List<ScriptEngine> availableScriptEngines;

    private final int maxEngines;
    private final String defaultScript;
    public String getDefaultScript () { return empty(defaultScript) ? "" : defaultScript; }

    public JsEngine() { this(new JsEngineConfig(1, 1, null)); }

    public JsEngine(JsEngineConfig config) {
        availableScriptEngines = new ArrayList<>(config.getMinEngines());
        maxEngines = config.getMaxEngines();
        defaultScript = config.getDefaultScript();
        for (int i=0; i<config.getMinEngines(); i++) {
            availableScriptEngines.add(getEngine(false));
        }
        engCounter = new AtomicInteger(availableScriptEngines.size());
    }

    protected ScriptEngine getEngine() { return getEngine(true); }

    private final AtomicInteger engCounter;
    private final AtomicInteger inUse = new AtomicInteger(0);
    protected ScriptEngine getEngine(boolean report) {
        final ScriptEngine engine = GraalJSScriptEngine.create(null,
                Context.newBuilder("js")
                        .allowHostAccess(HostAccess.ALL)
                        .allowHostClassLookup(s -> true));
        if (report) log.info("getEngine: creating scripting engine #"+ engCounter.incrementAndGet()+" ("+availableScriptEngines.size()+" available, "+inUse.get()+" in use)");
        return engine;
    }

    public <T> T evaluate(String code, Map<String, Object> context) {
        ScriptEngine engine = null;
        final int numEngines;
        synchronized (availableScriptEngines) {
            numEngines = availableScriptEngines.size();
            if (numEngines > 0) {
                engine = availableScriptEngines.remove(0);
            }
        }
        if (engine == null) {
            if (numEngines >= maxEngines) return die("evaluate("+code+"): maxEngines ("+maxEngines+") reached, no js engines available to execute, "+inUse.get()+" in use");
            engine = getEngine();
        }
        inUse.incrementAndGet();

        try {
            final ScriptContext scriptContext = new SimpleScriptContext();
            final SimpleBindings bindings = new SimpleBindings();
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                final Object value = entry.getValue();
                final Object wrappedOut;
                Object[] wrappedArray = null;
                if (value == null) {
                    wrappedOut = null;
                } else if (value instanceof JsWrappable) {
                    wrappedOut = ((JsWrappable) value).jsObject();
                } else if (value instanceof ArrayNode) {
                    wrappedOut = fromJsonOrDie((JsonNode) value, Object[].class);
                } else if (value instanceof JsonNode) {
                    wrappedOut = fromJsonOrDie((JsonNode) value, Object.class);
                } else if (value.getClass().isArray()) {
                    wrappedArray = (Object[]) value;
                    wrappedOut = null;
                } else {
                    wrappedOut = value;
                }
                if (wrappedArray != null) {
                    bindings.put(entry.getKey(), wrappedArray);
                } else {
                    bindings.put(entry.getKey(), wrappedOut);
                }
            }
            scriptContext.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
            try {
                Object eval = engine.eval(getDefaultScript()+"\n"+code, scriptContext);
                return (T) eval;
            } catch (ScriptException e) {
                throw new IllegalStateException(e);
            }
        } finally {
            synchronized (availableScriptEngines) {
                availableScriptEngines.add(engine);
            }
            inUse.decrementAndGet();
        }
    }

    public boolean evaluateBoolean(String code, Map<String, Object> ctx) {
        final Object result = evaluate(code, ctx);
        return result == null ? false : Boolean.valueOf(result.toString().toLowerCase());
    }

    public boolean evaluateBoolean(String code, Map<String, Object> ctx, boolean defaultValue) {
        try {
            return evaluateBoolean(code, ctx);
        } catch (Exception e) {
            log.debug("evaluateBoolean: returning "+defaultValue+" due to exception:"+e);
            return defaultValue;
        }
    }

    public Integer evaluateInt(String code, Map<String, Object> ctx) {
        final Object result = evaluate(code, ctx);
        if (result == null) return null;
        if (result instanceof Number) return ((Number) result).intValue();
        return Integer.parseInt(result.toString().trim());
    }

    public Long evaluateLong(String code, Map<String, Object> ctx) {
        final Object result = evaluate(code, ctx);
        if (result == null) return null;
        if (result instanceof Number) return ((Number) result).longValue();
        return Long.parseLong(result.toString().trim());
    }

    public Long evaluateLong(String code, Map<String, Object> ctx, Long defaultValue) {
        try {
            return evaluateLong(code, ctx);
        } catch (Exception e) {
            log.debug("evaluateLong: returning "+defaultValue+" due to exception:"+e);
            return defaultValue;
        }
    }

    public String evaluateString(String condition, Map<String, Object> ctx) {
        final Object rval = evaluate(condition, ctx);
        if (rval == null) return null;

        if (rval instanceof String) return rval.toString();
        if (rval instanceof Number) {
            if (rval.toString().endsWith(".0")) return ""+((Number) rval).longValue();
            return rval.toString();
        }
        return rval.toString();
    }

    public String functionOfX(String value, String script) {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("x", value);
        try {
            return String.valueOf(evaluateInt(script, ctx));
        } catch (Exception e) {
            log.warn("functionOfX('"+value+"', '"+script+"', NOT applying due to exception: "+e);
            return value;
        }
    }
}
