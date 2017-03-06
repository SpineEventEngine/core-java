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
package org.spine3.server.outbus;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import org.spine3.base.MessageClass;
import org.spine3.base.MessageEnvelope;
import org.spine3.base.Response;
import org.spine3.base.Responses;
import org.spine3.server.bus.Bus;
import org.spine3.server.bus.MessageDispatcher;
import org.spine3.server.delivery.Delivery;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A base bus responsible for delivering the {@link org.spine3.base.Command command} output.
 *
 * <p>The typical output artifacts of the command processing are:
 *
 * <ul>
 *     <li>{@linkplain org.spine3.base.Event events} — in case the command is handled successfully;
 *     <li>{@linkplain org.spine3.base.Failure business failures} — if the command contradicts
 *          the business rules.
 * </ul>
 *
 * <p>The instances of {@code CommandOutputBus} are responsible for a delivery of such output
 * artifacts to the corresponding destinations.
 *
 * @author Alex Tymchenko
 */
public abstract class CommandOutputBus< M extends Message,
                                        E extends MessageEnvelope<M>,
                                        C extends MessageClass,
                                        D extends MessageDispatcher<C,E>> extends Bus<M, E, C, D> {

    /**
     * The strategy to deliver the messages to the dispatchers.
     */
    private final Delivery<E, D> delivery;

    protected CommandOutputBus(Delivery<E, D> delivery) {
        this.delivery = delivery;
    }

    protected abstract void store(M message);

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
    protected Delivery<E, D> delivery() {
        return this.delivery;
    }

    protected abstract E createEnvelope(M message);

    /**
     * @inheritDoc <p>Overrides for return type covariance.
     */
    @Override
    protected abstract OutputDispatcherRegistry<C, D> createRegistry();

    /**
     * Validates and posts the message for handling.
     *
     * <p>If the message is invalid, the {@code responseObserver} is notified of an error.
     *
     * <p>If the message is valid, it {@linkplain #store(Message) may be stored} in the associated
     * store before passing it to dispatchers.
     *
     * <p>The {@code responseObserver} is then notified of a successful acknowledgement of the
     * passed message.
     *
     * @param message          the message to be handled
     * @param responseObserver the observer to be notified
     * @see #validateMessage(Message, StreamObserver)
     */
    @Override
    public void post(M message, StreamObserver<Response> responseObserver) {
        checkNotNull(responseObserver);

        final boolean validationPassed = validateMessage(message, responseObserver);

        if (validationPassed) {
            responseObserver.onNext(Responses.ok());
            store(message);
            final M enriched = enrich(message);
            final int dispatchersCalled = callDispatchers(createEnvelope(enriched));

            if (dispatchersCalled == 0) {
                handleDeadMessage(createEnvelope(message), responseObserver);
            }
            responseObserver.onCompleted();
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
     * @inheritDoc <p>Overrides for return type covariance.
     */

    @Override
    protected OutputDispatcherRegistry<C, D> registry() {
        return (OutputDispatcherRegistry<C, D>) super.registry();
    }
}
