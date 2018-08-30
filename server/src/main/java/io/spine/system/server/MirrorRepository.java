/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.system.server;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import io.spine.server.projection.ProjectionRepository;

import java.util.Set;

/**
 * @author Dmytro Dashenkov
 */
public class MirrorRepository extends ProjectionRepository<MirrorId, MirrorProjection, Mirror> {

    @Override
    public void onRegistered() {
        super.onRegistered();
        prepareRouting();
    }

    private void prepareRouting() {
        getEventRouting()
                .route(EntityStateChanged.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityArchived.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityDeleted.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityExtractedFromArchive.class,
                       (message, context) -> targetsFrom(message.getId()))
                .route(EntityRestored.class,
                       (message, context) -> targetsFrom(message.getId()));
    }

    private static Set<MirrorId> targetsFrom(EntityHistoryId historyId) {
        return ImmutableSet.of(idFrom(historyId));
    }

    private static MirrorId idFrom(EntityHistoryId historyId) {
        Any any = historyId.getEntityId()
                           .getId();
        MirrorId result = MirrorId
                .newBuilder()
                .setValue(any)
                .build();
        return result;
    }
}
