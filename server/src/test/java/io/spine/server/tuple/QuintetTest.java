/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.tuple;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.tuple.given.QuintetTestEnv.InstrumentFactory.newViola;
import static io.spine.server.tuple.given.QuintetTestEnv.InstrumentFactory.newViolin;
import static io.spine.server.tuple.given.QuintetTestEnv.InstrumentFactory.newViolinCello;
import static io.spine.server.tuple.given.QuintetTestEnv.QuintetFactory.NUM_1;
import static io.spine.server.tuple.given.QuintetTestEnv.QuintetFactory.NUM_2;
import static io.spine.server.tuple.given.QuintetTestEnv.QuintetFactory.newCelloQuintet;
import static io.spine.server.tuple.given.QuintetTestEnv.QuintetFactory.newViolaQuintet;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link Quintet} tuple.
 *
 * <p>This test suite uses
 * <a href="https://en.wikipedia.org/wiki/String_quintet">String quintets</a> types just to have
 * test data other than default Protobuf types, and for some fun.
 *
 * <p>In a real app a {@link Quintet} should have only event messages, and not value objects.
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Quintet should")
class QuintetTest {

    private final Quintet<?, ?, ?, ?, ?> celloQuintet = newCelloQuintet();
    private final Quintet<?, ?, ?, ?, ?> violaQuintet = newViolaQuintet();

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester().setDefault(Message.class, newViola())
                               .setDefault(Optional.class, Optional.of(newViolinCello()))
                               .testAllPublicStaticMethods(Quintet.class);
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        reserializeAndAssert(celloQuintet);
        reserializeAndAssert(violaQuintet);

        reserializeAndAssert(
                Quintet.withNullable(newViola(), newViola(), newViola(), newViola(), null)
        );
        reserializeAndAssert(Quintet.withNullable2(newViola(), newViola(), newViola(), null, null));
        reserializeAndAssert(Quintet.withNullable3(newViola(), newViola(), null, null, null));
        reserializeAndAssert(Quintet.withNullable4(newViola(), null, null, null, null));
    }

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        new EqualsTester().addEqualityGroup(newCelloQuintet(), newCelloQuintet())
                          .addEqualityGroup(newViolaQuintet())
                          .testEquals();
    }

    @Test
    @DisplayName("return elements")
    void returnElements() {
        assertEquals(newViolin(NUM_1), celloQuintet.getA());
        assertEquals(newViolin(NUM_2), celloQuintet.getB());
        assertEquals(newViola(), celloQuintet.getC());
        assertEquals(newViolinCello(NUM_1), celloQuintet.getD());
        assertEquals(newViolinCello(NUM_2), celloQuintet.getE());
    }

    @Test
    @DisplayName("tell if elements are present if they are indeed set")
    void tellElementsPresent() {
        assertThat(celloQuintet.hasA()).isTrue();
        assertThat(celloQuintet.hasB()).isTrue();
        assertThat(celloQuintet.hasC()).isTrue();
        assertThat(celloQuintet.hasD()).isTrue();
        assertThat(celloQuintet.hasE()).isTrue();
    }

    @Test
    @DisplayName("tell if elements are absent if set as `null`")
    void tellElementsAbsent() {
        assertThat(Quintet.withNullable(newViola(), newViola(), newViola(), newViola(), null)
                          .hasE()).isFalse();
        assertThat(Quintet.withNullable2(newViola(), newViola(), newViola(), null, null)
                          .hasD()).isFalse();
        assertThat(Quintet.withNullable3(newViola(), newViola(), null, null, null)
                          .hasC()).isFalse();
        assertThat(Quintet.withNullable4(newViola(), null, null, null, null)
                          .hasB()).isFalse();
    }
}
