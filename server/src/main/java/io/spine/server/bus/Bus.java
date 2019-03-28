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

package io.spine.server.bus;

import com.google.errorprone.annotations.concurrent.LazyInit;
import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.validate.Validate.isNotDefault;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

/**
 * Abstract base for buses.
 *
 * @param <T> the type of outer objects (containing messages of interest) that are posted to the bus
 * @param <E> the type of envelopes for outer objects used by this bus
 * @param <C> the type of message class
 * @param <D> the type of dispatches used by this bus
 */
public abstract class Bus<T extends Message,
                          E extends MessageEnvelope<?, T, ?>,
                          C extends MessageClass<? extends Message>,
                          D extends MessageDispatcher<C, E, ?>>
        implements AutoCloseable {

    /** A queue of envelopes to post. */
    private @Nullable DispatchingQueue<E> queue;

    /** Dispatchers of messages by their class. */
    @LazyInit
    private @MonotonicNonNull DispatcherRegistry<C, E, D> registry;

    /** The supplier of filter chain for this bus. */
    private final FilterChainSupplier chainSupplier;

    protected Bus(BusBuilder<E, T, ?> builder) {
        super();
        this.chainSupplier = new FilterChainSupplier(builder);
    }

    /**
     * Registers the passed dispatcher.
     *
     * @param dispatcher the dispatcher to register
     * @throws IllegalArgumentException
     *         if the set of message classes {@linkplain MessageDispatcher#messageClasses()
     *         exposed} by the dispatcher is empty
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
     * @param message  the message to post
     * @param observer the observer to receive outcome of the operation
     * @see #post(Iterable, StreamObserver) for posing multiple messages at once
     */
    public final void post(T message, StreamObserver<Ack> observer) {
        checkNotNull(message);
        checkNotNull(observer);
        checkArgument(isNotDefault(message));

        post(singleton(message), observer);
    }

    /**
     * Posts the given messages to the bus.
     *
     * <p>The {@linkplain StreamObserver observer} serves to notify the consumer about the result
     * of the call. The {@link StreamObserver#onNext StreamObserver.onNext()} is called for each
     * message posted to the bus.
     *
     * <p>In case the message is accepted by the bus, {@link Ack} with the
     * {@link io.spine.core.Status.StatusCase#OK OK} status is passed to the observer.
     *
     * <p>If the message cannot be sent due to some issues, a corresponding
     * {@link io.spine.base.Error Error} status is passed in {@code Ack} instance.
     *
     * <p>Depending on the underlying {@link MessageDispatcher}, a message which causes a business
     * {@linkplain io.spine.base.ThrowableMessage rejection} may result either a rejection status or
     * an {@link io.spine.core.Status.StatusCase#OK OK} status {@link Ack} instance. Usually,
     * the rejection status may only pop up if the {@link MessageDispatcher} processes the message
     * sequentially and throws the rejection (wrapped in a
     * the {@linkplain io.spine.base.ThrowableMessage ThrowableMessages}) instead of handling them.
     * Otherwise, the {@code OK} status should be expected.
     *
     * <p>Note that {@linkplain StreamObserver#onError StreamObserver.onError()} is never called
     * for the passed observer, since errors are propagated as statuses of {@code Ack} response.
     *
     * @param messages the messages to post
     * @param observer the observer to receive outcome of the operation
     */
    public final void post(Iterable<T> messages, StreamObserver<Ack> observer) {
        checkNotNull(messages);
        checkNotNull(observer);

        StreamObserver<Ack> wrappedObserver = prepareObserver(messages, observer);
        filterAndPost(messages, wrappedObserver);
    }

    private void filterAndPost(Iterable<T> messages, StreamObserver<Ack> observer) {
        Map<T, E> filteredMessages = filter(messages, observer);
        if (!filteredMessages.isEmpty()) {
            store(filteredMessages.keySet());
            Iterable<E> envelopes = filteredMessages.values();
            doPost(envelopes, observer);
        }
        observer.onCompleted();
    }

    /**
     * Prepares the given {@link StreamObserver} in order to post messages into this bus.
     *
     * <p>This method is an extension point of a {@code Bus}.
     *
     * <p>When {@linkplain #post(Iterable, StreamObserver) posting} messages into the bus,
     * the message {@linkplain Ack acknowledgements} are passed to the observer created by this
     * method.
     *
     * <p>Conventionally, the resulting {@link StreamObserver} should delegate calls to
     * the {@code source} observer, so that the caller receives the posting outcome. If violating
     * this convention, the {@code Bus} implementation should specify the altered behavior
     * explicitly.
     *
     * <p>The {@code messages} can be used in order to construct the observer. The resulting
     * observer is used only for acknowledgment of the given messages.
     *
     * <p>By default, this method returns the {@code source} observer. See {@code Bus} subclasses
     * for the altered behavior specification.
     *
     * @param messages the messages to create an observer for
     * @param source   the source {@link StreamObserver} to be transformed
     * @return a transformed observer of {@link Ack} streams
     */
    protected StreamObserver<Ack> prepareObserver(Iterable<T> messages,
                                                  StreamObserver<Ack> source) {
        return source;
    }

    /**
     * Closes the {@linkplain BusFilter filters} of this bus and unregisters all the dispatchers.
     *
     * @throws Exception if either filters or the {@linkplain DispatcherRegistry} throws
     *         an exception
     */
    @Override
    public void close() throws Exception {
        filterChain().close();
        registry().unregisterAll();
    }

    /**
     * Obtains the instance of {@link DeadMessageHandler} for this bus.
     */
    protected abstract DeadMessageHandler<E> deadMessageHandler();

    /**
     * Obtains the instance of {@link EnvelopeValidator} for this bus.
     */
    protected abstract EnvelopeValidator<E> validator();

    /**
     * Obtains the dispatcher registry.
     */
    protected DispatcherRegistry<C, E, D> registry() {
        if (registry == null) {
            registry = createRegistry();
        }
        return registry;
    }

    /**
     * Returns the filter chain for this bus.
     */
    private BusFilter<E> filterChain() {
        return chainSupplier.get();
    }

    /**
     * Obtains the {@link BusFilter}s to append to the chain tail.
     *
     * <p>By default, returns an empty collection.
     *
     * @see #filterChain()
     */
    protected Collection<BusFilter<E>> filterChainTail() {
        return emptyList();
    }

    /**
     * Obtains the {@link BusFilter}s to prepend to the chain head.
     *
     * <p>By default, returns an empty collection.
     *
     * @see #filterChain()
     */
    protected Collection<BusFilter<E>> filterChainHead() {
        return emptyList();
    }

    /**
     * Obtains the queue of the envelopes.
     *
     * <p>Posted envelopes are organized into a queue to maintain the order of dispatching.
     *
     * @see DispatchingQueue
     */
    private DispatchingQueue<E> queue() {
        if (queue == null) {
            queue = new DispatchingQueue<>(this::dispatch);
        }
        return queue;
    }

    /**
     * Factory method for creating an instance of the registry for dispatchers of the bus.
     */
    protected abstract DispatcherRegistry<C, E, D> createRegistry();

    /**
     * Filters the given messages.
     *
     * <p>Each message goes through the filter chain, specific to the {@code Bus} implementation.
     *
     * <p>If a message passes the filtering, it is included into the resulting {@link Iterable};
     * otherwise, {@linkplain StreamObserver#onNext StreamObserver.onNext()} is called for that
     * message.
     *
     * <p>Any filter in the filter chain may process the message by itself. In this case an observer
     * is notified by the filter directly.
     *
     * @param messages the message to filter
     * @param observer the observer to receive the negative outcome of the operation
     * @return a map of filtered messages where keys are messages, and values are envelopes with
     *         these messages
     * @implNote This method returns a map to avoid repeated creation of envelopes when dispatching.
     * Messages in the returned map come in the same order as in the incoming sequence.
     */
    private Map<T, E> filter(Iterable<T> messages, StreamObserver<Ack> observer) {
        checkNotNull(messages);
        checkNotNull(observer);
        Map<T, E> result = new LinkedHashMap<>();
        for (T message : messages) {
            E envelope = toEnvelope(message);
            Optional<Ack> response = filter(envelope);
            if (response.isPresent()) {
                observer.onNext(response.get());
            } else {
                result.put(message, envelope);
            }
        }
        return result;
    }

    /**
     * Feeds the given message to the bus filters.
     *
     * <p>If the given message is completely processed and should not be passed to the dispatchers,
     * the returned {@link Optional} contains a value with either status.
     *
     * <p>If the message should be passed to the dispatchers, the result of this method is
     * {@link Optional#empty()}.
     *
     * @param message the {@linkplain MessageEnvelope message envelope} to pre-process
     * @return the result of message processing by this bus if any, or
     * {@link Optional#empty()} otherwise
     */
    private Optional<Ack> filter(E message) {
        Optional<Ack> filterOutput = filterChain().accept(message);
        return filterOutput;
    }

    /**
     * Packs the given message of type {@code T} into an envelope of type {@code E}.
     *
     * @param message the message to pack
     * @return new envelope with the given message inside
     */
    protected abstract E toEnvelope(T message);

    /**
     * Passes the given envelope for dispatching.
     *
     * <p>Finds and invokes the {@linkplain MessageDispatcher MessageDispatcher(s)} for the given
     * message.
     *
     * <p>This method assumes that the given message has passed the filtering.
     *
     * @see #post(Message, StreamObserver) for the public API
     */
    protected abstract void dispatch(E envelope);

    /**
     * Posts each of the given envelopes into the bus and notifies the given observer.
     *
     * @param envelopes the envelopes to post
     * @param observer  the observer to be notified of the operation result
     * @see #dispatch(MessageEnvelope)
     */
    private void doPost(Iterable<E> envelopes, StreamObserver<Ack> observer) {
        for (E message : envelopes) {
            queue().add(message, observer);
        }
    }

    /**
     * Stores the given messages into the underlying storage.
     *
     * @param messages the messages to store
     */
    protected abstract void store(Iterable<T> messages);

    /**
     * Initializes the filter chain upon the first invocation and returns the initialized instance
     * for all next calls.
     *
     * <p>Adds the {@link DeadMessageFilter} and the {@link ValidatingFilter} to the chain, so that
     * a chain always has the following format:
     *
     * <pre>
     *     Chain head -> {@link ValidatingFilter} -> {@link DeadMessageFilter} -> custom filters from {@linkplain BusBuilder Builder} -> chain tail.
     * </pre>
     *
     * <p>The head and the tail of the chain are created by the {@code Bus} itself. Those are
     * typically empty. Override {@link #filterChainHead()} and {@link #filterChainHead()} to add
     * some filters to the respective chain side.
     */
    private class FilterChainSupplier implements Supplier<FilterChain<E>> {

        private final ChainBuilder<E> chainBuilder;

        @LazyInit
        private @MonotonicNonNull FilterChain<E> chain;

        private FilterChainSupplier(BusBuilder<E, T, ?> builder) {
            this.chainBuilder = builder.chainBuilderCopy();
        }

        @Override
        public FilterChain<E> get() {
            if(chain == null) {
                chain = buildChain();
            }
            return chain;
        }
        private FilterChain<E> buildChain() {
            Collection<BusFilter<E>> tail = filterChainTail();
            tail.forEach(chainBuilder::append);
            BusFilter<E> deadMsgFilter = new DeadMessageFilter<>(deadMessageHandler(), registry());
            BusFilter<E> validatingFilter = new ValidatingFilter<>(validator());

            chainBuilder.prepend(deadMsgFilter);
            chainBuilder.prepend(validatingFilter);
            Collection<BusFilter<E>> head = filterChainHead();
            head.forEach(chainBuilder::prepend);

            return chainBuilder.build();
        }
    }
}
