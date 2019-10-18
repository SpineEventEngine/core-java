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

package io.spine.server.entity.storage;

import com.google.protobuf.Message;
import io.spine.server.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

import static io.spine.util.Exceptions.newIllegalStateException;

public final class Column {

    private final ColumnName name;
    private final Class<?> type;
    private final Getter getter;

    Column(ColumnName name, Class<?> type, Getter getter) {
        this.name = name;
        this.type = type;
        this.getter = getter;
    }

    public ColumnName name() {
        return name;
    }

    public Class<?> type() {
        return type;
    }

    public @Nullable Object valueIn(Entity<?, ? extends Message> entity) {
        return getter.apply(entity);
    }

    interface Getter extends Function<Entity<?, ? extends Message>, Object> {

        Object invoke(Entity<?, ? extends Message> entity) throws Exception;

        @Override
        default Object apply(Entity<?, ? extends Message> entity) {
            try {
                return invoke(entity);
            } catch (Exception e) {
                throw newIllegalStateException(e, "Error on column getter invocation.");
            }
        }
    }
}
