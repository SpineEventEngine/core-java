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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.FieldPath;
import io.spine.client.CompositeFilter;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.client.Filter;
import io.spine.client.IdFilter;
import io.spine.client.Subscription;
import io.spine.client.Target;
import io.spine.client.TargetFilters;
import io.spine.core.EventEnvelope;
import io.spine.protobuf.TypeConverter;
import io.spine.type.TypeUrl;

import java.util.function.Predicate;

import static io.spine.protobuf.FieldPaths.fieldAt;
import static io.spine.server.storage.OperatorEvaluator.eval;
import static io.spine.util.Exceptions.newIllegalArgumentException;
import static java.lang.String.join;

/**
 * Matches the incoming event against a subscription criteria.
 *
 * <p>The class descendants decide on how to turn the event envelope into a set of data pieces
 * suitable for filtering.
 */
abstract class SubscriptionMatcher implements Predicate<EventEnvelope> {

    private final Subscription subscription;

    SubscriptionMatcher(Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public boolean test(EventEnvelope event) {
        return isTypeMatching(event) && (includeAll() || matchByFilters(event));
    }

    /**
     * Matches the event to the subscription type.
     */
    private boolean isTypeMatching(EventEnvelope event) {
        TypeUrl typeUrl = extractType(event);
        TypeUrl requiredType = TypeUrl.parse(target().getType());
        return requiredType.equals(typeUrl);
    }

    /**
     * Checks if the subscription has "include_all" clause.
     */
    private boolean includeAll() {
        return target().getIncludeAll();
    }

    /**
     * Matches an event to the subscription filters.
     */
    private boolean matchByFilters(EventEnvelope event) {
        return checkIdMatches(event) && checkEventMessageMatches(event);
    }

    /**
     * Checks if the event matches the subscription ID filter.
     */
    private boolean checkIdMatches(EventEnvelope event) {
        Any id = extractId(event);
        TargetFilters filters = target().getFilters();
        IdFilter idFilter = filters.getIdFilter();
        boolean idFilterSet = !IdFilter.getDefaultInstance()
                                       .equals(idFilter);
        if (!idFilterSet) {
            return true;
        }
        boolean result = idFilter.getIdsList()
                                 .contains(id);
        return result;
    }

    /**
     * Checks if the event message matches the subscription filters.
     */
    private boolean checkEventMessageMatches(EventEnvelope event) {
        Message message = extractMessage(event);
        TargetFilters filters = target().getFilters();
        boolean result = filters
                .getFilterList()
                .stream()
                .allMatch(filter -> checkPasses(message, filter));
        return result;
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // OK for Proto enum.
    private static boolean checkPasses(Message message, CompositeFilter filter) {
        CompositeOperator operator = filter.getOperator();
        switch (operator) {
            case ALL:
                return filter.getFilterList()
                             .stream()
                             .allMatch(f -> checkPasses(message, f));
            case EITHER:
                return filter.getFilterList()
                             .stream()
                             .anyMatch(f -> checkPasses(message, f));
            default:
                throw newIllegalArgumentException("Unknown composite filter operator %s.",
                                                  operator);
        }
    }

    private static boolean checkPasses(Message state, Filter filter) {
        FieldPath fieldPath = filter.getFieldPath();
        Object actual = fieldAt(state, fieldPath);
        Any requiredAsAny = filter.getValue();
        Object required = TypeConverter.toObject(requiredAsAny, actual.getClass());
        try {
            return eval(actual, filter.getOperator(), required);
        } catch (IllegalArgumentException e) {
            throw newIllegalArgumentException(
                    e,
                    "Filter value %s cannot be properly compared to the message field %s of " +
                            "type %s",
                    required, join(".", fieldPath.getFieldNameList()), actual.getClass()
            );
        }
    }

    private Target target() {
        return subscription.getTopic()
                           .getTarget();
    }

    /**
     * Extracts the checked type from the event.
     */
    protected abstract TypeUrl extractType(EventEnvelope event);

    /**
     * Extracts the checked ID from the event.
     */
    protected abstract Any extractId(EventEnvelope event);

    /**
     * Extracts the checked message or state from the event.
     */
    protected abstract Message extractMessage(EventEnvelope event);
}
