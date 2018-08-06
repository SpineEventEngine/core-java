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

package io.spine.server.event.model;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.base.Time;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionEventContext;
import io.spine.server.event.model.given.EventAccessorTestEnv.EventReceiver;
import io.spine.test.event.ProjectCreated;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static com.google.common.testing.NullPointerTester.Visibility.PACKAGE;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.server.event.model.EventAcceptor.MESSAGE;
import static io.spine.server.event.model.EventAcceptor.MESSAGE_COMMAND_CXT;
import static io.spine.server.event.model.EventAcceptor.MESSAGE_COMMAND_MSG;
import static io.spine.server.event.model.EventAcceptor.MESSAGE_COMMAND_MSG_COMMAND_CXT;
import static io.spine.server.event.model.EventAcceptor.MESSAGE_EVENT_CXT;
import static io.spine.server.event.model.given.EventAccessorTestEnv.commandContext;
import static io.spine.server.event.model.given.EventAccessorTestEnv.commandMessage;
import static io.spine.server.event.model.given.EventAccessorTestEnv.eventContext;
import static io.spine.server.event.model.given.EventAccessorTestEnv.eventMessage;
import static io.spine.server.event.model.given.EventAccessorTestEnv.findMessageAndCmdContext;
import static io.spine.server.event.model.given.EventAccessorTestEnv.findMessageAndCommand;
import static io.spine.server.event.model.given.EventAccessorTestEnv.findMessageAndCommandMessageAndContext;
import static io.spine.server.event.model.given.EventAccessorTestEnv.findMessageAndContext;
import static io.spine.server.event.model.given.EventAccessorTestEnv.findMessageOnly;
import static io.spine.server.event.model.given.EventAccessorTestEnv.rejectionMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("EventAcceptor should")
@SuppressWarnings("InnerClassMayBeStatic") // Nested test suites.
class EventAcceptorTest {

    @Test
    @DisplayName("not accept nulls")
    void notAcceptNulls() throws NoSuchMethodException {
        Event dummyEvent = Event
                .newBuilder()
                .setMessage(pack(Time.getCurrentTime()))
                .build();
        Method defaultMethod =
                EventReceiver.class.getDeclaredMethod("messageOnly", ProjectCreated.class);
        new NullPointerTester()
                .setDefault(Method.class, defaultMethod)
                .setDefault(EventEnvelope.class, EventEnvelope.of(dummyEvent))
                .testStaticMethods(EventAcceptor.class, PACKAGE);
    }

    @Nested
    @DisplayName("find proper instance for given signature:")
    class FactoryTest {

        @Test
        @DisplayName("(Message)")
        void onlyMessage() {
            Method method = findMessageOnly();
            assertMethod(method, MESSAGE);
        }

        @Test
        @DisplayName("(Message, EventContext)")
        void messageAndContext() {
            Method method = findMessageAndContext();
            assertMethod(method, MESSAGE_EVENT_CXT);
        }

        @Test
        @DisplayName("(Message, CommandContext)")
        void messageAndCmdContext() {
            Method method = findMessageAndCmdContext();
            assertMethod(method, MESSAGE_COMMAND_CXT);
        }

        @Test
        @DisplayName("(Message, Message)")
        void messageAndCommand() {
            Method method = findMessageAndCommand();
            assertMethod(method, MESSAGE_COMMAND_MSG);
        }

        @Test
        @DisplayName("(Message, Message, CommandContext)")
        void messageAndCommandMessageAndContext() {
            Method method = findMessageAndCommandMessageAndContext();
            assertMethod(method, MESSAGE_COMMAND_MSG_COMMAND_CXT);
        }

        private void assertMethod(Method method, EventAcceptor expectedAcceptor) {
            Optional<EventAcceptor> actualAcceptor = EventAcceptor.from(method);
            assertTrue(actualAcceptor.isPresent());
            assertEquals(expectedAcceptor, actualAcceptor.get());
        }
    }

    @Nested
    @DisplayName("invoke method with arguments:")
    class InvokeTest {

        private EventReceiver receiver;

        @BeforeEach
        void setUp() {
            receiver = new EventReceiver();
        }

        @Test
        @DisplayName("(Message)")
        void onlyMessage() throws InvocationTargetException {
            Method method = findMessageOnly();
            invokeEvent(method, eventMessage(), null);
        }

        @Test
        @DisplayName("(Message, EventContext)")
        void messageAndContext() throws InvocationTargetException {
            Method method = findMessageAndContext();
            invokeEvent(method, eventMessage(), eventContext());
        }

        @Test
        @DisplayName("(Message, CommandContext)")
        void messageAndCmdContext() throws InvocationTargetException {
            Method method = findMessageAndCmdContext();
            invokeRejection(method, rejectionMessage(), null, commandContext());
        }

        @Test
        @DisplayName("(Message, Message)")
        void messageAndCommand() throws InvocationTargetException {
            Method method = findMessageAndCommand();
            invokeRejection(method, rejectionMessage(), commandMessage(), null);
        }

        @Test
        @DisplayName("(Message, Message, CommandContext)")
        void messageAndCommandMessageAndContext() throws InvocationTargetException {
            Method method = findMessageAndCommandMessageAndContext();
            invokeRejection(method, rejectionMessage(), commandMessage(), commandContext());
        }

        @SuppressWarnings("CheckReturnValue")
        private void invokeEvent(Method method,
                                 Message eventMessage,
                                 @Nullable EventContext eventContext)
                throws InvocationTargetException {
            Event.Builder builder = Event
                    .newBuilder()
                    .setMessage(pack(eventMessage));
            if (eventContext != null) {
                builder.setContext(eventContext);
            }
            Event event = builder.build();
            EventEnvelope envelope = EventEnvelope.of(event);
            EventAcceptor.accept(receiver, method, envelope);

            receiver.assertEvent(eventMessage);
            receiver.assertEventContext(eventContext);
        }

        @SuppressWarnings("CheckReturnValue")
        private void invokeRejection(Method method,
                                     Message rejectionMessage,
                                     @Nullable Message commandMessage,
                                     @Nullable CommandContext commandContext)
                throws InvocationTargetException {
            Event.Builder builder = Event
                    .newBuilder()
                    .setMessage(pack(rejectionMessage));
            EventContext.Builder contextBuilder = EventContext.newBuilder();
            Message command = commandMessage == null
                              ? Time.getCurrentTime()
                              : commandMessage;
            RejectionEventContext rejectionContext = RejectionEventContext
                    .newBuilder()
                    .setCommandMessage(pack(command))
                    .build();
            contextBuilder.setRejection(rejectionContext);
            contextBuilder.setCommandContext(commandContext == null
                                             ? CommandContext.getDefaultInstance()
                                             : commandContext);
            builder.setContext(contextBuilder);
            Event event = builder.build();
            EventEnvelope envelope = EventEnvelope.of(event);
            EventAcceptor.accept(receiver, method, envelope);

            receiver.assertEvent(rejectionMessage);
            receiver.assertCommand(commandMessage);
            receiver.assertCommandContext(commandContext);
        }
    }
}
