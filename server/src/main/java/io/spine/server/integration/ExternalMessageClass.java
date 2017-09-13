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
package io.spine.server.integration;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object holding {@linkplain ExternalMessage external message}.
 *
 * @author Alex Tymchenko
 */
@Internal
public class ExternalMessageClass extends MessageClass {

    private static final long serialVersionUID = 0L;

    private ExternalMessageClass(Class<? extends Message> value) {
        super(value);
    }

    public static ExternalMessageClass of(MessageClass messageClass) {
        checkNotNull(messageClass);
        return of(messageClass.value());
    }

    static ExternalMessageClass of(Class<? extends Message> clazz) {
        checkNotNull(clazz);
        return new ExternalMessageClass(clazz);
    }

    public static Set<ExternalMessageClass> fromEventClasses(Set<EventClass> classes) {
        checkNotNull(classes);
        final ImmutableSet.Builder<ExternalMessageClass> builder = ImmutableSet.builder();
        for (EventClass eventClass : classes) {
            builder.add(of(eventClass));
        }
        return builder.build();
    }

    public static Set<ExternalMessageClass> fromRejectionClasses(Set<RejectionClass> classes) {
        checkNotNull(classes);
        final ImmutableSet.Builder<ExternalMessageClass> builder = ImmutableSet.builder();
        for (RejectionClass rejectionClass : classes) {
            builder.add(of(rejectionClass));
        }
        return builder.build();
    }
}
