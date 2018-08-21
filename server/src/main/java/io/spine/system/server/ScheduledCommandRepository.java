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

import io.spine.core.CommandId;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.route.EventRouting;

import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;

/**
 * A repository for {@link ScheduledCommand}s.
 *
 * @author Dmytro Dashenkov
 */
final class ScheduledCommandRepository
        extends ProjectionRepository<CommandId, ScheduledCommand, ScheduledCommandRecord> {

    @Override
    public void onRegistered() {
        super.onRegistered();
        EventRouting<CommandId> routing = getEventRouting();
        routing.route(CommandDispatched.class,
                      (message, context) -> routeToExisting(message));
    }

    private Set<CommandId> routeToExisting(CommandDispatched event) {
        CommandId id = event.getId();
        Optional<ScheduledCommand> existing = find(id);
        return existing.isPresent()
               ? of(id)
               : of();
    }
}
