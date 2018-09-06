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
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.StringValue;
import io.spine.test.client.TestEntity;
import io.spine.test.queries.TaskId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.client.Targets.allOf;
import static io.spine.client.Targets.someOf;
import static io.spine.client.given.TargetsTestEnv.filtersForIds;
import static io.spine.client.given.TargetsTestEnv.newTaskId;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static io.spine.type.TypeUrl.of;
import static io.spine.type.TypeUrl.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author Alex Tymchenko
 */
@DisplayName("Targets utility should")
class TargetsTest {

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Targets.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(Targets.class);
    }

    @Test
    @DisplayName("compose Target for all of type")
    void composeForAllOfType() {
        Target target = allOf(TestEntity.class);

        assertEquals(of(TestEntity.class), parse(target.getType()));
    }

    @Test
    @DisplayName("compose Target with Messagee IDs")
    void composeWithMessageIds() {
        TaskId taskId = newTaskId();
        Target target = someOf(TestEntity.class, newSet(taskId));

        assertEquals(of(TestEntity.class), parse(target.getType()));

        EntityFilters filters = filtersForIds(taskId);
        assertEquals(filters, target.getFilters());
    }

    @Test
    @DisplayName("compose Target with String IDs")
    void composeWithStringIds() {
        String firstId = "a";
        String secondId = "b";
        String thirdId = "c";

        Target target = someOf(TestEntity.class, newSet(firstId, secondId, thirdId));

        assertEquals(of(TestEntity.class), parse(target.getType()));

        EntityFilters filters = filtersForIds(StringValue.of(firstId),
                                              StringValue.of(secondId),
                                              StringValue.of(thirdId));
        assertEquals(filters, target.getFilters());
    }

    @Test
    @DisplayName("compose Target with Integer IDs")
    void composeWithIntIds() {
        int firstId = 1;
        int secondId = 2;
        int thirdId = 3;

        Target target = someOf(TestEntity.class, newSet(firstId, secondId, thirdId));

        assertEquals(of(TestEntity.class), parse(target.getType()));

        EntityFilters filters = filtersForIds(Int32Value.of(firstId),
                                              Int32Value.of(secondId),
                                              Int32Value.of(thirdId));
        assertEquals(filters, target.getFilters());
    }

    @Test
    @DisplayName("compose Target with Long IDs")
    void composeWithLongIds() {
        long firstId = 1L;
        long secondId = 2L;
        long thirdId = 3L;

        Target target = someOf(TestEntity.class, newSet(firstId, secondId, thirdId));

        assertEquals(of(TestEntity.class), parse(target.getType()));

        EntityFilters filters = filtersForIds(Int64Value.of(firstId),
                                              Int64Value.of(secondId),
                                              Int64Value.of(thirdId));
        assertEquals(filters, target.getFilters());
    }

    @Test
    @DisplayName("throw IAE for unsupported IDs")
    void throwIaeForUnsupportedIds() {
        assertThrows(IllegalArgumentException.class,
                     () -> someOf(TaskId.class, newSet(new Object())));
    }
}
