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

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.protobuf.Messages;
import org.spine3.server.storage.CommandStoreRecord;
import org.spine3.server.storage.EventStoreRecord;
import org.spine3.util.FileNameEscaper;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static java.nio.file.Files.copy;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.spine3.server.storage.filesystem.FileSystemStoragePathHelper.*;
import static org.spine3.util.Identifiers.idToString;

/**
 * Util class for working with file system
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
public class FileSystemHelper {

    @SuppressWarnings("StaticNonFinalField")
    private static File backup = null;

    private FileSystemHelper() {}

    /**
     * Configures helper with file storage path.
     *
     * @param executorClass execution context class. Is used to choose target directory for storage.
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
     * Writes the {@code Message} to common event store.
     *
     * @param message {@code Message} message to write
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static void writeEntity(String idString, Message message) {

        final String path = getEntityStoreFilePath(idString);
        File file = new File(path);
        final Any any = Messages.toAny(message);
        writeMessage(file, any);
    }

    /**
     * Reads the {@code Message} from common event store by string id.
     * @return a message instance or empty message if there is no message with such ID
     */
    public static Message readEntity(String idString) {

        final String path = getEntityStoreFilePath(idString);
        File file = new File(path);

        Message message = Any.getDefaultInstance();

        if (file.exists()) {
            message = readMessage(file);
        }

        return message;
    }

    /**
     * Removes all previous existing data from file system storage.
     */
    public static void cleanTestData() {

        final File folder = new File(getFileStorePath());
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }

        try {
            deleteDirectory(folder);
        } catch (IOException e) {
            propagate(e);
        }
    }

    /**
     * Closes streams in turn silently. Logs IOException if occurs.
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
     * Flushes streams in turn silently. Logs IOException if occurs.
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
     * @throws java.lang.RuntimeException if IOException occurs
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
    public static FileInputStream tryOpenFileInputStream(File file) {
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            propagate(e);
        }

        return fileInputStream;
    }

    /**
     * Creates string representation of the passed ID. Escapes characters which are not allowed in file names.
     *
     * @param id the ID to convert
     * @return string representation of the ID
     * @see org.spine3.util.Identifiers#idToString(Object)
     * @see org.spine3.util.FileNameEscaper#escape(String)
     */
    public static <I> String idToStringWithEscaping(I id) {

        final I idNormalized = escapeStringFieldsIfIsMessage(id);
        String result = idToString(idNormalized);
        result = FileNameEscaper.getInstance().escape(result);

        return result;
    }

    private static <I> I escapeStringFieldsIfIsMessage(I input) {
        I result = input;
        if (input instanceof Message) {
            final Message message = escapeStringFields((Message) input);
            @SuppressWarnings("unchecked")
            I castedMessage = (I) message; // cast is safe because input is Message
            result = castedMessage;
        }
        return result;
    }

    private static Message escapeStringFields(Message message) {

        final Message.Builder result = message.toBuilder();
        final Map<Descriptors.FieldDescriptor, Object> fields = message.getAllFields();

        final FileNameEscaper escaper = FileNameEscaper.getInstance();

        for (Descriptors.FieldDescriptor descriptor : fields.keySet()) {

            Object value = fields.get(descriptor);

            if (descriptor.getJavaType() == STRING) {
                value = escaper.escape(value.toString());
            }

            result.setField(descriptor, value);
        }

        return result.build();
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

    private static Message readMessage(File file) {

        checkFileExists(file);

        InputStream fileInputStream = tryOpenFileInputStream(file);
        InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        Any any;

        try {
            any = Any.parseDelimitedFrom(bufferedInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read message from file: " + file.getAbsolutePath(), e);
        } finally {
            closeSilently(fileInputStream, bufferedInputStream);
        }

        final Message result = Messages.fromAny(any);
        return result;
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

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FileSystemHelper.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

}
