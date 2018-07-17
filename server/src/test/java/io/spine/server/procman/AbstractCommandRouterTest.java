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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.core.CommandEnvelope;
import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.testing.client.TestActorRequestFactory;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Set;

import static io.spine.protobuf.TypeConverter.toMessage;
import static java.util.Collections.unmodifiableList;

/**
 * @author Alexander Yevsyukov
 */
abstract class AbstractCommandRouterTest {

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    /** Command messages to be sent. */
    private final List<Message> messages = ImmutableList.of(toMessage("uno"),
                                                            toMessage("dos"),
                                                            toMessage("tres"),
                                                            toMessage("cuatro")
    );

    abstract CommandRouter
    createRouter(CommandBus commandBus, Message sourceMessage, CommandContext commandContext);

    public List<Message> getMessages() {
        return unmodifiableList(messages);
    }

    public TestActorRequestFactory getRequestFactory() {
        return requestFactory;
    }

    @BeforeEach
    void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        final CommandBus commandBus = boundedContext.getCommandBus();

        // Register dispatcher for `StringValue` message type.
        // Otherwise we won't be able to post.
        commandBus.register(new CommandDispatcher<String>() {
            @Override
            public Set<CommandClass> getMessageClasses() {
                return CommandClass.setOf(StringValue.class);
            }

            @Override
            public String dispatch(CommandEnvelope envelope) {
                // Do nothing.
                return "Anonymous";
            }

            @Override
            public void onError(CommandEnvelope envelope, RuntimeException exception) {
                // Do nothing.
            }
        });

        // The command message we route.
        Message sourceMessage = toMessage(getClass().getSimpleName());
        // The context of the command that we route.
        CommandContext sourceContext = requestFactory.createCommandContext();

        // The object we test. */
        CommandRouter router = createRouter(commandBus, sourceMessage, sourceContext);
        for (Message message : messages) {
            router.add(message);
        }
    }
}
