/*
 * Copyright 2020, TeamDev. All rights reserved.
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionId;
import io.spine.client.Subscriptions;
import io.spine.client.Topic;
import io.spine.type.TypeUrl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Multimaps.synchronizedSetMultimap;

/**
 * A slice with subscriptions belonging to one tenant in a multi-tenant application.
 */
final class TenantSubscriptionRegistry implements SubscriptionRegistry {

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
        Subscription subscription = Subscriptions.from(topic);
        add(subscription);
        return subscription;
    }

    @Override
    public void add(Subscription subscription) {
        SubscriptionRecord record = SubscriptionRecord.of(subscription);
        TypeUrl type = record.targetType();
        lockAndRun(() -> {
            typeToRecord.put(type, record);
            subscriptionToAttrs.put(subscription, record);
        });
    }

    @Override
    public void remove(Subscription subscription) {
        lockAndRun(() -> {
            if (!subscriptionToAttrs.containsKey(subscription)) {
                return;
            }
            SubscriptionRecord record = subscriptionToAttrs.get(subscription);
            typeToRecord.remove(record.targetType(), record);
            subscriptionToAttrs.remove(subscription);
        });
    }

    @Override
    public Set<SubscriptionRecord> byType(TypeUrl type) {
        return ImmutableSet.copyOf(lockAndGet(() -> typeToRecord.get(type)));
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

    private <T> T lockAndGet(Supplier<T> operation) {
        lock.lock();
        try {
            return operation.get();
        } finally {
            lock.unlock();
        }
    }
}
