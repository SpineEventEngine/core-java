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

package org.spine3.convert;

import com.google.common.reflect.TypeToken;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.junit.Test;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.base.EventId;
import org.spine3.base.Events;
import org.spine3.protobuf.Timestamps2;
import org.spine3.test.NullToleranceTest;
import org.spine3.test.identifiers.IdWithPrimitiveFields;
import org.spine3.test.identifiers.TimestampFieldId;

import java.text.ParseException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps2.getCurrentTime;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

public class StringifiersShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Stringifiers.class);
    }

    private static final Stringifier<IdWithPrimitiveFields> ID_TO_STRING_CONVERTER =
            new Stringifier<IdWithPrimitiveFields>() {
                @Override
                protected String doForward(IdWithPrimitiveFields id) {
                    return id.getName();
                }

                @Override
                protected IdWithPrimitiveFields doBackward(String s) {
                    return IdWithPrimitiveFields.newBuilder()
                                                .setName(s)
                                                .build();
                }
            };

    @Test
    public void convert_to_string_registered_id_message_type() {
        StringifierRegistry.getInstance()
                           .register(TypeToken.of(IdWithPrimitiveFields.class),
                                     ID_TO_STRING_CONVERTER);

        final String testId = "testId 123456";
        final IdWithPrimitiveFields id = IdWithPrimitiveFields.newBuilder()
                                                              .setName(testId)
                                                              .build();
        final String result = idToString(id);

        assertEquals(testId, result);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // OK as these are standard Stringifiers we add ourselves.
    @Test
    public void handle_null_in_standard_converters() {
        final StringifierRegistry registry = StringifierRegistry.getInstance();

        assertNull(registry.get(TypeToken.of(Timestamp.class))
                           .get()
                           .convert(null));
        assertNull(registry.get(TypeToken.of(EventId.class))
                           .get()
                           .convert(null));
        assertNull(registry.get(TypeToken.of(CommandId.class))
                           .get()
                           .convert(null));
    }

    @Test
    public void return_false_on_attempt_to_find_unregistered_type() {
        assertFalse(StringifierRegistry.getInstance()
                                       .hasStringifierFor(TypeToken.of(Random.class)));
    }

    @Test
    public void convert_command_id_to_string() {
        final CommandId id = Commands.generateId();
        final String actual = new Stringifiers.CommandIdStringifier().convert(id);

        assertEquals(idToString(id), actual);
    }

    @Test
    public void convert_string_to_command_id() {
        final String id = newUuid();
        final CommandId expected = CommandId.newBuilder()
                                            .setUuid(id)
                                            .build();
        final CommandId actual = new Stringifiers.CommandIdStringifier().reverse()
                                                                        .convert(id);

        assertEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_exception_when_try_to_convert_inappropriate_string_to_timestamp() {
        final String time = Timestamps2.getCurrentTime()
                                       .toString();
        new Stringifiers.TimestampStringifer().reverse()
                                              .convert(time);
    }

    @Test
    public void convert_string_to_timestamp() throws ParseException {
        final String date = "1972-01-01T10:00:20.021-05:00";
        final Timestamp expected = Timestamps.parse(date);
        final Timestamp actual = new Stringifiers.TimestampStringifer().reverse()
                                                                       .convert(date);
        assertEquals(expected, actual);
    }

    @Test
    public void convert_timestamp_to_string() {
        final Timestamp currentTime = getCurrentTime();
        final TimestampFieldId id = TimestampFieldId.newBuilder()
                                                    .setId(currentTime)
                                                    .build();
        final String expected = new Stringifiers.TimestampStringifer().convert(currentTime);
        final String actual = idToString(id);

        assertEquals(expected, actual);
    }

    @Test
    public void convert_event_id_to_string() {
        final EventId id = Events.generateId();
        final String actual = new Stringifiers.EventIdStringifier().convert(id);

        assertEquals(idToString(id), actual);
    }

    @Test
    public void convert_string_to_event_id() {
        final String id = newUuid();
        final EventId expected = EventId.newBuilder()
                                        .setUuid(id)
                                        .build();
        final EventId actual = new Stringifiers.EventIdStringifier().reverse()
                                                                    .convert(id);
        assertEquals(expected, actual);
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(Stringifiers.class)
                                                                     .build();
        final boolean passed = nullToleranceTest.check();
        assertTrue(passed);
    }
}
