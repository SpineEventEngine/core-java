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
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import io.spine.client.CompositeFilter;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.client.Filter;
import io.spine.client.Filters;
import io.spine.client.IdFilter;
import io.spine.client.Subscription;
import io.spine.client.Target;
import io.spine.core.EventEnvelope;
import io.spine.protobuf.TypeConverter;
import io.spine.type.TypeUrl;

import java.util.function.Predicate;

import static io.spine.server.storage.OperatorEvaluator.eval;
import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * Decides whether the given event matches a subscription criteria.
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

    private boolean isTypeMatching(EventEnvelope event) {
        TypeUrl typeUrl = getTypeToCheck(event);
        TypeUrl requiredType = TypeUrl.parse(target().getType());
        return requiredType.equals(typeUrl);
    }

    private boolean includeAll() {
        return target().getIncludeAll();
    }

    private boolean matchByFilters(EventEnvelope event) {
        return checkIdMatches(event) && checkStateMatches(event);
    }

    private boolean checkIdMatches(EventEnvelope event) {
        Any id = getIdToCheck(event);
        Filters filters = target().getFilters();
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

    private boolean checkStateMatches(EventEnvelope event) {
        Message state = getStateToCheck(event);
        Filters filters = target().getFilters();
        boolean result = filters.getFilterList()
                                .stream()
                                .allMatch(filter -> checkPasses(state, filter));
        return result;
    }

    @SuppressWarnings("EnumSwitchStatementWhichMissesCases") // OK for Proto enum.
    private static boolean checkPasses(Message state, CompositeFilter filter) {
        CompositeOperator operator = filter.getOperator();
        switch (operator) {
            case ALL:
                return filter.getFilterList()
                             .stream()
                             .allMatch(f -> checkPasses(state, f));
            case EITHER:
                return filter.getFilterList()
                             .stream()
                             .anyMatch(f -> checkPasses(state, f));
            default:
                throw newIllegalArgumentException("Unknown composite filter operator %s.",
                                                  operator);
        }
    }

    private static boolean checkPasses(Message state, Filter filter) {
        String fieldName = filter.getFieldName();
        FieldDescriptor fieldDescriptor = state.getDescriptorForType()
                                               .findFieldByName(fieldName);
        Object actual = state.getField(fieldDescriptor);

        Any requiredAsAny = filter.getValue();
        Object required = TypeConverter.toObject(requiredAsAny, actual.getClass());
        try {
            return eval(actual, filter.getOperator(), required);
        } catch (IllegalArgumentException e) {
            throw newIllegalArgumentException(
                    e,
                    "Filter value %s cannot be properly compared to the message field %s of " +
                            "type %s",
                    required, fieldDescriptor.getFullName(), fieldDescriptor.getType()
            );
        }
    }

    private Target target() {
        return subscription.getTopic()
                           .getTarget();
    }

    protected abstract TypeUrl getTypeToCheck(EventEnvelope event);

    protected abstract Any getIdToCheck(EventEnvelope event);

    protected abstract Message getStateToCheck(EventEnvelope event);
}
