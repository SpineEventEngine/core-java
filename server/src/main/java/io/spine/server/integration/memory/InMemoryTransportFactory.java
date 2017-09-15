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
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.spine.server.integration.Publisher;
import io.spine.server.integration.Subscriber;
import io.spine.server.integration.TransportFactory;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.integration.ExternalMessageClass.of;

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
    private final Multimap<MessageClass, InMemorySubscriber> subscribers =
            Multimaps.synchronizedMultimap(HashMultimap.<MessageClass, InMemorySubscriber>create());

    /** Prevent direct instantiation from the outside. */
    private InMemoryTransportFactory() {}

    /**
     * Creates a new instance of {@code InMemoryTransportFactory}.
     *
     * @return a new instance of this factory
     */
    public static InMemoryTransportFactory newInstance() {
        return new InMemoryTransportFactory();
    }

    @Override
    public Publisher createPublisher(MessageClass messageClass) {
        final InMemoryPublisher result = new InMemoryPublisher(of(messageClass),
                                                               providerOf(subscribers));
        return result;
    }

    @Override
    public Subscriber createSubscriber(MessageClass messageClass) {
        final InMemorySubscriber subscriber = new InMemorySubscriber(of(messageClass));
        subscribers.put(messageClass, subscriber);
        return subscriber;
    }

    /**
     * Wraps currently registered in-memory subscribers into a function, that returns a subset
     * of subscribers per message.
     *
     * @param subscribers currently registered subscribers and classes of messages they serve
     * @return a provider function allowing to fetch subscribers per message class.
     */
    private static Function<MessageClass, Iterable<InMemorySubscriber>>
    providerOf(final Multimap<MessageClass, InMemorySubscriber> subscribers) {
        return new Function<MessageClass, Iterable<InMemorySubscriber>>() {
            @Override
            public Iterable<InMemorySubscriber> apply(@Nullable MessageClass input) {
                checkNotNull(input);
                return subscribers.get(input);
            }
        };
    }
}
