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

import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.client.ActorRequestFactory;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.Rejection;
import io.spine.core.RejectionContext;
import io.spine.core.Rejections;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.command.TestEventFactory;

//@formatter:off Don't format javadoc because formatter doesn't indent <li> elements correctly.

/**
 * An abstract base class for testing a single message reactor.
 *
 * <p>It is expected that a test suite derived from this class ensures that:
 * <ol>
 *     <li>correct events are emitted by the reactor;
 *     <li>correct rejections (if applicable) are generated;
 *     <li>the state of an entity is correctly changed after the events are emitted.
 * </ol>
 *
 * @param <M> the type of the command message to test
 * @param <I> ID message of the command and the handling entity
 * @param <S> state message of the handling entity
 * @param <E> the type of the {@link CommandHandlingEntity} being tested
 *
 * @author Dmytro Dashenkov
 */
//@formatter:on
@SuppressWarnings("TestOnlyProblems")
public abstract class ReactionTest<M extends Message,
                                   I,
                                   S extends Message,
                                   E extends CommandHandlingEntity<I, S, ?>>
        extends MessageProducingMessageHandlerTest<M, I, S, E> {

    private final TestEventFactory eventFactory;
    private final ActorRequestFactory requestFactory;

    protected ReactionTest() {
        super();
        this.eventFactory = TestEventFactory.newInstance(getClass());
        this.requestFactory = TestActorRequestFactory.newInstance(getClass());
    }

    protected final Event createEvent(M message) {
        final Event event = eventFactory.createEvent(message);
        final EventContext context = event.getContext()
                                          .toBuilder()
                                          .setExternal(externalMessage())
                                          .build();
        return event.toBuilder()
                    .setContext(context)
                    .build();
    }

    protected final Rejection createRejection(M message) {
        final Empty cmd = Empty.getDefaultInstance();
        final Command command = requestFactory.command()
                                              .create(cmd);
        final Rejection rejection = Rejections.createRejection(message, command);
        final RejectionContext context = rejection.getContext()
                                                  .toBuilder()
                                                  .setExternal(externalMessage())
                                                  .build();
        return rejection.toBuilder()
                        .setContext(context)
                        .build();
    }

    protected boolean externalMessage() {
        return false;
    }
}
