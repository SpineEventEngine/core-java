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

package io.spine.testing.server.aggregate.given.agg;

import com.google.protobuf.util.Timestamps;
import io.spine.base.Time;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.event.React;
import io.spine.testing.server.given.entity.TuString;
import io.spine.testing.server.given.entity.TuStringVBuilder;
import io.spine.testing.server.log.FloatLogged;
import io.spine.testing.server.log.LogInteger;
import io.spine.testing.server.log.ValueLogged;

public class TuMessageLog extends Aggregate<Long, TuString, TuStringVBuilder> {

    public TuMessageLog(Long id) {
        super(id);
    }

    @Assign
    ValueLogged handle(LogInteger value) {
        String digitalPart = String.valueOf(value.getValue());
        return logItem(digitalPart);
    }

    @React
    ValueLogged handle(FloatLogged value) {
        String digitalPart = String.valueOf(value.getValue());
        return logItem(digitalPart);
    }

    @Apply
    void newLine(ValueLogged line) {
        String current = getState().getValue();
        getBuilder().setValue(current + System.lineSeparator() + line.getValue());
    }

    private static ValueLogged logItem(String digitalPart) {
        String str = Timestamps.toString(Time.getCurrentTime())
                + " - "
                + digitalPart;
        return ValueLogged.newBuilder()
                          .setValue(str)
                          .build();
    }
}
