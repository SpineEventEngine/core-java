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

package org.spine3.base;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Collections.synchronizedMap;

/** The registry of converters of ID types to string representations. */
public class ConverterRegistry {

    private final Map<Class<?>, Function<?, String>> entries = synchronizedMap(
            newHashMap(
                    ImmutableMap.<Class<?>, Function<?, String>>builder()
                            .put(Timestamp.class, new Identifiers.TimestampToStringConverter())
                            .put(EventId.class, new Identifiers.EventIdToStringConverter())
                            .put(CommandId.class, new Identifiers.CommandIdToStringConverter())
                            .build()
            )
    );

    private ConverterRegistry() {
    }

    public <I extends Message> void register(Class<I> idClass, Function<I, String> converter) {
        checkNotNull(idClass);
        checkNotNull(converter);
        entries.put(idClass, converter);
    }

    public <I> Function<I, String> getConverter(I id) {
        checkNotNull(id);
        checkNotClass(id);

        final Function<?, String> func = entries.get(id.getClass());

        @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
            @see #register(Class, Function) */
        final Function<I, String> result = (Function<I, String>) func;
        return result;
    }

    private static <I> void checkNotClass(I id) {
        if (id instanceof Class) {
            throw new IllegalArgumentException("Class instance passed instead of value: " + id.toString());
        }
    }

    public synchronized <I> boolean containsConverter(I id) {
        final Class<?> idClass = id.getClass();
        final boolean contains = entries.containsKey(idClass) && (entries.get(idClass) != null);
        return contains;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final ConverterRegistry value = new ConverterRegistry();
    }

    public static ConverterRegistry getInstance() {
        return Singleton.INSTANCE.value;
    }

}
