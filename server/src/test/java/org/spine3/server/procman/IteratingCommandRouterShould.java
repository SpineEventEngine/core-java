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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.base.Commands;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.CommandBus;
import org.spine3.server.command.CommandDispatcher;
import org.spine3.server.type.CommandClass;
import org.spine3.test.TestCommandFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Values.newStringValue;

/**
 * @author Alexander Yevsyukov
 */
public class IteratingCommandRouterShould {

    private final TestCommandFactory commandFactory = TestCommandFactory.newInstance(getClass());

    /** The object we test. */
    private IteratingCommandRouter router;

    /** The command message we route. */
    private Message sourceMessage;

    /** The context of the command that we route. */
    private CommandContext sourceContext;

    /** Command messages to be sent. */
    private final List<Message> messages = ImmutableList.<Message>of(
            newStringValue("uno"),
            newStringValue("dos"),
            newStringValue("tres"),
            newStringValue("cuatro")
    );

    @Before
    public void setUp() {
        final BoundedContext boundedContext = BoundedContext.newBuilder()
                                                            .build();
        final CommandBus commandBus = boundedContext.getCommandBus();
        commandBus.register(new CommandDispatcher() {
            @Override
            public Set<CommandClass> getCommandClasses() {
                return CommandClass.setOf(StringValue.class);
            }

            @Override
            public void dispatch(Command request) throws Exception {
                // Do nothing.
            }
        });

        sourceMessage = newStringValue(getClass().getSimpleName());
        sourceContext = commandFactory.createCommandContext();

        router = new IteratingCommandRouter(commandBus, sourceMessage, sourceContext);
        router.addAll(messages);
    }

    @Test
    public void return_CommandRouted_from_routeFirst() throws Exception {
        final CommandRouted commandRouted = router.routeFirst();

        // Check that the source command is stored.
        final Command source = commandRouted.getSource();
        assertEquals(sourceMessage, Commands.getMessage(source));
        assertEquals(sourceContext, source.getContext());

        // Test that only only one command was produced by `routeFirst()`.
        assertEquals(1, commandRouted.getProducedCount());

        // Test that there's only one produced command and it has correct message.
        final Command produced = commandRouted.getProduced(0);
        final StringValue commandMessage = Commands.getMessage(produced);
        assertEquals(messages.get(0), commandMessage);

        // Test that the produced command context has correct fields.
        assertTrue(Commands.sameActorAndTenant(sourceContext, produced.getContext()));

        // Test that the event contains messages to follow.
        assertEquals(messages.size() - 1, commandRouted.getMessageToFollowCount());

        final List<Any> messageToFollow = commandRouted.getMessageToFollowList();
        assertArrayEquals(messages.subList(1, messages.size()).toArray(),
                          unpackAll(messageToFollow).toArray());
    }

    @Test
    public void produce_a_command_on_routeNext() throws Exception {
        router.routeFirst();

        final Command command = router.routeNext();

        // Test that 2nd command message is in the next rounded command.
        assertEquals(messages.get(1), Commands.getMessage(command));
    }

    private static List<StringValue> unpackAll(List<Any> anyList) {
        return Lists.transform(anyList, new Function<Any, StringValue>() {
            @Nullable
            @Override
            public StringValue apply(@Nullable Any input) {
                if (input == null) {
                    return null;
                }
                return AnyPacker.unpack(input);
            }
        });
    }
}
