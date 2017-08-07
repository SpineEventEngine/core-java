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
 * Inspired by <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/PublishSubscribeChannel.html>
 * Publish-Subscriber Channel pattern.</a>
 *
 * @author Alex Tymchenko
 */
public interface TransportFactory {

    Publisher createPublisher(MessageClass messageClass);

    Subscriber createSubscriber(MessageClass messageClass);

    /**
     * A channel dedicated to exchanging the messages of a single message type.
     */
    @SuppressWarnings("unused") // the parameter is used to determine a type of the message.
    interface MessageChannel extends AutoCloseable {

        IntegrationMessageClass getMessageClass();

        boolean isStale();
    }

    /**
     * Publisher for messages of a specific type.
     *
     * <p>There can be many publishers per message type.
     */
    interface Publisher extends MessageChannel {

        Ack publish(Any id, IntegrationMessage message);
    }

    /**
     * Subscriber for messages of a specific type.
     *
     * <p>There can be many subscribers per message type.
     *
     */
    interface Subscriber extends MessageChannel {

        /**
         * Obtains an observer, which is used to feed the subscription updates to this instance of
         * {@code Subscriber}.
         *
         * @return the instance of observer
         */
        Iterable<StreamObserver<IntegrationMessage>> getObservers();

        void addObserver(StreamObserver<IntegrationMessage> observer);

        void removeObserver(StreamObserver<IntegrationMessage> observer);
    }

    abstract class ChannelHub<C extends MessageChannel> {

        private final TransportFactory transportFactory;
        private final Map<IntegrationMessageClass, C> channels =
                synchronizedMap(Maps.<IntegrationMessageClass, C>newHashMap());

        protected ChannelHub(TransportFactory transportFactory) {
            this.transportFactory = transportFactory;
        }

        protected abstract C newChannel(MessageClass channelKey);

        synchronized Set<IntegrationMessageClass> keys() {
            return ImmutableSet.copyOf(channels.keySet());
        }

        synchronized C get(MessageClass channelKey) {
            final IntegrationMessageClass key = IntegrationMessageClass.of(channelKey);
            if(!channels.containsKey(key)) {
                final C newChannel = newChannel(key);
                channels.put(key, newChannel);
            }
            return channels.get(key);
        }

        public void releaseStale() {
            final Set<IntegrationMessageClass> toRemove = newHashSet();
            for (IntegrationMessageClass cls : channels.keySet()) {
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
            for (IntegrationMessageClass cls : toRemove) {
                channels.remove(cls);
            }
        }

        protected TransportFactory transportFactory() {
            return transportFactory;
        }
    }

    class SubscriberHub extends ChannelHub<Subscriber> {

        protected SubscriberHub(TransportFactory transportFactory) {
            super(transportFactory);
        }

        @Override
        protected Subscriber newChannel(MessageClass channelKey) {
            return transportFactory().createSubscriber(channelKey);
        }
    }

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
