/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Timestamp;
import io.spine.annotation.SPI;
import io.spine.client.OrderBy;
import io.spine.client.ResponseFormat;
import io.spine.server.storage.QueryParameters;
import io.spine.server.storage.RecordColumns;
import io.spine.server.storage.RecordQueries;
import io.spine.server.storage.RecordQuery;
import io.spine.server.storage.RecordStorage;
import io.spine.server.storage.RecordStorageDelegate;
import io.spine.server.storage.RecordWithColumns;
import io.spine.server.storage.StorageFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Streams.stream;
import static io.spine.client.OrderBy.Direction.ASCENDING;
import static io.spine.server.delivery.InboxMessageStatus.TO_DELIVER;
import static io.spine.server.storage.QueryParameters.eq;
import static io.spine.server.storage.QueryParameters.gt;
import static java.util.stream.Collectors.toList;

/**
 * A contract for storages of {@link Inbox} messages.
 *
 * <p>The records a storage of this type are spreads across shards identified by a
 * {@linkplain ShardIndex shard index}.
 *
 * <p>Typically, the storage instance is specific to the
 * {@linkplain io.spine.server.ServerEnvironment server environment} and is used across
 * {@code BoundedContext}s to store the delivered messages.
 */
@SPI
public class InboxStorage extends RecordStorageDelegate<InboxMessageId, InboxMessage> {

    public InboxStorage(StorageFactory factory, boolean multitenant) {
        super(createStorage(factory, multitenant));
    }

    private static RecordStorage<InboxMessageId, InboxMessage>
    createStorage(StorageFactory factory, boolean multitenant) {
        RecordColumns<InboxMessage> columns =
                new RecordColumns<>(InboxMessage.class, InboxColumn.definitions());
        return factory.createRecordStorage(columns, multitenant);
    }

    /**
     * Reads the contents of the storage by the given shard index and returns the first page
     * of the results.
     *
     * <p>The older items go first.
     *
     * @param index
     *         the shard index to return the results for
     * @param pageSize
     *         the maximum number of the elements per page
     * @return the first page of the results
     */
    public Page<InboxMessage> readAll(ShardIndex index, int pageSize) {
        Page<InboxMessage> page = new InboxPage(sinceWhen -> readAll(index, sinceWhen, pageSize));
        return page;
    }

    public ImmutableList<InboxMessage>
    readAll(ShardIndex index, @Nullable Timestamp sinceWhen, int pageSize) {
        QueryParameters byIndex = eq(InboxColumn.shardIndex.column(), index);
        RecordQuery<InboxMessageId> query = RecordQueries.of(byIndex);
        if (sinceWhen != null) {
            QueryParameters byTime = gt(InboxColumn.receivedAt.column(), sinceWhen);
            query = query.append(byTime);
        }
        ResponseFormat limit = queryResponse(pageSize);
        Iterator<InboxMessage> iterator = readAll(query, limit);
        return ImmutableList.copyOf(iterator);
    }

    private static ResponseFormat queryResponse(int pageSize) {
        OrderBy olderFirst = OrderBy.newBuilder()
                                    .setColumn(InboxColumn.receivedAt.column()
                                                                     .name()
                                                                     .value())
                                    .setDirection(ASCENDING)
                                    .vBuild();
        OrderBy byVersion = OrderBy.newBuilder()
                                   .setColumn(InboxColumn.version.column()
                                                                 .name()
                                                                 .value())
                                   .setDirection(ASCENDING)
                                   .vBuild();

        return ResponseFormat.newBuilder()
                             .addOrderBy(olderFirst)
                             .addOrderBy(byVersion)
                             .setLimit(pageSize)
                             .vBuild();
    }

    /**
     * Finds the newest message {@linkplain InboxMessageStatus#TO_DELIVER to deliver}
     * in the given shard.
     *
     * @param index
     *         the shard index to look in
     * @return the message found or {@code Optional.empty()} if there are no messages to deliver
     *         in the specified shard
     */
    public Optional<InboxMessage> newestMessageToDeliver(ShardIndex index) {
        QueryParameters byIndex = eq(InboxColumn.shardIndex.column(), index);
        RecordQuery<InboxMessageId> query = RecordQueries.of(byIndex);
        query = query.append(eq(InboxColumn.status.column(), TO_DELIVER));
        ResponseFormat limitToOne = ResponseFormat.newBuilder()
                                                  .setLimit(1)
                                                  .vBuild();
        Iterator<InboxMessage> iterator = readAll(query, limitToOne);
        Optional<InboxMessage> result = iterator.hasNext() ? Optional.of(iterator.next())
                                                           : Optional.empty();
        return result;
    }

    public synchronized void write(InboxMessage message) {
        write(message.getId(), message);
    }

    /**
     * Writes several messages to the storage.
     *
     * @param messages
     *         messages to write
     */
    public synchronized void writeBatch(Iterable<InboxMessage> messages) {
        List<RecordWithColumns<InboxMessageId, InboxMessage>> toStore =
                stream(messages)
                        .map(m -> RecordWithColumns.create(m.getId(), m, columns()))
                        .collect(toList());
        writeAll(toStore);
    }

    /**
     * Removes the passed messages from the storage.
     *
     * <p>Does nothing for messages that aren't in the storage already.
     *
     * @param messages
     *         the messages to remove
     */
    synchronized void removeBatch(Iterable<InboxMessage> messages) {
        List<InboxMessageId> toRemove = stream(messages).map(InboxMessage::getId)
                                                        .collect(toList());
        deleteAll(toRemove);
    }
}
