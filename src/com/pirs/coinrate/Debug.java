package com.pirs.coinrate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

public final class Debug {
    private static PrintWriter writer;

    private Debug() {
    }

    public static synchronized void log(String msg) {
        try {
            if (writer == null) {
                Path path = Path.of(System.getProperty("user.home"), ".coinrate.log");
                writer = new PrintWriter(new BufferedWriter(new FileWriter(path.toFile(), true)));
            }
            writer.println("[" + java.time.LocalTime.now() + "] " + msg);
            writer.flush();
        } catch (IOException e) {
            // ignore, mod must never crash because of logging
        }
    }
}
