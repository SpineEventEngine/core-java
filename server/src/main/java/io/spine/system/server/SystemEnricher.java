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

import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.event.EventEnricher;

import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A factory of {@link EventEnricher} instances for the system bounded context.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class SystemEnricher {

    /**
     * Prevents the utility class instantiation.
     */
    private SystemEnricher() {
    }

    /**
     * Creates a new {@link EventEnricher} for the system bounded context.
     *
     * @param commandRepository repository to find enrichment values in
     * @return new {@link EventEnricher}
     */
    public static EventEnricher create(CommandLifecycleRepository commandRepository) {
        checkNotNull(commandRepository);
        EventEnricher enricher = EventEnricher.newBuilder()
                                              .add(CommandId.class, Command.class,
                                                   commandLookup(commandRepository))
                                              .build();
        return enricher;
    }

    private static Function<CommandId, Command>
    commandLookup(CommandLifecycleRepository repository) {
        return commandId -> findCommand(repository, commandId);
    }

    private static Command findCommand(CommandLifecycleRepository repository, CommandId id) {
        Optional<CommandLifecycleAggregate> commandLifecycle = repository.find(id);
        Command command = commandLifecycle.map(Aggregate::getState)
                                          .map(CommandLifecycle::getCommand)
                                          .orElse(Command.getDefaultInstance());
        return command;
    }
}
