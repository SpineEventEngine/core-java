/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.projection;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventContext;
import org.spine3.server.event.Subscribe;

import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Values.newStringValue;

@SuppressWarnings("InstanceMethodNamingConvention")
public class ProjectionShould {

    private static class TestProjection extends Projection<Integer, StringValue> {

        protected TestProjection(Integer id) {
            super(id);
        }

        @Subscribe
        public void on(StringValue event, EventContext ignored) {
            final StringValue newSate = createNewState("string", event.getValue());
            incrementState(newSate);
        }

        @Subscribe
        public void on(UInt32Value event, EventContext ignored) {
            final StringValue newSate = createNewState("integer", String.valueOf(event.getValue()));
            incrementState(newSate);
        }

        private StringValue createNewState(String type, String value) {
            final String currentState = getState().getValue();
            final String result = currentState + (currentState.length() > 0 ? " + " : "") +
                    type + '(' + value + ')' + System.lineSeparator();
            return newStringValue(result);
        }

        /**
         * We expose this method to be called directly in {@link #setUp()}.
         * Normally this method would be called by a repository upon creation of a new instance.
         */
        @Override
        @VisibleForTesting
        protected void setDefault() {
            super.setDefault();
        }
    }

    private TestProjection test;

    @Before
    public void setUp() {
        test = new TestProjection(1);
        test.setDefault();
    }

    @Test
    public void handle_events() {
        final String stringValue = "something new";
        test.handle(newStringValue(stringValue), EventContext.getDefaultInstance());
        assertTrue(test.getState().getValue().contains(stringValue));

        final Integer integerValue = 1024;
        test.handle(UInt32Value.newBuilder().setValue(integerValue).build(), EventContext.getDefaultInstance());
        assertTrue(test.getState().getValue().contains(String.valueOf(integerValue)));
    }
}
