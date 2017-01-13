/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package org.spine3.server.stand;

import org.spine3.base.Identifiers;
import org.spine3.client.Subscription;
import org.spine3.client.Target;
import org.spine3.protobuf.TypeUrl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Registry for subscription management.
 *
 * <p>Provides a quick access to the subscription records by {@link TypeUrl}.
 *
 * <p>Responsible for {@link Subscription} object instantiation.
 *
 * @author Alex Tymchenko
 */
final class SubscriptionRegistry {
    private final Map<TypeUrl, Set<SubscriptionRecord>> typeToAttrs = newHashMap();
    private final Map<Subscription, SubscriptionRecord> subscriptionToAttrs = newHashMap();

    /**
     * Activate the subscription with the passed callback.
     *
     * <p>The callback passed will become associated with the subscription.
     *
     * @param subscription the subscription to activate
     * @param callback     the callback to make active
     */
    synchronized void activate(Subscription subscription, Stand.EntityUpdateCallback callback) {
        checkState(subscriptionToAttrs.containsKey(subscription),
                   "Cannot find the subscription in the registry.");
        final SubscriptionRecord subscriptionRecord = subscriptionToAttrs.get(subscription);
        subscriptionRecord.activate(callback);
    }

    /**
     * Creates a subscription for the passed {@code Target} and adds it to the registry.
     *
     * @param target the target for a new subscription
     * @return the created subscription
     */
    synchronized Subscription addSubscription(Target target) {
        final String subscriptionId = Identifiers.newUuid();
        final String typeAsString = target.getType();
        final TypeUrl type = TypeUrl.of(typeAsString);
        final Subscription subscription = Subscription.newBuilder()
                                                      .setId(subscriptionId)
                                                      .setType(typeAsString)
                                                      .build();
        final SubscriptionRecord attributes = new SubscriptionRecord(subscription, target, type);

        if (!typeToAttrs.containsKey(type)) {
            typeToAttrs.put(type, new HashSet<SubscriptionRecord>());
        }
        typeToAttrs.get(type)
                   .add(attributes);

        subscriptionToAttrs.put(subscription, attributes);
        return subscription;
    }

    /**
     * Remove the subscription from this registry.
     *
     * <p>If there is no such subscription in this instance of {@code SubscriptionRegistry}, invocation has no effect.
     *
     * @param subscription the subscription to remove
     */
    synchronized void removeSubscription(Subscription subscription) {
        if (!subscriptionToAttrs.containsKey(subscription)) {
            return;
        }
        final SubscriptionRecord attributes = subscriptionToAttrs.get(subscription);

        if (typeToAttrs.containsKey(attributes.getType())) {
            typeToAttrs.get(attributes.getType())
                       .remove(attributes);
        }
        subscriptionToAttrs.remove(subscription);
    }

    /**
     * Filter the registered {@link SubscriptionRecord}s by their type.
     *
     * @param type the type to filter by
     * @return the collection of filtered records
     */
    synchronized Set<SubscriptionRecord> byType(TypeUrl type) {
        final Set<SubscriptionRecord> result = typeToAttrs.get(type);
        return result;
    }

    /**
     * Checks whether the current registry has the records related to a given type.
     *
     * @param type the type to check records for
     * @return {@code true} if there are records with the given type, {@code false} otherwise
     */
    synchronized boolean hasType(TypeUrl type) {
        final boolean result = typeToAttrs.containsKey(type);
        return result;
    }
}
