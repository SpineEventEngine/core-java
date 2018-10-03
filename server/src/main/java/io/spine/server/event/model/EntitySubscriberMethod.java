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

import com.google.common.base.Objects;
import io.spine.core.ByField;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.model.declare.ParameterSpec;
import io.spine.system.server.EntityStateChanged;
import io.spine.type.TypeUrl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Dmytro Dashenkov
 */
public final class EntitySubscriberMethod extends SubscriberMethod {

    EntitySubscriberMethod(Method method, ParameterSpec<EventEnvelope> parameterSpec) {
        super(method, parameterSpec);
    }

    @Override
    protected ByField getFilter() {
        TypeUrl targetType = TypeUrl.of(rawMessageClass());
        return new TypeFilteringFilter(targetType);
    }

    @Override
    public boolean isExternal() {
        return true;
    }

    @Override
    public boolean isDomestic() {
        return false;
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(EntityStateChanged.class);
    }

    @SuppressWarnings("ClassExplicitlyAnnotation")
    private static final class TypeFilteringFilter implements ByField {

        private final TypeUrl targetType;

        private TypeFilteringFilter(TypeUrl type) {
            targetType = type;
        }

        @Override
        public String path() {
            return "id.type_url";
        }

        @Override
        public String value() {
            return targetType.value();
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ByField.class;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ByField)) {
                return false;
            }
            ByField filter = (ByField) o;
            return Objects.equal(this.path(), filter.path())
                    && Objects.equal(this.value(), filter.value());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value());
        }
    }
}
