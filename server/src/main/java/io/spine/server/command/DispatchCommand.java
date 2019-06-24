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

package io.spine.server.command;

import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.server.entity.EntityLifecycle;
import io.spine.server.type.CommandEnvelope;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A command dispatch operation.
 *
 * <p>Dispatches the given {@linkplain CommandEnvelope command} to the given
 * {@linkplain CommandHandlingEntity entity} and triggers the {@link EntityLifecycle}.
 *
 * @param <I>
 *         the type of entity ID
 */
@Internal
public final class DispatchCommand<I> {

    private final EntityLifecycle lifecycle;
    private final CommandHandlingEntity<I, ?, ?> entity;
    private final CommandEnvelope command;

    private DispatchCommand(EntityLifecycle lifecycle,
                            CommandHandlingEntity<I, ?, ?> entity,
                            CommandEnvelope command) {
        this.lifecycle = lifecycle;
        this.entity = entity;
        this.command = command;
    }

    public static <I> DispatchCommand<I> operationFor(EntityLifecycle lifecycle,
                                                      CommandHandlingEntity<I, ?, ?> entity,
                                                      CommandEnvelope command) {
        checkNotNull(lifecycle);
        checkNotNull(entity);
        checkNotNull(command);

        return new DispatchCommand<>(lifecycle, entity, command);
    }

    /**
     * Performs the operation.
     *
     * <p>First, the command is {@linkplain CommandHandlingEntity#dispatchCommand(CommandEnvelope)
     * passed} to the entity.
     *
     * <p>Then, depending on the command handling result, either
     * {@link EntityLifecycle#onCommandHandled EntityLifecycle.onCommandHandled(...)} or
     * {@link EntityLifecycle#onCommandRejected EntityLifecycle.onCommandRejected(...)} callback
     * is triggered.
     *
     * @return the produced events including the rejections thrown by the command handler
     */
    public List<Event> perform() {
        List<Event> result = entity.dispatchCommand(command);
        onCommandResult(command.command(), result);
        return result;
    }

    public CommandHandlingEntity<I, ?, ?> entity() {
        return entity;
    }

    public CommandEnvelope command() {
        return command;
    }

    private void onCommandResult(Command command, List<Event> produced) {
        Optional<Event> rejectionEvent = rejection(produced);
        if (rejectionEvent.isPresent()) {
            lifecycle.onCommandRejected(command.getId(), rejectionEvent.get());
        } else {
            lifecycle.onCommandHandled(command);
        }
    }

    private static Optional<Event> rejection(List<Event> produced) {
        if (produced.size() != 1) {
            return Optional.empty();
        }
        Event singleEvent = produced.get(0);
        Optional<Event> result = singleEvent.isRejection()
                                 ? Optional.of(singleEvent)
                                 : Optional.empty();
        return result;
    }
}
