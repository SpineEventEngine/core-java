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

import com.google.protobuf.Any;
import io.spine.client.IdFilter;
import io.spine.client.Subscription;
import io.spine.client.SubscriptionUpdate;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.logging.Logging;
import io.spine.server.type.EventEnvelope;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Optional;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Handles the domain and system events which correspond to the specified {@code Subscription}.
 *
 * <p>Compares the event data to the filtering criteria of the {@code Subscription} and
 * notifies the {@linkplain #setCallback(SubscriptionCallback) callback} with
 * the detected {@linkplain SubscriptionUpdate subscription updates}.
 */
abstract class UpdateHandler implements Logging {

    private final Subscription subscription;

    /**
     * An action which accepts the update and notifies the read-side accordingly.
     */
    private @MonotonicNonNull SubscriptionCallback callback = null;

    /**
     * Creates an update handler acting according to the criteria of the passed
     * {@code Subscription}.
     */
    UpdateHandler(Subscription subscription) {
        this.subscription = subscription;
    }

    /**
     * Analyzes the incoming event against the {@code Subscription} of this handler and notifies
     * the read-side if the event triggered any subscription updates.
     *
     * <p>The subscription must be {@linkplain #isActive() active}.
     *
     * @param event
     *         the event which may trigger subscription updates
     */
    void handle(EventEnvelope event) {
        checkActive(event);
        detectUpdate(event).ifPresent(this::deliverUpdate);
    }

    /**
     * Ensures that the handler is active, otherwise throws {@link IllegalStateException} with
     * the information on the passed event.
     */
    private void checkActive(EventEnvelope event) {
        if (!isActive()) {
            throw newIllegalStateException(
                    "Dispatched an event of type `%s` to the non-active subscription with the ID `%s`.",
                    event.typeUrl(),
                    subscription.getId()
                                .getValue()
            );
        }
    }

    private void deliverUpdate(SubscriptionUpdate update) {
        try {
            callback.accept(update);
        } catch (Throwable t) {
            _error().withCause(t).log();
        }
    }

    /**
     * Obtains the {@code Target} of the handled subscription.
     */
    final Target target() {
        return subscription.getTopic()
                           .getTarget();
    }

    /**
     * Obtains the handled subscription.
     */
    final Subscription subscription() {
        return subscription;
    }

    /**
     * Tries to detect the passed event as a {@code SubscriptionUpdate} according
     * to the {@code Subscription} criteria.
     *
     * @param event
     *         the event to analyze against this subscription
     * @return {@code SubscriptionUpdate} packed as {@code Optional} if the event matches the
     *         subscription criteria, {@code Optional.empty()} otherwise
     */
    abstract Optional<SubscriptionUpdate> detectUpdate(EventEnvelope event);

    /**
     * Extracts the ID value of the updated {@code Entity} or received {@code Event}.
     *
     * @param event
     *         system event transmitting the {@code Entity} update info
     *         or the domain event to which a subscription exists
     */
    abstract Any extractId(EventEnvelope event);

    /**
     * Tells whether the type of the updated {@code Entity} or received domain {@code Event}
     * matches the type configured for the handled {@code Subscription}.
     *
     * @param event
     *         system event transmitting the {@code Entity} update info
     *         or the domain event to which a subscription exists
     */
    abstract boolean typeMatches(EventEnvelope event);

    /**
     * Checks if the event matches the subscription ID filter.
     */
    boolean idMatches(EventEnvelope event) {
        TargetFilters filters = target().getFilters();
        IdFilter idFilter = filters.getIdFilter();
        boolean idFilterSet = !IdFilter.getDefaultInstance()
                                       .equals(idFilter);
        if (!idFilterSet) {
            return true;
        }
        Any id = extractId(event);
        boolean result = idFilter.getIdList()
                                 .contains(id);
        return result;
    }

    /**
     * Checks if the subscription has {@code include_all} clause.
     */
    boolean includeAll() {
        return target().getIncludeAll();
    }

    /**
     * Activates this handler with a given callback.
     */
    void setCallback(SubscriptionCallback callback) {
        this.callback = callback;
    }

    /**
     * Checks if this handler has a callback set.
     */
    boolean isActive() {
        return callback != null;
    }
}
