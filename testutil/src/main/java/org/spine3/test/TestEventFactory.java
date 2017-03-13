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

package org.spine3.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.annotations.Internal;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Events;
import org.spine3.protobuf.AnyPacker;
import org.spine3.server.reflect.CommandHandlerMethod;

import static org.spine3.protobuf.Values.newStringValue;

/**
 * A factory for creating tests instances of {@link Event}s.
 *
 * @author Alexander Yevsyukov
 */
@Internal
@VisibleForTesting
public class TestEventFactory {

    private final Any producerId;

    private TestEventFactory(Class<?> testClass) {
        this.producerId = AnyPacker.pack(newStringValue(testClass.getName()));
    }

    public static TestEventFactory newInstance(Class<?> testClass) {
        return new TestEventFactory(testClass);
    }

    public Event createEvent(Message eventMessage, CommandContext commandContext) {
        final EventContext eventContext = CommandHandlerMethod.createEventContext(
                producerId,
                null,
                commandContext);
        return Events.createEvent(eventMessage, eventContext);
    }
}
