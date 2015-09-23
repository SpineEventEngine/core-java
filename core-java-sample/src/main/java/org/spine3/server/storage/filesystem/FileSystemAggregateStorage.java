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
import com.google.protobuf.Any;
import org.spine3.server.aggregate.AggregateId;
import org.spine3.server.storage.AggregateStorage;
import org.spine3.server.storage.AggregateStorageRecord;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

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
            //TODO:2015-09-23: Check if it's correct.
            throw new NullPointerException(INVALID_AGGREGATE_ID);
        }

        final String aggregateFile = Helper.getAggregateFilePath(shortTypeName, r.getAggregateId());

        writeToFile(aggregateFile, r);
    }

    @Override
    protected Iterator<AggregateStorageRecord> historyBackward(I id) {
        return new FileIterator(new File(
                Helper.getAggregateFilePath(shortTypeName, AggregateId.of(id).toString())));
    }

    private static void writeToFile(String aggregateFilePath, AggregateStorageRecord r) {
        final Any packedAny = Any.pack(r);
        final String eventType = r.getEventType();
        final FileSystemEventRecord fileSystemRecord = FileSystemEventRecord.newBuilder()
                .setMessage(packedAny)
                .setMessageSize(packedAny.getSerializedSize())
                .setMessageType(eventType)
                .setMessageTypeSize(eventType.getBytes().length)
                .build();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(aggregateFilePath);
        } catch (FileNotFoundException e) {
            Throwables.propagate(e);
        }
        DataOutputStream dos = new DataOutputStream(fos);

        try {
            writeRecord(dos, fileSystemRecord);
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        Helper.closeSilently(dos);
        Helper.closeSilently(fos);
    }

    @SuppressWarnings("TypeMayBeWeakened")
    private static void writeRecord(DataOutputStream stream, FileSystemEventRecord r) throws IOException {
        byte[] bytes = r.toByteArray();
        stream.write(bytes);
        stream.writeLong(bytes.length);

//        stream.writeUTF(r.getMessage().getValue().toStringUtf8());
//        stream.writeLong(r.getMessageSize());
//        stream.writeUTF(r.getMessageType());
//        stream.writeLong(r.getMessageTypeSize());
    }

    private static class FileIterator implements Iterator<AggregateStorageRecord> {

        //TODO:2015-09-22:mikhail.mikhaylov: Note: each of these objects instantly allocates 100K memory.
        public static final int PAGE_SIZE = 102400;
        public static final int LONG_SIZE_IN_BYTES = 8;

        private final File file;
        private final long fileLength;

        private final byte[] page = new byte[PAGE_SIZE];

        private long fileOffset; //the whole file offset
        private final ByteBuffer longBuffer = ByteBuffer.allocate(LONG_SIZE_IN_BYTES);
        private int pageOffset; // page offset

        private FileIterator(File file) {
            this.file = file;
            fileLength = file.length();
            fileOffset = fileLength;
            pageOffset = 0;
        }

        @Override
        public boolean hasNext() {
            return pageOffset + fileOffset > LONG_SIZE_IN_BYTES;
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

        private void allocatePage() throws IOException {
            if (fileOffset > PAGE_SIZE + pageOffset) {
                pageOffset = PAGE_SIZE;
                fileOffset -= PAGE_SIZE - pageOffset;
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
        }

        private AggregateStorageRecord readEntry() throws IOException {
            if (pageOffset < LONG_SIZE_IN_BYTES) {
                allocatePage();
                if (pageOffset == 0) {
                    return null;
                }
                if (pageOffset < LONG_SIZE_IN_BYTES) {
                    //TODO:2015-09-23:mikhail.mikhaylov: Have a proper exception here.
                    throw new InvalidObjectException(INVALID_OBJECT_EXCEPTION);
                }
            }

            pageOffset -= LONG_SIZE_IN_BYTES;
            longBuffer.put(page, pageOffset - 1, LONG_SIZE_IN_BYTES);

            longBuffer.flip();
            long messageSize = longBuffer.getLong();

            if (pageOffset < messageSize) {
                allocatePage();
                if (pageOffset < messageSize) {
                    //TODO:2015-09-23:mikhail.mikhaylov: Have a proper exception here.
                    throw new InvalidObjectException(INVALID_OBJECT_EXCEPTION);
                }
            }

            //noinspection NumericCastThatLosesPrecision
            pageOffset -= (int) messageSize;
            //noinspection NumericCastThatLosesPrecision
            final ByteBuffer messageBuffer = ByteBuffer.allocate((int) messageSize);
            messageBuffer.put(page, pageOffset - 1, (int) messageSize);

            final AggregateStorageRecord record =
                    AggregateStorageRecord.parseFrom(messageBuffer.array());
            return record;
        }
    }
}
