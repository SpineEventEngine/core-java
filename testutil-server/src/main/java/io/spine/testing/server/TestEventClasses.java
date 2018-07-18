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

package io.spine.testing.server;

import com.google.common.base.Function;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.EventClass;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

import static com.google.common.collect.Collections2.transform;
import static io.spine.protobuf.AnyPacker.unpack;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utilities for testing event classes.
 *
 * @author Alexander Yevsyukov
 */
@CheckReturnValue
public class TestEventClasses {

    /** Prevents instantiation of this utility class. */
    private TestEventClasses() {
    }

    @SafeVarargs
    public static void assertContains(Collection<EventClass> expected,
                                      Class<? extends Message>... eventClass) {
        for (Class<? extends Message> cls : eventClass) {
            assertTrue(expected.contains(EventClass.of(cls)));
        }
    }

    public static Collection<EventClass> getEventClasses(Collection<Event> events) {
        return transform(events, new Function<Event, EventClass>() {
            @Nullable // return null because an exception won't be propagated in this case
            @Override
            public EventClass apply(@Nullable Event record) {
                if (record == null) {
                    return null;
                }
                final Message eventMessage = unpack(record.getMessage());
                return EventClass.of(eventMessage);
            }
        });
    }
}
