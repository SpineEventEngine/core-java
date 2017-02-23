/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import com.google.common.base.Predicate;
import com.google.protobuf.Timestamp;
import org.spine3.base.Event;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.base.Predicates.alwaysTrue;
import static org.spine3.base.EventPredicates.isAfter;
import static org.spine3.base.EventPredicates.isBefore;
import static org.spine3.base.EventPredicates.isBetween;

/**
 * The predicate for filtering {@code Event} instances by
 * {@link EventStreamQuery}.
 *
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
public class MatchesStreamQuery implements Predicate<Event> {

    private final EventStreamQuery query;
    private final Predicate<Event> timePredicate;

    @SuppressWarnings("IfMayBeConditional")
    public MatchesStreamQuery(EventStreamQuery query) {
        this.query = query;
        final Timestamp after = query.getAfter();
        final Timestamp before = query.getBefore();
        final boolean afterSpecified = query.hasAfter();
        final boolean beforeSpecified = query.hasBefore();

        if (afterSpecified && !beforeSpecified) {
            this.timePredicate = isAfter(after);
        } else if (!afterSpecified && beforeSpecified) {
            this.timePredicate = isBefore(before);
        } else if (afterSpecified /* && beforeSpecified is `true` here too */) {
            this.timePredicate = isBetween(after, before);
        } else { // No timestamps specified.
            this.timePredicate = alwaysTrue();
        }
    }

    @Override
    public boolean apply(@Nullable Event input) {
        if (!timePredicate.apply(input)) {
            return false;
        }
        final List<EventFilter> filterList = query.getFilterList();
        if (filterList.isEmpty()) {
            return true; // The time range matches, and no filters specified.
        }
        // Check if one of the filters matches. If so, the event matches.
        for (EventFilter filter : filterList) {
            final Predicate<Event> filterPredicate = new MatchFilter(filter);
            if (filterPredicate.apply(input)) {
                return true;
            }
        }
        return false;
    }
}
