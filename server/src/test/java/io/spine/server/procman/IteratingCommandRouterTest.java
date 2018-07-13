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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;
import io.spine.core.Command;
import io.spine.core.CommandContext;
import io.spine.core.Commands;
import io.spine.server.commandbus.CommandBus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("IteratingCommandRouter should")
class IteratingCommandRouterTest
        extends AbstractCommandRouterTest<IteratingCommandRouter> {

    @Override
    IteratingCommandRouter createRouter(CommandBus commandBus,
                                        Message sourceMessage,
                                        CommandContext commandContext) {
        return new IteratingCommandRouter(commandBus, sourceMessage, commandContext);
    }

    @Test
    @DisplayName("return CommandRouted from `routeFirst`")
    void returnRoutedFirst() throws Exception {
        CommandRouted commandRouted = router().routeFirst();

        assertSource(commandRouted);

        // Test that only only one command was produced by `routeFirst()`.
        assertEquals(1, commandRouted.getProducedCount());

        // Test that there's only one produced command and it has correct message.
        Command produced = commandRouted.getProduced(0);
        StringValue commandMessage = Commands.getMessage(produced);
        assertEquals(messages().get(0), commandMessage);

        assertActorAndTenant(produced);

        // Test that the event contains messages to follow.
        assertEquals(messages().size() - 1, commandRouted.getMessageToFollowCount());

        List<Any> messageToFollow = commandRouted.getMessageToFollowList();
        assertArrayEquals(messages().subList(1, messages().size()).toArray(),
                          unpackAll(messageToFollow).toArray());
    }

    @Test
    @DisplayName("produce command on `routeNext`")
    void returnRoutedNext() throws Exception {

        /*
        This is a hack, aimed to resolve the wall-clock inaccuracy issue, that is randomly
        causing the test failure due to a fast execution.

        <p>The idea is to add some randomization to {@code nanoseconds} value of the
        current Timestamp obtained from the wall-clock provider.
        */
        Time.setProvider(() -> {
            Timestamp millis = Timestamps.fromMillis(System.currentTimeMillis());
            Timestamp nanos = Timestamps.fromNanos(System.nanoTime());

            Timestamp result = millis.toBuilder()
                                           .setNanos(nanos.toBuilder()
                                                          .getNanos())
                                           .build();
            return result;
        });

        CommandRouted firstRouted = router().routeFirst();
        assertTrue(router().hasNext());

        Command command = router().routeNext();

        // Test that 2nd command message is in the next routed command.
        assertEquals(messages().get(1), Commands.getMessage(command));

        // Verify that the context for the next routed command has been created, not just copied.
        Command firstCommand = firstRouted.getSource();
        assertNotEquals(firstCommand.getContext()
                                    .getActorContext()
                                    .getTimestamp(),
                        command.getContext()
                               .getActorContext()
                               .getTimestamp());

        // Revert the hack.
        Time.resetProvider();
    }
}
