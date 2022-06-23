/*
 * Copyright 2022, TeamDev. All rights reserved.
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
package io.spine.server.transport;

import io.spine.annotation.SPI;
import io.spine.server.Closeable;

/**
 * A factory for creating channel-based transport for {@code Message} inter-exchange between the
 * current deployment component and other application parts.
 *
 * Inspired by <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/PublishSubscribeChannel.html">
 * Publish-Subscriber Channel pattern.</a>
 */
@SPI
public interface TransportFactory extends Closeable {

    /**
     * Creates a {@link Publisher} channel with the given ID.
     *
     * @param id
     *         the identifier of the resulting channel
     * @return a new {@code Publisher} instance
     */
    Publisher createPublisher(ChannelId id);

    /**
     * Creates a {@link Subscriber} channel with the given ID.
     *
     * @param id
     *         the identifier of the resulting channel
     * @return a new {@code Subscriber} instance
     */
    Subscriber createSubscriber(ChannelId id);
}
