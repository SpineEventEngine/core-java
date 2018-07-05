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

package io.spine.server;

import com.google.protobuf.Message;
import io.spine.server.command.CommandHandlingEntity;

import java.util.List;

/**
 * The implementation base for the test suites of message handling methods which may post commands,
 * produce events and rejections.
 *
 * <p>A derived class tests a single message handler (such as command handler or event/rejection
 * reactor).
 *
 * @param <M> the type of the handled message
 * @param <I> the ID type of the entity, which handles the message
 * @param <S> the state type of the entity, which handles the message
 * @param <E> the entity type
 * @author Dmytro Dashenkov
 */
public abstract class MessageProducingMessageHandlerTest<M extends Message,
                                                         I,
                                                         S extends Message,
                                                         E extends CommandHandlingEntity<I, S, ?>>
        extends MessageHandlerTest<M, I, S, E, MessageProducingExpected<S>> {

    @Override
    protected MessageProducingExpected<S> expectThat(E entity) {
        final S initialState = entity.getState();
        final List<? extends Message> events = dispatchTo(entity);
        return new MessageProducingExpected<>(events, initialState, entity.getState(),
                                              interceptedCommands());
    }
}
