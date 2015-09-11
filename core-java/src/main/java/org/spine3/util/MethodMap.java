/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A map for storing methods handling messages.
 *
 * @author Alexander Yevsyukov
 */
public class MethodMap {

    private final ImmutableMap<Class<? extends Message>, Method> map;

    public MethodMap(Class<?> clazz, Predicate<Method> filter) {
        this.map = ImmutableMap.<Class<? extends Message>, Method>builder().putAll(
                Methods.scan(clazz, filter))
                .build();
    }

    /**
     * @return {@code true} if the map is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsHandlerFor(Class<? extends Message> messageClass) {
        return map.containsKey(checkNotNull(messageClass));
    }

    @Nullable
    public Method get(Class<? extends Message> messageClass) {
        return map.get(checkNotNull(messageClass));
    }
}
