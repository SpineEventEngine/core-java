/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.client;

import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.validate.ConstraintViolation;
import org.spine3.validate.ConstraintViolationThrowable;
import org.spine3.validate.MessageValidator;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Public API for creating {@link Command} instances, using the {@code ActorRequestFactory}
 * configuration.
 *
 * @see ActorRequestFactory#command()
 */
public final class CommandFactory {

    private final ActorRequestFactory actorRequestFactory;

    CommandFactory(ActorRequestFactory actorRequestFactory) {
        this.actorRequestFactory = checkNotNull(actorRequestFactory);
    }

    /**
     * Creates new {@code Command} with the passed message.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * <p>The message passed is validated according to the constraints set in its Protobuf
     * definition. In case the message isn't valid, an {@linkplain ConstraintViolationThrowable
     * exception} is thrown.
     *
     * <p>Therefore it is recommended to use an appropriate
     * {@linkplain org.spine3.validate.ValidatingBuilder validating Builder} implementation
     * to create the message.
     *
     * @param message the command message
     * @return new command instance
     * @throws ConstraintViolationThrowable if the passed message does not satisfy the constraints
     *                                      set for it in its Protobuf definition
     */
    public Command create(Message message) throws ConstraintViolationThrowable {
        checkNotNull(message);

        validate(message);

        final CommandContext context = createContext();
        final Command result = Commands.createCommand(message, context);
        return result;
    }

    /**
     * Creates command context for a new command.
     */
    CommandContext createContext() {
        return Commands.createContext(actorRequestFactory.getTenantId(),
                                      actorRequestFactory.getActor(),
                                      actorRequestFactory.getZoneOffset());
    }

    /**
     * Creates new {@code Command} with the passed message and target entity version.
     *
     * <p>The command contains a {@code CommandContext} instance with the current time.
     *
     * <p>The message passed is validated according to the constraints set in its Protobuf
     * definition. In case the message isn't valid, an {@linkplain ConstraintViolationThrowable
     * exception} is thrown.
     *
     * <p>Therefore it is recommended to use an appropriate
     * {@linkplain org.spine3.validate.ValidatingBuilder validating Builder} implementation
     * to create the message.
     *
     * @param message       the command message
     * @param targetVersion the ID of the entity for applying commands if {@code null}
     *                      the commands can be applied to any entity
     * @return new command instance
     * @throws ConstraintViolationThrowable if the passed message does not satisfy the constraints
     *                                      set for it in its Protobuf definition
     */
    public Command create(Message message, int targetVersion) throws ConstraintViolationThrowable {
        checkNotNull(message);
        checkNotNull(targetVersion);
        validate(message);

        final CommandContext context = createContext(targetVersion);
        final Command result = Commands.createCommand(message, context);
        return result;
    }

    /**
     * Creates command context for a new command with entity ID.
     */
    private CommandContext createContext(int targetVersion) {
        return Commands.createContext(actorRequestFactory.getTenantId(),
                                      actorRequestFactory.getActor(),
                                      actorRequestFactory.getZoneOffset(), targetVersion);
    }

    /**
     * Validates the given message according to its definition and throws
     * {@code ConstraintViolationThrowable} if any constraints are violated.
     */
    private static void validate(Message message) {
        final List<ConstraintViolation> violations = MessageValidator.newInstance()
                                                                     .validate(message);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationThrowable(violations);
        }
    }
}
