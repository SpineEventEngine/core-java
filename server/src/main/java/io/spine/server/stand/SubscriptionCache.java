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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.spine.core.EventEnvelope;

import java.util.Collection;

import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * Caches the subscription records for the given event.
 */
final class SubscriptionCache {

    private final Multimap<EventEnvelope, SubscriptionRecord> cache =
            synchronizedMultimap(HashMultimap.create());

    Collection<SubscriptionRecord> get(EventEnvelope event) {
        Collection<SubscriptionRecord> records = cache.get(event);
        return records;
    }

    void put(EventEnvelope event, SubscriptionRecord record) {
        cache.put(event, record);
    }

    void removeRecords(EventEnvelope event) {
        cache.removeAll(event);
    }
}
