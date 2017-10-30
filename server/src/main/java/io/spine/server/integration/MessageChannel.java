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

/**
 * A channel for exchanging the {@link com.google.protobuf.Message Message}s.
 *
 * <p>Identified by a {@linkplain ChannelId channel ID}, which must be unique in scope of the
 * underlying messaging system.
 *
 * <p>The identifier is also used to specify the set of {@code com.google.protobuf.Message}s,
 * suitable for this channel. The definition of the matching criterion is done according to the
 * {@linkplain ChannelId#getKindCase() kind} value.
 *
 * @author Alex Tymchenko
 * @author Dmitry Ganzha
 * @see ChannelId
 */
public interface MessageChannel extends AutoCloseable {

    /**
     * Returns the channel identifier.
     *
     * @return the channel identifier
     */
    ChannelId getChannelId();

    /**
     * Allows to understand whether this channel is stale and can be closed.
     *
     * @return {@code true} if the channel is stale, {@code false} otherwise
     */
    boolean isStale();
}
