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
import org.spine3.protobuf.Timestamps2;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;
import static org.spine3.util.Exceptions.conversionArgumentException;

/**
 * The registry of converters of types to their string representations.
 *
 * @author Alexander Yevsyukov
 */
public class StringifierRegistry {

    private final Map<TypeToken<?>, Stringifier<?>> entries = synchronizedMap(
            newHashMap(
                    ImmutableMap.<TypeToken<?>, Stringifier<?>>builder()
                            .put(TypeToken.of(Timestamp.class), Timestamps2.stringifier())
                            .put(TypeToken.of(EventId.class), Events.idStringifier())
                            .put(TypeToken.of(CommandId.class), Commands.idStringifier())
                            .build()
            )
    );

    private StringifierRegistry() {
        // Prevent external instantiation of this singleton class.
    }

    static <T> Stringifier<T> getStringifier(TypeToken<T> typeToken) {
        final Optional<Stringifier<T>> stringifierOptional = getInstance().get(typeToken);

        if (!stringifierOptional.isPresent()) {
            final String exMessage =
                    format("Stringifier for the %s is not provided", typeToken);
            throw conversionArgumentException(exMessage);
        }
        final Stringifier<T> stringifier = stringifierOptional.get();
        return stringifier;
    }

    public <T extends Message> void register(TypeToken<T> valueClass, Stringifier<T> converter) {
        checkNotNull(valueClass);
        checkNotNull(converter);
        entries.put(valueClass, converter);
    }

    /**
     * Obtains a {@code Stringifier} for the passed type.
     *
     * @param <T> the type of the values to convert
     * @return the found {@code Stringifer} or empty {@code Optional}
     */
    public <T> Optional<Stringifier<T>> get(TypeToken<T> valueToken) {
        checkNotNull(valueToken);

        final Stringifier<?> func = entries.get(valueToken);

        final Stringifier<T> result = cast(func);
        return Optional.fromNullable(result);
    }

    /**
     * Casts the passed instance.
     *
     * <p>The cast is safe as we check the first type when
     * {@linkplain #register(TypeToken, Stringifier) adding}.
     */
    @SuppressWarnings("unchecked")
    private static <T> Stringifier<T> cast(Stringifier<?> func) {
        return (Stringifier<T>) func;
    }

    /**
     * Tells whether there is a Stringifier registered for the passed type.
     *
     * @param valueToken the token with the type info
     * @param <T> the type to be converted to a string
     * @return {@code true} if there is a registered stringifier, {@code false} otherwise
     */
    public synchronized <T> boolean hasStringifierFor(TypeToken<T> valueToken) {
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
