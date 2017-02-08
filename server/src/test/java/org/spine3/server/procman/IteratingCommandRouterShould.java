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

package org.spine3.server.procman;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.server.command.CommandBus;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
public class IteratingCommandRouterShould
        extends AbstractCommandRouterShould<IteratingCommandRouter> {

    @Override
    protected IteratingCommandRouter createRouter(CommandBus commandBus,
                                                  Message sourceMessage,
                                                  CommandContext commandContext) {
        return new IteratingCommandRouter(commandBus, sourceMessage, commandContext);
    }

    @Test
    public void return_CommandRouted_from_routeFirst() throws Exception {
        final CommandRouted commandRouted = router().routeFirst();

        assertSource(commandRouted);

        // Test that only only one command was produced by `routeFirst()`.
        assertEquals(1, commandRouted.getProducedCount());

        // Test that there's only one produced command and it has correct message.
        final Command produced = commandRouted.getProduced(0);
        final StringValue commandMessage = Commands.getMessage(produced);
        assertEquals(messages().get(0), commandMessage);

        assertActorAndTenant(produced);

        // Test that the event contains messages to follow.
        assertEquals(messages().size() - 1, commandRouted.getMessageToFollowCount());

        final List<Any> messageToFollow = commandRouted.getMessageToFollowList();
        assertArrayEquals(messages().subList(1, messages().size()).toArray(),
                          unpackAll(messageToFollow).toArray());
    }

    @Test
    public void produce_a_command_on_routeNext() throws Exception {
        router().routeFirst();

        assertTrue(router().hasNext());
        
        final Command command = router().routeNext();

        // Test that 2nd command message is in the next rounded command.
        assertEquals(messages().get(1), Commands.getMessage(command));
    }
}
