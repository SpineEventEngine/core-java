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

package io.spine.server.aggregate.given;

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.validate.StringValueVBuilder;

/**
 * @author Alexander Yevsyukov
 */
public class ReactingMethodTestEnv {

    private ReactingMethodTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /**
     * An aggregate that accumulates string representation of messages to which it reacts.
     */
    public static class ReactingLog extends Aggregate<Long, StringValue, StringValueVBuilder> {

        private ReactingLog(Long id) {
            super(id);
        }

        @React
        StringValue on(UInt32Value msg) {
            final String value = String.valueOf(msg.getValue());
            return toEventMessage(value);
        }

        @React
        StringValue on(BoolValue msg) {
            return toEventMessage(String.valueOf(msg.getValue()));
        }

        private static StringValue toEventMessage(String value) {
            return StringValue.newBuilder()
                              .setValue(value)
                              .build();
        }

        @Apply
        private void apply(StringValue v) {
            final String newValue = getState().getValue() +
                                    System.lineSeparator() +
                                    v.getValue();
            getBuilder().setValue(newValue);
        }
    }

    public static class ReactingLogRepository
            extends AggregateRepository<Long, ReactingLog> {
        //TODO:2017-07-06:alexander.yevsyukov: Add event targets function
    }
}
