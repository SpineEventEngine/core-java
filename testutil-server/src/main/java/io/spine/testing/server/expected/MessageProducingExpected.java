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

package io.spine.testing.server.expected;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Assertions for any messages produced by handling the tested message.
 *
 * @param <S> the type of the tested entity state
 * @author Vladyslav Lubenskyi
 */
public abstract class MessageProducingExpected<S extends Message,
                                               X extends MessageProducingExpected<S, X>>
        extends AbstractExpected<S, X> {

    private final ImmutableList<? extends Message> events;
    private final ImmutableList<Message> commands;

    public MessageProducingExpected(List<? extends Message> events,
                                    S initialState,
                                    S state,
                                    List<Message> interceptedCommands) {
        super(initialState, state);
        this.events = copyOf(events);
        this.commands = copyOf(interceptedCommands);
    }

    @Override
    public X ignoresMessage() {
        assertTrue(commands.isEmpty(), format("Message produced commands: %s", commands));
        if (!events.isEmpty()) {
            assertEquals(1, events.size());
            assertTrue(Empty.class.isInstance(events.get(0)));
        }
        return super.ignoresMessage();
    }

    /**
     * Ensures that the command produces an event of {@code eventClass} type and performs
     * validation of the produced event.
     *
     * @param eventClass type of the event expected to be produced
     * @param validator  a {@link Consumer} that performs all required assertions for
     *                   the resulting event
     * @param <M>        class of the event's Protobuf message
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public <M extends Message> X producesEvent(Class<M> eventClass, Consumer<M> validator) {
        assertNotNull(validator);
        assertEquals(1, events.size());
        assertSingleEvent(events.get(0), eventClass, validator);
        return self();
    }

    /**
     * Ensures that the command produces events of the given types.
     *
     * @param eventClasses types of the expected events
     */
    @SuppressWarnings("UnusedReturnValue")
    public X producesEvents(Class<?>... eventClasses) {
        assertEquals(eventClasses.length, events.size(), () -> format(
                "Unexpected number of events: %s (%s). Expected %s",
                events.size(), events.stream()
                                     .map(Message::getClass)
                                     .map(Class::getSimpleName)
                                     .collect(toList()),
                eventClasses.length
        ));
        List<? extends Class<?>> actualClasses = events.stream()
                                                       .map(Message::getClass)
                                                       .collect(toList());
        assertThat(actualClasses, containsInAnyOrder(eventClasses));
        return self();
    }

    /**
     * Ensures that the message handler posts a command of {@code commandClass} type and performs
     * validation of the produced command.
     *
     * <p>It is applicable for process managers.
     *
     * @param commandClass type of the command expected to be posted
     * @param validator    a {@link Consumer} that performs all required assertions for
     *                     the resulting command
     * @param <M>          class of the command's Protobuf message
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public <M extends Message> X routesCommand(Class<M> commandClass, Consumer<M> validator) {
        assertNotNull(validator);
        assertEquals(1, commands.size());
        assertSingleCommand(commands.get(0), commandClass, validator);
        return self();
    }

    /**
     * Ensures that the message handler posts commands of the given types.
     *
     * <p>It is applicable for process managers.
     *
     * @param commandClasses types of the expected commands
     */
    @SuppressWarnings("UnusedReturnValue")
    public X routesCommands(Class<?>... commandClasses) {
        assertEquals(commandClasses.length, commands.size(), () -> format(
                "Unexpected number of commands: %s (%s). Expected %s",
                commands.size(), commands.stream()
                                         .map(Message::getClass)
                                         .map(Class::getSimpleName)
                                         .collect(toList()),
                commandClasses.length
        ));
        List<? extends Class<?>> actualClasses = commands.stream()
                                                         .map(Message::getClass)
                                                         .collect(toList());
        assertThat(actualClasses, containsInAnyOrder(commandClasses));
        return self();
    }

    /**
     * Checks that the given command is as expected.
     *
     * @param generatedCommand     message produced by the message handler
     * @param expectedCommandClass an expected class of the {@code generatedCommand}
     * @param validator            a {@link Consumer} that performs all required assertions for
     *                             the command
     * @param <M>                  an expected class of the {@code generatedCommand}
     */
    @SuppressWarnings("unchecked") // Type check is performed manually using assertion.
    private static <M extends Message> void assertSingleCommand(Message generatedCommand,
                                                                Class<M> expectedCommandClass,
                                                                Consumer<M> validator) {
        assertTrue(expectedCommandClass.isInstance(generatedCommand));
        validator.accept((M) generatedCommand);
    }

    /**
     * Checks that the given event is as expected.
     *
     * @param emittedEvent       message produced by the message handler
     * @param expectedEventClass an expected class of the {@code emittedMessage}
     * @param validator          a {@link Consumer} that performs all required assertions for
     *                           the event
     * @param <M>                an expected class of the {@code emittedMessage}
     */
    @SuppressWarnings("unchecked") // Type check is performed manually using assertion.
    private static <M extends Message> void assertSingleEvent(Message emittedEvent,
                                                              Class<M> expectedEventClass,
                                                              Consumer<M> validator) {
        assertTrue(expectedEventClass.isInstance(emittedEvent));
        validator.accept((M) emittedEvent);
    }
}
