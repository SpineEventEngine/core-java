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

package io.spine.server.command;

import io.spine.annotation.Internal;
import io.spine.core.Command;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.entity.EntityLifecycle;

import java.util.List;
import java.util.Optional;

import static io.spine.core.Events.isRejection;

/**
 * A command dispatch operation.
 *
 * <p>Dispatches the given {@linkplain CommandEnvelope command} to the given
 * {@linkplain CommandHandlingEntity entity} and triggers the {@link EntityLifecycle}.
 *
 * @author Dmytro Dashenkov
 */
@Internal
public final class DispatchCommand {

    private final EntityLifecycle lifecycle;
    private final CommandHandlingEntity<?, ?, ?> entity;
    private final CommandEnvelope command;

    private DispatchCommand(Builder builder) {
        this.lifecycle = builder.lifecycle;
        this.entity = builder.entity;
        this.command = builder.command;
    }

    /**
     * Performs the operation.
     *
     * <p>First, the {@link EntityLifecycle#onDispatchCommand(Command)} callback is triggered.
     * Then, the command is {@linkplain CommandHandlingEntity#dispatchCommand(CommandEnvelope)
     * passed} to the entity. Lastly, depending on the command handling result, either
     * {@link EntityLifecycle#onCommandHandled} or {@link EntityLifecycle#onCommandRejected}
     * callback is triggered.
     *
     * @return the produced events, may be a single rejection event
     */
    public List<Event> perform() {
        Command cmd = command.getCommand();
        lifecycle.onDispatchCommand(cmd);
        List<Event> result = entity.dispatchCommand(command);
        onCommandResult(cmd, result);
        return result;
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
        Optional<Event> result = isRejection(singleEvent)
                                 ? Optional.of(singleEvent)
                                 : Optional.empty();
        return result;
    }

    /**
     * Creates a new instance of {@code Builder} for {@code DispatchCommand} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder for the {@code DispatchCommand} instances.
     */
    public static final class Builder {

        private EntityLifecycle lifecycle;
        private CommandHandlingEntity entity;
        private CommandEnvelope command;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        public Builder setLifecycle(EntityLifecycle lifecycle) {
            this.lifecycle = lifecycle;
            return this;
        }

        public Builder setEntity(CommandHandlingEntity entity) {
            this.entity = entity;
            return this;
        }

        public Builder setCommand(CommandEnvelope command) {
            this.command = command;
            return this;
        }

        /**
         * Creates a new instance of {@code DispatchCommand}.
         *
         * @return new instance of {@code DispatchCommand}
         */
        public DispatchCommand build() {
            return new DispatchCommand(this);
        }
    }
}
