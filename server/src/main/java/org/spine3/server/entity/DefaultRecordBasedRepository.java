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

package org.spine3.server.entity;

import com.google.protobuf.Message;
import org.spine3.server.BoundedContext;

/**
 * Implementation of {@link RecordBasedRepository} that manges entities
 * derived from {@link AbstractEntity}.
 *
 * @param <I> the type of IDs of entities
 * @param <E> the type of entities
 * @param <S> the type of entity state messages
 * @author Alexander Yevsyukov
 */
public abstract class DefaultRecordBasedRepository<I,
                                                   E extends AbstractEntity<I, S>,
                                                   S extends Message>
                extends RecordBasedRepository<I, E, S> {

    private final EntityFactory<I, E> entityFactory;
    private final EntityStorageConverter<I, E, S> storageConverter;

    /**
     * {@inheritDoc}
     *
     * @param boundedContext
     */
    @SuppressWarnings("ThisEscapedInObjectConstruction") // OK as we only pass the reference.
    protected DefaultRecordBasedRepository(BoundedContext boundedContext) {
        super(boundedContext);
        this.entityFactory = new DefaultEntityFactory<>(this);
        this.storageConverter = DefaultEntityStorageConverter.forAllFields(this);
    }

    @Override
    protected EntityFactory<I, E> entityFactory() {
        return this.entityFactory;
    }

    @Override
    protected EntityStorageConverter<I, E, S> entityConverter() {
        return this.storageConverter;
    }
}
