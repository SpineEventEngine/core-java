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
import com.google.common.base.Optional;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.Internal;
import io.spine.base.Error;
import io.spine.base.Event;
import io.spine.base.Failure;
import io.spine.base.MessageAcked;
import io.spine.base.Status;
import io.spine.envelope.MessageWithIdEnvelope;
import io.spine.server.bus.Bus;
import io.spine.server.bus.MessageDispatcher;
import io.spine.server.delivery.Delivery;
import io.spine.type.MessageClass;
import io.spine.util.Exceptions;

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
                                       E extends MessageWithIdEnvelope<?, M>,
                                       C extends MessageClass,
                                       D extends MessageDispatcher<C,E>>
                extends Bus<M, E, C, D> {

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
     *                         // TODO:2017-06-20:dmytro.dashenkov: Update Javadoc.
     * @return {@code true} if a message is supported and valid and can be posted,
     * {@code false} otherwise
     */
    public final Optional<Throwable> validate(Message event) {
        final Optional<Throwable> validationResult = validateMessage(event);
        return validationResult;
    }

    /**
     * Validates the message and notifies the observer of those (if any).
     * // TODO:2017-06-20:dmytro.dashenkov: Update Javadoc.
     * <p>Does not call {@link StreamObserver#onNext(Object) StreamObserver.onNext(..)} or
     * {@link StreamObserver#onCompleted() StreamObserver.onCompleted(..)}
     * for the given {@code acknowledgement} observer.
     */
    protected abstract Optional<Throwable> validateMessage(Message event);

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
    protected Optional<MessageAcked> preProcess(final E message) {
        final Optional<Throwable> violation = validateMessage(message.getMessage());
        final Optional<MessageAcked> result = violation.transform(
                new Function<Throwable, MessageAcked>() {
                    @Override
                    public MessageAcked apply(@Nullable Throwable input) {
                        checkNotNull(input);
                        final Error error = Exceptions.toError(input);
                        final Status status = Status.newBuilder()
                                                    .setError(error)
                                                    .build();
                        return message.acknowledge(status);
                    }
                }
        );
        return result;
    }

    @Override
    protected MessageAcked doPost(E envelope) {
        final M enriched = enrich(envelope.getOuterObject());
        final E enrichedEnvelope = toEnvelope(enriched);
        final int dispatchersCalled = callDispatchers(enrichedEnvelope);

        if (dispatchersCalled == 0) {
            handleDeadMessage(enrichedEnvelope);
        }
        final MessageAcked acknowledgement = envelope.acknowledge();
        return acknowledgement;
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
