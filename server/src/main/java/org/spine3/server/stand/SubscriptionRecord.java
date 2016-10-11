/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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
 *
 */
package org.spine3.server.stand;

import com.google.common.base.Objects;
import com.google.protobuf.Any;
import org.spine3.Internal;
import org.spine3.base.Identifiers;
import org.spine3.client.EntityFilters;
import org.spine3.client.EntityId;
import org.spine3.client.EntityIdFilter;
import org.spine3.client.Subscription;
import org.spine3.client.Target;
import org.spine3.protobuf.TypeUrl;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents the attributes of a single subscription.
 *
 * @see SubscriptionRegistry
 */
@Internal
/* package */ final class SubscriptionRecord {
    private final Subscription subscription;
    private final Target target;
    private final TypeUrl type;

    /**
     * The {@code callback} is null after the creation and until the subscription is activated.
     *
     * @see SubscriptionRegistry#addSubscription(Target)
     * @see SubscriptionRegistry#activate(Subscription, Stand.EntityUpdateCallback)
     */
    @Nullable
    private Stand.EntityUpdateCallback callback = null;

    /* package */ SubscriptionRecord(Subscription subscription, Target target, TypeUrl type) {
        this.subscription = subscription;
        this.target = target;
        this.type = type;
    }

    /**
     * Attach an activation callback to this record.
     *
     * @param callback the callback to attach
     */
    /* package */  void activate(Stand.EntityUpdateCallback callback) {
        this.callback = callback;
    }

    /**
     * Checks whether this record has a callback attached.
     */
    /* package */  boolean isActive() {
        final boolean result = this.callback != null;
        return result;
    }

    /**
     * Checks whether this record matches the given parameters.
     *
     * @param type        the type to match
     * @param id          the ID to match
     * @param entityState the entity state to match
     * @return {@code true} if this record matches all the given parameters, {@code false} otherwise.
     */
    /* package */  boolean matches(TypeUrl type,
                                   Object id,
                                   // entityState will be later used for more advanced filtering
                                   @SuppressWarnings("UnusedParameters") Any entityState) {
        final boolean result;

        final boolean typeMatches = this.type.equals(type);
        if (typeMatches) {
            final boolean includeAll = target.getIncludeAll();
            final EntityFilters filters = target.getFilters();
            result = includeAll || matchByFilters(id, filters);
        } else {
            result = false;
        }
        return result;
    }

    private static boolean matchByFilters(Object id, EntityFilters filters) {
        final boolean result;
        final EntityIdFilter givenIdFilter = filters.getIdFilter();
        final boolean idFilterSet = !EntityIdFilter.getDefaultInstance()
                                                   .equals(givenIdFilter);
        if (idFilterSet) {
            final Any idAsAny = Identifiers.idToAny(id);
            final EntityId givenEntityId = EntityId.newBuilder()
                                                   .setId(idAsAny)
                                                   .build();
            final List<EntityId> idsList = givenIdFilter.getIdsList();
            result = idsList.contains(givenEntityId);
        } else {
            result = false;
        }
        return result;
    }

    /* package */  TypeUrl getType() {
        return type;
    }

    /* package */  Stand.EntityUpdateCallback getCallback() {
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
