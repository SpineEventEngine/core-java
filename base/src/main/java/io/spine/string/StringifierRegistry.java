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

package io.spine.string;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;

import java.lang.reflect.Type;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static io.spine.protobuf.Messages.isMessage;
import static java.lang.String.format;
import static java.util.Collections.synchronizedMap;

/**
 * The registry of converters of types to their string representations.
 *
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 */
public final class StringifierRegistry {

    private final Map<Type, Stringifier<?>> stringifiers = synchronizedMap(
            newHashMap(
                    ImmutableMap.<Type, Stringifier<?>>builder()
                            .put(Boolean.class, Stringifiers.forBoolean())
                            .put(Integer.class, Stringifiers.forInteger())
                            .put(Long.class, Stringifiers.forLong())
                            .put(String.class, Stringifiers.forString())
                            .build()
            )
    );

    private StringifierRegistry() {
        // Prevent external instantiation of this singleton class.
    }

    static <T> Stringifier<T> getStringifier(Type typeOfT) {
        checkNotNull(typeOfT);
        final Optional<Stringifier<T>> stringifierOptional = getInstance().get(typeOfT);

        if (stringifierOptional.isPresent()) {
            final Stringifier<T> stringifier = stringifierOptional.get();
            return stringifier;
        }

        if (isMessage(typeOfT)) {
            return getDefaultStringifier(typeOfT);
        }

        final String errMsg = format("No stringifier registered for the type: %s", typeOfT);
        throw new MissingStringifierException(errMsg);
    }

    @SuppressWarnings("unchecked") // It is OK because the class is checked before the cast.
    private static <T> Stringifier<T> getDefaultStringifier(Type typeOfT) {
        final Stringifier<T> result =
                (Stringifier<T>) Stringifiers.newForMessage((Class<Message>) typeOfT);
        return result;
    }

    /**
     * Casts the passed instance.
     *
     * <p>The cast is safe as we check the first type when
     * {@linkplain #register(Stringifier, Type) adding}.
     */
    @SuppressWarnings("unchecked")
    private static <T> Stringifier<T> cast(Stringifier<?> func) {
        return (Stringifier<T>) func;
    }

    public static StringifierRegistry getInstance() {
        return Singleton.INSTANCE.value;
    }

    /**
     * Registers the passed stringifier in the registry.
     *
     * @param stringifier the stringifier to register
     * @param typeOfT     the value of the type of objects handled by the stringifier
     * @param <T>         the type of the objects handled by the stringifier
     */
    public <T> void register(Stringifier<T> stringifier, Type typeOfT) {
        checkNotNull(typeOfT);
        checkNotNull(stringifier);
        stringifiers.put(typeOfT, stringifier);
    }

    /**
     * Obtains a {@code Stringifier} for the passed type.
     *
     * @param typeOfT the type to stringify
     * @param <T>     the type of the values to convert
     * @return the found {@code Stringifier} or empty {@code Optional}
     */
    public <T> Optional<Stringifier<T>> get(Type typeOfT) {
        checkNotNull(typeOfT);

        final Stringifier<?> func = stringifiers.get(typeOfT);

        final Stringifier<T> result = cast(func);
        return Optional.fromNullable(result);
    }

    /**
     * Tells whether there is a Stringifier registered for the passed type.
     *
     * @param type the type for which to find a stringifier
     * @return {@code true} if there is a registered stringifier, {@code false} otherwise
     */
    synchronized boolean hasStringifierFor(Type type) {
        final boolean contains = stringifiers.containsKey(type);
        return contains;
    }

    private enum Singleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final StringifierRegistry value = new StringifierRegistry();
    }
}
