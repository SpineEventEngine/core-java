/*
 * Copyright 2020, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.spine.client.Topic;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantFunction;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

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
    public void add(Subscription subscription) {
        registrySlice().add(subscription);
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

    private boolean isMultitenant() {
        return multitenant;
    }

    private SubscriptionRegistry registrySlice() {
        TenantFunction<SubscriptionRegistry> func =
                new TenantFunction<SubscriptionRegistry>(isMultitenant()) {
                    @Override
                    public SubscriptionRegistry apply(@Nullable TenantId tenantId) {
                        checkNotNull(tenantId);
                        SubscriptionRegistry slice = tenantSlices.computeIfAbsent(
                                tenantId,
                                id -> new TenantSubscriptionRegistry()
                        );
                        return slice;
                    }
                };
        SubscriptionRegistry result = func.execute();
        return result;
    }
}
