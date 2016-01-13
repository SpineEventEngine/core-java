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
import org.spine3.base.EventRecord;
import org.spine3.base.EventRecordFilter;
import org.spine3.server.EventStreamQuery;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.Iterator;

import static org.spine3.server.storage.StorageUtil.toEventStorageRecord;
import static org.spine3.util.EventRecords.*;

/**
 * A storage used by {@link org.spine3.server.EventStore} for keeping event data.
 *
 * @author Alexander Yevsyukov
 */
public abstract class EventStorage implements Closeable {

    @SuppressWarnings("TypeMayBeWeakened")
    public void store(EventRecord record) {
        final EventStorageRecord storeRecord = toEventStorageRecord(record);
        write(storeRecord);
    }

    /**
     * Returns iterator through event records matching the passed query.
     *
     * @param query a filtering query
     * @return iterator instance
     */
    public abstract Iterator<EventRecord> iterator(EventStreamQuery query);

    /**
     * Writes record into the storage.
     *
     * @param record the record to write
     * @throws java.lang.NullPointerException if record is null
     */
    protected abstract void write(EventStorageRecord record);

    /**
     * @deprecated Use {@link #iterator(EventStreamQuery)} instead
     */
    @Deprecated
    public Iterator<EventRecord> allEvents() {
        return iterator(EventStreamQuery.getDefaultInstance());
    }

    public static class MatchesStreamQuery implements Predicate<EventRecord> {

        private final EventStreamQuery query;
        private final Predicate<EventRecord> timePredicate;

        @SuppressWarnings("MethodWithMoreThanThreeNegations")
        public MatchesStreamQuery(EventStreamQuery query) {
            this.query = query;

            final Timestamp defaultTimestamp = Timestamp.getDefaultInstance();
            final Timestamp after = query.getAfter();
            final Timestamp before = query.getBefore();

            final boolean afterSpecified = !after.equals(defaultTimestamp);
            final boolean beforeSpecified = !before.equals(defaultTimestamp);

            //noinspection IfStatementWithTooManyBranches
            if (afterSpecified && !beforeSpecified) {
                this.timePredicate = new IsAfter(after);
            } else if (!afterSpecified && beforeSpecified) {
                this.timePredicate = new IsBefore(before);
            } else if (afterSpecified /* && beforeSpecified is true here too */){
                this.timePredicate = new IsBetween(after, before);
            } else { // No timestamps specified.
                this.timePredicate = Predicates.alwaysTrue();
            }
        }

        @Override
        public boolean apply(@Nullable EventRecord input) {
            if (!timePredicate.apply(input)) {
                return false;
            }

            for (EventRecordFilter filter : query.getFilterList()) {
                final Predicate<EventRecord> filterPredicate = new MatchesFilter(filter);
                if (!filterPredicate.apply(input)) {
                    return false;
                }
            }
            return true;
        }
    }
}
