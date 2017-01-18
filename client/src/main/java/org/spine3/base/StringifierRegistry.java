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

/**
 * The registry of converters of types to their string representations.
 *
 * @author Alexander Yevsyukov
 */
public class StringifierRegistry {

    private final Map<Class<?>, Stringifier<?>> entries = synchronizedMap(
            newHashMap(
                    ImmutableMap.<Class<?>, Stringifier<?>>builder()
                            .put(Timestamp.class, new Stringifiers.TimestampIdStringifer())
                            .put(EventId.class, new Stringifiers.EventIdStringifier())
                            .put(CommandId.class, new Identifiers.CommandIdStringifier())
                            .build()
            )
    );

    private StringifierRegistry() {
        // Prevent external instantiation of this singleton class.
    }

    public <I extends Message> void register(Class<I> idClass, Stringifier<I> converter) {
        checkNotNull(idClass);
        checkNotNull(converter);
        entries.put(idClass, converter);
    }

    /**
     * Obtains a {@code Stringifier} for the passed type.
     *
     * @param <I> the type of the values to convert
     * @return
     */
    public <I> Stringifier<I> get(Class<I> valueClass) {
        checkNotNull(valueClass);

        final Stringifier<?> func = entries.get(valueClass);

        @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
            @see #register(Class, Function) */
        final Stringifier<I> result = (Stringifier<I>) func;
        return result;
    }

    public synchronized <I> boolean hasStringiferFor(Class<I> valueClass) {
        final boolean contains = entries.containsKey(valueClass) && (entries.get(valueClass) != null);
        return contains;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final StringifierRegistry value = new StringifierRegistry();
    }

    public static StringifierRegistry getInstance() {
        return Singleton.INSTANCE.value;
    }
}
