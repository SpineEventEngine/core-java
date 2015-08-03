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

package org.spine3.sample.server;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import org.spine3.AggregateCommand;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.protobuf.Messages;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides common file system access for {@link FileSystemStorage} and {@link FileSystemSnapshotStorage}.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class FileSystemHelper {

    private static final String STORAGE_PATH_IS_NOT_SET = "Storage path is not set.";

    @SuppressWarnings("StaticNonFinalField") //Is being rewritten
    private static File backup = null;

    @SuppressWarnings("StaticNonFinalField") //To divide initialization from class loading
    private static String fileStoragePath = null;

    private static final Map<Class<?>, Helper<?>> helpers = ImmutableMap.<Class<?>, Helper<?>>builder()
            .put(CommandRequest.class, new CommandHelper())
            .put(EventRecord.class, new EventHelper())
            .build();

    private FileSystemHelper() {
    }

    /**
     * Configures helper with file storage path.
     *
     * @param storagePath file storage path
     */
    public static void configure(String storagePath) {
        fileStoragePath = storagePath;
    }

    protected static File getSnapshotsFile(Message aggregateId) {
        String snapshots = "snapshots";
        return getFile(aggregateId, snapshots);
    }

    private static File getFile(Message aggregateId, String fileName) {
        if (Strings.isNullOrEmpty(fileStoragePath)) {
            throw new RuntimeException(STORAGE_PATH_IS_NOT_SET);
        }
        File result = new File(fileStoragePath + '/'
                + aggregateId.getClass().getSimpleName()
                + '-' + Messages.toString(aggregateId) + '/' + fileName);
        return result;
    }

    @SuppressWarnings("TypeMayBeWeakened")
    protected static void writeMessage(File file, Message message) {
        OutputStream outputStream = null;
        try {
            if (file.exists()) {
                backup = makeBackupCopy(file);
            } else {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
            }

            outputStream = getObjectOutputStream(file);

            message.writeDelimitedTo(outputStream);

            if (backup != null) {
                //noinspection ResultOfMethodCallIgnored
                backup.delete();
            }
        } catch (IOException ignored) {
            restoreFromBackup(file);
        } finally {
            closeSilently(outputStream);
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
        File backupFile = new File(sourceFile.toPath() + "_backup");

        Files.copy(sourceFile.toPath(), backupFile.toPath());

        return backupFile;
    }

    protected static void closeSilently(@Nullable Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            //NOP
        }
    }

    private static OutputStream getObjectOutputStream(File file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file, true));
    }

    protected static InputStream getObjectInputStream(File file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    protected static <M extends Message> List<M> read(Class<M> messageClass, Message parentId) {
        //noinspection unchecked
        final Helper<M> helper = (Helper<M>) helpers.get(messageClass);
        final File file = helper.getFile(parentId);
        final List<M> fileMessages = helper.readFromFile(file);
        return fileMessages;
    }

    protected static <M extends Message> void write(M message) {
        final Class<? extends Message> messageClass = message.getClass();
        //noinspection unchecked
        final Helper<M> helper = (Helper<M>) helpers.get(messageClass);

        helper.write(message);
    }

    protected static <M extends Message> List<M> readAll(Class<M> messageClass) {

        List<M> allMessages = Lists.newArrayList();

        final File dir = new File(fileStoragePath + '/');
        for (String fileName : dir.list()) {
            final Helper helper = helpers.get(messageClass);
            final File file = new File(fileStoragePath + '/' + fileName + '/' + helper.getFileNameSuffix());
            if (file.exists()) {
                //noinspection unchecked //we assume that helper produces messages of type M
                List<M> fileMessages = helper.readFromFile(file);
                allMessages.addAll(fileMessages);
            }
        }
        return allMessages;
    }

    private static class CommandHelper extends Helper<CommandRequest> {

        @Override
        public List<CommandRequest> readFromFile(File file) {
            if (file.exists()) {
                InputStream inputStream = null;

                List<CommandRequest> commands = new ArrayList<>();
                try {
                    inputStream = getObjectInputStream(file);

                    while (inputStream.available() > 0) {
                        CommandRequest command = CommandRequest.parseDelimitedFrom(inputStream);
                        commands.add(command);
                    }
                } catch (IOException e) {
                    //NOP
                } finally {
                    closeSilently(inputStream);
                }
                return commands;
            }
            return Collections.emptyList();
        }

        @Override
        public String getFileNameSuffix() {
            return "commands";
        }

        @Override
        protected void write(CommandRequest message) {
            Message command = AggregateCommand.getCommandValue(message);
            Message aggregateId = AggregateCommand.getAggregateId(command);
            File file = FileSystemHelper.getFile(aggregateId, getFileNameSuffix());

            writeMessage(file, message);
        }
    }

    private static class EventHelper extends Helper<EventRecord> {

        @Override
        public List<EventRecord> readFromFile(File file) {
            if (file.exists()) {
                InputStream inputStream = null;

                List<EventRecord> events = new ArrayList<>();
                try {
                    inputStream = getObjectInputStream(file);

                    while (inputStream.available() > 0) {
                        EventRecord event = EventRecord.parseDelimitedFrom(inputStream);
                        events.add(event);
                    }
                } catch (IOException e) {
                    //NOP
                } finally {
                    closeSilently(inputStream);
                }
                return events;
            }
            return Collections.emptyList();
        }

        @Override
        public String getFileNameSuffix() {
            return "events";
        }

        @Override
        protected void write(EventRecord message) {
            Message aggregateId = Messages.fromAny(message.getContext().getAggregateId());
            File file = FileSystemHelper.getFile(aggregateId, getFileNameSuffix());

            writeMessage(file, message);
        }
    }

    private abstract static class Helper<M extends Message> {

        protected File getFile(Message parentId) {
            return FileSystemHelper.getFile(parentId, getFileNameSuffix());
        }

        protected abstract List<M> readFromFile(File file);

        protected abstract String getFileNameSuffix();

        protected abstract void write(M message);
    }
}
