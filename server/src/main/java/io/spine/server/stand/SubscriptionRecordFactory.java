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

final class SubscriptionRecordFactory {

    private SubscriptionRecordFactory() {
    }

    /**
     * ...
     *
     * <p>By default assumes that all non-event subscriptions are entity subscriptions.
     *
     * @param subscription
     * @return
     */
    static SubscriptionRecord newRecordFor(Subscription subscription) {
        TypeUrl type = getType(subscription);
        if (isEvent(type)) {
            return createEventRecord(subscription, type);
        }
        return createEntityRecord(subscription);
    }

    private static SubscriptionRecord createEntityRecord(Subscription subscription) {
        TypeUrl type = TypeUrl.of(EntityStateChanged.class);
        SubscriptionMatcher matcher = EntitySubscriptionMatcher.createFor(subscription);
        SubscriptionCallbackRunner callbackRunner = new EntitySubscriptionRunner(subscription);
        return new SubscriptionRecord(subscription, type, matcher, callbackRunner);
    }

    private static SubscriptionRecord createEventRecord(Subscription subscription, TypeUrl type) {
        SubscriptionMatcher matcher = EventSubscriptionMatcher.createFor(subscription);
        SubscriptionCallbackRunner callbackRunner = new EventSubscriptionRunner(subscription);
        return new SubscriptionRecord(subscription, type, matcher, callbackRunner);
    }

    private static TypeUrl getType(Subscription subscription) {
        Topic topic = subscription.getTopic();
        String typeAsString = topic.getTarget()
                                   .getType();
        TypeUrl result = TypeUrl.parse(typeAsString);
        return result;
    }

    private static boolean isEvent(TypeUrl type) {
        Class<?> javaClass = type.getJavaClass();
        boolean result = EventMessage.class.isAssignableFrom(javaClass);
        return result;
    }
}
