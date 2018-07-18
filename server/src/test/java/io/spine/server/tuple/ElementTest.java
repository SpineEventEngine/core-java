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

package io.spine.server.tuple;

import com.google.common.testing.EqualsTester;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.test.TestValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("DuplicateStringLiteralInspection") // Common test display names.
@DisplayName("Element should")
class ElementTest {

    @Test
    @DisplayName("support equality")
    void supportEquality() {
        Timestamp time = Time.getCurrentTime();
        new EqualsTester().addEqualityGroup(new Element(time), new Element(time))
                          .addEqualityGroup(new Element(TestValues.newUuidValue()))
                          .addEqualityGroup(new Element(Optional.empty()))
                          .testEquals();
    }

    @Test
    @DisplayName("be serializable")
    void serialize() {
        reserializeAndAssert(new Element(Time.getCurrentTime()));
        reserializeAndAssert(new Element(Optional.of(Time.getCurrentTime())));
        reserializeAndAssert(new Element(Optional.empty()));
    }

    @Test
    @DisplayName("restrict possible value types")
    void restrictPossibleValueTypes() {
        assertThrows(IllegalArgumentException.class, () -> new Element(getClass()));
    }
}
