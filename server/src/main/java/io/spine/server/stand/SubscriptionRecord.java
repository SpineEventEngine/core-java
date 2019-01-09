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
import com.google.protobuf.Any;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.client.EntityFilters;
import io.spine.client.EntityId;
import io.spine.client.EntityIdFilter;
import io.spine.client.Subscription;
import io.spine.client.Target;
import io.spine.client.Topic;
import io.spine.server.stand.Stand.OnEventCallback;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * Represents the attributes of a single subscription.
 *
 * @see SubscriptionRegistry
 */
final class SubscriptionRecord {

    private final Subscription subscription;
    private final Target target;
    private final TypeUrl type;

    /**
     * The {@code callback} is null after the creation and until the subscription is activated.
     *
     * @see SubscriptionRegistry#add(Topic)
     * @see SubscriptionRegistry#activate(Subscription, OnEventCallback)
     */
    private @Nullable OnEventCallback callback = null;

    SubscriptionRecord(Subscription subscription, Target target, TypeUrl type) {
        this.subscription = subscription;
        this.target = target;
        this.type = type;
    }

    /**
     * Attach an activation callback to this record.
     *
     * @param callback the callback to attach
     */
    void activate(OnEventCallback callback) {
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
     * @param type        the type to match
     * @param event       the event to match
     * @return {@code true} if this record matches all the given parameters,
     * {@code false} otherwise.
     */
    boolean matches(TypeUrl type, EventMessage event) {
        boolean typeMatches = this.type.equals(type);
        if (typeMatches) {
            boolean includeAll = target.getIncludeAll();
            EntityFilters filters = target.getFilters();
            return includeAll || matchByFilters(event, filters);
        }
        return false;
    }

    private static boolean matchByFilters(EventMessage event, EntityFilters filters) {
        boolean result;
        EntityIdFilter givenIdFilter = filters.getIdFilter();
        boolean idFilterSet = !EntityIdFilter.getDefaultInstance()
                                             .equals(givenIdFilter);
        if (idFilterSet) {
            Any idAsAny = Identifier.pack(event);
            EntityId givenEntityId = EntityId.newBuilder()
                                             .setId(idAsAny)
                                             .build();
            List<EntityId> idsList = givenIdFilter.getIdsList();
            result = idsList.contains(givenEntityId);
        } else {
            result = false;
        }
        return result;
    }

    TypeUrl getType() {
        return type;
    }

    @Nullable
    OnEventCallback getCallback() {
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
