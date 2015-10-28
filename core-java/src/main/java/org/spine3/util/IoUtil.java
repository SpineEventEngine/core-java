/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;

import static com.google.common.base.Throwables.propagate;

/**
 * Utility class working with I/O: streams, files etc.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class IoUtil {

    private IoUtil() {}

    /**
     * Closes passed closeables one by one silently.
     * <p/>
     * Logs each {@link java.io.IOException} if it occurs.
     */
    @SuppressWarnings("ConstantConditions")
    public static void closeSilently(@Nullable Closeable... closeables) {
        if (closeables == null) {
            return;
        }
        try {
            for (Closeable c : closeables) {
                if (c != null) {
                    c.close();
                }
            }
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Exception while closing stream", e);
            }
        }
    }

    /**
     * Flushes passed streams one by one.
     * <p/>
     * Logs each {@link IOException} if it occurs.
     */
    public static void flushSilently(@Nullable Flushable... flushables) {
        try {
            flush(flushables);
        } catch (IOException e) {
            if (log().isWarnEnabled()) {
                log().warn("Exception while flushing stream", e);
            }
        }
    }

    /**
     * Flushes streams in turn.
     *
     * @throws java.lang.RuntimeException if {@link IOException} occurs
     */
    public static void tryToFlush(@Nullable Flushable... flushables) {
        try {
            flush(flushables);
        } catch (IOException e) {
            propagate(e);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static void flush(@Nullable Flushable[] flushables) throws IOException {
        if (flushables == null) {
            return;
        }
        for (Flushable f : flushables) {
            if (f != null) {
                f.flush();
            }
        }
    }

    /**
     * Flushes and closes output streams in turn silently. Logs IOException if occurs.
     */
    public static void flushAndCloseSilently(@Nullable OutputStream... streams) {
        if (streams == null) {
            return;
        }
        flushSilently(streams);
        closeSilently(streams);
    }

    /**
     * @param file file to check
     * @throws IllegalStateException if there is no such file
     */
    public static void checkFileExists(File file) {
        if (!file.exists()) {
            throw new IllegalStateException("No such file: " + file.getAbsolutePath());
        }
    }

    /**
     * Tries to open {@code FileInputStream} from file
     *
     * @throws RuntimeException if there is no such file
     */
    public static FileInputStream open(File file) {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            propagate(e);
        }

        return fileInputStream;
    }
    
    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
    
    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(IoUtil.class);
    }
}
