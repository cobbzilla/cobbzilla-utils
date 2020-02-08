package org.cobbzilla.util.io.regex;

import lombok.Getter;
import org.cobbzilla.util.collection.NameAndValue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexChunkStreamer {

    private RegexChunkConfig config;
    private List<RegexChunk> chunks = new ArrayList<>();
    private int index = 0;
    @Getter private int total = 0;

    public boolean hasMoreChunks () { return index < chunks.size(); }

    public RegexChunk nextChunk () { return chunks.get(index++); }

    public RegexChunkStreamer(StringBuilder buffer, RegexChunkConfig config, boolean eof) {
        this.config = config;

        final Matcher chunkStartMatcher = config.getChunkStartPattern().matcher(buffer);

        final Pattern chunkEndRegex = config.getChunkEndPattern();
        final Matcher chunkEndMatcher = chunkEndRegex == null ? null : chunkEndRegex.matcher(buffer);

        int start = 0;
        while (chunkStartMatcher.find(start)) {
            if (chunks.isEmpty() && start != 0 && chunkStartMatcher.start() != 0) {
                addChunk(new RegexChunk()
                        .setType(RegexChunkType.content)
                        .setData(buffer.substring(0, chunkStartMatcher.start())));

            } else if (chunkStartMatcher.start() != start) {
                // add a content chunk
                addChunk(new RegexChunk()
                        .setType(RegexChunkType.content)
                        .setData(buffer.substring(start, chunkStartMatcher.start())));
            }

            // figure out where the chunk starts and stops
            RegexChunk chunk = null;
            if (chunkEndMatcher != null) {
                if (chunkEndMatcher.find(chunkStartMatcher.end())) {
                    chunk = new RegexChunk()
                            .setType(RegexChunkType.chunk)
                            .setData(buffer.substring(chunkStartMatcher.start(), chunkEndMatcher.end()));
                    start = chunkEndMatcher.end();

                } else {
                    // chunk end not found, if we are at EOF that is OK, this must be last comment
                    if (eof) {
                        chunk = new RegexChunk()
                                .setType(RegexChunkType.chunk)
                                .setData(buffer.substring(chunkStartMatcher.start()));
                        start = buffer.length();
                        addChunk(chunk);

                    } else {
                        // this blocked comment must extend to end of buffer
                        // ask for more data, we have to restart processing
                        chunk = new RegexChunk()
                                .setPartial(true)
                                .setType(RegexChunkType.chunk)
                                .setData(buffer.substring(chunkStartMatcher.start()));
                        addChunk(chunk);
                        return;
                    }
                }
            }
            if (chunk == null) {
                int chunkStart = chunkStartMatcher.start();
                if (chunkStartMatcher.find(chunkStartMatcher.end())) {
                    // found the next comment, cut up until then
                    chunk = new RegexChunk()
                            .setType(RegexChunkType.chunk)
                            .setData(buffer.substring(chunkStart, chunkStartMatcher.start()));
                    start = chunkStartMatcher.start();
                } else {
                    // no more comments! I guess we go from here until the end
                    if (eof) {
                        chunk = new RegexChunk()
                                .setType(RegexChunkType.chunk)
                                .setData(buffer.substring(chunkStart));
                        start = buffer.length();
                    } else {
                        // ask for more data
                        // this blocked comment must extend to end of buffer
                        // ask for more data, we have to restart processing
                        chunk = new RegexChunk()
                                .setPartial(true)
                                .setType(RegexChunkType.chunk)
                                .setData(buffer.substring(chunkStart));
                        addChunk(chunk);
                        return;
                    }
                }
            }
            addChunk(chunk);
        }
        if (chunks.isEmpty()) {
            // we found nothing, so the whole buffer is the entire thing, and it's partial so we
            // can re-evaluate when there is more data
            addChunk(new RegexChunk()
                    .setPartial(true)
                    .setData(buffer.toString()));
        } else {
            // add any remainder as the footer
            addChunk(new RegexChunk()
                    .setType(RegexChunkType.content)
                    .setData(buffer.substring(start)));
        }
    }

    private void addChunk (RegexChunk chunk) {
        if (chunk.getType() == RegexChunkType.chunk) {
            if (config.hasChunkProperties()) {
                for (NameAndValue prop : config.getChunkProperties()) {
                    final Pattern p = Pattern.compile(prop.getValue(), Pattern.CASE_INSENSITIVE|Pattern.MULTILINE);
                    final Matcher m = p.matcher(chunk.getData());
                    if (m.find()) {
                        chunk.setProperty(prop.getName(), m.group(1));
                    }
                }
            }
        }
        chunks.add(chunk);
        total += chunk.getData().length();
    }

}
