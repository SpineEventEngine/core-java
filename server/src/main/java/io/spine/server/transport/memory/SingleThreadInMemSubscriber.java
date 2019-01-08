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

import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The in-memory subscriber, which uses single-thread delivery of messages.
 *
 * <p>The messages posted by any number of concurrent publishers are delivered to the observers
 * of this subscriber one-by-one in a single thread.
 *
 * <p>This implementation should not be used in production environments, as it is not designed
 * to operate with external transport.
 *
 * @author Alex Tymchenko
 */
class SingleThreadInMemSubscriber extends InMemorySubscriber {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    SingleThreadInMemSubscriber(ChannelId channelId) {
        super(channelId);
    }

    @Override
    public void onMessage(ExternalMessage message) {
        executor.submit(new SubmissionJob(message));
    }

    private final class SubmissionJob implements Runnable {

        private final ExternalMessage message;

        private SubmissionJob(ExternalMessage message) {
            this.message = message;
        }

        @Override
        public void run() {
            callObservers(message);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
