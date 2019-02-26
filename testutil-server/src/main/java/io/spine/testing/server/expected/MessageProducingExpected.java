/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;

import java.util.List;
import java.util.function.Consumer;

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
 * @param <X> the type of {@code MessageProducingExpected} for type covariance of returned values
 * @author Vladyslav Lubenskyi
 */
public abstract class MessageProducingExpected<S extends Message,
                                               X extends MessageProducingExpected<S, X>>
        extends AbstractExpected<S, X> {

    private final ImmutableList<? extends Message> generatedMessages;
    private final ImmutableList<Message> commands;

    MessageProducingExpected(List<? extends Message> generatedMessages,
                             S initialState,
                             S state,
                             List<Message> interceptedCommands) {
        super(initialState, state);
        this.generatedMessages = ImmutableList.copyOf(generatedMessages);
        this.commands = ImmutableList.copyOf(interceptedCommands);
    }

    @Override
    @CanIgnoreReturnValue
    protected X ignoresMessage() {
        assertTrue(commands.isEmpty(), format("Message produced commands: %s", commands));
        if (!generatedMessages.isEmpty()) {
            assertEquals(1, generatedMessages.size());
            assertTrue(generatedMessages.get(0) instanceof Empty);
        }
        return super.ignoresMessage();
    }

    /**
     * Ensures that the handler produces a message of the passed class and
     * performs validation of the produced message.
     *
     * @param messageClass
     *         type of the messages expected to be produced
     * @param validator
     *         a {@link Consumer} that performs all required assertions for the resulting event
     * @param <M>
     *         class of the event's Protobuf message
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    @CanIgnoreReturnValue
    protected <M extends Message> X producesMessage(Class<M> messageClass, Consumer<M> validator) {
        assertNotNull(validator);
        assertEquals(1, generatedMessages.size());
        assertSingleEvent(generatedMessages.get(0), messageClass, validator);
        return self();
    }

    /**
     * Ensures that the command produces events of the given types.
     *
     * @param messageClasses types of the expected events
     */
    @CanIgnoreReturnValue
    protected X producesMessages(Class<?>... messageClasses) {
        List<? extends Class<?>> actualClasses =
                generatedMessages.stream()
                                 .map(Message::getClass)
                                 .collect(toList());
        assertEquals(
                messageClasses.length, generatedMessages.size(),
                format(
                        "Unexpected number of messages: %s. Expected: %s. Generated messages: %s.",
                        generatedMessages.size(),
                        messageClasses.length,
                        toString(actualClasses)
                )
        );
        assertThat(actualClasses, containsInAnyOrder(messageClasses));
        return self();
    }

    /**
     * Ensures that the message handler posts a command of {@code commandClass} type and performs
     * validation of the produced command.
     *
     * <p>It is applicable for process managers.
     *
     * @param commandClass
     *         type of the command expected to be posted
     * @param validator
     *         a {@link Consumer} that performs all required assertions for the resulting command
     * @param <M>
     *         class of the command's Protobuf message
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    @CanIgnoreReturnValue
    protected <M extends Message> X producesCommand(Class<M> commandClass, Consumer<M> validator) {
        assertNotNull(validator);
        assertEquals(1, commands.size());
        assertSingleCommand(commands.get(0), commandClass, validator);
        return self();
    }

    /**
     * Ensures that the message handler posts commands of the given types.
     *
     * @param commandClasses types of the expected commands
     */
    @CanIgnoreReturnValue
    protected X producesCommands(Class<?>... commandClasses) {
        List<? extends Class<?>> actualClasses =
                commands.stream()
                        .map(Message::getClass)
                        .collect(toList());

        assertEquals(
                commandClasses.length, commands.size(),
                format(
                        "Unexpected number of commands: %s. Expected: %s. " +
                                "Intercepted commands: %s.",
                        commands.size(),
                        commandClasses.length,
                        toString(actualClasses)
                )
        );
        assertThat(actualClasses, containsInAnyOrder(commandClasses));
        return self();
    }

    private static String toString(List<? extends Class<?>> classes) {
        return Joiner.on(", ")
                     .join(classes.stream()
                                  .map(Class::getName)
                                  .map(s -> format("`%s`", s))
                                  .collect(toList())
              );
    }

    /**
     * Checks that the given command is as expected.
     *
     * @param generatedCommand
     *         message produced by the message handler
     * @param expectedCommandClass
     *         an expected class of the {@code generatedCommand}
     * @param validator
     *         a {@link Consumer} that performs all required assertions for the command
     * @param <M>
     *         an expected class of the {@code generatedCommand}
     */
    @SuppressWarnings("unchecked") // Type check is performed manually using assertion.
    private static <M extends Message>
    void assertSingleCommand(Message generatedCommand,
                             Class<M> expectedCommandClass,
                             Consumer<M> validator) {
        assertTrue(expectedCommandClass.isInstance(generatedCommand));
        validator.accept((M) generatedCommand);
    }

    /**
     * Checks that the given event is as expected.
     *
     * @param emittedEvent
     *         message produced by the message handler
     * @param expectedEventClass
     *         an expected class of the {@code emittedMessage}
     * @param validator
     *         a {@link Consumer} that performs all required assertions for the event
     * @param <M>
     *         an expected class of the {@code emittedMessage}
     */
    @SuppressWarnings("unchecked") // Type check is performed manually using assertion.
    private static <M extends Message>
    void assertSingleEvent(Message emittedEvent,
                           Class<M> expectedEventClass,
                           Consumer<M> validator) {
        assertTrue(expectedEventClass.isInstance(emittedEvent));
        validator.accept((M) emittedEvent);
    }
}
