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

import io.spine.annotation.SPI;
import io.spine.query.RecordQuery;
import io.spine.server.storage.MessageRecordSpec;
import io.spine.server.storage.MessageStorage;
import io.spine.server.storage.StorageFactory;
import io.spine.type.TypeUrl;

import java.util.Iterator;

import static io.spine.server.delivery.CatchUpColumn.projection_type;

/**
 * A storage for the state of the ongoing catch-up processes.
 */
@SPI
public class CatchUpStorage extends MessageStorage<CatchUpId, CatchUp> {

    public CatchUpStorage(StorageFactory factory, boolean multitenant) {
        super(factory.createRecordStorage(getSpec(), multitenant));
    }

    @SuppressWarnings("ConstantConditions")     // Protobuf getters do not return {@code null}.
    private static MessageRecordSpec<CatchUpId, CatchUp> getSpec() {
        return new MessageRecordSpec<>(CatchUp.class, CatchUp::getId, CatchUpColumn.definitions());
    }

    /**
     * Reads all the catch-up processes which update the projection of the specified type.
     *
     * @param projectionType
     *         the type of the projection state to use for filtering
     */
    public Iterator<CatchUp> readByType(TypeUrl projectionType) {
        RecordQuery<CatchUpId, CatchUp> query =
                queryBuilder().where(projection_type)
                              .is(projectionType.value())
                              .build();
        Iterator<CatchUp> result = readAll(query);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open as a part of the public API.
     */
    @Override
    protected Iterator<CatchUp> readAll() {
        return super.readAll();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to open as a part of the public API.
     */
    @Override
    public void write(CatchUp message) {
        super.write(message);
    }
}
