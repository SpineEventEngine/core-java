/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.delivery;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextName;
import io.spine.core.MessageEnvelope;
import io.spine.protobuf.AnyPacker;
import io.spine.reflect.GenericTypeIndex;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessages;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.string.Stringifiers;
import io.spine.type.ClassName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.delivery.ShardedStream.GenericParameter.MESSAGE_CLASS;
import static io.spine.util.Exceptions.newIllegalStateException;

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
    private final DeliveryTag<E> tag;
    private final Class<I> targetIdClass;
    private final Subscriber subscriber;
    private final Publisher publisher;
    private final ExternalMessageObserver channelObserver;

    /**
     * A lazily-initialized converter for the sharded messages.
     */
    private @Nullable ShardedMessageConverter<I, M, E> converter;

    ShardedStream(AbstractBuilder<I, E, ?, ? extends ShardedStream> builder) {
        this.boundedContextName = builder.boundedContextName;
        this.key = builder.key;
        this.tag = builder.tag;
        this.targetIdClass = builder.targetIdClass;
        final Class<E> envelopeCls = getEnvelopeClass();
        final ChannelId channelId = ChannelIds.toChannelId(builder.boundedContextName, key, envelopeCls);
        this.subscriber = builder.transportFactory.createSubscriber(channelId);
        this.publisher = builder.transportFactory.createPublisher(channelId);

        this.channelObserver = new ExternalMessageObserver(builder.consumer);
        this.subscriber.addObserver(channelObserver);
    }

    @SuppressWarnings("unchecked")  // Ensured by the generic type definition.
    private Class<E> getEnvelopeClass() {
        return (Class<E>) MESSAGE_CLASS.getArgumentIn(getClass());
    }

    private Class<I> getTargetIdClass() {
        return targetIdClass;
    }

    private ShardedMessageConverter<I, M, E> converter() {
        if(converter == null) {
            converter = newConverter();
        }
        return converter;
    }

    protected abstract ShardedMessageConverter<I, M, E> newConverter();

    /**
     * Obtains the key defining the subset of entities (i.e. a shard), to which the messages
     * of this stream are addressed.
     *
     * @return the key of the shard being a destination for the messages transferred by this stream
     */
    public ShardingKey getKey() {
        return key;
    }

    /**
     * Obtains the tag which describes the delivery destination for this stream of messages
     *
     * @return the delivery tag for this stream
     */
    public DeliveryTag<E> getTag() {
        return tag;
    }

    /**
     * Posts the message to this stream.
     *
     * <p>The message is sent to the corresponding shard via the underlying transport channel.
     *
     * @param targetId the ID of target entity, that this message should be dispatched to
     * @param messageEnvelope the message to post, packed as an envelope
     */
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

    @Override
    public void close() {
        if (channelObserver != null) {
            subscriber.removeObserver(channelObserver);
        }
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
                Objects.equals(key, that.key) &&
                Objects.equals(tag, that.tag) &&
                Objects.equals(targetIdClass, that.targetIdClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(boundedContextName, key, tag, targetIdClass);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("Bounded Context name", boundedContextName)
                          .add("Sharding key", key)
                          .add("Delivery tag", tag)
                          .add("Target ID class", targetIdClass)
                          .toString();
    }

    /**
     * The observer of stream messages.
     *
     * <p>Passes the received messages to the respective {@linkplain ShardedStreamConsumer
     * delegate}.
     */
    private class ExternalMessageObserver implements StreamObserver<ExternalMessage> {

        private final ShardedStreamConsumer<I, E> delegate;

        private ExternalMessageObserver(ShardedStreamConsumer<I, E> delegate) {
            this.delegate = delegate;
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
            throw newIllegalStateException(t,
                                           "Error observing the messages for consumer with tag %s.",
                                           delegate.getTag());
        }

        @Override
        public void onCompleted() {
            // The publishing side never "completes" the transmission. So throwing an exception.
            throw newIllegalStateException(
                    "Unexpected 'onCompleted()' occurred while observing the external messages " +
                            "for consumer with tag %s.", delegate.getTag());
        }

        @Override
        public String toString() {
            return "ExternalMessage observer for the ShardedStream " + ShardedStream.this;
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

    /**
     * An internal utility aimed for working with {@linkplain ChannelId channel identifiers}.
     */
    private static class ChannelIds {

        /**
         * Prevents the instantiation of this utility class.
         */
        private ChannelIds() {
        }

        private static <E extends MessageEnvelope<?, ?, ?>>
        StringValue asChannelName(BoundedContextName bcName,
                                  Class<E> envelopeClass,
                                  ClassName className,
                                  ShardIndex shardIndex) {
            final String value = on("__").join(Prefix.BOUNDED_CONTEXT, bcName.getValue(),
                                               Prefix.TARGET_CLASS, className,
                                               Prefix.SHARD_INDEX,
                                               Stringifiers.toString(shardIndex),
                                               Prefix.ENVELOPE_CLASS, envelopeClass);
            final StringValue result = StringValue.newBuilder()
                                                  .setValue(value)
                                                  .build();
            return result;
        }

        private static <E extends MessageEnvelope<?, ?, ?>>
        ChannelId toChannelId(BoundedContextName boundedContextName,
                              ShardingKey key,
                              Class<E> envelopeClass) {
            checkNotNull(key);
            checkNotNull(boundedContextName);
            checkNotNull(envelopeClass);

            final Class<?> keyClass = key.getEntityClass()
                                         .value();
            final ClassName className = ClassName.of(keyClass);
            final ShardIndex shardIndex = key.getIndex();

            final StringValue asMsg = asChannelName(boundedContextName, envelopeClass, className,
                                                    shardIndex);
            final Any asAny = AnyPacker.pack(asMsg);
            final ChannelId result = ChannelId.newBuilder()
                                              .setIdentifier(asAny)
                                              .build();
            return result;
        }

        /**
         * Prefixes used for the channel name formatting.
         */
        enum Prefix {

            BOUNDED_CONTEXT("bc_"),
            TARGET_CLASS("target_"),
            SHARD_INDEX("shardidx_"),
            ENVELOPE_CLASS("env_");

            private final String value;

            Prefix(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return value;
            }
        }
    }

    /**
     * An abstract base for builders of the {@code ShardedStream} descendants.
     *
     * @param <I> the type of the identifiers of sharded message targets.
     * @param <E> the type of message envelopes, in form of which messages travel
     * @param <B> the exact type of the descendant builder
     * @param <S> the exact type of the {@code ShardedStream} descendant to build
     */
    public abstract static class AbstractBuilder<I,
                                                 E extends MessageEnvelope<?, ?, ?>,
                                                 B extends AbstractBuilder<I, ?, B, S>,
                                                 S extends ShardedStream<I, ?, ?>> {

        private BoundedContextName boundedContextName;
        private ShardingKey key;
        private DeliveryTag<E> tag;
        private TransportFactory transportFactory;
        private Class<I> targetIdClass;
        private ShardedStreamConsumer<I, E> consumer;

        /** Prevents the instantiation of this builder. */
        protected AbstractBuilder() {
        }

        public BoundedContextName getBoundedContextName() {
            return boundedContextName;
        }

        @CanIgnoreReturnValue
        public B setBoundedContextName(BoundedContextName boundedContextName) {
            checkNotNull(boundedContextName);
            this.boundedContextName = boundedContextName;
            return thisAsB();
        }

        public ShardingKey getKey() {
            return key;
        }

        @CanIgnoreReturnValue
        public B setKey(ShardingKey key) {
            checkNotNull(key);
            this.key = key;
            return thisAsB();
        }

        public DeliveryTag<E> getTag() {
            return tag;
        }

        @CanIgnoreReturnValue
        public B setTag(DeliveryTag<E> tag) {
            checkNotNull(tag);
            this.tag = tag;
            return thisAsB();
        }

        @CanIgnoreReturnValue
        public B setTargetIdClass(Class<I> targetIdClass) {
            checkNotNull(targetIdClass);
            this.targetIdClass = targetIdClass;
            return thisAsB();
        }

        public Class<I> getTargetIdClass() {
            return targetIdClass;
        }

        @CanIgnoreReturnValue
        public B setConsumer(ShardedStreamConsumer<I, E> consumer) {
            checkNotNull(consumer);
            this.consumer = consumer;
            return thisAsB();
        }

        public ShardedStreamConsumer<I, E> getConsumer() {
            return consumer;
        }

        @SuppressWarnings("unchecked")
        private B thisAsB() {
            return (B) this;
        }

        protected abstract S createStream();

        public S build(TransportFactory transportFactory) {
            checkPresent(transportFactory, "TransportFactory");
            checkPresent(key, "ShardingKey");
            checkPresent(tag, "DeliveryTag");
            checkPresent(targetIdClass, "Target ID Class");
            checkPresent(consumer, "Consumer");

            this.transportFactory = transportFactory;
            final S result = createStream();
            return result;
        }

        private static void checkPresent(Object value, final String fieldName) {
            checkNotNull(value, fieldName + " must be set");
        }
    }
}
