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
package io.spine.client;

import com.google.common.testing.NullPointerTester;
import io.spine.test.client.TestEntity;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.test.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Alex Tymchenko
 */
@DisplayName("Queries utility should")
class QueriesTest {

    private static final String TARGET_ENTITY_TYPE_URL =
            "type.spine.io/spine.test.queries.TestEntity";

    @SuppressWarnings("DuplicateStringLiteralInspection") // Display name for utility c-tor test.
    @Test
    @DisplayName("have private parameterless constructor")
    void haveUtilityCtor() {
        assertHasPrivateParameterlessCtor(Queries.class);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Display name for null test.
    @Test
    @DisplayName("not accept nulls for non-Nullable public method arguments")
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Queries.class);
    }

    @Test
    @DisplayName("obtain proper entity type url for known query target")
    void returnTypeForKnownTarget() {
        final Target target = Targets.allOf(TestEntity.class);
        final Query query = Query.newBuilder()
                                 .setTarget(target)
                                 .build();
        final TypeUrl type = Queries.typeOf(query);
        assertNotNull(type);
        assertEquals(TARGET_ENTITY_TYPE_URL, type.toString());
    }

    @Test
    @DisplayName("throw IllegalStateException for query target of unknown type")
    void throwErrorForUnknownType() {
        final Target target = Target.newBuilder()
                                    .setType("nonexistent/message.type")
                                    .build();
        final Query query = Query.newBuilder()
                                 .setTarget(target)
                                 .build();
        assertThrows(IllegalStateException.class, () -> Queries.typeOf(query));
    }
}
