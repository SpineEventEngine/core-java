/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.stand;

import com.google.common.collect.ImmutableCollection;
import com.google.protobuf.FieldMask;
import org.spine3.SPI;
import org.spine3.protobuf.TypeUrl;
import org.spine3.server.entity.EntityRecord;
import org.spine3.server.storage.RecordStorage;

/**
 * Serves as a storage for the latest {@link org.spine3.server.aggregate.Aggregate Aggregate} states.
 *
 * <p>Used by an instance of {@link Stand} to optimize the {@code Aggregate} state fetch performance.
 *
 * @author Alex Tymchenko
 * @see com.google.protobuf.Any#getTypeUrl() Any.getTypeUrl()
 * @see Stand
 */
@SPI
public abstract class StandStorage extends RecordStorage<AggregateStateId> {

    protected StandStorage(boolean multitenant) {
        super(multitenant);
    }

    /**
     * Reads all the state records by the given type.
     *
     * @param type a {@link TypeUrl} instance
     * @return the state records which {@link com.google.protobuf.Any#getTypeUrl() Any.getTypeUrl()}
     *          equals the argument value
     */
    public abstract ImmutableCollection<EntityRecord> readAllByType(TypeUrl type);

    /**
     * Reads all the state records by the given type.
     *
     * @param type a {@link TypeUrl} instance
     * @return the state records which {@link com.google.protobuf.Any#getTypeUrl() Any.getTypeUrl()}
     *          equals the argument value
     */
    public abstract ImmutableCollection<EntityRecord> readAllByType(TypeUrl type, FieldMask fieldMask);
}
