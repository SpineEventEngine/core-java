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
import io.spine.base.EventMessage;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.core.EventEnvelope;
import io.spine.server.stand.Stand.SubscriptionUpdateCallback;
import io.spine.system.server.EntityStateChanged;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the attributes of a single subscription.
 *
 * @see SubscriptionRegistry
 */
final class SubscriptionRecord {

    private final Subscription subscription;
    private final TypeUrl type;
    private final SubscriptionMatcher matcher;

    /**
     * The {@code callback} is null after the creation and until the subscription is activated.
     *
     * @see SubscriptionRegistry#add(Topic)
     * @see SubscriptionRegistry#activate(Subscription, SubscriptionUpdateCallback)
     */
    private @Nullable SubscriptionUpdateCallback callback = null;

    private SubscriptionRecord(Subscription subscription,
                               TypeUrl type,
                               SubscriptionMatcher matcher) {
        this.subscription = subscription;
        this.type = type;
        this.matcher = matcher;
    }

    static SubscriptionRecord of(Subscription subscription) {
        Topic topic = subscription.getTopic();
        String typeAsString = topic.getTarget()
                                   .getType();
        TypeUrl type = TypeUrl.parse(typeAsString);
        Class<?> javaClass = type.getJavaClass();
        if (EventMessage.class.isAssignableFrom(javaClass)) {
            type = TypeUrl.of(EntityStateChanged.class);
        }
        SubscriptionMatcher matcher = SubscriptionMatcher.of(subscription);
        return new SubscriptionRecord(subscription, type, matcher);
    }

    /**
     * Attach an activation callback to this record.
     *
     * @param callback the callback to attach
     */
    void activate(SubscriptionUpdateCallback callback) {
        this.callback = callback;
    }

    /**
     * Checks whether this record has a callback attached.
     */
    boolean isActive() {
        boolean result = this.callback != null;
        return result;
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

    @Nullable
    SubscriptionUpdateCallback getCallback() {
        return callback;
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
