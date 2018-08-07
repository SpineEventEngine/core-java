/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.event.given;

import com.google.protobuf.BoolValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.UInt32Value;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.server.event.AbstractEventSubscriber;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Alexander Yevsyukov
 */
public class EventSubscriberTestEnv {

    /** Prevents instantiation of this utility class. */
    private EventSubscriberTestEnv() {
    }

    /** The subscriber which throws exception from the subscriber method. */
    public static class FailingSubscriber extends AbstractEventSubscriber {

        private boolean methodCalled = false;
        private @Nullable EventEnvelope lastErrorEnvelope;
        private @Nullable RuntimeException lastException;

        @Subscribe
        public void on(BoolValue message, EventContext context) {
            methodCalled = true;
            if (!message.getValue()) {
                throw new UnsupportedOperationException("Do not want false messages!");
            }
        }

        @Subscribe
        public void on(FloatValue message) {
            // Do nothing. Just expose the method.
        }

        @Subscribe
        public void on(UInt32Value message) {
            // Do nothing. Just expose the method.
        }

        public boolean isMethodCalled() {
            return this.methodCalled;
        }

        @Override
        public void onError(EventEnvelope envelope, RuntimeException exception) {
            super.onError(envelope, exception);
            lastErrorEnvelope = envelope;
            lastException = exception;
        }

        public @Nullable EventEnvelope getLastErrorEnvelope() {
            return lastErrorEnvelope;
        }

        public @Nullable RuntimeException getLastException() {
            return lastException;
        }
    }
}
