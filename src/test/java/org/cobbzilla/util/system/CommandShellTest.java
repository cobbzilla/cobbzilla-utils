package org.cobbzilla.util.system;

import lombok.Getter;
import org.apache.tools.ant.util.LineOrientedOutputStream;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.util.string.StringUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandShellTest {

    public static final String TEST_SCRIPT = StringUtil.getPackagePath(CommandShellTest.class) + "/test.sh";

    private static File script;

    @BeforeClass public static void setup() throws Exception {
        script = StreamUtil.loadResourceAsFile(TEST_SCRIPT);
        CommandShell.chmod(script, "a+rx");
    }

    @AfterClass public static void teardown() throws Exception { script.delete(); }

    @Test public void testStreamCallbacks() throws Exception {
        final TestFilterOutputStream out = new TestFilterOutputStream();
        final Command command = new Command(script).setOut(out);
        CommandShell.exec(command);
        assertEquals(5, out.getFoundPercentages().size());
        for (int i = 20; i <= 100; i += 20) {
            assertTrue(out.getFoundPercentages().contains(i));
        }
    }

    @Test public void testProgressFilter() throws Exception {
        final TestProgressCallback callback = new TestProgressCallback();
        final CommandProgressFilter filter = new CommandProgressFilter()
                .addIndicator("marker::cat", 20)
                .addIndicator("marker::dog", 40)
                .addIndicator("marker::bird", 60)
                .addIndicator("marker::fish", 80)
                .addIndicator("marker::hedgehog", 100)
                .setCallback(callback);
        final Command command = new Command(script).setOut(filter);
        CommandShell.exec(command);
        assertEquals(100, filter.getPctDone());
        assertEquals(5, callback.count);
        assertEquals(5, callback.patternsFound.size());
    }

    public static final Pattern PCT_COMPLETE_PATTERN = Pattern.compile("Process is (\\d+)% complete.*");

    private class TestFilterOutputStream extends LineOrientedOutputStream {

        @Getter final List<Integer> foundPercentages = new ArrayList<>(5);

        @Override protected void processLine(String line) throws IOException {
            if (line.startsWith("Process is")) {
                final Matcher matcher = PCT_COMPLETE_PATTERN.matcher(line);
                if (matcher.find()) {
                    foundPercentages.add(Integer.valueOf(matcher.group(1)));
                }
            }
        }

    }

    private class TestProgressCallback implements CommandProgressCallback {

        public Set<String> patternsFound = new HashSet<>();
        public int count = 0;

        @Override public void updateProgress(CommandProgressMarker marker) {
            patternsFound.add(marker.getPattern().toString());
            count++;
        }
    }
}
