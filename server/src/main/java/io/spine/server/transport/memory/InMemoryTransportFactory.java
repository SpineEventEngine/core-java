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
package io.spine.server.transport.memory;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.spine.server.integration.ChannelId;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * In-memory implementation of the {@link TransportFactory}.
 *
 * <p>Publishers and subscribers must be in the same JVM. Therefore this factory usage should
 * be limited to tests.
 *
 * @author Alex Tymchenko
 */
public class InMemoryTransportFactory implements TransportFactory {

    /**
     * An in-memory storage of subscribers per message class.
     */
    private final Multimap<ChannelId, InMemorySubscriber> subscribers =
            synchronizedMultimap(HashMultimap.<ChannelId, InMemorySubscriber>create());

    /** Prevent direct instantiation from outside of the inheritance tree. */
    protected InMemoryTransportFactory() {}

    /**
     * Creates a new instance of {@code InMemoryTransportFactory}.
     *
     * @return a new instance of this factory
     */
    public static InMemoryTransportFactory newInstance() {
        return new InMemoryTransportFactory();
    }

    @Override
    public final synchronized Publisher createPublisher(ChannelId channelId) {
        final InMemoryPublisher result = new InMemoryPublisher(channelId,
                                                               providerOf(subscribers));
        return result;
    }

    @Override
    public final synchronized Subscriber createSubscriber(ChannelId channelId) {
        final InMemorySubscriber subscriber = newSubscriber(channelId);
        subscribers.put(channelId, subscriber);
        return subscriber;
    }

    protected InMemorySubscriber newSubscriber(ChannelId channelId) {
        return new InMemorySubscriber(channelId);
    }

    /**
     * Wraps currently registered in-memory subscribers into a function, that returns a subset
     * of subscribers per channel ID.
     *
     * @param subscribers currently registered subscribers and their channel identifiers
     * @return a provider function allowing to fetch subscribers by the channel ID.
     */
    private static Function<ChannelId, Iterable<InMemorySubscriber>>
    providerOf(final Multimap<ChannelId, InMemorySubscriber> subscribers) {
        return new Function<ChannelId, Iterable<InMemorySubscriber>>() {
            @Override
            public Iterable<InMemorySubscriber> apply(@Nullable ChannelId channelId) {
                checkNotNull(channelId);
                return subscribers.get(channelId);
            }
        };
    }
}
