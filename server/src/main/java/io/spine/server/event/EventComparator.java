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

package io.spine.server.event;

import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.core.Event;
import io.spine.core.EventContext;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator which compares events by their timestamp in chronological order.
 *
 * <p>In case the timestamp is the same, the {@linkplain EventContext#getVersion() event versions}
 * are compared.
 *
 * <p>If the versions are the same, the values of the {@linkplain Event#getId() event identifiers}
 * are compared lexicographically.
 */
@Internal
public final class EventComparator implements Comparator<Event>, Serializable {

    private static final long serialVersionUID = 0L;
    public static final EventComparator chronologically = new EventComparator();

    private EventComparator() {
    }

    @Override
    public int compare(Event e1, Event e2) {
        int timeComparison = Timestamps.compare(e1.time(), e2.time());
        if (timeComparison != 0) {
            return timeComparison;
        }
        EventContext ctx1 = e1.context();
        EventContext ctx2 = e2.context();
        int v1 = ctx1.getVersion()
                     .getNumber();
        int v2 = ctx2.getVersion()
                     .getNumber();
        int versionComparison = Integer.compare(v1, v2);
        if (versionComparison != 0) {
            return versionComparison;
        }
        String id1 = e1.getId()
                       .getValue();
        String id2 = e2.getId()
                       .getValue();
        return id1.compareTo(id2);

    }
}
