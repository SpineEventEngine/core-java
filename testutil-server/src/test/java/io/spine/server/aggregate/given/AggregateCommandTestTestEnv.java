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

package io.spine.server.aggregate.given;

import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.client.ActorRequestFactory;
import io.spine.core.TenantId;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateCommandTest;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.given.Given;
import io.spine.time.ZoneOffsets;
import io.spine.validate.StringValueVBuilder;

import static io.spine.core.given.GivenUserId.newUuid;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Kuzmin
 */
public class AggregateCommandTestTestEnv {

    /** Prevents instantiation of this utility class. */
    private AggregateCommandTestTestEnv() {
    }

    public static ActorRequestFactory newRequestFactory(Class<?> clazz) {
        return ActorRequestFactory.newBuilder()
                                  .setActor(newUuid())
                                  .setZoneOffset(ZoneOffsets.utc())
                                  .setTenantId(TenantId.newBuilder()
                                                       .setValue(clazz.getSimpleName())
                                                       .build())
                                  .build();
    }

    /**
     * A dummy aggregate that accepts a {@code Timestamp} as a command message
     * and prints it into its state.
     */
    public static final class TimePrinter
            extends Aggregate<Long, StringValue, StringValueVBuilder> {

        TimePrinter(Long id) {
            super(id);
        }

        @Assign
        public Timestamp handle(Timestamp timestamp) {
            return timestamp;
        }

        @Apply
        void on(Timestamp timestamp) {
            getBuilder().setValue(Timestamps.toString(timestamp));
        }
    }

    /**
     * The test harness class that tests how {@code TimePrinter} handles its command.
     */
    public static class TimePrintingTest extends AggregateCommandTest<Timestamp, TimePrinter> {

        public TimePrintingTest(ActorRequestFactory requestFactory) {
            super(requestFactory);
        }

        public TimePrintingTest() {
            super();
        }

        @Override
        protected TimePrinter createAggregate() {
            final TimePrinter result = Given.aggregateOfClass(TimePrinter.class)
                                            .withId(1L)
                                            .withVersion(64)
                                            .build();
            return result;
        }
    }
}
