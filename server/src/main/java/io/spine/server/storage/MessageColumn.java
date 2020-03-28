/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.storage;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.server.entity.storage.ColumnName;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * @author Alex Tymchenko
 */
@Immutable
@Internal
public final class MessageColumn<V, M extends Message> extends AbstractColumn {

    private final Getter<M, V> getter;

    public MessageColumn(ColumnName name, Class<V> type, Getter<M, V> getter) {
        super(name, type);
        this.getter = getter;
    }

    public @Nullable V valueIn(M record) {
        return getter.apply(record);
    }

    @Immutable
    public interface Getter<M extends Message, V> extends Function<M, V> {
    }
}
