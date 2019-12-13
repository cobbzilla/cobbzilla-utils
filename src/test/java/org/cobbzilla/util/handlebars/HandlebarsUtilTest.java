package org.cobbzilla.util.handlebars;

import com.github.jknack.handlebars.Handlebars;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.string.StringUtil.getPackagePath;
import static org.junit.Assert.assertEquals;

public class HandlebarsUtilTest {

    @Test public void testAltChars () throws Exception {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("name", "TEST_NAME");
        final String template = stream2string(getPackagePath(getClass())+"/alt_test.txt.hbs");
        final String expected = stream2string(getPackagePath(getClass())+"/alt_test.txt");

        final Handlebars hbs = new Handlebars(new HandlebarsUtil(getClass().getSimpleName()));
        final String result = HandlebarsUtil.apply(hbs, template, ctx, '<', '>');

        assertEquals("handlebars template processed incorrectly", expected, result);
    }

}
