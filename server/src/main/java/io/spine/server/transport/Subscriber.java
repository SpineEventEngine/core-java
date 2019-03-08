/*
 * Copyright 2019, TeamDev. All rights reserved.
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
package io.spine.server.transport;

import com.google.common.collect.ImmutableSet;
import io.grpc.stub.StreamObserver;
import io.spine.annotation.SPI;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newConcurrentHashSet;

/**
 * Subscriber for messages of a channel with the {@linkplain MessageChannel#getId() specified ID}.
 *
 * <p>There can be many subscribers per channel ID.
 */
@SPI
public abstract class Subscriber extends AbstractChannel {

    /**
     * Observers, that actually are informed about the messages arriving through this channel.
     */
    private final Set<StreamObserver<ExternalMessage>> observers = newConcurrentHashSet();

    protected Subscriber(ChannelId channelId) {
        super(channelId);
    }

    /**
     * Obtains current observers registered in this instance of {@code Subscriber},
     * which receive the subscription updates.
     *
     * @return observers for this subscriber
     */
    public Iterable<StreamObserver<ExternalMessage>> getObservers() {
        return ImmutableSet.copyOf(observers);
    }

    /**
     * Adds an observer, which will be receiving the subscription updates.
     *
     * @param observer an observer to register
     */
    public void addObserver(StreamObserver<ExternalMessage> observer) {
        checkNotNull(observer);
        observers.add(observer);
    }

    /**
     * Removes an existing observer and disconnects it from this subscription channel.
     *
     * <p>In case the given observer is not registered at the moment, does nothing.
     *
     * @param observer an observer to remove
     */
    public void removeObserver(StreamObserver<ExternalMessage> observer) {
        checkNotNull(observer);
        observers.remove(observer);
    }

    @Override
    public boolean isStale() {
        return observers.isEmpty();
    }

    /**
     * Passes the given message to observers.
     *
     * @param message the subscription update to pass to the observers for this channel
     */
    public void onMessage(ExternalMessage message) {
        callObservers(message);
    }

    protected final void callObservers(ExternalMessage message) {
        for (StreamObserver<ExternalMessage> observer : getObservers()) {
            observer.onNext(message);
        }
    }
}
