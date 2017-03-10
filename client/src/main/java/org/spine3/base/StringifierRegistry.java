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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
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

    private final Map<TypeToken<?>, Stringifier<?>> entries = synchronizedMap(
            newHashMap(
                    ImmutableMap.<TypeToken<?>, Stringifier<?>>builder()
                            .put(TypeToken.of(Timestamp.class),
                                 new Stringifiers.TimestampIdStringifer())
                            .put(TypeToken.of(EventId.class),
                                 new Stringifiers.EventIdStringifier())
                            .put(TypeToken.of(CommandId.class),
                                 new Stringifiers.CommandIdStringifier())
                            .build()
            )
    );

    private StringifierRegistry() {
        // Prevent external instantiation of this singleton class.
    }

    public <I extends Message> void register(TypeToken<I> valueClass, Stringifier<I> converter) {
        checkNotNull(valueClass);
        checkNotNull(converter);
        entries.put(valueClass, converter);
    }

    /**
     * Obtains a {@code Stringifier} for the passed type.
     *
     * @param <I> the type of the values to convert
     * @return the found {@code Stringifer} or empty {@code Optional}
     */
    public <I> Optional<Stringifier<I>> get(TypeToken<I> valueToken) {
        checkNotNull(valueToken);

        final Stringifier<?> func = entries.get(valueToken);

        @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
         @see #register(Class, Stringifier) */
        final Stringifier<I> result = (Stringifier<I>) func;
        return Optional.fromNullable(result);
    }

    public synchronized <I> boolean hasStringifierFor(TypeToken<I> valueToken) {
        final boolean contains = entries.containsKey(valueToken);
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
