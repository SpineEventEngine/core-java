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

package io.spine.test;

import com.google.protobuf.Message;
import io.spine.base.Event;
import io.spine.base.EventContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * Test utilities for creating events used in tests.
 *
 * @author Alexander Yevsyukov
 */
public class EventTests {

    private EventTests() {
        // Prevent instantiation of this utility class.
    }

    private static Event.Builder newBuilderWith(Message eventMessage) {
        return Event.newBuilder()
                    .setMessage(pack(eventMessage));
    }

    public static Event createContextlessEvent(Message eventMessage) {
        checkNotNull(eventMessage);
        return newBuilderWith(eventMessage)
                .build();
    }

    public static Event createEvent(Message eventMsg, EventContext context) {
        checkNotNull(eventMsg);
        checkNotNull(context);
        return newBuilderWith(eventMsg)
                .setContext(context)
                .build();
    }
}
