/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.type.TypeUrl;

/**
 * Implementation of {@link RecordBasedRepository} that manages entities
 * derived from {@link AbstractEntity}.
 *
 * @author Alexander Yevsyukov
 */
public abstract class DefaultRecordBasedRepository<I,
                                                   E extends AbstractEntity<I, S>,
                                                   S extends Message>
                extends RecordBasedRepository<I, E, S> {

    private final EntityFactory<I, E> entityFactory;
    private final EntityStorageConverter<I, E, S> storageConverter;

    /**
     * Creates a new instance with the {@linkplain #entityFactory() factory} of entities of class
     * specified as the {@code <E>} generic parameter, and with the default
     * {@linkplain #entityConverter() entity storage converter}.
     */
    protected DefaultRecordBasedRepository() {
        super();
        @SuppressWarnings("OverridableMethodCallDuringObjectConstruction") // get generic param
        Class<E> entityClass = entityClass();
        this.entityFactory = new DefaultEntityFactory<>(entityClass);
        TypeUrl stateType = entityModelClass().stateType();
        this.storageConverter = DefaultEntityStorageConverter.forAllFields(stateType,
                                                                           this.entityFactory);
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
