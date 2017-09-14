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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.SPI;
import io.spine.core.Ack;
import io.spine.type.MessageClass;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;
import static java.util.Collections.synchronizedMap;

/**
 * A factory for creating channel-based transport for {@code Message} inter-exchange between the
 * current deployment component and other application parts.
 *
 * Inspired by <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/PublishSubscribeChannel.html">
 * Publish-Subscriber Channel pattern.</a>
 *
 * @author Alex Tymchenko
 */
@SPI
public interface TransportFactory {

    /**
     * Creates a {@link Publisher} for the messages of given class.
     *
     * @param messageClass the class of messages that will be published
     *                     via a created {@code Publisher}
     * @return a new {@code Publisher} instance
     */
    Publisher createPublisher(MessageClass messageClass);

    /**
     * Creates a {@link Subscriber} for the messages of given class.
     *
     * @param messageClass the class of messages that will be received
     *                     via a created {@code Subscriber}
     * @return a new {@code Subscriber} instance
     */
    Subscriber createSubscriber(MessageClass messageClass);

    /**
     * A channel dedicated to exchanging the messages of a single message type.
     *
     * <p>The class of message serves as a channel key.
     */
    interface MessageChannel extends AutoCloseable {

        /**
         * Returns a class of messages that are being exchanged within this channel.
         *
         * @return the message class
         */
        ExternalMessageClass getMessageClass();

        /**
         * Allows to understand whether this channel is stale and can be closed.
         *
         * @return {@code true} if the channel is stale, {@code false} otherwise
         */
        boolean isStale();
    }

    /**
     * Publisher for messages of a specific type.
     *
     * <p>There can be many publishers per message type.
     */
    interface Publisher extends MessageChannel {

        /**
         * Publishes a given {@code ExternalMessage} to the channel under a given ID.
         *
         * @param id      an ID of the message packed into {@linkplain Any}.
         * @param message the message to publish
         * @return an acknowledgment of message publishing;
         * @see Ack
         */
        @SuppressWarnings("UnusedReturnValue")      // Return value is planned for future use.
        Ack publish(Any id, ExternalMessage message);
    }

    /**
     * Subscriber for messages of a specific type.
     *
     * <p>There can be many subscribers per message type.
     *
     */
    interface Subscriber extends MessageChannel {

        /**
         * Obtains current observers registered in this instance of {@code Subscriber},
         * which receive the subscription updates.
         *
         * @return observers for this subscriber
         */
        Iterable<StreamObserver<ExternalMessage>> getObservers();

        /**
         * Adds an observer, which will be receiving the subscription updates.
         *
         * @param observer an observer to register
         */
        void addObserver(StreamObserver<ExternalMessage> observer);

        /**
         * Removes an existing observer and disconnects it from this subscription channel.
         *
         * <p>In case the given observer is not registered at the moment, does nothing.
         *
         * @param observer an observer to remove
         */
        void removeObserver(StreamObserver<ExternalMessage> observer);
    }

    /**
     * The hub of channels, grouped in some logical way.
     *
     * <p>Serves for channel creation and storage-per-key, which in a way makes the hub similar to
     * an entity repository.
     */
    abstract class ChannelHub<C extends MessageChannel> {

        private final TransportFactory transportFactory;
        private final Map<ExternalMessageClass, C> channels =
                synchronizedMap(Maps.<ExternalMessageClass, C>newHashMap());

        protected ChannelHub(TransportFactory transportFactory) {
            this.transportFactory = transportFactory;
        }

        /**
         * Creates a new channel under the specified key
         *
         * @param channelKey the channel key to use
         * @return the created channel.
         */
        protected abstract C newChannel(MessageClass channelKey);

        public synchronized Set<ExternalMessageClass> keys() {
            return ImmutableSet.copyOf(channels.keySet());
        }

        /**
         * Obtains a channel from this hub according to the channel key.
         *
         * <p>If there is no channel with this key in this hub, creates it and adds to the hub
         * prior to returning it as a result of this method call.
         *
         * @param channelKey the channel key to obtain a channel with
         * @return a channel with the key
         */
        public synchronized C get(MessageClass channelKey) {
            final ExternalMessageClass key = ExternalMessageClass.of(channelKey);
            if(!channels.containsKey(key)) {
                final C newChannel = newChannel(key);
                channels.put(key, newChannel);
            }
            return channels.get(key);
        }

        /**
         * Closes the stale channels and removes those from the hub.
         */
        public void closeStaleChannels() {
            final Set<ExternalMessageClass> staleChannels = detectStale();
            for (ExternalMessageClass cls : staleChannels) {
                channels.remove(cls);
            }
        }

        private Set<ExternalMessageClass> detectStale() {
            final Set<ExternalMessageClass> toRemove = newHashSet();
            for (ExternalMessageClass cls : channels.keySet()) {
                final C channel = channels.get(cls);
                if(channel.isStale()) {
                    try {
                        channel.close();
                    } catch (Exception e) {
                        throw illegalStateWithCauseOf(e);
                    } finally {
                        toRemove.add(cls);
                    }
                }
            }
            return toRemove;
        }

        TransportFactory transportFactory() {
            return transportFactory;
        }
    }

    /**
     * The hub of {@link Subscriber}s.
     *
     * <p>Creates and manages the existing channels born in the given {@linkplain TransportFactory}.
     */
    class SubscriberHub extends ChannelHub<Subscriber> {

        protected SubscriberHub(TransportFactory transportFactory) {
            super(transportFactory);
        }

        @Override
        protected Subscriber newChannel(MessageClass channelKey) {
            return transportFactory().createSubscriber(channelKey);
        }
    }

    /**
     * The hub of {@link Publisher}s.
     *
     * <p>Creates and manages the existing channels born in the given {@linkplain TransportFactory}.
     */
    class PublisherHub extends ChannelHub<Publisher> {

        protected PublisherHub(TransportFactory transportFactory) {
            super(transportFactory);
        }

        @Override
        protected Publisher newChannel(MessageClass channelKey) {
            return transportFactory().createPublisher(channelKey);
        }
    }
}
