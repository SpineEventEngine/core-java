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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessages;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.string.Stringifiers;
import io.spine.type.ClassName;
import io.spine.util.GenericTypeIndex;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.sharding.ShardedStream.GenericParameter.MESSAGE_CLASS;

/**
 * The stream of messages of a particular type sent for the processing to a specific shard.
 *
 * <p>Uses a {@linkplain io.spine.server.transport.MessageChannel MessageChannel} as an
 * underlying transport.
 *
 * <p>A typical example is a stream of {@code Event}s, being sent from the write-side
 * to a particular shard of read-side {@code Projection}s.
 *
 * @param <I> the type of identifiers of message targets, such as entities
 * @param <M> the type of messages, which are being sharded and sent via this stream
 * @param <E> the type of envelopes into which the messages are packed
 *
 * @author Alex Tymchenko
 */
public abstract class ShardedStream<I, M extends Message, E extends MessageEnvelope<?, M, ?>>
        implements AutoCloseable {

    /**
     * A name of the bounded context, within which the sharded stream exists.
     */
    private final BoundedContextName boundedContextName;

    /**
     * A key, which defines a shard.
     */
    private final ShardingKey key;

    /**
     * A tag which defines a type
     */
    private final ShardingTag<E> tag;
    private final Class<I> targetIdClass;
    private final Subscriber subscriber;
    private final Publisher publisher;

    /**
     * A lazily-intialized converted for the sharded messages.
     */
    @Nullable
    private ShardedMessageConverter<I, M, E> converter;

    @Nullable
    private ExternalMessageObserver channelObserver;

    ShardedStream(AbstractBuilder<I, ?, ? extends ShardedStream> builder) {
        this.boundedContextName = builder.boundedContextName;
        this.key = builder.key;
        this.tag = (ShardingTag<E>) builder.tag;
        this.targetIdClass = builder.targetIdClass;
        final Class<E> envelopeCls = getEnvelopeClass();
        final ChannelId channelId = toChannelId(builder.boundedContextName, key, envelopeCls);
        this.subscriber = builder.transportFactory.createSubscriber(channelId);
        this.publisher = builder.transportFactory.createPublisher(channelId);
    }

    @SuppressWarnings("unchecked")  // Ensured by the generic type definition.
    private Class<E> getEnvelopeClass() {
        return (Class<E>) MESSAGE_CLASS.getArgumentIn(getClass());
    }

    private Class<I> getTargetIdClass() {
        return targetIdClass;
    }

    private final ShardedMessageConverter<I, M, E> converter() {
        if(converter == null) {
            converter = newConverter();
        }
        return converter;
    }

    protected abstract ShardedMessageConverter<I, M, E> newConverter();

    public ShardingKey getKey() {
        return key;
    }

    public ShardingTag<E> getTag() {
        return tag;
    }

    public final void post(I targetId, E messageEnvelope) {
        checkNotNull(messageEnvelope);

        final ShardedMessage shardedMessage = converter().convert(targetId, messageEnvelope);
        checkNotNull(shardedMessage);

        post(shardedMessage);
    }

    private void post(ShardedMessage message) {
        final ExternalMessage externalMessage = ExternalMessages.of(message, boundedContextName);
        publisher.publish(externalMessage.getId(), externalMessage);
    }

    public void setConsumer(ShardedStreamConsumer<I, E> consumer) {
        checkNotNull(consumer);
        if (channelObserver == null) {
            channelObserver = new ExternalMessageObserver(consumer);
            subscriber.addObserver(channelObserver);
        } else {
            channelObserver.updateDelegate(consumer);
        }
    }

    @Override
    public void close() {
        if (channelObserver != null) {
            subscriber.removeObserver(channelObserver);
        }
    }

    private static <E extends MessageEnvelope<?, ?, ?>>
    ChannelId toChannelId(BoundedContextName boundedContextName,
                          ShardingKey key,
                          Class<E> envelopeClass) {
        checkNotNull(key);
        checkNotNull(boundedContextName);
        checkNotNull(envelopeClass);

        final ClassName className = key.getEntityClass()
                                       .getClassName();
        final ShardIndex shardIndex = key.getIndex();

        final String value = on("__").join("bc_", boundedContextName.getValue(),
                                           "target_", className,
                                           "prd_", Stringifiers.toString(shardIndex),
                                           "env_", envelopeClass);
        final StringValue asMsg = StringValue.newBuilder()
                                             .setValue(value)
                                             .build();
        final Any asAny = AnyPacker.pack(asMsg);
        final ChannelId result = ChannelId.newBuilder()
                                          .setIdentifier(asAny)
                                          .build();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ShardedStream<?, ?, ?> that = (ShardedStream<?, ?, ?>) o;
        return Objects.equals(boundedContextName, that.boundedContextName) &&
                Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextName, key);
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
            final I targetId = converter().targetIdOf(shardedMessage, getTargetIdClass());
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

        /** The index of the generic type {@code <I>}. */
        TARGET_ID_CLASS(0),

        /** The index of the generic type {@code <M>}. */
        MESSAGE_CLASS(1);

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

    public abstract static class AbstractBuilder<I,
            B extends AbstractBuilder<I, B, S>,
            S extends ShardedStream<I, ?, ?>> {

        private BoundedContextName boundedContextName;
        private ShardingKey key;
        private ShardingTag tag;
        private TransportFactory transportFactory;
        private Class<I> targetIdClass;

        /** Prevents the instantiation of this builder. */
        protected AbstractBuilder() {
        }

        public BoundedContextName getBoundedContextName() {
            return boundedContextName;
        }

        public B setBoundedContextName(BoundedContextName boundedContextName) {
            checkNotNull(boundedContextName);
            this.boundedContextName = boundedContextName;
            return thisAsB();
        }

        public ShardingKey getKey() {
            return key;
        }

        public B setKey(ShardingKey key) {
            checkNotNull(key);
            this.key = key;
            return thisAsB();
        }

        public ShardingTag getTag() {
            return tag;
        }

        public B setTag(ShardingTag tag) {
            checkNotNull(tag);
            this.tag = tag;
            return thisAsB();
        }

        public B setTargetIdClass(Class<I> targetIdClass) {
            checkNotNull(targetIdClass);
            this.targetIdClass = targetIdClass;
            return thisAsB();
        }

        @SuppressWarnings("unchecked")
        private B thisAsB() {
            return (B) this;
        }

        protected abstract S createStream();

        public S build(TransportFactory transportFactory) {
            checkNotNull(transportFactory);
            this.transportFactory = transportFactory;
            final S result = createStream();
            return result;
        }
    }
}
