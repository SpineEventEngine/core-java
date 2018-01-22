/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import com.google.protobuf.FloatValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.google.protobuf.util.Timestamps;
import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.time.Time;
import io.spine.validate.StringValueVBuilder;

public class AggregateMessageDispatcherTestEnv {

    /** Prevents instantiation on this utility class. */
    private AggregateMessageDispatcherTestEnv() {
    }

    public static class MessageLog extends Aggregate<Long, StringValue, StringValueVBuilder> {

        public MessageLog(Long id) {
            super(id);
        }

        @Assign
        StringValue handle(UInt32Value value) {
            final String digitalPart = String.valueOf(value.getValue());
            return logItem(digitalPart);
        }

        @React
        StringValue handle(FloatValue value) {
            final String digitalPart = String.valueOf(value.getValue());
            return logItem(digitalPart);
        }

        @Apply
        void newLine(StringValue line) {
            final String current = getState().getValue();
            getBuilder().setValue(current + System.lineSeparator() + line.getValue());
        }

        private static StringValue logItem(String digitalPart) {
            final String str =
                    Timestamps.toString(Time.getCurrentTime())
                            + " - "
                            + digitalPart;
            return StringValue.newBuilder()
                              .setValue(str)
                              .build();
        }
    }
}
