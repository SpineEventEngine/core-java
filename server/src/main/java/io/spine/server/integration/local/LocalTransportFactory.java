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
package io.spine.server.integration.local;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.server.bus.Buses;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.TransportFactory;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newConcurrentHashSet;
import static io.spine.server.integration.ExternalMessageClass.of;

/**
 * @author Alex Tymchenko
 */
public class LocalTransportFactory implements TransportFactory {

    /**
     * An in-memory storage of subscribers per message class.
     */
    private final Multimap<MessageClass, LocalSubscriber> subscribers =
            Multimaps.synchronizedMultimap(HashMultimap.<MessageClass, LocalSubscriber>create());

    private LocalTransportFactory() {
        // Prevent direct instantiation from the outside.
    }

    public static LocalTransportFactory newInstance() {
        return new LocalTransportFactory();
    }

    @Override
    public Publisher createPublisher(MessageClass messageClass) {
        return new LocalPublisher(of(messageClass),
                                  new Function<MessageClass, Iterable<LocalSubscriber>>() {
                                      @Override
                                      public Iterable<LocalSubscriber> apply(
                                              @Nullable MessageClass input) {
                                          checkNotNull(input);
                                          return subscribers.get(input);
                                      }
                                  });
    }

    @Override
    public Subscriber createSubscriber(MessageClass messageClass) {
        final LocalSubscriber subscriber = new LocalSubscriber(of(messageClass));

        subscribers.put(messageClass, subscriber);
        return subscriber;
    }

    abstract static class AbstractLocalChannel implements MessageChannel {

        private final ExternalMessageClass messageClass;

        protected AbstractLocalChannel(ExternalMessageClass messageClass) {
            this.messageClass = messageClass;
        }

        @SuppressWarnings("NoopMethodInAbstractClass")  // See the method body for the explanation.
        @Override
        public void close() throws Exception {
            // There is nothing to close in the in-memory local channel implementation.
        }

        @Override
        public ExternalMessageClass getMessageClass() {
            return messageClass;
        }
    }

    /**
     * An in-memory implementation of the
     * {@link io.spine.server.integration.TransportFactory.Publisher Publisher}.
     *
     * <p>To use only in scope of the same JVM as subscribers.
     */
    static class LocalPublisher extends AbstractLocalChannel implements Publisher {

        private final Function<MessageClass, Iterable<LocalSubscriber>> subscriberProvider;

        private LocalPublisher(ExternalMessageClass messageClass,
                               Function<MessageClass, Iterable<LocalSubscriber>> provider) {
            super(messageClass);
            this.subscriberProvider = provider;
        }

        @Override
        public Ack publish(Any messageId, ExternalMessage message) {
            final Iterable<LocalSubscriber> localSubscribers = getSubscribers(getMessageClass());
            for (LocalSubscriber localSubscriber : localSubscribers) {
                callSubscriber(message, localSubscriber);
            }
            return Buses.acknowledge(messageId);
        }

        private static void callSubscriber(ExternalMessage message, LocalSubscriber subscriber) {
            final Iterable<StreamObserver<ExternalMessage>> callees = subscriber.getObservers();
            for (StreamObserver<ExternalMessage> observer : callees) {
                observer.onNext(message);
            }
        }

        private Iterable<LocalSubscriber> getSubscribers(
                MessageClass genericCls) {
            return subscriberProvider.apply(genericCls);
        }

        @Override
        public boolean isStale() {
            return false;   // publishers are never stale.
        }
    }

    /**
     * An in-memory implementation of the
     * {@link io.spine.server.integration.TransportFactory.Subscriber Subscriber}.
     *
     * <p>To use only in scope of the same JVM as publishers.
     */
    static class LocalSubscriber extends AbstractLocalChannel implements Subscriber {

        private final Set<StreamObserver<ExternalMessage>> observers = newConcurrentHashSet();

        private LocalSubscriber(ExternalMessageClass messageClass) {
            super(messageClass);
        }

        @Override
        public Iterable<StreamObserver<ExternalMessage>> getObservers() {
            return ImmutableSet.copyOf(observers);
        }

        @Override
        public void addObserver(StreamObserver<ExternalMessage> observer) {
            checkNotNull(observer);
            observers.add(observer);
        }

        @Override
        public void removeObserver(StreamObserver<ExternalMessage> observer) {
            checkNotNull(observer);
            observers.remove(observer);
        }

        @Override
        public boolean isStale() {
            return observers.isEmpty();
        }

    }
}
