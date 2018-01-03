/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessageClass;
import io.spine.server.integration.Publisher;
import io.spine.type.MessageClass;

/**
 * An in-memory implementation of the {@link Publisher}.
 *
 * <p>To use only in scope of the same JVM as {@linkplain InMemorySubscriber subscribers}.
 *
 * @author Alex Tymchenko
 */
final class InMemoryPublisher extends AbstractInMemoryChannel implements Publisher {

    /**
     * A provider of subscribers, to which a message of the specified message type should be headed.
     */
    private final Function<MessageClass, Iterable<InMemorySubscriber>> subscriberProvider;

    InMemoryPublisher(ExternalMessageClass messageClass,
                      Function<MessageClass, Iterable<InMemorySubscriber>> provider) {
        super(messageClass);
        this.subscriberProvider = provider;
    }

    @Override
    public Ack publish(Any messageId, ExternalMessage message) {
        final Iterable<InMemorySubscriber> localSubscribers = getSubscribers(getMessageClass());
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

    private Iterable<InMemorySubscriber> getSubscribers(MessageClass genericCls) {
        return subscriberProvider.apply(genericCls);
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
