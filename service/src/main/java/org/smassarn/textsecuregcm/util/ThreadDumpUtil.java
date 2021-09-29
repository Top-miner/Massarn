package org.smassarn.textsecuregcm.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadDumpUtil {

    private static final Logger log = LoggerFactory.getLogger(ThreadDumpUtil.class);

    public static void writeThreadDump() {
        try {
            try (final PrintWriter out = new PrintWriter(File.createTempFile("thread_dump_", ".txt"))) {
                for (ThreadInfo info : ManagementFactory.getThreadMXBean().dumpAllThreads(true, true)) {
                    out.print(info);
                }
            }
        } catch (final IOException e) {
            log.warn("Failed to write thread dump", e);
        }
    }
}
