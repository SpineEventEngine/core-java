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

package io.spine.server.storage.memory;

import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.core.BoundedContextId;
import io.spine.type.TypeUrl;
import org.junit.Test;

import static io.spine.server.BoundedContext.newId;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Yevsyukov
 */
public class StorageSpecShould {

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester()
                .setDefault(TypeUrl.class, TypeUrl.of(Empty.class))
                .setDefault(BoundedContextId.class, newId("default"))
                .testAllPublicStaticMethods(StorageSpec.class);
    }

    @Test
    public void create_new_instances() {
        final BoundedContextId bcId = newId(getClass().getName());
        final TypeUrl stateUrl = TypeUrl.of(StringValue.class);
        final Class<Long> idClass = Long.class;

        final StorageSpec<Long> spec = StorageSpec.of(bcId, stateUrl, idClass);

        assertEquals(bcId, spec.getBoundedContextId());
        assertEquals(stateUrl, spec.getEntityStateUrl());
        assertEquals(idClass, spec.getIdClass());
    }

    @Test
    public void provide_equals_based_on_values() {
        final BoundedContextId bcId = newId(getClass().getName());

        new EqualsTester()
                .addEqualityGroup(
                        StorageSpec.of(bcId, TypeUrl.of(StringValue.class), String.class),
                        StorageSpec.of(bcId, TypeUrl.of(StringValue.class), String.class))
                .addEqualityGroup(
                        StorageSpec.of(bcId, TypeUrl.of(Timestamp.class), Integer.class),
                        StorageSpec.of(bcId, TypeUrl.of(Timestamp.class), Integer.class))
                .testEquals();
    }

    @Test
    public void serialize() {
        SerializableTester.reserializeAndAssert(
                StorageSpec.of(newId(getClass().getSimpleName()),
                               TypeUrl.of(DoubleValue.class),
                               String.class));
    }
}
