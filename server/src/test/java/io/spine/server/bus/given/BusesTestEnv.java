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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.Identifier;
import io.spine.base.Error;
import io.spine.core.AbstractMessageEnvelope;
import io.spine.core.Ack;
import io.spine.core.EventContext;
import io.spine.core.MessageInvalid;
import io.spine.core.MessageRejection;
import io.spine.core.Status;
import io.spine.server.bus.AbstractBusFilter;
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

import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Queues.newArrayDeque;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.AnyPacker.unpack;
import static io.spine.server.bus.Buses.acknowledge;
import static io.spine.server.bus.Buses.reject;
import static io.spine.server.bus.given.BusesTestEnv.TestMessageClass.TEST_MESSAGE_CONTENTS_CLASS;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * @author Mykhailo Drachuk
 */
public class BusesTestEnv {

    public static final Status STATUS_OK = statusOk();

    private BusesTestEnv() {
        // Prevents instantiation.
    }

    public static TestMessageBus.Builder busBuilder() {
        return TestMessageBus.newBuilder();
    }

    public static TestMessageContents testContents() {
        return TestMessageContents.newBuilder()
                                  .build();
    }

    public static TestMessageContents testContents(boolean flag) {
        return TestMessageContents.newBuilder()
                                  .setFlag(flag)
                                  .build();
    }

    public static BusMessage busMessage(Message message) {
        return ((BusMessage.Builder) Sample.builderForType(BusMessage.class))
                .setContents(pack(message))
                .build();
    }

    public static String errorType(Ack response) {
        return response.getStatus()
                       .getError()
                       .getType();
    }

    private static Error error(String type) {
        return Error.newBuilder()
                    .setType(type)
                    .build();
    }

    private static Status statusOk() {
        return Status.newBuilder()
                     .setOk(Empty.getDefaultInstance())
                     .build();
    }

    /**
     * A valid message bus that stores the messages to a list.
     */
    public static class TestMessageBus extends Bus<BusMessage, TestEnvelope, TestMessageClass, TestMessageDispatcher> {

        private final EnvelopeValidator<TestEnvelope> validator;
        private final List<BusFilter<TestEnvelope>> filters;
        private final List<BusMessage> storedMessages;

        private TestMessageBus(Builder builder) {
            storedMessages = newArrayList();
            filters = builder.filters;
            validator = builder.validator;
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
            return newArrayDeque(filters);
        }

        @Override
        protected TestEnvelope toEnvelope(BusMessage message) {
            return new TestEnvelope(message);
        }

        @Override
        protected Ack doPost(TestEnvelope envelope) {
            final Any packedId = pack(envelope.getId());
            final Ack result = acknowledge(packedId);
            return result;
        }

        @Override
        protected void store(Iterable<BusMessage> messages) {
            Iterables.addAll(storedMessages, messages);
        }

        public List<BusMessage> storedMessages() {
            return copyOf(storedMessages);
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * A {@link TestMessageBus} builder that returns a a bus instance that successfully dispatches messages by default.
         * This default behavior can be modified to make the bus err using builder settings.
         */
        public static class Builder {
            private final List<BusFilter<TestEnvelope>> filters;
            private EnvelopeValidator<TestEnvelope> validator;
            private boolean addDefaultDispatcher;

            private Builder() {
                validator = new Validators.PassingValidator();
                filters = newArrayList();
                addDefaultDispatcher = true;
            }

            public Builder failingValidation() {
                validator = new Validators.FailingValidator();
                return this;
            }

            public Builder withNoDispatchers() {
                addDefaultDispatcher = false;
                return this;
            }

            public Builder addFilter(BusFilter<TestEnvelope> filter) {
                filters.add(filter);
                return this;
            }

            public TestMessageBus build() {
                final TestMessageBus bus = new TestMessageBus(this);
                if (addDefaultDispatcher) {
                    bus.register(new TestMessageContentsDispatcher());
                }
                return bus;
            }
        }

        private static class TestDeadMessageTap implements DeadMessageTap<TestEnvelope> {
            @Override
            public MessageUnhandled capture(TestEnvelope message) {
                return new Exceptions.DeadMessageException();
            }
        }

    }

    public static class Filters {

        private Filters() {
            // Prevents instantiation.
        }

        /**
         * A filter which always rejects the message with a {@link Exceptions.FailingFilterException}.
         */
        public static class FailingFilter extends AbstractBusFilter<TestEnvelope> {

            @Override
            public Optional<Ack> accept(TestEnvelope envelope) {
                final MessageRejection exception = new Exceptions.FailingFilterException();
                final Any packedId = Identifier.pack(envelope.getId());
                final Ack result = reject(packedId, exception.asError());
                return Optional.of(result);
            }
        }

