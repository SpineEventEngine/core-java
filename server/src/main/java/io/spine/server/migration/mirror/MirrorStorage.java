/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.migration.mirror;

import io.spine.query.RecordQuery;
import io.spine.server.ContextSpec;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.system.server.Mirror;
import io.spine.system.server.MirrorId;

import java.util.Iterator;
import java.util.List;

/**
 * A contract for storages of legacy {@link  Mirror} projections.
 *
 * <p>{@link Mirror} was deprecated in Spine 2.0. The storage is intended to be used
 * only in a scope of the {@link MirrorMigration migration}.
 *
 * <p>It exposes methods for performing read and write operations in batches.
 */
public final class MirrorStorage extends MessageStorage<MirrorId, Mirror> {

    /**
     * Creates a new instance.
     *
     * @param contextSpec
     *         a specification of a Bounded Context within which the created storage is used
     */
    public MirrorStorage(ContextSpec contextSpec, StorageFactory factory) {
        super(contextSpec, factory.createRecordStorage(contextSpec, messageSpec()));
    }

    @SuppressWarnings("ConstantConditions")
    private static MessageRecordSpec<MirrorId, Mirror> messageSpec() {
        var spec = new MessageRecordSpec<>(
                MirrorId.class,
                Mirror.class,
                Mirror::getId,
                List.of(
                        Mirror.Column.aggregateType(),
                        Mirror.Column.wasMigrated()
                )
        );
        return spec;
    }

    @Override
    public Iterator<Mirror> readAll(RecordQuery<MirrorId, Mirror> query) {
        return super.readAll(query);
    }

    @Override
    public void writeBatch(Iterable<Mirror> messages) {
        super.writeBatch(messages);
    }
}
