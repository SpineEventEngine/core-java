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

package org.spine3.server.storage;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.protobuf.Timestamp;
import org.spine3.SPI;
import org.spine3.base.Event;
import org.spine3.base.EventId;
import org.spine3.server.event.EventFilter;
import org.spine3.server.event.EventStore;
import org.spine3.server.event.EventStreamQuery;
import org.spine3.server.event.Events;

import javax.annotation.Nullable;
import java.util.Iterator;

import static org.spine3.server.storage.StorageUtil.toEvent;
import static org.spine3.server.storage.StorageUtil.toEventStorageRecord;

/**
 * A storage used by {@link EventStore} for keeping event data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class EventStorage extends AbstractStorage<EventId, Event> {

    @Override
    public void write(EventId id, Event record) {
        checkNotClosed();

        final EventStorageRecord storeRecord = toEventStorageRecord(record);
        writeInternal(storeRecord);
    }

    @Nullable
    @Override
    public Event read(EventId id) {
        checkNotClosed();

        final EventStorageRecord storeRecord = readInternal(id);
        if (storeRecord == null) {
            return null;
        }
        final Event result = toEvent(storeRecord);
        return result;
    }

    /**
     * Returns iterator through events matching the passed query.
     *
     * @param query a filtering query
     * @return iterator instance
     */
    public abstract Iterator<Event> iterator(EventStreamQuery query);

    /**
     * Writes record into the storage.
     *
     * @param record the record to write
     */
    protected abstract void writeInternal(EventStorageRecord record);

    /**
     * Reads storage format record.
     * @param eventId the ID of the event to read
     * @return the record instance of null if there's not record with such ID
     */
    @Nullable
    protected abstract EventStorageRecord readInternal(EventId eventId);

    /**
     * The predicate for filtering {@code Event} instances by
     * {@link EventStreamQuery}.
     */
    public static class MatchesStreamQuery implements Predicate<Event> {

        private final EventStreamQuery query;
        private final Predicate<Event> timePredicate;

        @SuppressWarnings({"MethodWithMoreThanThreeNegations", "IfMayBeConditional"})
        public MatchesStreamQuery(EventStreamQuery query) {
            this.query = query;

            final Timestamp after = query.getAfter();
            final Timestamp before = query.getBefore();

            final boolean afterSpecified = query.hasAfter();
            final boolean beforeSpecified = query.hasBefore();

            //noinspection IfStatementWithTooManyBranches
            if (afterSpecified && !beforeSpecified) {
                this.timePredicate = new Events.IsAfter(after);
            } else if (!afterSpecified && beforeSpecified) {
                this.timePredicate = new Events.IsBefore(before);
            } else if (afterSpecified /* && beforeSpecified is true here too */){
                this.timePredicate = new Events.IsBetween(after, before);
            } else { // No timestamps specified.
                this.timePredicate = Predicates.alwaysTrue();
            }
        }

        @Override
        public boolean apply(@Nullable Event input) {
            if (!timePredicate.apply(input)) {
                return false;
            }

            for (EventFilter filter : query.getFilterList()) {
                final Predicate<Event> filterPredicate = new Events.MatchesFilter(filter);
                if (!filterPredicate.apply(input)) {
                    return false;
                }
            }
            return true;
        }
    }
}
