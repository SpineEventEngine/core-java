/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.aggregate.given.repo;

import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.entity.rejection.CannotModifyArchivedEntity;
import io.spine.server.event.React;
import io.spine.server.test.shared.StringAggregate;
import io.spine.server.test.shared.StringAggregateVBuilder;
import io.spine.test.aggregate.number.DoNothing;
import io.spine.test.aggregate.number.FloatEncountered;
import io.spine.test.aggregate.number.NumberPassed;
import io.spine.test.aggregate.number.RejectNegativeInt;
import io.spine.test.aggregate.number.RejectNegativeLong;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * The aggregate which throws {@link IllegalArgumentException} in response to negative numbers.
 *
 * <p>Normally aggregates should reject commands via command rejections. This class is test
 * environment for testing of now
 * {@linkplain io.spine.server.aggregate.AggregateRepository#logError(String, io.spine.core.MessageEnvelope, RuntimeException)
 * logs errors}.
 *
 * @see FailingAggregateRepository
 */
class FailingAggregate extends Aggregate<Long, StringAggregate, StringAggregateVBuilder> {

    private FailingAggregate(Long id) {
        super(id);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision") // Int. part as ID.
    static long toId(FloatEncountered message) {
        float floatValue = message.getNumber();
        return (long) Math.abs(floatValue);
    }

    /** Rejects a negative value via command rejection. */
    @Assign
    NumberPassed on(RejectNegativeInt value) {
        if (value.getNumber() < 0) {
            throw new IllegalArgumentException("Negative value passed");
        }
        return now();
    }

    /** Rejects a negative value via command rejection. */
    @Assign
    NumberPassed on(RejectNegativeLong value) throws CannotModifyArchivedEntity {
        if (value.getNumber() < 0L) {
            throw CannotModifyArchivedEntity
                    .newBuilder()
                    .setEntityId(Identifier.pack(getId()))
                    .build();
        }
        return now();
    }

    /** Invalid command handler, which does not produce events. */
    @Assign
    List<Message> on(DoNothing value) {
        return emptyList();
    }

    @React
    NumberPassed on(FloatEncountered value) {
        float floatValue = value.getNumber();
        if (floatValue < 0) {
            long longValue = toId(value);
            // Complain only if the passed value represents ID of this aggregate.
            // This would allow other aggregates react on this message.
            if (longValue == getId()) {
                throw new IllegalArgumentException("Negative floating point value passed");
            }
        }
        return now();
    }

    @Apply
    void apply(NumberPassed event) {
        getBuilder().setValue(getState().getValue()
                                      + System.lineSeparator()
                                      + Timestamps.toString(event.getWhen()));
    }

    private static NumberPassed now() {
        Timestamp time = Time.getCurrentTime();
        return NumberPassed
                .newBuilder()
                .setWhen(time)
                .build();
    }
}
