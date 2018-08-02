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

import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.CommandId;
import io.spine.core.EventContext;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.Enrichments.getEnrichment;

/**
 * Information about a scheduled command.
 *
 * @author Dmytro Dashenkov
 */
public final class ScheduledCommand
        extends Projection<CommandId, ScheduledCommandRecord, ScheduledCommandRecordVBuilder> {

    private ScheduledCommand(CommandId id) {
        super(id);
    }

    @Subscribe
    public void on(CommandScheduled event, EventContext context) {
        Optional<CommandEnrichment> command = getEnrichment(CommandEnrichment.class, context);
        checkState(command.isPresent(), "Command enrichment must be present.");
        Command commandWithSchedule = withSchedule(command.get().getCommand(),
                                                   event.getSchedule());
        getBuilder().setId(event.getId())
                    .setCommand(commandWithSchedule)
                    .setSchedulingTime(event.getWhen());
    }

    private static Command withSchedule(Command source, CommandContext.Schedule schedule) {
        CommandContext updatedContext = source.getContext()
                                              .toBuilder()
                                              .setSchedule(schedule)
                                              .build();
        Command updatedCommand = source.toBuilder()
                                       .setContext(updatedContext)
                                       .build();
        return updatedCommand;
    }

    @Subscribe
    public void on(@SuppressWarnings("unused") // Defines the event type.
                   CommandDispatched ignored) {
        setDeleted(true);
    }
}
