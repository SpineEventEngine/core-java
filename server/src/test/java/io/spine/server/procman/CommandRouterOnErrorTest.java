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

package io.spine.server.procman;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.protobuf.TypeConverter.toMessage;

/**
 * @author Alexaneder Yevsyukov
 */
@DisplayName("CommandRouter `onError` should")
class CommandRouterOnErrorTest extends AbstractCommandRouterTest<CommandRouter> {

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

    @SuppressWarnings("ResultOfMethodCallIgnored") // We're not interested in routing results.
    @Test
    @DisplayName("ignore any error occurred on dispatching")
    void ignoreErrorWhenDispatching() {
        BoundedContext boundedContext = BoundedContext.newBuilder()
                                                      .build();
        CommandBus commandBus = boundedContext.getCommandBus();

        // Register dispatcher for `StringValue` message type.
        // Fails each time of dispatch().
        commandBus.register(new CommandDispatcher<Message>() {
            @Override
            public Set<CommandClass> getMessageClasses() {
                return CommandClass.setOf(StringValue.class);
            }

            @Override
            public Message dispatch(CommandEnvelope envelope) {
                throw new IllegalStateException("I am faulty!");
            }

            @Override
            public void onError(CommandEnvelope envelope, RuntimeException exception) {
                // Do nothing.
            }
        });

        StringValue sourceMessage = toMessage(getClass().getSimpleName());
        CommandContext sourceContext = getRequestFactory().createCommandContext();

        CommandRouter router = createRouter(commandBus, sourceMessage, sourceContext);
        for (Message message : getMessages()) {
            router.add(message);
        }
        router.routeAll();
    }
}
