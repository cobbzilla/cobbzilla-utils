package org.cobbzilla.util.network;

import java.io.IOException;
import java.net.ServerSocket;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class PortPicker {

    public static int pick() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    public static int pickOrDie() {
        try {
            return pick();
        } catch (IOException e) {
            return die("Error picking port: "+e, e);
        }
    }

}