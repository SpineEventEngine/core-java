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

package io.spine.server.event.model;

import com.google.protobuf.Any;
import io.spine.base.FieldPath;
import io.spine.core.ByField;
import io.spine.core.EventEnvelope;
import io.spine.core.Subscribe;
import io.spine.protobuf.FieldPaths;
import io.spine.server.model.MessageFilter;
import io.spine.server.model.declare.ParameterSpec;

import java.lang.reflect.Method;

import static io.spine.protobuf.FieldPaths.typeOfFieldAt;
import static io.spine.protobuf.TypeConverter.toAny;
import static io.spine.string.Stringifiers.fromString;

/**
 * A wrapper for an event subscriber method.
 *
 * @author Alexander Yevsyukov
 * @see Subscribe
 */
public final class EventSubscriberMethod extends SubscriberMethod {

    /** Creates a new instance. */
    EventSubscriberMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    @Override
    public MessageFilter filter() {
        Subscribe annotation = getRawMethod().getAnnotation(Subscribe.class);
        ByField byFieldFilter = annotation.filter();
        String rawFieldPath = byFieldFilter.path();
        if (rawFieldPath.isEmpty()) {
            return MessageFilter.getDefaultInstance();
        }
        FieldPath fieldPath = FieldPaths.parse(rawFieldPath);
        Class<?> fieldType = typeOfFieldAt(rawMessageClass(), fieldPath);
        Object expectedValue = fromString(byFieldFilter.value(), fieldType);
        Any packedValue = toAny(expectedValue);
        MessageFilter messageFilter = MessageFilter
                .newBuilder()
                .setField(fieldPath)
                .setValue(packedValue)
                .build();
        return messageFilter;
    }

    @Override
    protected void checkAttributesMatch(EventEnvelope envelope) throws IllegalArgumentException {
        super.checkAttributesMatch(envelope);
        ensureExternalMatch(envelope.getEventContext().getExternal());
    }
}
