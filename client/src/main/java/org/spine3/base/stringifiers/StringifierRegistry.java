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

package org.spine3.base.stringifiers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import org.spine3.base.CommandId;
import org.spine3.base.EventId;

import java.util.List;
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

    private final Map<RegistryKey, Stringifier<?>> entries = synchronizedMap(
            newHashMap(
                    ImmutableMap.<RegistryKey, Stringifier<?>>builder()
                            .put(new SingularKey<>(Timestamp.class),
                                 new Stringifiers.TimestampIdStringifer())
                            .put(new SingularKey<>(EventId.class),
                                 new Stringifiers.EventIdStringifier())
                            .put(new SingularKey<>(CommandId.class),
                                 new Stringifiers.CommandIdStringifier())
                            .put(new SingularKey<>(Integer.class),
                                 new Stringifiers.IntegerStringifier())
                            .put(new PluralKey<>(List.class, Integer.class),
                                 new Stringifiers.ListStringifier<>(Integer.class))
                            .build()
            )
    );

    private StringifierRegistry() {
        // Prevent external instantiation of this singleton class.
    }

    public <I extends Message> void register(RegistryKey registryKey, Stringifier<I> converter) {
        checkNotNull(registryKey);
        checkNotNull(converter);
        entries.put(registryKey, converter);
    }

    /**
     * Obtains a {@code Stringifier} for the passed type.
     *
     * @param <I> the type of the values to convert
     * @return the found {@code Stringifer} or empty {@code Optional}
     */
    public <I> Optional<Stringifier<I>> get(RegistryKey registryKey) {
        checkNotNull(registryKey);

        final Stringifier<?> func = entries.get(registryKey);

        @SuppressWarnings("unchecked") /** The cast is safe as we check the first type when adding.
         @see #register(Class, Stringifier) */
        final Stringifier<I> result = (Stringifier<I>) func;
        return Optional.fromNullable(result);
    }

    public synchronized <I> boolean hasStringifierFor(RegistryKey registryKey) {
        final boolean contains = entries.containsKey(registryKey);
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
