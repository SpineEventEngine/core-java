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

import org.spine3.client.Subscription;
import org.spine3.client.SubscriptionId;
import org.spine3.client.Subscriptions;
import org.spine3.client.Target;
import org.spine3.client.Topic;
import org.spine3.server.tenant.TenantFunction;
import org.spine3.type.TypeName;
import org.spine3.type.TypeUrl;
import org.spine3.users.TenantId;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Registry for subscription management in a multi-tenant context.
 *
 * @author Alex Tymchenko
 */
final class MultitenantSubscriptionRegistry implements SubscriptionRegistry {

    /** The map from {@code TenantId} to its slice of data. */
    private final Map<TenantId, SubscriptionRegistry> tenantSlices = newConcurrentMap();

    private final boolean multitenant;

    MultitenantSubscriptionRegistry(boolean multitenant) {
        this.multitenant = multitenant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void activate(Subscription subscription,
                                      Stand.EntityUpdateCallback callback) {
        registrySlice().activate(subscription, callback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Subscription addSubscription(Topic topic) {
        return registrySlice().addSubscription(topic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void removeSubscription(Subscription subscription) {
        registrySlice().removeSubscription(subscription);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Set<SubscriptionRecord> byType(TypeUrl type) {
        return registrySlice().byType(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean hasType(TypeUrl type) {
        return registrySlice().hasType(type);
    }

    boolean isMultitenant() {
        return multitenant;
    }

    private SubscriptionRegistry registrySlice() {
        final TenantFunction<SubscriptionRegistry> func =
                new TenantFunction<SubscriptionRegistry>(isMultitenant()) {
                    @Nullable
                    @Override
                    public SubscriptionRegistry apply(@Nullable TenantId tenantId) {
                        checkNotNull(tenantId);
                        SubscriptionRegistry registryForTenant = tenantSlices.get(tenantId);
                        if (registryForTenant == null) {
                            registryForTenant = new TenantRegistry();
                            tenantSlices.put(tenantId, registryForTenant);
                        }
                        return registryForTenant;
                    }
                };
        final SubscriptionRegistry result = func.execute();
        return result;
    }

    private static class TenantRegistry implements SubscriptionRegistry {

        private final Map<TypeUrl, Set<SubscriptionRecord>> typeToRecord = newHashMap();
        private final Map<Subscription, SubscriptionRecord> subscriptionToAttrs = newHashMap();

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void activate(Subscription subscription,
                                          Stand.EntityUpdateCallback callback) {
            checkState(subscriptionToAttrs.containsKey(subscription),
                       "Cannot find the subscription in the registry.");
            final SubscriptionRecord subscriptionRecord = subscriptionToAttrs.get(subscription);
            subscriptionRecord.activate(callback);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized Subscription addSubscription(Topic topic) {
            final SubscriptionId subscriptionId = Subscriptions.newId();
            final Target target = topic.getTarget();
            final String typeAsString = target
                    .getType();
            final TypeUrl type = TypeName.of(typeAsString)
                                         .toUrl();
            final Subscription subscription = Subscription.newBuilder()
                                                          .setId(subscriptionId)
                                                          .setTopic(topic)
                                                          .build();
            final SubscriptionRecord record = new SubscriptionRecord(subscription, target, type);

            if (!typeToRecord.containsKey(type)) {
                typeToRecord.put(type, new HashSet<SubscriptionRecord>());
            }
            typeToRecord.get(type)
                        .add(record);

            subscriptionToAttrs.put(subscription, record);
            return subscription;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized void removeSubscription(Subscription subscription) {
            if (!subscriptionToAttrs.containsKey(subscription)) {
                return;
            }
            final SubscriptionRecord record = subscriptionToAttrs.get(subscription);

            if (typeToRecord.containsKey(record.getType())) {
                typeToRecord.get(record.getType())
                            .remove(record);
            }
            subscriptionToAttrs.remove(subscription);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized Set<SubscriptionRecord> byType(TypeUrl type) {
            final Set<SubscriptionRecord> result = typeToRecord.get(type);
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public synchronized boolean hasType(TypeUrl type) {
            final boolean result = typeToRecord.containsKey(type);
            return result;
        }
    }
}
