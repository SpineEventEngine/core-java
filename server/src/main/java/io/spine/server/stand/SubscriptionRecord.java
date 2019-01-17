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

import com.google.common.base.Objects;
import io.spine.client.Subscription;
import io.spine.core.EventEnvelope;
import io.spine.server.stand.Stand.NotifySubscriptionAction;
import io.spine.type.TypeUrl;

/**
 * A {@link SubscriptionRegistry} entry that manages a single {@link Subscription}.
 */
final class SubscriptionRecord {

    private final Subscription subscription;
    private final TypeUrl type;
    private final SubscriptionMatcher matcher;
    private final SubscriptionCallback callback;

    SubscriptionRecord(Subscription subscription,
                       TypeUrl type,
                       SubscriptionMatcher matcher,
                       SubscriptionCallback callback) {
        this.subscription = subscription;
        this.type = type;
        this.matcher = matcher;
        this.callback = callback;
    }

    /**
     * Attach an activation callback to this record.
     *
     * @param notifyAction the callback to attach
     */
    void activate(Stand.NotifySubscriptionAction notifyAction) {
        callback.setNotifyAction(notifyAction);
    }

    /**
     *
     * @param event
     * @throws IllegalStateException
     *         if the subscription is not activated
     * @see #activate(NotifySubscriptionAction)
     */
    void update(EventEnvelope event) {
        callback.run(event);
    }

    /**
     * Checks whether this record has a callback attached.
     */
    boolean isActive() {
        return callback.isActive();
    }

    /**
     * Checks whether this record matches the given parameters.
     *
     * @param event       the event to match
     * @return {@code true} if this record matches all the given parameters,
     * {@code false} otherwise.
     */
    boolean matches(EventEnvelope event) {
        return matcher.test(event);
    }

    TypeUrl getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubscriptionRecord)) {
            return false;
        }
        SubscriptionRecord that = (SubscriptionRecord) o;
        return Objects.equal(subscription, that.subscription);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subscription);
    }
}
