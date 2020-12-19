package org.cobbzilla.util.javascript;

import lombok.extern.slf4j.Slf4j;

import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;

@Slf4j
public class StandardJsEngine extends JsEngine {

    public StandardJsEngine() { this(1, 1); }

    public StandardJsEngine(int minEngines, int maxEngines) { super(new JsEngineConfig(minEngines, maxEngines, STANDARD_FUNCTIONS)); }

    public static final String STANDARD_FUNCTIONS = stream2string(getPackagePath(StandardJsEngine.class)+"/standard_js_lib.js");

}
