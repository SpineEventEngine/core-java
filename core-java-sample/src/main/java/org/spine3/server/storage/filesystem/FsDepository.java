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
import org.spine3.protobuf.Messages;
import org.spine3.util.FileNameEscaper;

import java.io.*;
import java.util.Map;

import static com.google.common.base.Throwables.propagate;
import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static java.nio.file.Files.copy;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.spine3.util.Identifiers.idToString;
import static org.spine3.util.IoUtil.*;

/**
 * Reads/writes messages from/to file system.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
class FsDepository {

    private static final String COMMAND_STORE_FILE_NAME = "/command-store";
    private static final String EVENT_STORE_FILE_NAME = "/event-store";
    private static final String AGGREGATE_FILE_NAME_PREFIX = "/aggregate/";
    private static final String PATH_DELIMITER = "/";
    private static final String ENTITY_STORE_DIR = "/entity-store/";

    private final String fileStorePath;
    private final String commandStoreFilePath;
    private final String eventStoreFilePath;

    private static File backup = null;

    /**
     * Creates new depository instance.
     *
     * @param executorClass execution context class used to choose target directory for storage.
     */
    public static FsDepository newInstance(Class executorClass) {
        return new FsDepository(executorClass);
    }

    private FsDepository(Class executorClass) {

        final String tempDir = getTempDir().getAbsolutePath();

        fileStorePath = tempDir + PATH_DELIMITER + executorClass.getSimpleName();
        commandStoreFilePath = fileStorePath + COMMAND_STORE_FILE_NAME;
        eventStoreFilePath = fileStorePath + EVENT_STORE_FILE_NAME;
    }

    /**
     * Writes {@link Message} into {@link File} using {@link Message#writeDelimitedTo}.
     *
     * @param file    the {@link File} to write data in
     * @param message the data to extract
     */
    @SuppressWarnings({"TypeMayBeWeakened", "ResultOfMethodCallIgnored", "OverlyBroadCatchBlock"})
    public static void writeMessage(File file, Message message) {

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

    /**
     * Reads {@link Message} from {@link File}.
     *
     * @param file the {@link File} to read from.
     */
    public static Message readMessage(File file) {

        checkFileExists(file);

        InputStream fileInputStream = open(file);
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

    /**
     * Removes all data from the file system storage.
     */
    public void deleteAll() {

        final File folder = new File(fileStorePath);
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
     * @param aggregateType the type of an aggregate.
     * @param aggregateIdString String representation of aggregate id.
     * @return path for aggregate storage file.
     */
    public File getAggregateFile(String aggregateType, String aggregateIdString) {

        final String filePath = fileStorePath + AGGREGATE_FILE_NAME_PREFIX +
                aggregateType + PATH_DELIMITER + aggregateIdString;
        final File file = new File(filePath);
        return file;
    }

    /**
     * @return common command store file.
     */
    public File getCommandStoreFile() {
        final File file = new File(commandStoreFilePath);
        return file;
    }

    /**
     * @return common event store file path.
     */
    public File getEventStoreFile() {
        final File file = new File(eventStoreFilePath);
        return file;
    }

    /**
     * @return entity file path.
     */
    public File getEntityStoreFile(String entityId) {
        final String filePath = fileStorePath + ENTITY_STORE_DIR + entityId;
        final File file = new File(filePath);
        return file;
    }

    private static void restoreFromBackup(File file) {
        boolean isDeleted = file.delete();
        if (isDeleted && backup != null) {
            //noinspection ResultOfMethodCallIgnored
            backup.renameTo(file);
        }
    }

    private static File makeBackupCopy(File sourceFile) throws IOException {

        final String backupFilePath = sourceFile.toPath() + "_backup";
        File backupFile = new File(backupFilePath);

        copy(sourceFile.toPath(), backupFile.toPath());

        return backupFile;
    }

    private static File getTempDir() {
        try {
            File tmpFile = File.createTempFile("temp-dir-check", ".tmp");
            File result = new File(tmpFile.getParent());
            if (tmpFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmpFile.delete();
            }
            return result;
        } catch (IOException e) {
            propagate(e);
        }
        throw new IllegalStateException("Unable to get temporary directory for storage");
    }

    /**
     * Utility class for converting IDs to strings acceptable for file names.
     */
    @SuppressWarnings("UtilityClass")
    public static class FsIdentifiers {

        private FsIdentifiers() {}

        /**
         * Creates string representation of the passed ID escaping characters
         * that are not allowed in file names.
         *
         * @param id the ID to convert
         * @return string representation of the ID
         * @see org.spine3.util.Identifiers#idToString(Object)
         * @see FileNameEscaper#escape(String)
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
    }
}
