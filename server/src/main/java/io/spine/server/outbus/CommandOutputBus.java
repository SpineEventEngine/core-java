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
package io.spine.server.outbus;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Event;
import io.spine.base.Failure;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.envelope.MessageEnvelope;
import io.spine.server.bus.Bus;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.delivery.Delivery;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base bus responsible for delivering the {@link io.spine.base.Command command} output.
 *
 * <p>The typical output artifacts of the command processing are:
 *
 * <ul>
 *     <li>{@linkplain Event events} — in case the command is handled successfully;
 *     <li>{@linkplain Failure business failures} — if the command contradicts
 *          the business rules.
 * </ul>
 *
 * <p>The instances of {@code CommandOutputBus} are responsible for a delivery of such output
 * artifacts to the corresponding destinations.
 *
 * @author Alex Tymchenko
 */
@Internal
public abstract class CommandOutputBus<M extends Message,
                                       I,
                                       E extends MessageEnvelope<I, M>,
                                       C extends MessageClass,
                                       D extends MessageDispatcher<C,E>>
                extends Bus<M, I, E, C, D> {

    /**
     * The strategy to deliver the messages to the dispatchers.
     */
    private final CommandOutputDelivery<E, C, D> delivery;

    protected CommandOutputBus(CommandOutputDelivery<E, C, D> delivery) {
        super();
        this.delivery = delivery;
        injectDispatcherProvider();
    }

    /**
     * Sets up the {@code CommandOutputDelivery} with an ability to obtain
     * {@linkplain MessageDispatcher message dispatchers} by a given
     * {@linkplain MessageClass message class} instance at runtime.
     */
    private void injectDispatcherProvider() {
        delivery().setConsumerProvider(
                new Function<C, Set<D>>() {
                    @Nullable
                    @Override
                    public Set<D> apply(@Nullable C messageClass) {
                        checkNotNull(messageClass);
                        final Set<D> dispatchers =
                                registry().getDispatchers(messageClass);
                        return dispatchers;
                    }
                });
    }

    /**
     * Verifies that a message can be posted to this {@code CommandOutputBus}.
     *
     * <p>A command output can be posted if its message has either dispatcher or handler
     * registered with this {@code CommandOutputBus}.
     *
     * <p>The message also must satisfy validation constraints defined in its Protobuf type.
     *
     * @param message          the command output message to check
     * @param responseObserver the observer to obtain the result of the call;
     *                         {@link StreamObserver#onError(Throwable)} is called if
     *                         a message is unsupported or invalid
     * @return {@code true} if a message is supported and valid and can be posted,
     * {@code false} otherwise
     */
    public boolean validate(Message message, StreamObserver<Response> responseObserver) {
        if (!validateMessage(message, responseObserver)) {
            return false;
        }
        responseObserver.onNext(Responses.ok());
        responseObserver.onCompleted();
        return true;
    }

    /**
     * Validates the message and notifies the observer of those (if any).
     *
     * <p>Does not call {@link StreamObserver#onNext(Object) StreamObserver.onNext(..)} or
     * {@link StreamObserver#onCompleted() StreamObserver.onCompleted(..)}
     * for the given {@code responseObserver}.
     */
    protected abstract boolean validateMessage(Message message,
                                               StreamObserver<Response> responseObserver);

    /**
     * Enriches the message posted to this instance of {@code CommandOutputBus}.
     *
     * @param originalMessage the original message posted to the bus
     * @return the enriched message
     */
    protected abstract M enrich(M originalMessage);

    /**
     * Obtains the {@linkplain Delivery delivery strategy} configured for this bus.
     *
     * @return the delivery strategy
     */
    protected CommandOutputDelivery<E, C, D> delivery() {
        return this.delivery;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected abstract OutputDispatcherRegistry<C, D> createRegistry();

    @Override
    protected Iterable<M> filter(Iterable<M> messages, final StreamObserver<Response> responseObserver) {
        final Iterable<M> result = Iterables.filter(messages, new Predicate<M>() {
            @Override
            public boolean apply(@Nullable M message) {
                checkNotNull(message);
                return validateMessage(message, responseObserver);
            }
        });
        return result;
    }

    @Override
    protected void doPost(E envelope) {
        final M enriched = enrich(envelope.getOuterObject());
        final E enrichedParceledMessage = toEnvelope(enriched);
        final int dispatchersCalled = callDispatchers(enrichedParceledMessage);

        if (dispatchersCalled == 0) {
            handleDeadMessage(enrichedParceledMessage);
        }
    }

    /**
     * Call the dispatchers for the {@code eventEnvelope}.
     *
     * @param messageEnvelope the event envelope to pass to the dispatchers.
     * @return the number of the dispatchers called, or {@code 0} if there weren't any.
     */
    private int callDispatchers(E messageEnvelope) {
        @SuppressWarnings("unchecked")  // it's fine, since the message is validated previously.
        final C messageClass = (C) messageEnvelope.getMessageClass();
        final Collection<D> dispatchers = registry().getDispatchers(messageClass);
        delivery().deliver(messageEnvelope);
        return dispatchers.size();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides for return type covariance.
     */
    @Override
    protected OutputDispatcherRegistry<C, D> registry() {
        return (OutputDispatcherRegistry<C, D>) super.registry();
    }

    @Override
    public void close() throws Exception {
        registry().unregisterAll();
    }
}
