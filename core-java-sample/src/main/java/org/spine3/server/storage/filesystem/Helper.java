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

package org.spine3.server.storage.filesystem;

import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.storage.CommandStoreRecord;
import org.spine3.server.storage.EventStoreRecord;

import javax.annotation.Nullable;
import java.io.*;

import static com.google.common.base.Throwables.propagate;
import static java.nio.file.Files.copy;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.spine3.server.storage.filesystem.FileSystemStoragePathHelper.*;
import static org.spine3.server.storage.filesystem.FileSystemStoragePathHelper.getCommandStoreFilePath;
import static org.spine3.server.storage.filesystem.FileSystemStoragePathHelper.getEventStoreFilePath;

/**
 * Util class for working with file system
 * TODO:2015-09-22:mikhail.mikhaylov:
 * This class should replace {@code org.spine3.sample.server.FileSystemHelper}.
 * Also, the old mechanism of working
 * with files should also be removed as soon as this is ready.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
class Helper {

    private static final Logger LOG = LoggerFactory.getLogger(Helper.class);

    @SuppressWarnings("StaticNonFinalField")
    private static File backup = null;

    private Helper() {}

    /**
     * Configures helper with file storage path.
     *
     * @param executorClass execution context class. Is used to choose target directory.
     */
    public static void configure(Class executorClass) {
        FileSystemStoragePathHelper.configure(executorClass);
    }

    /**
     * Writes the {@code CommandStoreRecord} to common command store.
     *
     * @param record {@code CommandStoreRecord} instance
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static void write(CommandStoreRecord record) {

        final String filePath = getCommandStoreFilePath();
        File file = new File(filePath);
        writeMessage(file, record);
    }

    /**
     * Writes the {@code EventStoreRecord} to common event store.
     *
     * @param record {@code EventStoreRecord} instance
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static void write(EventStoreRecord record) {

        final String filePath = getEventStoreFilePath();
        File file = new File(filePath);
        writeMessage(file, record);
    }

    /**
     * Removes all previous existing data from file system storage.
     */
    public static void cleanTestData() {

        final File folder = new File(getFileStoragePath());
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }

        try {
            deleteDirectory(folder);
        } catch (IOException e) {
            propagate(e);
        }
    }

    /*
     * Closes streams in turn.
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
            if (LOG.isWarnEnabled()) {
                LOG.warn("Exception while closing stream", e);
            }
        }
    }

    /*
     * Flushes streams in turn.
     */
    @SuppressWarnings("ConstantConditions")
    public static void flushSilently(@Nullable Flushable... flushables) {
        if (flushables == null) {
            return;
        }
        try {
            for (Flushable f : flushables) {
                if (f != null) {
                    f.flush();
                }
            }
        } catch (IOException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Exception while flushing stream", e);
            }
        }
    }

    /*
     * Flushes and closes output streams in turn.
     */
    @SuppressWarnings("ConstantConditions")
    public static void flushAndCloseSilently(@Nullable OutputStream... streams) {
        if (streams == null) {
            return;
        }
        flushSilently(streams);
        closeSilently(streams);
    }

    /**
     * Writes {@code Message} into {@code File} using {@code Message.writeDelimitedTo}.
     *
     * @param file    a {@code File} to write data in
     * @param message data to extract
     */
    @SuppressWarnings({"TypeMayBeWeakened", "ResultOfMethodCallIgnored", "OverlyBroadCatchBlock"})
    private static void writeMessage(File file, Message message) {

        FileOutputStream fileOutputStream = null;
        OutputStream bufferedOutputStream = null;

        try {
            if (file.exists()) {
                backup = makeBackupCopy(file);
            } else {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            fileOutputStream = new FileOutputStream(file, true);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            message.writeDelimitedTo(bufferedOutputStream);

            if (backup != null) {
                backup.delete();
            }
        } catch (IOException ignored) {
            restoreFromBackup(file);
        } finally {
            flushAndCloseSilently(fileOutputStream, bufferedOutputStream);
        }
    }

    private static void restoreFromBackup(File file) {
        boolean isDeleted = file.delete();
        if (isDeleted && backup != null) {
            //noinspection ResultOfMethodCallIgnored
            backup.renameTo(file);
        }
    }

    private static File makeBackupCopy(File sourceFile) throws IOException {
        final String backupFilePath = getBackupFilePath(sourceFile);
        File backupFile = new File(backupFilePath);

        copy(sourceFile.toPath(), backupFile.toPath());

        return backupFile;
    }
}
