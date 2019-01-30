/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.event.model;

import io.spine.base.EventMessage;
import io.spine.core.EventClass;
import io.spine.server.event.EventReceiver;
import io.spine.type.MessageClass;

import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * The helper class for holding messaging information on behalf of another model class.
 *
 * @param <T> the type of the raw class for obtaining messaging information
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public final class ReactorClassDelegate<T extends EventReceiver>
        extends EventReceivingClassDelegate<T, EventReactorMethod>
        implements ReactingClass {

    private static final long serialVersionUID = 0L;

    public ReactorClassDelegate(Class<T> cls) {
        super(cls, new EventReactorSignature());
    }

    @Override
    public EventReactorMethod getReactor(EventClass eventClass, MessageClass originClass) {
        return getMethod(eventClass, originClass);
    }

    @Override
    public Set<Class<? extends EventMessage>> reactsWith() {
        // todo try doing something about these casts
        Set<Class<? extends EventMessage>> result = getProducedTypes()
                .stream()
                .map(cls -> (Class<? extends EventMessage>) cls)
                .collect(toSet());
        return result;
    }
}
