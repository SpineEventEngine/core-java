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

package io.spine.server.event.given;

import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import io.spine.core.React;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.validate.StringValueVBuilder;

/**
 * @author Alexander Yevsyukov
 */
public class EventReactorMethodTestEnv {

    public static class ReactingAggregate
            extends Aggregate<Long, StringValue, StringValueVBuilder> {

        public ReactingAggregate(Long id) {
            super(id);
        }

        @Assign
        StringValue handle(Int32Value value) {
            final String str = String.valueOf(value.getValue());
            return toMessage(str);
        }

        private static StringValue toMessage(String str) {
            return StringValue.newBuilder()
                              .setValue(str)
                              .build();
        }

        @Apply
        private void event(StringValue value) {
            final String currentState = getState().getValue();
            getBuilder().setValue(currentState + System.lineSeparator() + value.getValue());
        }

        @React
        StringValue on(UInt32Value value) {
            final String str = String.valueOf(value.getValue());
            return toMessage(str);
        }
    }
}
