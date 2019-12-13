package org.cobbzilla.util.system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.tools.ant.util.LineOrientedOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Accessors(chain=true)
public class CommandProgressFilter extends LineOrientedOutputStream {

    @Getter private int pctDone = 0;
    @Getter private int indicatorPos = 0;
    @Getter private boolean closed = false;
    @Getter @Setter private CommandProgressCallback callback;

    @AllArgsConstructor
    private class CommandProgressIndicator {
        @Getter @Setter private int percent;
        @Getter @Setter private Pattern pattern;
    }

    private List<CommandProgressIndicator> indicators = new ArrayList<>();

    public CommandProgressFilter addIndicator(String pattern, int pct) {
        indicators.add(new CommandProgressIndicator(pct, Pattern.compile(pattern)));
        return this;
    }

    @Override public void close() throws IOException { closed = true; }

    @Override protected void processLine(String line) throws IOException {
        for (int i=indicatorPos; i<indicators.size(); i++) {
            final CommandProgressIndicator indicator = indicators.get(indicatorPos);
            if (indicator.getPattern().matcher(line).find()) {
                pctDone = indicator.getPercent();
                indicatorPos++;
                if (callback != null) callback.updateProgress(new CommandProgressMarker(pctDone, indicator.getPattern(), line));
                return;
            }
        }
    }

}
