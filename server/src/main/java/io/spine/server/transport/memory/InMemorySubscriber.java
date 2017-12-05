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
package io.spine.server.transport.memory;

import com.google.common.collect.ImmutableSet;
import io.grpc.stub.StreamObserver;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.transport.Subscriber;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newConcurrentHashSet;

/**
 * An in-memory implementation of the {@link Subscriber}.
 *
 * <p>To use only in scope of the same JVM as {@linkplain InMemoryPublisher publishers}.
 *
 * @author Alex Tymchenko
 */
final class InMemorySubscriber extends AbstractInMemoryChannel implements Subscriber {

    /**
     * Observers, that actually are informed about the messages arriving through this channel.
     */
    private final Set<StreamObserver<ExternalMessage>> observers = newConcurrentHashSet();

    InMemorySubscriber(ChannelId channelId) {
        super(channelId);
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
