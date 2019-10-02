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

package io.spine.server.model;

import com.google.common.reflect.TypeToken;
import com.google.common.testing.NullPointerTester;
import io.spine.base.EventMessage;
import io.spine.core.UserId;
import io.spine.server.tuple.Pair;
import io.spine.server.tuple.Triplet;
import io.spine.testing.UtilityClassTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.server.model.Types.matches;

@DisplayName("`Types` utility class should")
class TypesTest extends UtilityClassTest<Types> {

    TypesTest() {
        super(Types.class);
    }

    @Override
    protected void configure(NullPointerTester tester) {
        super.configure(tester);
        tester.setDefault(TypeToken.class, TypeToken.of(TypesTest.class));
    }

    @Test
    @DisplayName("not accept nulls in package-private static methods")
    @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert") /* Asserting via `NullPointerTester. */
    void nullCheckPublicStaticMethods() {
        NullPointerTester tester = new NullPointerTester();
        configure(tester);
        tester.testStaticMethods(getUtilityClass(), NullPointerTester.Visibility.PACKAGE);
    }

    @SuppressWarnings({"SerializableNonStaticInnerClassWithoutSerialVersionUID",
            "SerializableInnerClassWithNonSerializableOuterClass"})
    @Test
    @DisplayName("match the same type against itself")
    void matchSameTypes() {
        assertSameTypeMatches(TypeToken.of(UserId.class));
        assertSameTypeMatches(new TypeToken<List<EventMessage>>() {});
        assertSameTypeMatches(new TypeToken<Pair<UserId, EventMessage>>() {});
        assertSameTypeMatches(
                new TypeToken<Triplet<UserId, EventMessage, Optional<EventMessage>>>() {});
    }

    private static void assertSameTypeMatches(TypeToken<?> type) {
        assertThat(matches(type, type)).isTrue();
    }
}
