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
package io.spine.server.sharding;

import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageEnvelope;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessages;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.util.GenericTypeIndex;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.sharding.ShardedMessages.toChannelId;
import static io.spine.server.sharding.ShardedStream.GenericParameter.MESSAGE_CLASS;

/**
 * @author Alex Tymchenko
 */
public abstract class ShardedStream<I, E extends MessageEnvelope<?, ?, ?>> {

    private final TransportFactory transportFactory;
    private final BoundedContextName boundedContextName;
    private final ShardingKey key;
    private final ChannelId channelId;

    @Nullable
    private ExternalMessageObserver channelObserver;

    ShardedStream(ShardingKey key, TransportFactory transportFactory, BoundedContextName name) {
        this.transportFactory = transportFactory;
        this.boundedContextName = name;
        this.key = key;
        final Class<E> envelopeCls = getEnvelopeClass();
        this.channelId = toChannelId(name, key, envelopeCls);
    }

    @SuppressWarnings("unchecked")  // Ensured by the generic type definition.
    private Class<E> getEnvelopeClass() {
        return (Class<E>) MESSAGE_CLASS.getArgumentIn(getClass());
    }


    protected abstract ShardedMessageConverter<I, E> converter();

    public ShardingKey getKey() {
        return key;
    }

    public final void post(I targetId, E messageEnvelope) {
        checkNotNull(messageEnvelope);

        final ShardedMessage shardedMessage = converter().convert(targetId, messageEnvelope);
        checkNotNull(shardedMessage);

        post(shardedMessage);
    }

    private void post(ShardedMessage message) {
        final ExternalMessage externalMessage = ExternalMessages.of(message, boundedContextName);
        final Publisher publisher = getPublisher();
        publisher.publish(externalMessage.getId(), externalMessage);
    }

    public void setConsumer(ShardedStreamConsumer<I, E> consumer) {
        checkNotNull(consumer);
        if(channelObserver == null) {
            channelObserver = new ExternalMessageObserver(consumer);
            getSubscriber().addObserver(channelObserver);
        } else {
            channelObserver.updateDelegate(consumer);
        }
    }

    private Publisher getPublisher() {
        final Publisher result = transportFactory.createPublisher(channelId);
        return result;
    }

    private Subscriber getSubscriber() {
        final Subscriber result = transportFactory.createSubscriber(channelId);
        return result;
    }

    private class ExternalMessageObserver implements StreamObserver<ExternalMessage> {

        private ShardedStreamConsumer<I, E> delegate;

        private ExternalMessageObserver(ShardedStreamConsumer<I, E> delegate) {
            this.delegate = delegate;
        }

        private void updateDelegate(ShardedStreamConsumer<I, E> newDelegate) {
            checkNotNull(newDelegate);
            this.delegate = newDelegate;
        }

        @Override
        public void onNext(ExternalMessage value) {
            checkNotNull(value);
            final ShardedMessage shardedMessage = ExternalMessages.asShardedMessage(value);

            final E envelope = converter().envelopeOf(shardedMessage);
            final I targetId = converter().targetIdOf(shardedMessage);
            checkNotNull(envelope);
            delegate.onNext(targetId, envelope);
        }

        @Override
        public void onError(Throwable t) {
            //TODO:2018-03-8:alex.tymchenko: figure out what should happen.
        }

        @Override
        public void onCompleted() {
            //TODO:2018-03-8:alex.tymchenko: figure out what should happen.
        }
    }

    /**
     * Enumeration of generic type parameters of this class.
     */
    enum GenericParameter implements GenericTypeIndex<ShardedStream> {

        /** The index of the generic type {@code <M>}. */
        MESSAGE_CLASS(0);

        private final int index;

        GenericParameter(int index) {
            this.index = index;
        }

        @Override
        public int getIndex() {
            return this.index;
        }

        @Override
        public Class<?> getArgumentIn(Class<? extends ShardedStream> cls) {
            return Default.getArgument(this, cls);
        }
    }
}
