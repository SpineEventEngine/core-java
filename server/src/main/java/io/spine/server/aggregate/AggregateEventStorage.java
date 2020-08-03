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

package io.spine.server.aggregate;

import io.spine.query.RecordQuery;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.StorageFactory;

import java.util.Iterator;

/**
 * Storage of events for each {@link Aggregate}.
 */
public class AggregateEventStorage
        extends MessageStorage<AggregateEventRecordId, AggregateEventRecord> {

    @SuppressWarnings("ConstantConditions")     // Protobuf getters return non-{@code null} values.
    private static final MessageRecordSpec<AggregateEventRecordId, AggregateEventRecord> spec =
            new MessageRecordSpec<>(
                    AggregateEventRecordId.class,
                    AggregateEventRecord.class,
                    AggregateEventRecord::getId,
                    AggregateEventRecordColumn.definitions()
            );

    public AggregateEventStorage(StorageFactory factory, boolean multitenant) {
        super(factory.createRecordStorage(spec, multitenant));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose this method as a part of storage's package-level API.
     */
    @Override
    protected Iterator<AggregateEventRecord>
    readAll(RecordQuery<AggregateEventRecordId, AggregateEventRecord> query) {
        return super.readAll(query);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose this method as a part of storage's package-level API.
     */
    @Override
    protected boolean delete(AggregateEventRecordId id) {
        return super.delete(id);
    }
}
