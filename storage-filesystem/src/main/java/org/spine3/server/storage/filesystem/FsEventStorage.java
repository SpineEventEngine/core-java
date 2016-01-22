/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.collect.Iterators;
import org.spine3.base.EventId;
import org.spine3.base.EventRecord;
import org.spine3.io.IoUtil;
import org.spine3.io.file.FileUtil;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStorageRecord;
import org.spine3.server.storage.StorageUtil;
import org.spine3.server.stream.EventStreamQuery;

import javax.annotation.Nullable;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static org.spine3.server.storage.filesystem.FsStorageUtil.writeMessage;

/**
 * An event storage based on the file system.
 *
 * @author Alexander Litus
 */
class FsEventStorage extends EventStorage {

    private static final String EVENT_STORE_FILE_NAME = "/event-store";

    private final List<EventRecordFileIterator> iterators = newLinkedList();
    private final File eventStorageFile;


    /**
     * Creates new storage instance.
     * @param rootDirectoryPath an absolute path to the root storage directory (without the delimiter at the end)
     * @throws IOException - if failed to create event storage file
     */
    protected static FsEventStorage newInstance(String rootDirectoryPath) throws IOException {
        return new FsEventStorage(rootDirectoryPath);
    }

    private FsEventStorage(String rootDirectoryPath) throws IOException {
        this.eventStorageFile = FileUtil.createIfDoesNotExist(rootDirectoryPath + EVENT_STORE_FILE_NAME);
    }

    @Override
    public Iterator<EventRecord> iterator(EventStreamQuery query) {
        final EventRecordFileIterator iterator = new EventRecordFileIterator(eventStorageFile);
        iterators.add(iterator);
        final Iterator<EventRecord> result = Iterators.filter(iterator, new MatchesStreamQuery(query));
        return result;
    }

    @Override
    protected void writeInternal(EventStorageRecord record) {
        checkNotNull(record);
        writeMessage(eventStorageFile, record);
    }

    @Nullable
    @Override
    protected EventStorageRecord readInternal(EventId eventId) {

        //TODO:2016-01-21:alexander.yevsyukov: Implement

        return null;
    }

    @Override
    public void close() throws Exception {
        for (EventRecordFileIterator i : iterators) {
            i.close();
        }
        super.close();
    }

    private static class EventRecordFileIterator implements Iterator<EventRecord>, Closeable {

        private final File file;
        private FileInputStream fileInputStream;
        private BufferedInputStream bufferedInputStream;
        private boolean areResourcesReleased;

        private EventRecordFileIterator(File file) {
            this.file = file;
        }

        @Override
        public boolean hasNext() {
            if (!file.exists() || areResourcesReleased) {
                return false;
            }

            final boolean hasNext;
            try {
                final int availableByteCount = getInputStream().available();
                hasNext = availableByteCount > 0;
            } catch (IOException e) {
                throw new RuntimeException("Failed to get estimate of bytes available.", e);
            }
            return hasNext;
        }

        @Override
        public EventRecord next() {
            FileUtil.checkFileExists(file, "event storage");
            checkHasNextBytes();

            final EventStorageRecord storeRecord = parseEventRecord();
            final EventRecord result = StorageUtil.toEventRecord(storeRecord);

            if (!hasNext()) {
                close();
            }

            checkNotNull(result, "event record from the file");

            return result;
        }

        private EventStorageRecord parseEventRecord() {
            final EventStorageRecord event;
            try {
                event = EventStorageRecord.parseDelimitedFrom(getInputStream());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read event record from file: " + file.getAbsolutePath(), e);
            }
            return event;
        }

        private InputStream getInputStream() {
            if (bufferedInputStream == null || fileInputStream == null) {
                fileInputStream = FileUtil.open(file);
                bufferedInputStream = new BufferedInputStream(fileInputStream);
            }

            return bufferedInputStream;
        }

        @Override
        public void close() {
            if (!areResourcesReleased) {
                IoUtil.closeSilently(fileInputStream, bufferedInputStream);
                areResourcesReleased = true;
            }
        }

        private void checkHasNextBytes() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more records to read from file.");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This operation is not supported for FileSystemStorage");
        }
    }
}
