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

package io.spine.testing.server.command;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.core.MessageEnvelope;
import io.spine.core.Version;
import io.spine.server.event.EventFactory;
import io.spine.testing.client.TestActorRequestFactory;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.protobuf.AnyPacker.pack;

/**
 * The factory or producing events for tests.
 *
 * @author Alexander Yevsyukov
 */
public class TestEventFactory extends EventFactory {

    private TestEventFactory(MessageEnvelope<?, ?, ?> origin, Any producerId, int eventCount) {
        super(origin, producerId);
    }

    public static TestEventFactory newInstance(Any producerId, Class<?> testSuiteClass) {
        return newInstance(producerId, TestActorRequestFactory.newInstance(testSuiteClass));
    }

    public static TestEventFactory newInstance(Any producerId,
                                               TestActorRequestFactory requestFactory) {
        checkNotNull(requestFactory);
        final CommandEnvelope cmd = requestFactory.generateEnvelope();
        return new TestEventFactory(cmd, producerId, 1);
    }

    public static TestEventFactory newInstance(TestActorRequestFactory requestFactory) {
        final Message producerId = requestFactory.getActor();
        return newInstance(pack(producerId), requestFactory);
    }

    public static TestEventFactory newInstance(Class<?> testSuiteClass) {
        return newInstance(TestActorRequestFactory.newInstance(testSuiteClass));
    }

    /**
     * Creates an event without version information.
     */
    public Event createEvent(Message messageOrAny) {
        return createEvent(messageOrAny, null);
    }

    /**
     * Creates an event produced at the passed time.
     */
    public Event createEvent(Message messageOrAny, @Nullable Version version, Timestamp atTime) {
        final Event event = createEvent(messageOrAny, version);
        final EventContext context = event.getContext()
                                          .toBuilder()
                                          .setTimestamp(atTime)
                                          .build();
        final Event result = event.toBuilder()
                                  .setContext(context)
                                  .build();
        return result;
    }
}
