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

package io.spine.server.stand;

import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.core.EventEnvelope;
import io.spine.server.stand.Stand.SubscriptionUpdateCallback;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.base.Preconditions.checkState;

abstract class SubscriptionCallbackRunner {

    private final Subscription subscription;

    private @MonotonicNonNull SubscriptionUpdateCallback callback = null;

    SubscriptionCallbackRunner(Subscription subscription) {
        this.subscription = subscription;
    }

    protected void run(EventEnvelope event) {
        checkState(isActive(), "Notifying by a non-activated subscription.");
        SubscriptionUpdate update = buildSubscriptionUpdate(event);
        callback.run(update);
    }

    void setCallback(SubscriptionUpdateCallback callback) {
        this.callback = callback;
    }

    boolean isActive() {
        return callback == null;
    }

    public Subscription subscription() {
        return subscription;
    }

    protected abstract SubscriptionUpdate buildSubscriptionUpdate(EventEnvelope event);
}
