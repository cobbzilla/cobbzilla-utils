package org.cobbzilla.util.main;

import static org.cobbzilla.util.daemon.ZillaRuntime.shortError;
import static org.cobbzilla.util.reflect.ReflectionUtil.constValue;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

/**
 * Write a constant value to stdout.
 *
 * This will only ever write the constant value to stdout if it can successfully be read.
 * If the constant can be read, its value is printed with the .toString() method.
 * If an error occurs, nothing is written to stdout, and the error will be written to stderr.
 * If the value of the constant is null, nothing is printed to stdout, and "null" is printed to stderr.
 */
public class ConstMain extends BaseMain<ConstOptions> {

    public static void main(String[] args) { main(ConstMain.class, args); }

    @Override protected void run() throws Exception {
        final String cm = getOptions().getClassAndMember();
        final int lastDot = cm.lastIndexOf('.');
        if (lastDot == -1 || lastDot == cm.length()-1) {
            die("invalid value: "+cm);
        } else {
            try {
                final Object val = constValue(forName(cm.substring(0, lastDot)), cm.substring(lastDot + 1));
                if (val == null) {
                    err("null");
                } else {
                    out(val.toString());
                }
            } catch (Exception e) {
                die("Exception getting constant value "+cm+": "+shortError(e), e);
            }
        }
    }

}
