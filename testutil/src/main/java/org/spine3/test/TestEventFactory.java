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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandContext;
import org.spine3.base.Event;
import org.spine3.base.EventContext;
import org.spine3.base.Version;
import org.spine3.server.command.EventFactory;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.protobuf.AnyPacker.pack;
import static org.spine3.protobuf.Values.newStringValue;

/**
 * The factory or producing events for tests.
 *
 * @author Alexander Yevsyukov
 */
public class TestEventFactory extends EventFactory {

    private TestEventFactory(Builder builder) {
        super(builder);
    }

    public static TestEventFactory newInstance(Class<?> testSuiteClass,
                                               CommandContext commandContext) {
        checkNotNull(testSuiteClass);
        checkNotNull(commandContext);

        final StringValue producerId = newStringValue(testSuiteClass.getName());
        final Builder builder = EventFactory.newBuilder()
                                            .setProducerId(producerId)
                                            .setCommandContext(commandContext);

        final TestEventFactory result = new TestEventFactory(builder);
        return result;
    }

    public static TestEventFactory newInstance(Any producerId, Class<?> testSuiteClass) {
        final TestActorRequestFactory commandFactory =
                TestActorRequestFactory.newInstance(testSuiteClass);
        return newInstance(producerId, commandFactory);
    }

    public static TestEventFactory newInstance(Any producerId,
                                               TestActorRequestFactory requestFactory) {
        checkNotNull(requestFactory);
        final CommandContext commandContext = requestFactory.createCommandContext();
        final Builder builder = EventFactory.newBuilder()
                                            .setProducerId(producerId)
                                            .setCommandContext(commandContext);
        final TestEventFactory result = new TestEventFactory(builder);
        return result;
    }

    public static TestEventFactory newInstance(TestActorRequestFactory requestFactory) {
        final Message producerId = requestFactory.getActor();
        return newInstance(pack(producerId), requestFactory);
    }

    public static TestEventFactory newInstance(Class<?> testSuiteClass) {
        final TestActorRequestFactory requestFactory =
                TestActorRequestFactory.newInstance(testSuiteClass);
        return newInstance(requestFactory);
    }

    /**
     * Creates an event without version information.
     */
    public Event createEvent(Message messageOrAny) {
        return createEvent(messageOrAny, Tests.<Version>nullRef());
    }

    /**
     * Creates an event produced at the passed time.
     */
    public Event createEvent(Message messageOrAny,
                             @Nullable Version version,
                             Timestamp atTime) {
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
