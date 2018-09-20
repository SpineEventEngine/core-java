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

package io.spine.server.projection.given;

import com.google.protobuf.StringValue;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.test.projection.event.Int32Imported;
import io.spine.test.projection.event.StringImported;
import io.spine.validate.StringValueVBuilder;

import static io.spine.protobuf.TypeConverter.toMessage;

/**
 * @author Alex Tymchenko
 * @author Dmytro Kuzmin
 */
public class ProjectionTestEnv {

    /** Prevents instantiation of this utility class. */
    private ProjectionTestEnv() {
    }

    public static class TestProjection
            extends Projection<String, StringValue, StringValueVBuilder> {

        /** The number of events this class handles. */
        public static final int HANDLING_EVENT_COUNT = 2;

        protected TestProjection(String id) {
            super(id);
        }

        @Subscribe
        public void on(StringImported event) {
            StringValue newState = createNewState("stringState", event.getValue());
            getBuilder().mergeFrom(newState);
        }

        @Subscribe
        public void on(Int32Imported event) {
            StringValue newState = createNewState("integerState",
                                                  String.valueOf(event.getValue()));
            getBuilder().mergeFrom(newState);
        }

        private StringValue createNewState(String type, String value) {
            // Get the current state within the transaction.
            String currentState = getBuilder().internalBuild()
                                              .getValue();
            String result = currentState + (currentState.length() > 0 ? " + " : "") +
                    type + '(' + value + ')' + System.lineSeparator();
            return toMessage(result);
        }
    }
}
