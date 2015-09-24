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

import com.google.common.base.Throwables;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.AggregateStorageRecord;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.spine3.util.Identifiers.idToString;

/**
 * Max available message size is about 2GB.
 */
class FileSystemAggregateStorage<I> extends AggregateStorage<I> {

    private static final String INVALID_OBJECT_EXCEPTION = "Could not deserialize record";
    private static final String INVALID_AGGREGATE_ID = "Aggregate Id can not be null";

    private final String shortTypeName;

    FileSystemAggregateStorage(String shortTypeName) {
        this.shortTypeName = shortTypeName;
    }

    @Override
    protected void write(AggregateStorageRecord r) {

        if (r.getAggregateId() == null) {
            Throwables.propagate(new NullPointerException(INVALID_AGGREGATE_ID));
        }

        final String aggregateFilePath = Helper.getAggregateFilePath(shortTypeName, r.getAggregateId());
        final File aggregateFile = new File(aggregateFilePath);

        if (!aggregateFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            aggregateFile.getParentFile().mkdirs();
            try {
                //noinspection ResultOfMethodCallIgnored
                aggregateFile.createNewFile();
            } catch (IOException e) {
                Throwables.propagate(e);
            }
        }

        writeToFile(aggregateFilePath, r);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {
        final String stringId = idToString(id);
        final Iterator<AggregateStorageRecord> iterator = new FileIterator(new File(
                Helper.getAggregateFilePath(shortTypeName, stringId)));
        return iterator;
    }

    @Override
    protected void releaseResources() {
        //NOP
        //reading mechanism closes streams as soon as the page is read.
    }

    private static void writeToFile(String aggregateFilePath, AggregateStorageRecord r) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(aggregateFilePath, true);
        } catch (FileNotFoundException e) {
            Throwables.propagate(e);
        }
        DataOutputStream dos = new DataOutputStream(fos);

        try {
            writeRecord(dos, r);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        try {
            dos.flush();
        } catch (IOException e) {
            Throwables.propagate(e);
        }

        Helper.closeSilently(dos);
        Helper.closeSilently(fos);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private static void writeRecord(DataOutputStream stream, AggregateStorageRecord r) throws IOException {
        byte[] bytes = r.toByteArray();
        stream.write(bytes);
        stream.writeInt(bytes.length);
    }

    private static class FileIterator implements Iterator<AggregateStorageRecord> {

        //Note: each of these objects instantly allocates 100K memory.
        public static final int DEFAULT_PAGE_SIZE = 102400;
        public static final int INT_SIZE_IN_BYTES = 4;

        private final File file;
        private final long fileLength;

        private int pageSize = DEFAULT_PAGE_SIZE;
        private byte[] page = new byte[DEFAULT_PAGE_SIZE];

        private long fileOffset; //the whole file offset
        private int pageOffset; // page offset

        private FileIterator(File file) {
            this.file = file;
            fileLength = file.length();
            fileOffset = fileLength;
            pageOffset = 0;
        }

        @Override
        public boolean hasNext() {
            return pageOffset + fileOffset > INT_SIZE_IN_BYTES;
        }

        @Override
        public AggregateStorageRecord next() {
            AggregateStorageRecord record = null;

            try {
                record = readEntry();
            } catch (IOException e) {
                Throwables.propagate(e);
            }
            if (record == null) {
                //noinspection NewExceptionWithoutArguments
                throw new NoSuchElementException();
            }
            return record;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        private void allocatePage() throws IOException {
            allocatePage(pageSize);
        }

        private void allocatePage(int minimumPageSize) throws IOException {
            if (minimumPageSize > pageSize) {
                pageSize = minimumPageSize;
                page = new byte[pageSize];
            }

            if (fileOffset > pageSize + pageOffset) {
                fileOffset -= pageSize - pageOffset;
                pageOffset = pageSize;
            } else {
                //noinspection NumericCastThatLosesPrecision
                pageOffset += (int) fileOffset;
                fileOffset = 0;
            }

            final FileInputStream fis = new FileInputStream(file.getAbsolutePath());
            final BufferedInputStream bis = new BufferedInputStream(fis);
            //noinspection ResultOfMethodCallIgnored
            bis.skip(fileOffset);

            //noinspection ResultOfMethodCallIgnored
            bis.read(page, 0, page.length);

            bis.close();
            fis.close();
        }

        private AggregateStorageRecord readEntry() throws IOException {
            if (pageOffset < INT_SIZE_IN_BYTES) {
                allocatePage();
                if (pageOffset == 0) {
                    return null;
                }
                if (pageOffset < INT_SIZE_IN_BYTES) {
                    throw new InvalidObjectException(INVALID_OBJECT_EXCEPTION);
                }
            }

            final ByteBuffer longBuffer = ByteBuffer.allocate(INT_SIZE_IN_BYTES);

            pageOffset -= INT_SIZE_IN_BYTES;
            longBuffer.put(page, pageOffset, INT_SIZE_IN_BYTES);

            longBuffer.flip();
            int messageSize = longBuffer.getInt();

            if (pageOffset < messageSize) {
                allocatePage(messageSize + INT_SIZE_IN_BYTES);
                if (pageOffset < messageSize) {
                    throw new InvalidObjectException(INVALID_OBJECT_EXCEPTION);
                }
            }

            //noinspection NumericCastThatLosesPrecision
            pageOffset -= messageSize;
            //noinspection NumericCastThatLosesPrecision
            final ByteBuffer messageBuffer = ByteBuffer.allocate(messageSize);
            messageBuffer.put(page, pageOffset, messageSize);

            final AggregateStorageRecord record =
                    AggregateStorageRecord.parseFrom(messageBuffer.array());
            return record;
        }
    }
}
