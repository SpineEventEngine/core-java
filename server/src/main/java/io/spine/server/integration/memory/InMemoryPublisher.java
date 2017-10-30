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
package io.spine.server.integration.memory;

import com.google.common.base.Function;
import com.google.protobuf.Any;
import io.grpc.stub.StreamObserver;
import io.spine.core.Ack;
import io.spine.server.bus.Buses;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.Publisher;
import io.spine.server.integration.specification.MessageSuitabilitySpecification;
import io.spine.server.integration.specification.Specification;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * An in-memory implementation of the {@link Publisher}.
 *
 * <p>To use only in scope of the same JVM as {@linkplain InMemorySubscriber subscribers}.
 *
 * @author Alex Tymchenko
 * @author Dmitry Ganzha
 */
final class InMemoryPublisher extends AbstractInMemoryChannel implements Publisher {

    /**
     * A provider of subscribers, to which a message should be headed.
     */
    private final Function<ChannelId, Iterable<InMemorySubscriber>> subscriberProvider;

    /**
     * The specification which checks if the message is suitable for the channel.
     */
    private final Specification<ExternalMessage> messageSuitabilitySpecification;

    InMemoryPublisher(ChannelId channelId,
                      Function<ChannelId, Iterable<InMemorySubscriber>> provider) {
        super(channelId);
        this.subscriberProvider = provider;
        this.messageSuitabilitySpecification = new MessageSuitabilitySpecification(getChannelId());
    }

    @Override
    public Ack publish(Any messageId, ExternalMessage message) {
        final Boolean messageSuitable = messageSuitabilitySpecification.isSatisfiedBy(message);
        if (!messageSuitable) {
            throw newIllegalArgumentException("The message is not suitable for this channel.");
        }

        final Iterable<InMemorySubscriber> localSubscribers = getSubscribers(getChannelId());
        for (InMemorySubscriber localSubscriber : localSubscribers) {
            callSubscriber(message, localSubscriber);
        }
        return Buses.acknowledge(messageId);
    }

    private static void callSubscriber(ExternalMessage message, InMemorySubscriber subscriber) {
        final Iterable<StreamObserver<ExternalMessage>> callees = subscriber.getObservers();
        for (StreamObserver<ExternalMessage> observer : callees) {
            observer.onNext(message);
        }
    }

    private Iterable<InMemorySubscriber> getSubscribers(ChannelId channelId) {
        return subscriberProvider.apply(channelId);
    }

    /**
     * Always returns {@code false}, as publishers don't get stale.
     *
     * @return {@code false} always.
     */
    @Override
    public boolean isStale() {
        return false;
    }
}
