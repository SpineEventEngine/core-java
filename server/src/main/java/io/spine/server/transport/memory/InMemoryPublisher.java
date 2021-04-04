/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
package io.spine.server.transport.memory;

import com.google.protobuf.Any;
import io.spine.core.Ack;
import io.spine.server.bus.MessageIdExtensionsKt;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.transport.AbstractChannel;
import io.spine.server.transport.ChannelId;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;

import java.util.function.Function;

/**
 * An in-memory implementation of the {@link Publisher}.
 *
 * <p>To use only in scope of the same JVM as {@linkplain InMemorySubscriber subscribers}.
 */
public final class InMemoryPublisher extends AbstractChannel implements Publisher {

    /**
     * A provider of subscribers per message type.
     */
    private final Function<ChannelId, Iterable<Subscriber>> subscriberProvider;

    InMemoryPublisher(ChannelId id, Function<ChannelId, Iterable<Subscriber>> provider) {
        super(id);
        this.subscriberProvider = provider;
    }

    @Override
    public Ack publish(Any messageId, ExternalMessage message) {
        Iterable<Subscriber> localSubscribers = subscribers();
        for (Subscriber localSubscriber : localSubscribers) {
            localSubscriber.onMessage(message);
        }
        return MessageIdExtensionsKt.acknowledge(messageId);
    }

    private Iterable<Subscriber> subscribers() {
        return subscriberProvider.apply(id());
    }

    /**
     * Always returns {@code false} as publishers don't get stale.
     *
     * @return {@code false} always.
     */
    @Override
    public boolean isStale() {
        return false;
    }

    /**
     * Does nothing as there are no resources to close in the in-memory implementation.
     */
    @Override
    public void close() {
        // Do nothing.
    }
}
