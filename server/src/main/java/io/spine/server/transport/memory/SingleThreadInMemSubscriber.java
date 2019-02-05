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
package io.spine.server.transport.memory;

import io.spine.base.Identifier;
import io.spine.logging.Logging;
import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * The in-memory subscriber, which uses single-thread delivery of messages.
 *
 * <p>The messages posted by any number of concurrent publishers are delivered to the observers
 * of this subscriber one-by-one in a single thread.
 *
 * <p>This implementation should not be used in production environments, as it is not designed
 * to operate with external transport.
 */
class SingleThreadInMemSubscriber extends InMemorySubscriber implements Logging {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    SingleThreadInMemSubscriber(ChannelId channelId) {
        super(channelId);
    }

    @SuppressWarnings("FutureReturnValueIgnored") // Error handling is done manually.
    @Override
    public void onMessage(ExternalMessage message) {
        CompletableFuture.runAsync(() -> callObservers(message), executor)
                         .exceptionally(throwable -> logError(throwable, message));
    }

    /**
     * Conveniently logs an error in {@link CompletableFuture#exceptionally(Function)}.
     *
     * @return {@code null} always
     */
    private @Nullable Void logError(Throwable throwable, ExternalMessage message) {
        Object id = Identifier.unpack(message.getId());
        _error(throwable,
               "Error dispatching an external message {} with ID {} to observers: {}",
               message.getOriginalMessage().getTypeUrl(),
               id,
               throwable.getLocalizedMessage());
        return null;
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
