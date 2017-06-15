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

package io.spine.server.bus;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.base.Response;
import io.spine.base.Responses;
import io.spine.envelope.MessageEnvelope;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static io.spine.validate.Validate.isNotDefault;

/**
 * Abstract base for buses.
 *
 * @param <T> the type of outer objects (containing messages of interest) that are posted the bus
 * @param <E> the type of envelopes for outer objects used by this bus
 * @param <C> the type of message class
 * @param <D> the type of dispatches used by this bus
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public abstract class Bus<T extends Message,
                          E extends MessageEnvelope<T>,
                          C extends MessageClass,
                          D extends MessageDispatcher<C, E>> implements AutoCloseable {

    @Nullable
    private DispatcherRegistry<C, D> registry;

    /**
     * Registers the passed dispatcher.
     *
     * @param dispatcher the dispatcher to register
     * @throws IllegalArgumentException if the set of message classes
     *                                  {@linkplain MessageDispatcher#getMessageClasses() exposed}
     *                                  by the dispatcher is empty
     */
    public void register(D dispatcher) {
        registry().register(checkNotNull(dispatcher));
    }

    /**
     * Unregisters dispatching for message classes of the passed dispatcher.
     *
     * @param dispatcher the dispatcher to unregister
     */
    public void unregister(D dispatcher) {
        registry().unregister(checkNotNull(dispatcher));
    }

    /**
     * Posts the message to the bus.
     *
     * <p>Use the {@code Bus} class abstract methods to modify the behavior of posting.
     *
     * @param message          the message to post
     * @param responseObserver the observer to receive outcome of the operation
     */
    // Left non-final for testing purposes.
    public void post(T message, StreamObserver<Response> responseObserver) {
        checkNotNull(message);
        checkNotNull(responseObserver);
        checkArgument(isNotDefault(message));

        final Optional<T> filtered = filter(message, responseObserver);
        if (filtered.isPresent()) {
            final T validMessage = filtered.get();
            final E envelope = parcel(validMessage);
            store(message);
            responseObserver.onNext(Responses.ok());
            doPost(envelope);
        }
        responseObserver.onCompleted();
    }

    /**
     * Posts the given messages to the bus.
     *
     * <p>Use the {@code Bus} class abstract methods to modify the behavior of posting.
     *
     * @param messages         the message to post
     * @param responseObserver the observer to receive outcome of the operation
     */
    public final void post(Iterable<T> messages, StreamObserver<Response> responseObserver) {
        checkNotNull(messages);
        checkNotNull(responseObserver);

        final Iterable<T> filteredMessages = filter(messages, responseObserver);
        if (!isEmpty(filteredMessages)) {
            store(messages);
            final Iterable<E> envelopes = transform(filteredMessages, parcel());
            doPost(envelopes, responseObserver);
            responseObserver.onCompleted();
        }
    }

    /**
     * Handles the message, for which there is no dispatchers registered in the registry.
     *
     * @param message the message that has no target dispatchers, packed into an envelope
     */
    public abstract void handleDeadMessage(E message);

    /**
     * Obtains the dispatcher registry.
     */
    protected DispatcherRegistry<C, D> registry() {
        if (registry == null) {
            registry = createRegistry();
        }
        return registry;
    }

    /**
     * Factory method for creating an instance of the registry for
     * dispatchers of the bus.
     */
    protected abstract DispatcherRegistry<C, D> createRegistry();

    /**
     * Filters the given message.
     *
     * <p>The implementations may apply some validation to the passed message. If the validation is
     * passed, the {@link Optional#of Optional.of(message)} is returned; otherwise,
     * {@link Optional#absent() Optional.absent()} is returned and
     * {@link StreamObserver#onError StreamObserver.onError} may be called with the failure reasons.
     *
     * @param message          the message to filter
     * @param responseObserver the observer to receive the negative outcome of the operation
     * @return the message itself if it passes the filtering or
     *         {@link Optional#absent() Optional.absent()} otherwise
     */
    protected abstract Optional<T> filter(T message,
                                          StreamObserver<Response> responseObserver);

    /**
     * Packs the given message of type {@code T} into an envelope of type {@code E}.
     *
     * @param message the message to pack
     * @return new envelope with the given message inside
     */
    protected abstract E parcel(T message);

    /**
     * Posts the given envelope to the bus.
     *
     * <p>Finds and invokes the {@linkplain MessageDispatcher MessageDispatcher(s)} for the given
     * message.
     *
     * <p>This method assumes that the given massage has passed the filtering.
     *
     * @see #post(Message, StreamObserver) for the public API
     */
    protected abstract void doPost(E envelope);

    /**
     * Posts each of the given envelopes into the bus and acknowledges the message posing with
     * the {@code responseObserver}.
     *
     * @param envelopes        the envelopes to post
     * @param responseObserver the observer of the message posting
     */
    private void doPost(Iterable<E> envelopes, StreamObserver<Response> responseObserver) {
        for (E message : envelopes) {
            responseObserver.onNext(Responses.ok());
            doPost(message);
        }
    }

    /**
     * Persists the message.
     *
     * <p>The method may perform no action if it's specified by the implementer.
     *
     * @param message the message to store
     */
    protected abstract void store(T message);

    /**
     * Stores the given messages into the underlying storage.
     *
     * @param messages the messages to store
     */
    protected void store(Iterable<T> messages) {
        for (T message : messages) {
            store(message);
        }
    }

    private Iterable<T> filter(Iterable<T> messages,
                               final StreamObserver<Response> responseObserver) {
        final Iterable<T> filtered = Iterables.filter(messages, matchesFilter(responseObserver));
        return filtered;
    }

    private Predicate<T> matchesFilter(StreamObserver<Response> responseObserver) {
        return new MatchesFilter(responseObserver);
    }

    private Function<T, E> parcel() {
        return new MessageParceler();
    }

    /**
     * A predicate on the message checking if the given message matches the bus
     * {@linkplain #filter(Message, StreamObserver) filter} or not.
     */
    private class MatchesFilter implements Predicate<T> {

        private final StreamObserver<Response> responseObserver;

        private MatchesFilter(StreamObserver<Response> responseObserver) {
            this.responseObserver = responseObserver;
        }

        @Override
        public boolean apply(@Nullable T message) {
            checkNotNull(message);
            final Optional<T> optional = filter(message, responseObserver);
            return optional.isPresent();
        }
    }

    /**
     * A function creating the instances of {@link MessageEnvelope} from the given message.
     */
    private class MessageParceler implements Function<T, E> {

        @Override
        public E apply(@Nullable T message) {
            checkNotNull(message);
            final E result = parcel(message);
            return result;
        }
    }
}
