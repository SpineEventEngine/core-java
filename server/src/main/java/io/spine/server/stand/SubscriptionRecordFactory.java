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

import io.spine.base.EventMessage;
import io.spine.client.Subscription;
import io.spine.client.Topic;
import io.spine.system.server.EntityStateChanged;
import io.spine.type.TypeUrl;

/**
 * A factory which assists in subscription record creation.
 */
final class SubscriptionRecordFactory {

    /** Prevents the instantiation of this class. */
    private SubscriptionRecordFactory() {
    }

    /**
     * Creates a subscription record for the given subscription.
     *
     * <p>Distinguishes event and entity subscriptions via the target type URL.
     *
     * <p>By default, assumes that all subscriptions with a non-event type are entity
     * subscriptions.
     */
    static SubscriptionRecord newRecordFor(Subscription subscription) {
        if (isEventSubscription(subscription)) {
            return createEventRecord(subscription);
        }
        return createEntityRecord(subscription);
    }

    /**
     * Creates a record managing an event subscription.
     */
    private static SubscriptionRecord createEventRecord(Subscription subscription) {
        TypeUrl type = getSubscriptionType(subscription);
        SubscriptionMatcher matcher = new EventSubscriptionMatcher(subscription);
        SubscriptionCallback callback = new EventSubscriptionCallback(subscription);
        return new SubscriptionRecord(subscription, type, matcher, callback);
    }

    /**
     * Creates a record managing an entity subscription.
     *
     * <p>In fact, this is a subscription to an {@link EntityStateChanged} event with a custom
     * callback and matcher (to validate the entity state packed inside the event).
     */
    private static SubscriptionRecord createEntityRecord(Subscription subscription) {
        TypeUrl type = TypeUrl.of(EntityStateChanged.class);
        SubscriptionMatcher matcher = new EntitySubscriptionMatcher(subscription);
        SubscriptionCallback callback = new EntitySubscriptionCallback(subscription);
        return new SubscriptionRecord(subscription, type, matcher, callback);
    }

    private static boolean isEventSubscription(Subscription subscription) {
        TypeUrl type = getSubscriptionType(subscription);
        Class<?> javaClass = type.getJavaClass();
        boolean result = EventMessage.class.isAssignableFrom(javaClass);
        return result;
    }

    private static TypeUrl getSubscriptionType(Subscription subscription) {
        Topic topic = subscription.getTopic();
        String typeAsString = topic.getTarget()
                                   .getType();
        TypeUrl result = TypeUrl.parse(typeAsString);
        return result;
    }
}
