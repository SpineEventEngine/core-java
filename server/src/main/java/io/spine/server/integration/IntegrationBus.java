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
package io.spine.server.integration;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.Identifier;
import io.spine.core.Ack;
import io.spine.core.BoundedContextId;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.core.MessageInvalid;
import io.spine.core.Rejection;
import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.grpc.StreamObservers;
import io.spine.protobuf.AnyPacker;
import io.spine.server.bus.Bus;
import io.spine.server.bus.BusFilter;
import io.spine.server.bus.DeadMessageTap;
import io.spine.server.bus.DispatcherRegistry;
import io.spine.server.bus.EnvelopeValidator;
import io.spine.server.bus.MulticastBus;
import io.spine.server.delivery.MulticastDelivery;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventSubscriber;
import io.spine.server.integration.TransportFactory.Publisher;
import io.spine.server.integration.TransportFactory.PublisherHub;
import io.spine.server.integration.TransportFactory.Subscriber;
import io.spine.server.integration.TransportFactory.SubscriberHub;
import io.spine.server.integration.local.LocalTransportFactory;
import io.spine.server.rejection.RejectionBus;
import io.spine.server.rejection.RejectionDispatcher;
import io.spine.server.rejection.RejectionSubscriber;
import io.spine.type.KnownTypes;
import io.spine.type.MessageClass;
import io.spine.type.TypeUrl;
import io.spine.util.Exceptions;

import javax.annotation.Nullable;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newHashSet;
import static io.spine.core.EventClass.asEventClass;
import static io.spine.core.RejectionClass.asRejectionClass;
import static io.spine.server.bus.Buses.acknowledge;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validate.Validate.checkNotDefault;
import static java.lang.String.format;

/**
 * Dispatches external messages from and to the current bounded context.
 *
 * @author Alex Tymchenko
 */
