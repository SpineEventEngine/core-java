/*
 * Copyright 2018, TeamDev. All rights reserved.
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
package io.spine.server.integration;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.EventClass;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object holding a class of {@linkplain ExternalMessage external message}.
 *
 * @author Alex Tymchenko
 */
@Internal
public final class ExternalMessageClass extends MessageClass<Message> {

    private static final long serialVersionUID = 0L;

    private ExternalMessageClass(Class<? extends Message> value) {
        super(value);
    }

    /**
     * Creates an instance of {@code ExternalMessageClass} on top of existing message class.
     *
     * @param messageClass a message class to wrap
     * @return a new instance of {@code ExternalMessageClass}.\
     */
    public static ExternalMessageClass of(MessageClass<?> messageClass) {
        checkNotNull(messageClass);
        return of(messageClass.value());
    }

    static ExternalMessageClass of(Class<? extends Message> clazz) {
        checkNotNull(clazz);
        return new ExternalMessageClass(clazz);
    }

    /**
     * Transforms a given set of {@linkplain EventClass event classes} into a set
     * of {@code ExternalMessageClass}es by wrapping each event class
     * into an external message class.
     *
     * @param classes the set of event classes to transform
     * @return a set of {@code ExternalMessageClass}es, each wrapping an item from the original set
     */
    public static Set<ExternalMessageClass> fromEventClasses(Set<EventClass> classes) {
        checkNotNull(classes);
        ImmutableSet.Builder<ExternalMessageClass> builder = ImmutableSet.builder();
        for (EventClass eventClass : classes) {
            builder.add(of(eventClass));
        }
        return builder.build();
    }
}
