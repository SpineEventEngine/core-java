/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.event;

import io.spine.core.Event;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The predicate for filtering {@code Event} instances by {@link EventStreamQuery}.
 *
 * <p>The predicate ignores the time of bounds and matches only
 * the {@link io.spine.base.FieldFilter fields} of the event message and
 * the {@link io.spine.core.EventContext EventContext}.
 *
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
final class MatchesStreamQuery implements Predicate<Event> {

    private final List<EventFilter> filterList;

    MatchesStreamQuery(EventStreamQuery query) {
        checkNotNull(query);
        this.filterList = query.getFilterList();
    }

    @Override
    public boolean test(@Nullable Event input) {
        checkNotNull(input);
        if (filterList.isEmpty()) {
            return true; // No filters specified.
        }
        // Check if one of the filters matches. If so, the event matches.
        for (EventFilter filter : filterList) {
            Predicate<Event> filterPredicate = new MatchFilter(filter);
            if (filterPredicate.test(input)) {
                return true;
            }
        }
        return false;
    }
}