        /**
         * A filter which always passes successfully.
         */
        public static class PassingFilter extends AbstractBusFilter<TestEnvelope> {

            private boolean passed = false;

            @Override
            public Optional<Ack> accept(TestEnvelope envelope) {
                passed = true;
                return Optional.absent();
            }

            public boolean passed() {
                return passed;
            }
        }

        /**
         * A filter which rejects only {@link TestMessageContents} messages with
         * {@link TestMessageContents#getFlag()} set to {@code false} with a
         * {@link Exceptions.FailingFilterException}.
         */
        public static class ContentsFlagFilter extends AbstractBusFilter<TestEnvelope> {

            @Override
            public Optional<Ack> accept(TestEnvelope envelope) {
                final Message message = envelope.getMessage();
                if (!TEST_MESSAGE_CONTENTS_CLASS.equals(TestMessageClass.of(message))) {
                    return Optional.absent();
                }
                final TestMessageContents contents = (TestMessageContents) message;
                if (contents.getFlag()) {
                    return Optional.absent();
                }
                final MessageRejection exception = new Exceptions.FailingFilterException();
                final Any packedId = Identifier.pack(envelope.getId());
                final Ack result = reject(packedId, exception.asError());
                return Optional.of(result);
            }
        }
    }

    public static class Exceptions {

        private Exceptions() {
            // Prevents instantiation.
        }

        public enum ErrorType {
            DEAD_MESSAGE,
            FAILING_VALIDATION,
            FAILING_FILTER
        }

        /**
         * An exception returned by a {@link TestMessageBus.TestDeadMessageTap} in case the message is dead.
         */
        public static class DeadMessageException extends RuntimeException implements MessageUnhandled {

            private static final long serialVersionUID = 0L;

            @Override
            public Error asError() {
                return error(ErrorType.DEAD_MESSAGE.toString());
            }

            @Override
            public Throwable asThrowable() {
                return this;
            }
        }

        /**
         * An exception for {@link Validators.FailingValidator}.
         */
        public static class FailingValidationException extends RuntimeException implements MessageInvalid {

            private static final long serialVersionUID = 0L;

            @Override
            public Error asError() {
                return error(ErrorType.FAILING_VALIDATION.toString());
            }

            @Override
            public Throwable asThrowable() {
                return this;
            }
        }

        /**
         * An exception for {@link Filters.FailingFilter}.
         */
        public static class FailingFilterException extends RuntimeException implements MessageInvalid {

            private static final long serialVersionUID = 0L;

            @Override
            public Error asError() {
                return error(ErrorType.FAILING_FILTER.toString());
            }

            @Override
            public Throwable asThrowable() {
                return this;
            }
        }
    }

    static class Validators {

        private Validators() {
            // Prevents instantiation.
        }

        /**
         * A validator which always passes validation returning an {@link Optional#absent()}.
         */
        public static class PassingValidator implements EnvelopeValidator<TestEnvelope> {

            @Override
            public Optional<MessageInvalid> validate(TestEnvelope envelope) {
                return Optional.absent();
            }
        }

        /**
         * A validator which always fails validation returning an {@link Optional} with {@link Exceptions.FailingValidationException}.
         */
        public static class FailingValidator implements EnvelopeValidator<TestEnvelope> {

            @Override
            public Optional<MessageInvalid> validate(TestEnvelope envelope) {
                final MessageInvalid error = new Exceptions.FailingValidationException();
                return Optional.of(error);
            }
        }
    }

    public static class TestMessageContentsDispatcher implements TestMessageDispatcher {

        @Override
        public Set<TestMessageClass> getMessageClasses() {
            return TestMessageClass.setOf(TestMessageContents.class);
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

    static class TestMessageClass extends MessageClass {
        static final TestMessageClass TEST_MESSAGE_CONTENTS_CLASS = of(TestMessageContents.class);

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

        public static Set<TestMessageClass> setOf(Iterable<Class<? extends Message>> classes) {
            final ImmutableSet.Builder<TestMessageClass> builder = ImmutableSet.builder();
            for (Class<? extends Message> cls : classes) {
                builder.add(of(cls));
            }
            return builder.build();
        }

        @SafeVarargs
        public static Set<TestMessageClass> setOf(Class<? extends Message>... classes) {
            return setOf(Arrays.asList(classes));
        }
    }

    interface TestMessageDispatcher
            extends MessageDispatcher<TestMessageClass, TestEnvelope, BusMessageId> {
    }
}
