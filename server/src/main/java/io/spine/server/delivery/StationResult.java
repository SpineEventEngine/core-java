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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Tymchenko
 */
final class StationResult {

    private final List<InboxMessage> outcome;
    private final ImmutableList<InboxMessage> pendingUpdates;
    private final ImmutableList<InboxMessage> pendingRemovals;

    private StationResult(Builder builder) {
        this.outcome = builder.outcome;
        this.pendingUpdates = ImmutableList.copyOf(builder.updates);
        this.pendingRemovals = ImmutableList.copyOf(builder.removals);
    }

    ImmutableList<InboxMessage> pendingUpdates() {
        return pendingUpdates;
    }

    ImmutableList<InboxMessage> pendingRemovals() {
        return pendingRemovals;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    List<InboxMessage> outcome() {
        return outcome;
    }

    static Builder newBuilder(List<InboxMessage> outcome) {
        return new Builder(outcome);
    }

    static class Builder {

        private final List<InboxMessage> outcome;
        private final List<InboxMessage> updates = new ArrayList<>();
        private final List<InboxMessage> removals = new ArrayList<>();

        private Builder(List<InboxMessage> outcome) {
            this.outcome = outcome;
        }

        void update(InboxMessage message) {
            updates.add(message);
        }

        void remove(InboxMessage message) {
            removals.add(message);
        }
    }
}
