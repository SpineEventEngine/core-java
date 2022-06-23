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

package io.spine.system.server;

import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.server.enrich.Enricher;
import io.spine.server.enrich.EventEnrichmentFn;
import io.spine.server.event.EventEnricher;
import io.spine.server.projection.Projection;
import io.spine.system.server.event.CommandScheduled;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory of {@link Enricher} instances for the system bounded context.
 */
@Internal
final class SystemEnricher {

    /**
     * Prevents the utility class instantiation.
     */
    private SystemEnricher() {
    }

    /**
     * Creates a new {@link Enricher} for the system bounded context.
     *
     * @param repo the repository for obtaining scheduled command instances
     * @return new {@link Enricher}
     */
    public static EventEnricher create(CommandLogRepository repo) {
        checkNotNull(repo);
        EventEnricher enricher = EventEnricher
                .newBuilder()
                .add(CommandScheduled.class, Command.class, commandLookup(repo))
                .build();
        return enricher;
    }

    private static EventEnrichmentFn<CommandScheduled, Command>
    commandLookup(CommandLogRepository repo) {
        return (commandScheduled, context) -> findCommand(repo, commandScheduled.getId());
    }

    private static Command findCommand(CommandLogRepository repo, CommandId id) {
        Optional<CommandLogProjection> commandLifecycle = repo.find(id);
        Command command = commandLifecycle.map(Projection::state)
                                          .map(CommandLog::getCommand)
                                          .orElse(Command.getDefaultInstance());
        return command;
    }
}
