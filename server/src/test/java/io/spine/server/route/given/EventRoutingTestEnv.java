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

package io.spine.server.route.given;

import com.google.protobuf.BoolValue;
import com.google.protobuf.FloatValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.core.Subscribe;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.string.Stringifiers;
import io.spine.time.Time;
import io.spine.validate.StringValueVBuilder;

import static java.lang.String.format;

/**
 * @author Alexander Yevsyukov
 */
public class EventRoutingTestEnv {

    /**
     * A projection that subscribes to several standard Protobuf types and records their value
     * as it receives them.
     */
    public static class LoggingProjection
            extends Projection<Long, StringValue, StringValueVBuilder> {

        protected LoggingProjection(Long id) {
            super(id);
        }

        @Subscribe
        void on(BoolValue msg) {
            append(msg);
        }

        @Subscribe
        void on(StringValue msg) {
            append(msg);
        }

        @Subscribe
        void on(FloatValue msg) {
            append(msg);
        }

        private void append(Message msg) {
            final String currentState = getState().getValue();
            final String newRecord = logRecord(msg);
            getBuilder().setValue(currentState + System.lineSeparator() + newRecord);
        }

        private static String logRecord(Message msg) {
            final String value = Stringifiers.toString(msg);
            final Timestamp currentTime = Time.getCurrentTime();
            return format("%s %s", Timestamps.toString(currentTime), value);
        }
    }

    public static class LoggingRepository
            extends ProjectionRepository<Long, LoggingProjection, StringValue> {
    }

}
