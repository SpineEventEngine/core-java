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
import io.spine.client.SubscriptionId;
import io.spine.client.SubscriptionVBuilder;
import io.spine.client.Subscriptions;
import io.spine.client.Topic;
import io.spine.core.TenantId;
import io.spine.server.stand.Stand.SubscriptionUpdateCallback;
import io.spine.server.tenant.TenantFunction;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.collect.Maps.newHashMap;
import static io.spine.server.stand.SubscriptionRecordFactory.newRecordFor;

/**
 * Registry for subscription management in a multi-tenant context.
 *
 * @author Alex Tymchenko
 */
final class MultitenantSubscriptionRegistry implements SubscriptionRegistry {

    /** The map from {@code TenantId} to its slice of data. */
    private final Map<TenantId, SubscriptionRegistry> tenantSlices = newConcurrentMap();

    private final boolean multitenant;

    private MultitenantSubscriptionRegistry(boolean multitenant) {
        this.multitenant = multitenant;
    }

    static MultitenantSubscriptionRegistry newInstance(boolean multitenant) {
        return new MultitenantSubscriptionRegistry(multitenant);
    }

    @Override
    public synchronized void activate(Subscription subscription,
                                      SubscriptionUpdateCallback callback) {
        registrySlice().activate(subscription, callback);
    }

    @Override
    public synchronized Subscription add(Topic topic) {
        return registrySlice().add(topic);
    }

    @Override
    public synchronized void remove(Subscription subscription) {
        registrySlice().remove(subscription);
    }

    @Override
    public synchronized Set<SubscriptionRecord> byType(TypeUrl type) {
        return registrySlice().byType(type);
    }

    @Override
    public boolean containsId(SubscriptionId subscriptionId) {
        return registrySlice().containsId(subscriptionId);
    }

    @Override
    public synchronized boolean hasType(TypeUrl type) {
        return registrySlice().hasType(type);
    }

    boolean isMultitenant() {
        return multitenant;
    }

    private SubscriptionRegistry registrySlice() {
        TenantFunction<SubscriptionRegistry> func =
                new TenantFunction<SubscriptionRegistry>(isMultitenant()) {
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
        SubscriptionRegistry result = func.execute();
        return result;
    }

    private static class TenantRegistry implements SubscriptionRegistry {

        private final Map<TypeUrl, Set<SubscriptionRecord>> typeToRecord = newHashMap();
        private final Map<Subscription, SubscriptionRecord> subscriptionToAttrs = newHashMap();

        @Override
        public synchronized void activate(Subscription subscription,
                                          SubscriptionUpdateCallback callback) {
            checkState(subscriptionToAttrs.containsKey(subscription),
                       "Cannot find the subscription in the registry.");
            SubscriptionRecord subscriptionRecord = subscriptionToAttrs.get(subscription);
            subscriptionRecord.activate(callback);
        }

        @Override
        public synchronized Subscription add(Topic topic) {
            SubscriptionId subscriptionId = Subscriptions.generateId();
            Subscription subscription = SubscriptionVBuilder
                    .newBuilder()
                    .setId(subscriptionId)
                    .setTopic(topic)
                    .build();
            SubscriptionRecord record = newRecordFor(subscription);
            TypeUrl type = record.getType();

            if (!typeToRecord.containsKey(type)) {
                typeToRecord.put(type, new HashSet<>());
            }
            typeToRecord.get(type)
                        .add(record);

            subscriptionToAttrs.put(subscription, record);
            return subscription;
        }

        @Override
        public synchronized void remove(Subscription subscription) {
            if (!subscriptionToAttrs.containsKey(subscription)) {
                return;
            }
            SubscriptionRecord record = subscriptionToAttrs.get(subscription);

            if (typeToRecord.containsKey(record.getType())) {
                typeToRecord.get(record.getType())
                            .remove(record);
            }
            subscriptionToAttrs.remove(subscription);
        }

        @Override
        public synchronized Set<SubscriptionRecord> byType(TypeUrl type) {
            Set<SubscriptionRecord> result = typeToRecord.get(type);
            return result;
        }

        @Override
        public synchronized boolean hasType(TypeUrl type) {
            boolean result = typeToRecord.containsKey(type);
            return result;
        }

        @Override
        public boolean containsId(SubscriptionId subscriptionId) {
            for (Subscription existingItem : subscriptionToAttrs.keySet()) {
                if (existingItem.getId()
                                .equals(subscriptionId)) {
                    return true;
                }
            }
            return false;
        }
    }
}
