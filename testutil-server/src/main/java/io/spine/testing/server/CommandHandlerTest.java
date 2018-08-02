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

package io.spine.testing.server;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.core.Command;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.server.expected.CommandHandlerExpected;

import java.util.List;
import java.util.Optional;

import static io.spine.core.Events.isRejection;
import static java.util.Collections.emptyList;

/**
 * The implementation base for testing a single command handler.
 *
 * <p>It is expected that a test suite derived from this class ensures that:
 * <ol>
 * <li>correct events are emitted by the command handler;
 * <li>correct rejections (if applicable) are generated;
 * <li>the state of an entity is correctly changed after the events are emitted.
 * </ol>
 *
 * @param <I> ID message of the command and the handling entity
 * @param <C> the type of the command message to test
 * @param <S> state message of the handling entity
 * @param <E> the type of the {@link CommandHandlingEntity} being tested
 * @author Vladyslav Lubenskyi
 */
@CheckReturnValue
public abstract class CommandHandlerTest<I,
                                         C extends Message,
                                         S extends Message,
                                         E extends CommandHandlingEntity<I, S, ?>>
        extends MessageHandlerTest<I, C, S, E, CommandHandlerExpected<S>> {

    private final ActorRequestFactory requestFactory;

    /**
     * Creates a new instance with {@link TestActorRequestFactory}.
     */
    @SuppressWarnings("TestOnlyProblems") // OK for a test-util.
    protected CommandHandlerTest() {
        super();
        this.requestFactory = TestActorRequestFactory.newInstance(getClass());
    }

    /**
     * Creates a {@link Command} instance from the command message.
     *
     * @param commandMessage command message
     * @return {@link Command} ready to be dispatched
     */
    protected final Command createCommand(C commandMessage) {
        Command command = requestFactory.command()
                                        .create(commandMessage);
        return command;
    }

    @Override
    protected CommandHandlerExpected<S> expectThat(E entity) {
        S initialState = entity.getState();
        Message rejection = null;
        List<? extends Message> events = dispatchTo(entity);
        Optional<Message> rejectionMessage = rejectionFrom(events);
        if (rejectionMessage.isPresent()) {
            rejection = rejectionMessage.get();
            events = emptyList();
        }
        return new CommandHandlerExpected<>(events, rejection, initialState,
                                            entity.getState(), interceptedCommands());
    }

    private static Optional<Message> rejectionFrom(List<? extends Message> events) {
        if (events.size() != 1) {
            return Optional.empty();
        }
        Message singleEvent = events.get(0);
        if (isRejection(singleEvent.getClass())) {
            return Optional.of(singleEvent);
        } else {
            return Optional.empty();
        }
    }
}
