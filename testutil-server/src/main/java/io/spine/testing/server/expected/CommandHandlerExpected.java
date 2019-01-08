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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Message;
import io.spine.type.TypeUrl;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Assertions for a command handler invocation results.
 *
 * @param <S> the type of the tested entity state
 * @author Dmytro Dashenkov
 */
public class CommandHandlerExpected<S extends Message>
        extends MessageProducingExpected<S, CommandHandlerExpected<S>> {

    @Nullable
    private final Message rejectionMessage;

    public CommandHandlerExpected(List<? extends Message> events,
                                  @Nullable Message rejectionMessage,
                                  S initialState,
                                  S state,
                                  List<Message> interceptedCommands) {
        super(events, initialState, state, interceptedCommands);
        this.rejectionMessage = rejectionMessage;
    }

    @Override
    @CanIgnoreReturnValue
    public CommandHandlerExpected<S> ignoresMessage() {
        assertNull(rejectionMessage, "Message caused a rejection.");
        return super.ignoresMessage();
    }

    @Override
    protected CommandHandlerExpected<S> self() {
        return this;
    }

    @CanIgnoreReturnValue
    public <M extends Message>
    CommandHandlerExpected<S> producesEvent(Class<M> eventClass, Consumer<M> validator) {
        assertNotRejected(eventClass.getName());
        return producesMessage(eventClass, validator);
    }

    @CanIgnoreReturnValue
    public CommandHandlerExpected<S> producesEvents(Class<?>... eventClasses) {
        assertNotRejected(Stream.of(eventClasses)
                                .map(Class::getSimpleName)
                                .collect(joining(",")));
        return producesMessages(eventClasses);
    }

    @Override
    @CanIgnoreReturnValue
    public <M extends Message>
    CommandHandlerExpected<S> producesCommand(Class<M> commandClass, Consumer<M> validator) {
        assertNotRejected(commandClass.getName());
        return super.producesCommand(commandClass, validator);
    }

    @Override
    @CanIgnoreReturnValue
    public CommandHandlerExpected<S> producesCommands(Class<?>... commandClasses) {
        assertNotRejected(Stream.of(commandClasses)
                                .map(Class::getSimpleName)
                                .collect(joining(",")));
        return super.producesCommands(commandClasses);
    }

    private void assertNotRejected(String eventType) {
        boolean rejected = rejectionMessage != null;
        if (rejected) {
            fail(format("Message was rejected. Expected messages(s): [%s]. Rejection: %s%n%s.",
                        eventType,
                        TypeUrl.of(rejectionMessage),
                        rejectionMessage)
            );
        }
    }

    /**
     * Ensures that the command produces a rejection of {@code rejectionClass} type.
     *
     * @param rejectionClass type of the rejection expected to be produced
     */
    @CanIgnoreReturnValue
    public CommandHandlerExpected<S> throwsRejection(Class<? extends Message> rejectionClass) {
        assertNotNull(rejectionMessage, format("No rejection encountered. Expected %s",
                                               rejectionClass.getSimpleName()));
        assertTrue(rejectionClass.isInstance(rejectionMessage),
                   format("%s is not an instance of %s.",
                          rejectionMessage.getClass().getSimpleName(),
                          rejectionClass));
        return self();
    }
}
