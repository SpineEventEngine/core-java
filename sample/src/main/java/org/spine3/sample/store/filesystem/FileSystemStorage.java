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
package org.spine3.sample.store.filesystem;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.AggregateCommand;
import org.spine3.base.CommandRequest;
import org.spine3.base.EventRecord;
import org.spine3.base.Snapshot;
import org.spine3.util.Commands;
import org.spine3.util.Events;
import org.spine3.protobuf.Messages;
import org.spine3.protobuf.Timestamps;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Test file system based implementation of the {@link Message} repository.
 *
 * @author Mikhail Melnik
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods")
public class FileSystemStorage implements Storage {

    private static final String STORAGE_PATH_IS_NOT_SET = "Storage path is not set.";

    private File backup = null;
    private String fileStoragePath = null;

    public FileSystemStorage(String fileStoragePath) {
        this.fileStoragePath = fileStoragePath;
    }

    /**
     * Returns the event records for the given aggregate root.
     *
     * @param aggregateId the id of the aggregate
     * @return list of the event records
     */
    @Override
    public List<EventRecord> readEvents(Message aggregateId) {
        File file = getEventsFile(aggregateId);
        return readEventsFromFile(file);
    }

    /**
     * Returns the command requests for the given aggregate root.
     *
     * @param aggregateId the id of the aggregate
     * @return list of the command requests
     */
    @Override
    public List<CommandRequest> readCommands(Message aggregateId) {
        File file = getCommandsFile(aggregateId);
        return readCommandsFromFile(file);
    }

