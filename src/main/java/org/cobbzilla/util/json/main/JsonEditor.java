package org.cobbzilla.util.json.main;

import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonEdit;
import org.cobbzilla.util.json.JsonEditOperation;
import org.cobbzilla.util.main.BaseMain;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class JsonEditor extends BaseMain<JsonEditorOptions> {

    public static void main(String[] args) throws Exception { main(JsonEditor.class, args); }

    public void run() throws Exception {
        final JsonEditorOptions options = getOptions();
        JsonEdit edit = new JsonEdit()
                .setJsonData(options.getInputJson())
                .addOperation(new JsonEditOperation()
                        .setType(options.getOperationType())
                        .setPath(options.getPath())
                        .setJson(options.getValue()));

        final String json = edit.edit();

        if (options.hasOutfile()) {
            FileUtil.toFile(options.getOutfile(), json);
        } else {
            if (empty(json)) {
                System.exit(1);
            } else {
                System.out.print(json);
            }
        }
        System.exit(0);
    }

}