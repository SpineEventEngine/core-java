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
import com.google.protobuf.StringValue;
import io.grpc.stub.StreamObserver;
import io.spine.core.BoundedContextName;
import io.spine.protobuf.AnyPacker;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.integration.ExternalMessages;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.string.Stringifiers;
import io.spine.type.ClassName;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Alex Tymchenko
 */
public class ShardedStream {

    private final TransportFactory transportFactory;
    private final BoundedContextName boundedContextName;

    private final ShardingKey key;
    private final ChannelId channelId;

    protected ShardedStream(ShardingKey key,
                            TransportFactory transportFactory,
                            BoundedContextName boundedContextName) {
        this.transportFactory = transportFactory;
        this.boundedContextName = boundedContextName;
        this.key = key;
        this.channelId = getChannelId(key);
    }

    public ShardingKey getKey() {
        return key;
    }

    public void post(ShardedMessage message) {
        final ExternalMessage externalMessage = ExternalMessages.of(message, boundedContextName);
        final Publisher publisher = getPublisher();
        publisher.publish(externalMessage.getId(), externalMessage);
    }

    public void setConsumer(final StreamObserver<ShardedMessage> consumer) {
        checkNotNull(consumer);
        getSubscriber().addObserver(new StreamObserver<ExternalMessage>() {
            @Override
            public void onNext(ExternalMessage value) {
                checkNotNull(value);
                final ShardedMessage shardedMessage = ExternalMessages.asShardedMessage(value);
                consumer.onNext(shardedMessage);
            }

            @Override
            public void onError(Throwable t) {
                //TODO:2018-02-25:alex.tymchenko: define the expected behavior.
            }

            @Override
            public void onCompleted() {
                //TODO:2018-02-25:alex.tymchenko: is this legal at all?
            }
        });
    }

    private Publisher getPublisher() {
        final Publisher result = transportFactory.createPublisher(channelId);
        return result;
    }

    private Subscriber getSubscriber() {
        final Subscriber result = transportFactory.createSubscriber(channelId);
        return result;
    }

    private static ChannelId getChannelId(ShardingKey key) {
        final ClassName className = key.getModelClass()
                                       .getClassName();
        final IdPredicate idPredicate = key.getIdPredicate();
        final String value = on(':').join(className, Stringifiers.toString(idPredicate));
        final StringValue asMsg = StringValue.newBuilder()
                                             .setValue(value)
                                             .build();
        final Any asAny = AnyPacker.pack(asMsg);
        final ChannelId result = ChannelId.newBuilder()
                                          .setIdentifier(asAny)
                                          .build();
        return result;
    }
}
