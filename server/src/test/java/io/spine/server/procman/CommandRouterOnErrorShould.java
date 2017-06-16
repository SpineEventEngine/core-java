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

package io.spine.server.procman;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.base.CommandContext;
import io.spine.envelope.CommandEnvelope;
import io.spine.protobuf.Wrapper;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.type.CommandClass;
import org.junit.Test;

import java.util.Set;

/**
 * @author Alexaneder Yevsyukov
 */
public class CommandRouterOnErrorShould extends AbstractCommandRouterShould<CommandRouter> {

    /**
     * Creates a router with mocked {@code CommandBus} which always calls
     * {@link StreamObserver#onError(Throwable) StreamObserver.onError()} when
     * {@link CommandBus#post(Message, StreamObserver) CommandBus.post()} is invoked.
     */
    @Override
    CommandRouter createRouter(CommandBus commandBus,
                               Message sourceMessage,
                               CommandContext commandContext) {
        return new CommandRouter(commandBus, sourceMessage, commandContext);
    }

    @Test(expected = IllegalStateException.class)
    public void throw_IllegalStateException_when_caught_error_when_posting() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        final CommandBus commandBus = boundedContext.getCommandBus();

        // Register dispatcher for `StringValue` message type.
        // Fails each time of dispatch().
        commandBus.register(new CommandDispatcher() {
            @Override
            public Set<CommandClass> getMessageClasses() {
                return CommandClass.setOf(StringValue.class);
            }

            @Override
            public void dispatch(CommandEnvelope envelope) {
                throw new IllegalStateException("I am faulty!");
            }
        });

        final StringValue sourceMessage = Wrapper.forString(getClass().getSimpleName());
        final CommandContext sourceContext = getRequestFactory().createCommandContext();

        final CommandRouter router = createRouter(commandBus, sourceMessage, sourceContext);
        router.addAll(getMessages());

        router.routeAll();
    }
}
