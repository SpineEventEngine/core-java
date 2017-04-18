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

package org.spine3.base;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Timestamp;
import org.junit.Test;
import org.spine3.test.identifiers.IdWithPrimitiveFields;
import org.spine3.users.UserId;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.idToString;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.base.Stringifiers.booleanStringifier;
import static org.spine3.base.Stringifiers.integerStringifier;
import static org.spine3.base.Stringifiers.longStringifier;
import static org.spine3.test.Tests.assertHasPrivateParameterlessCtor;

public class StringifiersShould {

    @Test
    public void have_private_constructor() {
        assertHasPrivateParameterlessCtor(Stringifiers.class);
    }

    private static final Stringifier<IdWithPrimitiveFields> ID_TO_STRING_CONVERTER =
            new Stringifier<IdWithPrimitiveFields>() {
                @Override
                protected String toString(IdWithPrimitiveFields id) {
                    return id.getName();
                }

                @Override
                protected IdWithPrimitiveFields fromString(String str) {
                    return IdWithPrimitiveFields.newBuilder()
                                                .setName(str)
                                                .build();
                }
            };

    @Test
    public void convert_to_string_registered_id_message_type() {
        StringifierRegistry.getInstance()
                           .register(ID_TO_STRING_CONVERTER, IdWithPrimitiveFields.class);

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

        assertNull(registry.get(Timestamp.class)
                           .get()
                           .convert(null));
        assertNull(registry.get(EventId.class)
                           .get()
                           .convert(null));
        assertNull(registry.get(CommandId.class)
                           .get()
                           .convert(null));
    }

    @Test
    public void return_false_on_attempt_to_find_unregistered_type() {
        assertFalse(StringifierRegistry.getInstance()
                                       .hasStringifierFor(Random.class));
    }

    @Test
    public void pass_the_null_tolerance_check() {
        final NullPointerTester tester = new NullPointerTester();
        tester.testStaticMethods(Stringifiers.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    public void convert_long_to_string() {
        final String convertedLong = longStringifier().toString(1L);
        assertEquals("1", convertedLong);
    }

    @Test
    public void convert_int_to_string() {
        final String convertedInt = integerStringifier().toString(2);
        assertEquals("2", convertedInt);
    }

    @Test
    public void convert_from_json_to_message() {
        final Stringifier<UserId> messageStringifier =
                StringifierRegistry.getStringifier(UserId.class);
        final String idValue = newUuid();
        final UserId result = messageStringifier.reverse()
                                                .convert("{value:" + idValue + '}');
        assertNotNull(result);
        assertEquals(idValue, result.getValue());
    }

    @Test
    public void convert_from_message_to_json() {
        final String idValue = newUuid();
        final UserId userId = UserId.newBuilder()
                                    .setValue(idValue)
                                    .build();
        final Stringifier<UserId> messageStringifier =
                StringifierRegistry.getStringifier(UserId.class);
        final String convertedMessage = messageStringifier.convert(userId);

        assertNotNull(convertedMessage);
        assertTrue(convertedMessage.contains(idValue));
    }

    @SuppressWarnings("EmptyClass") // is the part of the test.
    @Test(expected = MissingStringifierException.class)
    public void raise_exception_on_missing_stringifer() {
        Stringifiers.toString(new Object() {});
    }

    @SuppressWarnings("ConstantConditions") // Nulls are not possible for boolean stringifier
    @Test
    public void provide_boolean_stringifier() {
        final Stringifier<Boolean> stringifier = booleanStringifier();

        assertTrue(stringifier.reverse()
                              .convert(stringifier.convert(true)));
        assertFalse(stringifier.reverse()
                              .convert(stringifier.convert(false)));
    }
}
