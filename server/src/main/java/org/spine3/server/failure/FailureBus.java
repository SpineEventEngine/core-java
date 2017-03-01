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
package org.spine3.server.failure;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.Failure;
import org.spine3.base.Response;
import org.spine3.server.bus.Bus;
import org.spine3.server.bus.DispatcherRegistry;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.validate.Validate.isNotDefault;

/**
 * Dispatches the business failures that occur during the command processing
 * to the corresponding subscriber.
 *
 * @author Alexander Yevsyuov
 * @author Alex Tymchenko
 * @see org.spine3.base.FailureThrowable
 * @see org.spine3.server.event.Subscribe Subscribe @Subscribe
 */
public class FailureBus extends Bus<Failure, FailureEnvelope, FailureClass, FailureDispatcher> {

    /**
     * Creates a new instance according to the pre-configured {@code Builder}.
     */
    private FailureBus(Builder builder) {
        //TODO:3/1/17:alex.tymchenko: initialize FailureStore via the Builder.
    }

    /**
     * Creates a new builder for the {@code FailureBus}.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public void post(Failure failure, StreamObserver<Response> responseObserver) {
        checkNotNull(failure);
        checkNotNull(responseObserver);
        checkArgument(isNotDefault(failure));

        final FailureEnvelope failureEnvelope = FailureEnvelope.of(failure);
        final FailureClass failureClass = failureEnvelope.getFailureClass();

        final Set<FailureDispatcher> dispatchers = registry().getDispatchers(failureClass);

        for (FailureDispatcher dispatcher : dispatchers) {
            dispatcher.dispatch(failureEnvelope);
        }
        final int dispatchersCalled = dispatchers.size();
        if (dispatchersCalled == 0) {
            handleDeadMessage(failureEnvelope, responseObserver);
        }
    }

    @Override
    protected DispatcherRegistry<FailureClass, FailureDispatcher> createRegistry() {
        return new FailureDispatcherRegistry();
    }

    @Override
    public void close() throws Exception {
        registry().unregisterAll();
    }

    @Override
    public void handleDeadMessage(FailureEnvelope message,
                                  StreamObserver<Response> responseObserver) {
        log().warn("No dispatcher defined for the failure class {}", message.getFailureClass());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected FailureDispatcherRegistry registry() {
        return (FailureDispatcherRegistry) super.registry();
    }

    /** The {@code Builder} for {@code FailureBus}. */
    public static class Builder {

        public FailureBus build() {
            return new FailureBus(this);
        }
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FailureBus.class);
    }

    static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
