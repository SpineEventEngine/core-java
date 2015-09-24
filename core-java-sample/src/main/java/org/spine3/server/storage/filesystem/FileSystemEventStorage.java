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

import org.spine3.base.EventRecord;
import org.spine3.server.storage.EventStorage;
import org.spine3.server.storage.EventStoreRecord;

import java.io.*;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static org.spine3.server.storage.filesystem.Helper.closeSilently;

public class FileSystemEventStorage extends EventStorage {

    private final List<EventRecordFileIterator> iterators = newLinkedList();

    // TODO:alexander.litus: release res on end
    @Override
    public Iterator<EventRecord> allEvents() {

        final File file = new File(Helper.getEventFilePath());

        final EventRecordFileIterator iterator = new EventRecordFileIterator(file);

        iterators.add(iterator);

        return iterator;
    }

    @Override
    protected void write(EventStoreRecord record) {
        checkNotNull(record, "Record shouldn't be null.");
        Helper.write(record);
    }

    @Override
    protected void releaseResources() {
        for (EventRecordFileIterator i : iterators) {
            i.releaseResources();
        }
    }


    private static class EventRecordFileIterator implements Iterator<EventRecord> {

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

            boolean hasNext;
            try {
                final int availableBytesCount = getInputStream().available();
                hasNext = availableBytesCount > 0;
            } catch (IOException e) {
                throw new RuntimeException("Failed to get estimate of bytes available.", e);
            }
            return hasNext;
        }

        @SuppressWarnings({"ReturnOfNull", "IteratorNextCanNotThrowNoSuchElementException"})
        @Override
        public EventRecord next() {

            checkFileExists();
            checkHasNextBytes();

            EventStoreRecord storeRecord = parseEventRecord();
            EventRecord result = toEventRecord(storeRecord);

            checkNotNull(result, "Event record from file shouldn't be null.");

            if (!hasNext()) {
                releaseResources();
            }

            return result;
        }

        private EventStoreRecord parseEventRecord() {
            EventStoreRecord event;
            try {
                event = EventStoreRecord.parseDelimitedFrom(getInputStream());
            } catch (IOException e) {
                throw new RuntimeException("Failed read event record from file: " + file.getAbsolutePath(), e);
            }
            return event;
        }

        @SuppressWarnings("OverlyBroadCatchBlock")
        private InputStream getInputStream() {

            if (bufferedInputStream == null || fileInputStream == null) {
                try {
                    fileInputStream = new FileInputStream(file);
                    bufferedInputStream = new BufferedInputStream(fileInputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to get input stream from file: " + file.getAbsolutePath(), e);
                }
            }

            return bufferedInputStream;
        }

        private void releaseResources() {
            if (!areResourcesReleased) {
                closeSilently(fileInputStream, bufferedInputStream);
                areResourcesReleased = true;
            }
        }

        @SuppressWarnings("TypeMayBeWeakened")
        private static EventRecord toEventRecord(EventStoreRecord storeRecord) {// TODO[alexander.litus]: extract
            final EventRecord.Builder builder = EventRecord.newBuilder()
                    .setEvent(storeRecord.getEvent())
                    .setContext(storeRecord.getContext());
            return builder.build();
        }

        private void checkFileExists() {
            if (!file.exists()) {
                throw new IllegalStateException("No such file: " + file.getAbsolutePath());
            }
        }

        private void checkHasNextBytes() {
            if (!hasNext()) {
                throw new IllegalStateException("No more records to read from file.");
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