public class IntegrationBus extends MulticastBus<Message,
                                                 ExternalMessageEnvelope,
                                                 MessageClass,
                                                 ExternalMessageDispatcher<?>> {

    /**
     * Buses that act inside the bounded context, e.g. {@code EventBus}, and which allow
     * dispatching their events to other bounded contexts.
     *
     * <p>{@code CommandBus} does <em>not</em> allow such a dispatching, as commands cannot be
     * sent to another bounded context for a postponed handling.
     */
    private final EventBus eventBus;
    private final RejectionBus rejectionBus;
    private final BoundedContextId boundedContextId;

    private final SubscriberHub subscriberHub;
    private final PublisherHub publisherHub;

    private IntegrationBus(Builder builder) {
        super(builder.getDelivery());
        this.eventBus = builder.eventBus;
        this.rejectionBus = builder.rejectionBus;
        this.boundedContextId = builder.boundedContextId;
        this.subscriberHub = new SubscriberHub(builder.transportFactory);
        this.publisherHub = new PublisherHub(builder.transportFactory);

        /*
         * React upon {@code RequestedMessageTypes} message arrival.
         */
        subscriberHub.get(IntegrationMessageClass.of(RequestedMessageTypes.class))
                     .addObserver(new ConfigurationChangeObserver());
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    protected LocalDispatcherRegistry createRegistry() {
        return new LocalDispatcherRegistry();
    }

    @Override
    protected DeadMessageTap<ExternalMessageEnvelope> getDeadMessageHandler() {
        return DeadExternalMessageTap.INSTANCE;
    }

    @Override
    protected EnvelopeValidator<ExternalMessageEnvelope> getValidator() {
        return LocalValidator.INSTANCE;
    }

    @Override
    protected Deque<BusFilter<ExternalMessageEnvelope>> createFilterChain() {
        return newLinkedList();
    }

    @Override
    protected ExternalMessageEnvelope toEnvelope(Message message) {
        if (message instanceof Event) {
            return ExternalMessageEnvelope.of((Event) message);
        }
        throw Exceptions.newIllegalArgumentException("The message of %s type isn't supported",
                                                     message.getClass());
    }

    @Override
    protected Ack doPost(ExternalMessageEnvelope envelope) {
        final ExternalMessageEnvelope markedEnvelope = markExternal(envelope);
        final int dispatchersCalled = callDispatchers(markedEnvelope);

        final Any packedId = Identifier.pack(markedEnvelope.getId());
        checkState(dispatchersCalled != 0,
                   format("External message %s has no local dispatchers.",
                          markedEnvelope.getMessage()));
        final Ack result = acknowledge(packedId);
        return result;
    }

    private static ExternalMessageEnvelope markExternal(ExternalMessageEnvelope envelope) {
        final Event event = (Event) envelope.getOuterObject();
        final Event.Builder eventBuilder = event.toBuilder();
        final EventContext modifiedContext = eventBuilder
                .getContext()
                .toBuilder()
                .setExternal(true)
                .build();

        final Event marked = eventBuilder.setContext(modifiedContext)
                                         .build();
        return ExternalMessageEnvelope.of(marked);
    }

    @Override
    protected void store(Iterable<Message> messages) {
        // we don't store the incoming messages yet.
    }

    /**
     * Register a local dispatcher, which is subscribed to {@code external} messages.
     *
     * @param dispatcher the dispatcher to register
     */
    @Override
    public void register(ExternalMessageDispatcher<?> dispatcher) {
        super.register(dispatcher);

        // Remember the message types, that we have been subscribed before.
        final Set<IntegrationMessageClass> requestedBefore = subscriberHub.keys();

        // Subscribe to incoming messages of requested types.
        subscribeToIncoming(dispatcher);

        final Set<IntegrationMessageClass> currentlyRequested = subscriberHub.keys();
        if (!currentlyRequested.equals(requestedBefore)) {
            // Notify others that the requested message set has been changed.

            final RequestedMessageTypes.Builder resultBuilder = RequestedMessageTypes.newBuilder();
            for (IntegrationMessageClass messageClass : currentlyRequested) {
                final TypeUrl typeUrl = KnownTypes.getTypeUrl(messageClass.getClassName());
                resultBuilder.addTypeUrls(typeUrl.value());
            }
            final RequestedMessageTypes result = resultBuilder.build();
            final IntegrationMessage integrationMessage = IntegrationMessages.of(result);
            final IntegrationMessageClass channelId = IntegrationMessageClass.of(result.getClass());
            publisherHub.get(channelId)
                        .publish(newId(), integrationMessage);
        }
    }

    /**
     * Registers the passed event subscriber as an external event dispatcher
     * by taking only external subscriptions into account.
     *
     * @param eventSubscriber the subscriber to register.
     */
    public void register(final EventSubscriber eventSubscriber) {
        final ExternalEventSubscriber wrapped = new ExternalEventSubscriber(eventSubscriber);
        register(wrapped);
    }

    private void subscribeToIncoming(ExternalMessageDispatcher<?> dispatcher) {
        final Set<MessageClass> messageClasses = dispatcher.getMessageClasses();

        final IntegrationBus integrationBus = this;
        final Iterable<IntegrationMessageClass> transformed = transform(
                messageClasses, new Function<MessageClass, IntegrationMessageClass>() {
                    @Override
                    public IntegrationMessageClass apply(@Nullable MessageClass input) {
                        checkNotNull(input);
                        return IntegrationMessageClass.of(input);
                    }
                });
        for (final IntegrationMessageClass imClass : transformed) {
            final Subscriber subscriber = subscriberHub.get(imClass);
            subscriber.addObserver(new ChannelObserver<IntegrationMessage>(imClass.value()) {
                @Override
                public void onNext(IntegrationMessage value) {
                    final Message unpackedMessage = AnyPacker.unpack(value.getOriginalMessage());
                    integrationBus.post(unpackedMessage, StreamObservers.<Ack>noOpObserver());
                }
            });
        }
    }

    private static Any newId() {
        final StringValue stringId = StringValue.newBuilder()
                                                .setValue(Identifier.newUuid())
                                                .build();
        final Any result = AnyPacker.pack(stringId);
        return result;
    }

    @Override
    public String toString() {
        return "Integration bus of BoundedContext ID = " + boundedContextId.getValue();
    }

    private static <M extends MessageClass>
    Iterable<M> asMessageClasses(Iterable<String> rawTypeUrls,
                                 final Function<Class<? extends Message>, M> transformFn) {
        return transform(rawTypeUrls,
                         new Function<String, M>() {
                             @Override
                             public M apply(@Nullable String typeUrlString) {
                                 checkNotNull(typeUrlString);

                                 final TypeUrl typeUrl = TypeUrl.parse(typeUrlString);
                                 final Class<Message> javaClass = typeUrl.getJavaClass();
                                 final M result = transformFn.apply(javaClass);
                                 return result;
                             }
                         });
    }

    /**
     * Delivers the messages from external sources to the local subscribers
     * of {@code external} messages in this bounded context.
     */
    static class LocalDelivery extends MulticastDelivery<ExternalMessageEnvelope,
            MessageClass,
            ExternalMessageDispatcher<?>> {

        @Override
        protected boolean shouldPostponeDelivery(ExternalMessageEnvelope deliverable,
                                                 ExternalMessageDispatcher<?> consumer) {
            return false;
        }

        @Override
        protected Runnable getDeliveryAction(final ExternalMessageDispatcher<?> consumer,
                                             final ExternalMessageEnvelope deliverable) {
            return new Runnable() {
                @Override
                public void run() {
                    consumer.dispatch(deliverable);
                }
            };
        }
    }

    /**
     * A validator of the incoming external messages to use in {@code IntegrationBus}.
     */
    private enum LocalValidator implements EnvelopeValidator<ExternalMessageEnvelope> {
        INSTANCE;

        @Override
        public Optional<MessageInvalid> validate(ExternalMessageEnvelope envelope) {
            return Optional.absent();
        }
    }

    /**
     * A registry of subscribers which {@linkplain io.spine.core.Subscribe#external() subscribe}
     * to handle external messages.
     */
    private static class LocalDispatcherRegistry
            extends DispatcherRegistry<MessageClass, ExternalMessageDispatcher<?>> {
        @Override
        protected void checkDispatcher(ExternalMessageDispatcher dispatcher)
                throws IllegalArgumentException {
            // Do not call `super()`, as long as we don't want to enforce
            // non-empty message class set for an external message dispatcher.
            checkNotNull(dispatcher);
        }
    }

    /**
     * Produces an {@link UnsupportedExternalMessageException} upon capturing an external message,
     * which has no targets to be dispatched to.
     */
    private enum DeadExternalMessageTap implements DeadMessageTap<ExternalMessageEnvelope> {
        INSTANCE;

        @Override
        public UnsupportedExternalMessageException capture(ExternalMessageEnvelope envelope) {
            final Message message = envelope.getMessage();
            final UnsupportedExternalMessageException exception =
                    new UnsupportedExternalMessageException(message);
            return exception;
        }
    }

    /**
     * Base routines for the {@linkplain io.spine.server.integration.TransportFactory.MessageChannel
     * message channel} observers.
     *
     * @param <M> the type of the message the observer is receiving updates on
     */
    private abstract static class ChannelObserver<M> implements StreamObserver<M> {

        private final Class<? extends Message> messageClass;

        protected ChannelObserver(Class<? extends Message> messageClass) {
            this.messageClass = messageClass;
        }

        @Override
        public void onError(Throwable t) {
            throw newIllegalStateException("Error caught when observing the incoming " +
                                                   "messages of type %s", messageClass);

        }

        @Override
        public void onCompleted() {
            throw newIllegalStateException("Unexpected 'onCompleted' when observing " +
                                                   "the incoming messages of type %s",
                                           messageClass);
        }
    }

    /**
     * An observer, which reacts to the configuration update messages sent by
     * external entities (such as {@code IntegrationBus}es of other bounded contexts).
     */
    private class ConfigurationChangeObserver extends ChannelObserver<IntegrationMessage> {

        /**
         * Current set of message type URLs, received in the latest
         * {@linkplain RequestedMessageTypes configuration message}.
         */
        private final Set<String> currentTypeUrls = newHashSet();


        private ConfigurationChangeObserver() {
            super(RequestedMessageTypes.class);
        }

        @Override
        public void onNext(IntegrationMessage value) {
            final RequestedMessageTypes message = AnyPacker.unpack(value.getOriginalMessage());

            final Iterable<String> typeUrlsList = message.getTypeUrlsList();
            final Set<String> newTypeUrls = newHashSet(typeUrlsList);

            final Set<String> toRemove = Sets.difference(currentTypeUrls, newTypeUrls)
                                             .immutableCopy();
            final Set<String> toAdd = Sets.difference(newTypeUrls, currentTypeUrls)
                                          .immutableCopy();

            addLocalSubscriptions(toAdd);
            removeLocalSubscriptions(toRemove);

            currentTypeUrls.clear();
            currentTypeUrls.addAll(newTypeUrls);
        }

        private void addLocalSubscriptions(Iterable<String> toAdd) {
            if(toAdd.iterator().hasNext()) {
                final Iterable<EventClass> eventClasses = asMessageClasses(toAdd, asEventClass());
                eventBus.register(newEventDispatcher(eventClasses));

                final Iterable<RejectionClass> rejectionClasses =
                        asMessageClasses(toAdd, asRejectionClass());
                rejectionBus.register(newRejectionDispatcher(rejectionClasses));
            }
        }

        private void removeLocalSubscriptions(Iterable<String> toRemove) {
            if(toRemove.iterator().hasNext()) {
                final Iterable<EventClass> eventClasses = asMessageClasses(toRemove,
                                                                           asEventClass());
                eventBus.unregister(newEventDispatcher(eventClasses));

                final Iterable<RejectionClass> rejectionClasses =
                        asMessageClasses(toRemove, asRejectionClass());
                rejectionBus.unregister(newRejectionDispatcher(rejectionClasses));
            }
        }


        private RejectionDispatcher<String> newRejectionDispatcher(
                final Iterable<RejectionClass> rejectionClasses) {
            return new LocalRejectionSubscriber(boundedContextId, publisherHub, rejectionClasses);
        }

        private EventDispatcher newEventDispatcher(final Iterable<EventClass> eventClasses) {
            return new LocalEventSubscriber(boundedContextId, publisherHub, eventClasses);
        }

        @Override
        public String toString() {
            return "Integration bus observer of `RequestedMessageTypes`; " +
                    "Bounded Context ID = " + boundedContextId.getValue();
        }
    }

    /**
     * A subscriber to local {@code RejectionBus}, which publishes each matching message to
     * a remote channel.
     *
     * <p>The messages to subscribe are those that are required by external application components
     * at this moment; their set is determined by the {@linkplain RequestedMessageTypes
     * configuration messages}, received by this instance of {@code IntegrationBus}.
     */
    private static final class LocalRejectionSubscriber extends RejectionSubscriber {
        private final BoundedContextId boundedContextId;
        private final PublisherHub publisherHub;
        private final Set<RejectionClass> rejectionClasses;

        private LocalRejectionSubscriber(BoundedContextId boundedContextId,
                                         PublisherHub publisherHub,
                                         Iterable<RejectionClass> rejectionClasses) {
            this.boundedContextId = boundedContextId;
            this.publisherHub = publisherHub;
            this.rejectionClasses = ImmutableSet.copyOf(rejectionClasses);
        }


        @SuppressWarnings("ReturnOfCollectionOrArrayField")    // Returning an immutable impl.
        @Override
        public Set<RejectionClass> getMessageClasses() {
            return rejectionClasses;
        }

        @Override
        public Set<String> dispatch(RejectionEnvelope envelope) {
            final Rejection rejection = envelope.getOuterObject();
            final IntegrationMessage message = IntegrationMessages.of(rejection);
            final IntegrationMessageClass messageClass = IntegrationMessageClass.of(
                    envelope.getMessageClass());
            final Publisher channel = publisherHub.get(messageClass);
            channel.publish(AnyPacker.pack(envelope.getId()), message);
            return ImmutableSet.of(channel.toString());
        }

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("boundedContextId", boundedContextId)
                              .add("rejectionClasses", rejectionClasses)
                              .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LocalRejectionSubscriber that = (LocalRejectionSubscriber) o;
            return Objects.equals(boundedContextId, that.boundedContextId) &&
                    Objects.equals(rejectionClasses, that.rejectionClasses);
        }

        @Override
        public int hashCode() {
            return Objects.hash(boundedContextId, rejectionClasses);
        }
    }


    /**
     * A subscriber to local {@code EventBus}, which publishes each matching message to
     * a remote channel.
     *
     * <p>The messages to subscribe are those that are required by external application components
     * at this moment; their set is determined by the {@linkplain RequestedMessageTypes
     * configuration messages}, received by this instance of {@code IntegrationBus}.
     */
    private static final class LocalEventSubscriber extends EventSubscriber {

        private final BoundedContextId boundedContextId;
        private final PublisherHub publisherHub;
        private final Set<EventClass> eventClasses;

        private LocalEventSubscriber(BoundedContextId boundedContextId,
                                     PublisherHub publisherHub,
                                     Iterable<EventClass> messageClasses) {
            this.boundedContextId = boundedContextId;
            this.publisherHub = publisherHub;
            this.eventClasses = ImmutableSet.copyOf(messageClasses);
        }

        @SuppressWarnings("ReturnOfCollectionOrArrayField")     // Returning an immutable impl.
        @Override
        public Set<EventClass> getMessageClasses() {
            return eventClasses;
        }

        @Override
        public Set<String> dispatch(EventEnvelope envelope) {
            final Event event = envelope.getOuterObject();
            final IntegrationMessage msg = IntegrationMessages.of(event);
            final IntegrationMessageClass messageClass = IntegrationMessageClass.of(
                    envelope.getMessageClass());
            final Publisher channel = publisherHub.get(messageClass);
            channel.publish(AnyPacker.pack(envelope.getId()), msg);

            return ImmutableSet.of(channel.toString());
        }

        @SuppressWarnings("DuplicateStringLiteralInspection")
        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("boundedContextId", boundedContextId)
                              .add("eventClasses", eventClasses)
                              .toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LocalEventSubscriber that = (LocalEventSubscriber) o;
            return Objects.equals(boundedContextId, that.boundedContextId) &&
                    Objects.equals(eventClasses, that.eventClasses);
        }

        @Override
        public int hashCode() {
            return Objects.hash(boundedContextId, eventClasses);
        }
    }

    /**
     * A {@code Builder} for {@code IntegrationBus} instances.
     */
    public static class Builder
            extends Bus.AbstractBuilder<ExternalMessageEnvelope, Message, Builder> {

        private LocalDelivery delivery;
        private EventBus eventBus;
        private BoundedContextId boundedContextId;
        private RejectionBus rejectionBus;
        private TransportFactory transportFactory;

        public Optional<EventBus> getEventBus() {
            return Optional.fromNullable(eventBus);
        }

        public Builder setEventBus(EventBus eventBus) {
            this.eventBus = checkNotNull(eventBus);
            return self();
        }

        public Optional<RejectionBus> getRejectionBus() {
            return Optional.fromNullable(rejectionBus);
        }

        public Builder setRejectionBus(RejectionBus rejectionBus) {
            this.rejectionBus = checkNotNull(rejectionBus);
            return self();
        }

        public Builder setBoundedContextId(BoundedContextId boundedContextId) {
            this.boundedContextId = checkNotNull(boundedContextId);
            return self();
        }

        public Builder setTransportFactory(TransportFactory transportFactory) {
            this.transportFactory = checkNotNull(transportFactory);
            return self();
        }

        public Optional<TransportFactory> getTransportFactory() {
            return Optional.fromNullable(transportFactory);
        }

        private LocalDelivery getDelivery() {
            return delivery;
        }

        @Override
        public IntegrationBus build() {

            checkState(eventBus != null,
                       "`eventBus` must be set for integration bus.");
            checkState(rejectionBus != null,
                       "`rejectionBus` must be set for integration bus.");
            checkNotDefault(boundedContextId,
                            "`boundedContextId` must be set for integration bus.");

            if (transportFactory == null) {
                transportFactory = initTransportFactory();
            }

            this.delivery = new LocalDelivery();

            return new IntegrationBus(this);
        }

        private static TransportFactory initTransportFactory() {
            return LocalTransportFactory.newInstance();
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
