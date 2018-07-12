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

package io.spine.server.storage.memory;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.core.BoundedContextName;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.core.BoundedContextNames.newName;
import static io.spine.test.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("StorageSpec should")
class StorageSpecTest {

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(TypeUrl.class, TypeUrl.of(Empty.class))
                .setDefault(BoundedContextName.class, newName("default"))
                .testAllPublicStaticMethods(StorageSpec.class);
    }

    @Test
    @DisplayName("create new instances")
    void createNewInstances() {
        final BoundedContextName bcName = newName(getClass().getName());
        final TypeUrl stateUrl = TypeUrl.of(StringValue.class);
        final Class<Long> idClass = Long.class;

        final StorageSpec<Long> spec = StorageSpec.of(bcName, stateUrl, idClass);

        assertEquals(bcName, spec.getBoundedContextName());
        assertEquals(stateUrl, spec.getEntityStateUrl());
        assertEquals(idClass, spec.getIdClass());
    }

    @Test
    @DisplayName("provide `equals` based on values")
    void provideEquals() {
        final BoundedContextName bcName = newName(getClass().getName());

        new EqualsTester()
                .addEqualityGroup(
                        StorageSpec.of(bcName, TypeUrl.of(StringValue.class), String.class),
                        StorageSpec.of(bcName, TypeUrl.of(StringValue.class), String.class))
                .addEqualityGroup(
                        StorageSpec.of(bcName, TypeUrl.of(Timestamp.class), Integer.class),
                        StorageSpec.of(bcName, TypeUrl.of(Timestamp.class), Integer.class))
                .testEquals();
    }

    @Test
    @DisplayName("be serialized")
    void beSerialized() {
        SerializableTester.reserializeAndAssert(
                StorageSpec.of(newName(getClass().getSimpleName()),
                               TypeUrl.of(DoubleValue.class),
                               String.class));
    }
}
