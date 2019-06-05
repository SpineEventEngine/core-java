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

package io.spine.system.server;

import io.spine.client.EntityId;
import io.spine.core.EntityQualifier;
import io.spine.server.route.EventRouting;

import static io.spine.server.route.EventRoute.withId;

/**
 * The repository for {@link EntityHistoryAggregate}s.
 */
final class EntityHistoryRepository
        extends SystemRepository<EntityHistoryId, EntityHistoryAggregate> {

    @Override
    protected void setupImportRouting(EventRouting<EntityHistoryId> routing) {
        super.setupImportRouting(routing);
        routing.route(ConstraintViolated.class,
                      (message, context) -> withId(asHistoryId(message.getEntity())));
    }

    private static EntityHistoryId asHistoryId(EntityQualifier qualifier) {
        return EntityHistoryId
                .newBuilder()
                .setEntityId(EntityId.newBuilder().setId(qualifier.getId()))
                .setTypeUrl(qualifier.getTypeUrl())
                .vBuild();
    }
}
