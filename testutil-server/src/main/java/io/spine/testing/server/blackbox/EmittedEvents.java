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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import io.spine.base.EventMessage;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.server.type.EventClass;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Provides information on events emitted in the {@link BlackBoxBoundedContext Bounded Context}.
 */
@VisibleForTesting
public final class EmittedEvents extends EmittedMessages<EventClass, Event, EventMessage> {

    EmittedEvents(List<Event> events) {
        super(events, counterFor(events), Event.class);
    }

    private static MessageTypeCounter<EventClass, Event, EventMessage>
    counterFor(List<Event> events) {
        return new MessageTypeCounter<>(events, EventClass::of, EventClass::from);
    }

    /**
     * Checks that the emitted events have given version numbers.
     *
     * <p>Fails if the number of emitted events is different then the number of given versions.
     *
     * @param versionNumbers the versions to check
     * @return {@code true} if the events have given version numbers, {@code false} otherwise
     */
    public boolean haveVersions(int... versionNumbers) {
        Collection<Event> messages = messages();
        assertEquals(versionNumbers.length, messages.size());
        Iterator<Event> events = this.messages().iterator();
        for (int version : versionNumbers) {
            Version actualVersion = events.next()
                                          .getContext()
                                          .getVersion();
            if (version != actualVersion.getNumber()) {
                return false;
            }
        }
        return true;
    }
}
