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

package io.spine.client.given;

import com.google.protobuf.Any;
import io.spine.client.EntityId;

import javax.annotation.Nullable;
import java.util.function.Function;

import static io.spine.protobuf.TypeConverter.toObject;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Mykhailo Drachuk
 */
public class EntityIdUnpacker<T> implements Function<EntityId, T> {

    private final Class<T> targetClass;

    public EntityIdUnpacker(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public T apply(@Nullable EntityId entityId) {
        assertNotNull(entityId);
        Any value = entityId.getId();
        T actual = toObject(value, targetClass);
        return actual;
    }

    public static <T> EntityIdUnpacker<T> unpacker(Class<T> targetClass) {
        return new EntityIdUnpacker<>(targetClass);
    }
}
