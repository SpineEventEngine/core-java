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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Subscriptions;
import io.spine.client.Topic;
import io.spine.core.TenantId;
import io.spine.server.stand.Stand.SubscriptionCallback;
import io.spine.server.tenant.TenantFunction;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;
import static io.spine.server.stand.SubscriptionRecordFactory.newRecordFor;

/**
 * Registry for subscription management in a multi-tenant context.
 */
final class MultitenantSubscriptionRegistry implements SubscriptionRegistry {

    /** The map from {@code TenantId} to its slice of data. */
    private final Map<TenantId, SubscriptionRegistry> tenantSlices = new ConcurrentHashMap<>();

    private final boolean multitenant;

    private MultitenantSubscriptionRegistry(boolean multitenant) {
        this.multitenant = multitenant;
    }

    static MultitenantSubscriptionRegistry newInstance(boolean multitenant) {
        return new MultitenantSubscriptionRegistry(multitenant);
    }

    @Override
    public void activate(Subscription subscription, SubscriptionCallback callback) {
        registrySlice().activate(subscription, callback);
    }

    @Override
    public Subscription add(Topic topic) {
        return registrySlice().add(topic);
    }

    @Override
    public void remove(Subscription subscription) {
        registrySlice().remove(subscription);
    }

    @Override
    public Set<SubscriptionRecord> byType(TypeUrl type) {
        return registrySlice().byType(type);
    }

    @Override
    public boolean containsId(SubscriptionId subscriptionId) {
        return registrySlice().containsId(subscriptionId);
    }

    @Override
    public boolean hasType(TypeUrl type) {
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

        private final SetMultimap<TypeUrl, SubscriptionRecord> typeToRecord =
                synchronizedSetMultimap(HashMultimap.create());
        private final Map<Subscription, SubscriptionRecord> subscriptionToAttrs =
                new ConcurrentHashMap<>();
        private final Lock lock = new ReentrantLock();

        @Override
        public void activate(Subscription subscription, SubscriptionCallback callback) {
            lockAndRun(() -> {
                checkState(subscriptionToAttrs.containsKey(subscription),
                           "Cannot find the subscription in the registry.");
                SubscriptionRecord subscriptionRecord = subscriptionToAttrs.get(subscription);
                subscriptionRecord.activate(callback);
            });
        }

        @Override
        public Subscription add(Topic topic) {
            SubscriptionId subscriptionId = Subscriptions.generateId();
            Subscription subscription = Subscription
                    .newBuilder()
                    .setId(subscriptionId)
                    .setTopic(topic)
                    .build();
            SubscriptionRecord record = newRecordFor(subscription);
            TypeUrl type = record.getType();
            typeToRecord.put(type, record);
            subscriptionToAttrs.put(subscription, record);
            return subscription;
        }

        @Override
        public void remove(Subscription subscription) {
            lockAndRun(() -> {
                if (!subscriptionToAttrs.containsKey(subscription)) {
                    return;
                }
                SubscriptionRecord record = subscriptionToAttrs.get(subscription);
                typeToRecord.get(record.getType()).remove(record);
                subscriptionToAttrs.remove(subscription);
            });
        }

        @Override
        public Set<SubscriptionRecord> byType(TypeUrl type) {
            Set<SubscriptionRecord> result = typeToRecord.get(type);
            return result;
        }

        @Override
        public boolean hasType(TypeUrl type) {
            boolean result = typeToRecord.containsKey(type);
            return result;
        }

        @Override
        public boolean containsId(SubscriptionId subscriptionId) {
            for (Subscription existingItem : subscriptionToAttrs.keySet()) {
                if (existingItem.getId().equals(subscriptionId)) {
                    return true;
                }
            }
            return false;
        }

        private void lockAndRun(Runnable operation) {
            lock.lock();
            try {
                operation.run();
            } finally {
                lock.unlock();
            }
        }
    }
}
