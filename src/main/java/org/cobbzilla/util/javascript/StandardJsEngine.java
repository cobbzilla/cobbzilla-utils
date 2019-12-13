package org.cobbzilla.util.javascript;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;

@Slf4j
public class StandardJsEngine extends JsEngine {

    public StandardJsEngine() { this(1, 1); }

    public StandardJsEngine(int minEngines, int maxEngines) { super(new JsEngineConfig(minEngines, maxEngines, STANDARD_FUNCTIONS)); }

    public static final String STANDARD_FUNCTIONS = stream2string(getPackagePath(StandardJsEngine.class)+"/standard_js_lib.js");

    private static final String ESC_DOLLAR = "__ESCAPED_DOLLAR_SIGN__";
    public static String replaceDollarSigns(String val) {
        return val.replace("'$", ESC_DOLLAR)
                .replaceAll("(\\$(\\d+(\\.\\d{2})?))", "($2 * 100)")
                .replace(ESC_DOLLAR, "'$");
    }

    public String round(String value, String script) {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("x", value);
        try {
            return String.valueOf(evaluateInt(script, ctx));
        } catch (Exception e) {
            log.warn("round('"+value+"', '"+script+"', NOT rounding due to exception: "+e);
            return value;
        }
    }
}
