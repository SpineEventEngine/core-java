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

import io.spine.server.integration.ChannelId;
import io.spine.server.integration.ExternalMessage;
import io.spine.server.transport.Subscriber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Alex Tymchenko
 */
public class SynchronizedInMemSubscriber extends Subscriber {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    SynchronizedInMemSubscriber(ChannelId channelId) {
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
    public void close() throws Exception {
        executor.shutdown();
    }
}
