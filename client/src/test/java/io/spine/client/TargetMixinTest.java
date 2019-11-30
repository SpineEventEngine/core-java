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

package io.spine.client;

import io.spine.test.client.TestEntity;
import io.spine.test.queries.ProjectId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`TargetMixin` should")
class TargetMixinTest {

    private static final TypeUrl TEST_ENTITY_TYPE = TypeUrl.of(TestEntity.class);
    private static final TypeUrl PROJECT_ID_TYPE = TypeUrl.of(ProjectId.class);

    @Nested
    @DisplayName("validate the target")
    class ValidateTarget {

        @Test
        @DisplayName("which is valid")
        void valid() {
            Filter filter = Filters.eq("first_field", "some str");
            Target target = target(TEST_ENTITY_TYPE, filter);
            target.checkValid();
        }

        @Test
        @DisplayName("which has invalid type")
        void withInvalidType() {
            Filter filter = Filters.eq("second_field", false);
            Target target = target(PROJECT_ID_TYPE, filter);
            assertInvalid(target);
        }

        @Test
        @DisplayName("which has invalid filters")
        void withInvalidFilters() {
            Filter filter = Filters.eq("non_existent_field", false);
            Target target = target(TEST_ENTITY_TYPE, filter);
            assertInvalid(target);
        }

        private void assertInvalid(Target target) {
            assertThrows(IllegalStateException.class, target::checkValid);
        }
    }

    private static Target target(TypeUrl type, Filter... filters) {
        Set<CompositeFilter> compositeFilters = stream(filters)
                .map(TargetMixinTest::compositeFilter)
                .collect(toSet());
        TargetFilters targetFilters = TargetFilters
                .newBuilder()
                .addAllFilter(compositeFilters)
                .build();
        Target result = Target
                .newBuilder()
                .setType(type.value())
                .setFilters(targetFilters)
                .build();
        return result;
    }

    private static CompositeFilter compositeFilter(Filter filter) {
        CompositeFilter result = CompositeFilter
                .newBuilder()
                .setOperator(ALL)
                .addFilter(filter)
                .build();
        return result;
    }
}
