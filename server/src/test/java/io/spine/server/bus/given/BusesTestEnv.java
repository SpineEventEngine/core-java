/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.bus.given;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.base.Error;
import io.spine.core.AbstractMessageEnvelope;
import io.spine.core.Ack;
import io.spine.core.EventContext;
import io.spine.core.MessageInvalid;
import io.spine.core.Status;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.bus.MessageUnhandled;
import io.spine.test.bus.BusMessage;
import io.spine.test.bus.BusMessageContext;
import io.spine.test.bus.BusMessageId;
import io.spine.test.bus.TestMessageContents;
import io.spine.testdata.Sample;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.bus.Buses.acknowledge;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Mykhailo Drachuk
 */
public class BusesTestEnv {

    public static final Status STATUS_OK = statusOk();

    private BusesTestEnv() {
        // Prevents instantiation.
    }

    public static BusMessage busMessage(Message message) {
        return ((BusMessage.Builder) Sample.builderForType(BusMessage.class))
                .setContents(pack(message))
                .build();
    }

    public static TestMessageContents testContents() {
        return TestMessageContents.newBuilder()
                                  .build();
    }

    private static Status statusOk() {
        return Status.newBuilder()
                     .setOk(Empty.getDefaultInstance())
                     .build();
    }

    public static String errorType(Ack response) {
        return response.getStatus()
                       .getError()
                       .getType();
    }

    interface TestMessageDispatcher extends MessageDispatcher<TestMessageClass, TestEnvelope, BusMessageId> {

    }

    public static class TestMessageContentsDispatcher implements TestMessageDispatcher {

        @Override
        public Set<TestMessageClass> getMessageClasses() {
            return ImmutableSet.of(TestMessageClass.of(TestMessageContents.class));
        }

        @Override
        public BusMessageId dispatch(TestEnvelope envelope) {
            return envelope.getId();
        }

        @Override
        public void onError(TestEnvelope envelope, RuntimeException exception) {
            // Do nothing.
        }
    }

    public static class TestMessageBus extends Bus<BusMessage, TestEnvelope, TestMessageClass, TestMessageDispatcher> {

        private EnvelopeValidator<TestEnvelope> validator;
        private final List storedMessages;

        private TestMessageBus() {
            storedMessages = Lists.newArrayList();
        }

        @Override
        protected DeadMessageTap<TestEnvelope> getDeadMessageHandler() {
            return new TestDeadMessageTap();
        }

        @Override
        protected EnvelopeValidator<TestEnvelope> getValidator() {
            return validator;
        }

        @Override
        protected DispatcherRegistry<TestMessageClass, TestMessageDispatcher> createRegistry() {
            return new DispatcherRegistry<>();
        }

        @Override
        protected Deque<BusFilter<TestEnvelope>> createFilterChain() {
            return Queues.newArrayDeque();
        }

        @Override
        protected TestEnvelope toEnvelope(BusMessage message) {
            return new TestEnvelope(message);
        }

        @Override
        protected Ack doPost(TestEnvelope envelope) {
            final Any packedId = Identifier.pack(envelope.getId());
            final Ack result = acknowledge(packedId);
            return result;
        }

        @Override
        protected void store(Iterable messages) {
            Iterables.addAll(storedMessages, messages);
        }

        public static TestMessageBus newInstance() {
            final TestMessageBus bus = new TestMessageBus();
            bus.setValidValidator();
            return bus;
        }

        private void setValidator(EnvelopeValidator<TestEnvelope> validator) {
            this.validator = validator;
        }

        public void setValidValidator() {
            setValidator(new ValidValidator());
        }

        public void setInvalidValidator() {
            setValidator(new InvalidValidator());
        }

        public Collection<?> storedMessages() {
            return ImmutableList.copyOf(storedMessages);
        }

        static class TestDeadMessageTap implements DeadMessageTap<TestEnvelope> {
            @Override
            public MessageUnhandled capture(TestEnvelope message) {
                return new DeadMessageException();
            }
        }

        /**
         * A validator which always returns an {@link Optional#absent()}.
         */
        static class ValidValidator implements EnvelopeValidator<TestEnvelope> {

            @Override
            public Optional<MessageInvalid> validate(TestEnvelope envelope) {
                return Optional.absent();
            }
        }

        /**
         * A validator which always returns an {@link Optional} with {@link TestValidatorException}.
         */
        static class InvalidValidator implements EnvelopeValidator<TestEnvelope> {

            @Override
            public Optional<MessageInvalid> validate(TestEnvelope envelope) {
                final MessageInvalid error = new TestValidatorException();
                return Optional.of(error);
            }
        }

    }

    static class TestEnvelope extends AbstractMessageEnvelope<BusMessageId, BusMessage, BusMessageContext> {

        private final BusMessage outer;
        private final Message message;
        private final TestMessageClass messageClass;
        private final BusMessageContext context;

        private TestEnvelope(BusMessage outer) {
            super(outer);
            this.outer = outer;
            message = unpack(outer.getContents());
            messageClass = TestMessageClass.of(message);
            context = outer.getContext();
        }

        @Override
        public BusMessageId getId() {
            return outer.getId();
        }

        @Override
        public Message getMessage() {
            return message;
        }

        @Override
        public MessageClass getMessageClass() {
            return messageClass;
        }

        @Override
        public BusMessageContext getMessageContext() {
            return context;
        }

        @Override
        public void passToEventContext(EventContext.Builder builder) {
            throw newIllegalStateException("Events should not be constructed during Bus tests.",
                                           this);
        }
    }

    static final class TestMessageClass extends MessageClass {

        private static final long serialVersionUID = 0L;

        private TestMessageClass(Class<? extends Message> value) {
            super(value);
        }

        public static TestMessageClass of(Class<? extends Message> value) {
            return new TestMessageClass(checkNotNull(value));
        }

        public static TestMessageClass of(Message message) {
            return of(message.getClass());
        }
    }

    public static class DeadMessageException extends RuntimeException implements MessageUnhandled {

        private static final long serialVersionUID = 0L;

        public static final String TYPE = DeadMessageException.class.getCanonicalName();

        @Override
        public Error asError() {
            return Error.newBuilder()
                        .setType(TYPE)
                        .build();
        }

        @Override
        public Throwable asThrowable() {
            return this;
        }
    }

    public static class TestValidatorException extends RuntimeException implements MessageInvalid {

        private static final long serialVersionUID = 0L;

        public static final String TYPE = TestValidatorException.class.getCanonicalName();

        @Override
        public Error asError() {
            return Error.newBuilder()
                        .setType(TYPE)
                        .build();
        }

        @Override
        public Throwable asThrowable() {
            return this;
        }
    }

}
