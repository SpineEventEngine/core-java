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

package io.spine.server.model;

import com.google.common.base.MoreObjects;
import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Provides information for dispatching a message to a handler method.
 */
@Immutable
public final class DispatchKey {

    private final Class<? extends Message> messageClass;
    private final @Nullable ArgumentFilter filter;
    private final @Nullable Class<? extends Message> originClass;

    public DispatchKey(Class<? extends Message> messageClass,
                       @Nullable ArgumentFilter filter,
                       @Nullable Class<? extends Message> originClass) {
        this.messageClass = messageClass;
        this.filter = filter;
        this.originClass = originClass;
    }

    /**
     * Obtains a filter-less version of this.
     *
     * <p>If this key has a filter, a new instance is created, which copies this key data
     * without the filter. Otherwise, this instance is returned.
     */
    DispatchKey withoutFilter() {
        if (filter == null) {
            return this;
        }
        return new DispatchKey(messageClass, null, originClass);
    }

    /**
     * Creates a new key copying its data and taking the passed filter.
     */
    public DispatchKey withFilter(ArgumentFilter filter) {
        return new DispatchKey(messageClass, filter, originClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageClass, filter, originClass);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final DispatchKey other = (DispatchKey) obj;
        return Objects.equals(this.messageClass, other.messageClass)
                && Objects.equals(this.filter, other.filter)
                && Objects.equals(this.originClass, other.originClass);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection")   // Both classes have `filter` field.
    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
        helper.add("messageClass", messageClass.getName());
        if (filter != null && !filter.acceptsAll()) {
            helper.add("filter", filter);
        }
        if (originClass != null) {
            helper.add("originClass", originClass);
        }
        return helper.toString();
    }
}
