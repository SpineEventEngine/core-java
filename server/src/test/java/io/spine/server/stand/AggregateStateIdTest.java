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

package io.spine.server.stand;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Any;
import io.spine.string.Stringifier;
import io.spine.string.StringifierRegistry;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dmytro Dashenkov
 */
@DisplayName("AggregateStateId should")
class AggregateStateIdTest {

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("not accept nulls on construction")
    void rejectNullsOnConstruction() {
        new NullPointerTester()
                .setDefault(TypeUrl.class, TypeUrl.of(Any.class))
                .testStaticMethods(AggregateStateId.class, NullPointerTester.Visibility.PACKAGE);
    }

    @Test
    @DisplayName("have stringifier")
    void haveStringifier() throws ClassNotFoundException {
        // Ensure class loaded.
        Class.forName(AggregateStateId.class.getCanonicalName());

        Optional<Stringifier<AggregateStateId>> stringifierOptional =
                StringifierRegistry.getInstance()
                                   .get(AggregateStateId.class);
        assertTrue(stringifierOptional.isPresent());
    }
}
