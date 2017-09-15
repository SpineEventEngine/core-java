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
package io.spine.server.integration;

import io.grpc.stub.StreamObserver;

/**
 * Subscriber for messages of a specific type.
 *
 * <p>There can be many subscribers per message type.
 *
 */
public interface Subscriber extends MessageChannel {

    /**
     * Obtains current observers registered in this instance of {@code Subscriber},
     * which receive the subscription updates.
     *
     * @return observers for this subscriber
     */
    Iterable<StreamObserver<ExternalMessage>> getObservers();

    /**
     * Adds an observer, which will be receiving the subscription updates.
     *
     * @param observer an observer to register
     */
    void addObserver(StreamObserver<ExternalMessage> observer);

    /**
     * Removes an existing observer and disconnects it from this subscription channel.
     *
     * <p>In case the given observer is not registered at the moment, does nothing.
     *
     * @param observer an observer to remove
     */
    void removeObserver(StreamObserver<ExternalMessage> observer);
}