    private static List<EventRecord> readEventsFromFile(File file) {
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

    private static List<CommandRequest> readCommandsFromFile(File file) {
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

    /**
     * Returns the event records for the given aggregate root
     * that has version greater than passed.
     *
     * @param sinceVersion the version of the aggregate root used as lower threshold for the result list
     * @return list of the event records
     */
    @Override
    public List<EventRecord> readEvents(int sinceVersion) {
        List<EventRecord> events = readAllEvents();
        Predicate<EventRecord> predicate = Events.getEventPredicate(sinceVersion);

        final ImmutableList<EventRecord> result = filter(events, predicate);
        return result;
    }

    /**
     * Returns all event records with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @return list of the event records
     */
    @Override
    public List<EventRecord> readEvents(Timestamp from) {
        checkNotNull(from);
        List<EventRecord> result = readEvents(from, Timestamps.now());
        return result;
    }

    /**
     * Returns all commands requests with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @return list of the command requests
     */
    @Override
    public List<CommandRequest> readCommands(Timestamp from) {
        checkNotNull(from);
        List<CommandRequest> result = readCommands(from, Timestamps.now());
        return result;
    }

    /**
     * Returns all event records with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @param to   the timestamp used as the upper threshold for the result list
     * @return list of the event records
     */
    @Override
    public List<EventRecord> readEvents(final Timestamp from, final Timestamp to) {
        checkNotNull(from);
        checkNotNull(to);

        List<EventRecord> events = readAllEvents();
        Predicate<EventRecord> predicate = Events.getEventPredicate(from, to);

        final ImmutableList<EventRecord> result = filter(events, predicate);
        return result;
    }

    /**
     * Returns all commands requests with timestamp greater than passed one.
     *
     * @param from the timestamp used as the lower threshold for the result list
     * @param to   the timestamp used as the upper threshold for the result list
     * @return list of the command requests
     */
    @Override
    public List<CommandRequest> readCommands(final Timestamp from, final Timestamp to) {
        checkNotNull(from);
        checkNotNull(to);

        List<CommandRequest> commands = readAllCommands();
        Predicate<CommandRequest> predicate = Commands.wereWithinPeriod(from, to);
        final ImmutableList<CommandRequest> result = filter(commands, predicate);

        return result;
    }

    private static <T> ImmutableList<T> filter(Iterable<T> list, Predicate<T> predicate) {
        return FluentIterable.from(list).filter(predicate).toList();
    }

    /**
     * Returns list of the all stored event records.
     *
     * @return list of the event records
     */
    @Override
    public List<EventRecord> readAllEvents() {
        List<EventRecord> result = readEventsForAggregates();
        Events.sort(result);
        return result;
    }

    /**
     * Returns list of the all stored command requests.
     *
     * @return list of the command requests
     */
    @Override
    public List<CommandRequest> readAllCommands() {
        List<CommandRequest> result = readCommandsForAggregates();
        Commands.sort(result);
        return result;
    }

    private List<EventRecord> readEventsForAggregates() {
        if (Strings.isNullOrEmpty(fileStoragePath)) {
            throw new RuntimeException(STORAGE_PATH_IS_NOT_SET);
        }

        List<EventRecord> result = Lists.newArrayList();

        File dir = new File(fileStoragePath + '/');
        for (String fileName : dir.list()) {
            File file = new File(fileStoragePath + '/' + fileName + "/events");
            if (file.exists()) {
                List<EventRecord> commands = readEventsFromFile(file);
                result.addAll(commands);
            }
        }
        return result;
    }

    private List<CommandRequest> readCommandsForAggregates() {
        if (Strings.isNullOrEmpty(fileStoragePath)) {
            throw new RuntimeException(STORAGE_PATH_IS_NOT_SET);
        }

        List<CommandRequest> result = Lists.newArrayList();

        File dir = new File(fileStoragePath + '/');
        for (String fileName : dir.list()) {
            File file = new File(fileStoragePath + '/' + fileName + "/commands");
            if (file.exists()) {
                List<CommandRequest> commands = readCommandsFromFile(file);
                result.addAll(commands);
            }
        }
        return result;
    }

    /**
     * Returns the last snapshot for the given aggregate root.
     *
     * @return the {@link Snapshot} object
     */
    @Override
    public Snapshot readLastSnapshot(Message aggregateId) {
        File file = getSnapshotsFile(aggregateId);

        Snapshot snapshot = readLastSnapshotFromFile(file);
        return snapshot;
    }

    private static Snapshot readLastSnapshotFromFile(File file) {
        if (file.exists()) {
            InputStream inputStream = null;

            try {
                inputStream = getObjectInputStream(file);

                Snapshot snapshot = Snapshot.parseDelimitedFrom(inputStream);
                return snapshot;
            } catch (IOException e) {
                //NOP
            } finally {
                closeSilently(inputStream);
            }
        }
        return null;
    }

    /**
     * Writes the passed event record to storage.
     *
     * @param eventRecord the event record to store
     */
    @Override
    public void writeEvent(EventRecord eventRecord) {
        Message aggregateId = Messages.fromAny(eventRecord.getContext().getAggregateId());
        File file = getEventsFile(aggregateId);

        writeMessage(file, eventRecord);
    }

    /**
     * Writes the passed command request to storage.
     *
     * @param commandRequest the command request to store
     */
    @Override
    public void writeCommand(CommandRequest commandRequest) {
        Message command = AggregateCommand.getCommandValue(commandRequest);
        Message aggregateId = AggregateCommand.getAggregateId(command);
        File file = getCommandsFile(aggregateId);

        writeMessage(file, commandRequest);
    }

    /**
     * Writes the snapshot of the given aggregate root to storage.
     *
     * @param aggregateId the aggregate root id
     * @param snapshot    the snapshot to store
     */
    @Override
    public void writeSnapshot(Message aggregateId, Snapshot snapshot) {
        File file = getSnapshotsFile(aggregateId);

        writeMessage(file, snapshot);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private void writeMessage(File file, Message message) {
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

    private File getEventsFile(Message aggregateId) {
        String events = "events";
        return getFile(aggregateId, events);
    }

    private File getCommandsFile(Message aggregateId) {
        String commands = "commands";
        return getFile(aggregateId, commands);
    }

    private File getSnapshotsFile(Message aggregateId) {
        String snapshots = "snapshots";
        return getFile(aggregateId, snapshots);
    }

    private File getFile(Message aggregateId, String fileName) {
        if (Strings.isNullOrEmpty(fileStoragePath)) {
            throw new RuntimeException(STORAGE_PATH_IS_NOT_SET);
        }
        File result = new File(fileStoragePath + '/'
                + aggregateId.getClass().getSimpleName()
                + '-' + Messages.toString(aggregateId) + '/' + fileName);
        return result;
    }

    private static OutputStream getObjectOutputStream(File file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file, true));
    }

    private static InputStream getObjectInputStream(File file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file));
    }

    private static File makeBackupCopy(File sourceFile) throws IOException {
        File backupFile = new File(sourceFile.toPath() + "_backup");

        Files.copy(sourceFile.toPath(), backupFile.toPath());

        return backupFile;
    }

    private void restoreFromBackup(File file) {
        boolean isDeleted = file.delete();
        if (isDeleted && backup != null) {
            //noinspection ResultOfMethodCallIgnored
            backup.renameTo(file);
        }
    }

    private static void closeSilently(@Nullable Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            //NOP
        }
    }

    public void setFileStoragePath(String fileStoragePath) {
        this.fileStoragePath = fileStoragePath;
    }

}
