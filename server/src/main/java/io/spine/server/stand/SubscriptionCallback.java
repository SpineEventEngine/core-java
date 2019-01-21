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
import io.spine.server.stand.Stand.NotifySubscriptionAction;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.base.Preconditions.checkState;

/**
 * A callback that updates a subscription based on the incoming event.
 *
 * <p>This callback is meant to be run on the subscription which was already matched to the event
 * and thus doesn't do any type checking and filter evaluation.
 *
 * <p>The class is abstract because the event and entity subscriptions receive different kinds of
 * updates. See descendants for details.
 */
abstract class SubscriptionCallback {

    /**
     * A subscription to include in the update.
     */
    private final Subscription subscription;

    /**
     * An action which accepts the update and notifies the read-side accordingly.
     */
    private @MonotonicNonNull NotifySubscriptionAction notifyAction = null;

    SubscriptionCallback(Subscription subscription) {
        this.subscription = subscription;
    }

    /**
     * Runs the subscription update with the incoming event.
     *
     * @throws IllegalStateException
     *         if the subscription hadn't been activated yet
     */
    protected void run(EventEnvelope event) {
        checkState(isActive(), "Notifying by a non-activated subscription.");
        SubscriptionUpdate update = createSubscriptionUpdate(event);
        notifyAction.accept(update);
    }

    /**
     * "Activates" this callback with a given action.
     */
    void setNotifyAction(NotifySubscriptionAction action) {
        this.notifyAction = action;
    }

    /**
     * Checks if this callback has a notify action set.
     */
    boolean isActive() {
        return notifyAction != null;
    }

    public Subscription subscription() {
        return subscription;
    }

    /**
     * Creates a subscription update based on the incoming event.
     */
    protected abstract SubscriptionUpdate createSubscriptionUpdate(EventEnvelope event);
}
