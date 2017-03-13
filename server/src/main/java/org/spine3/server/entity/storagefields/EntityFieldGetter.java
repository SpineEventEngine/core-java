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

package org.spine3.server.entity.storagefields;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.entity.Entity;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Dmytro Dashenkov.
 */
class EntityFieldGetter<E extends Entity<?, ?>> {

    private static final String GETTER_PREFIX = "get";
    private static final String BOOLEAN_GETTER_PREFIX = "is";

    private final String name;
    private final Class<E> entityClass;
    private Method getter;
    private boolean nullable;

    EntityFieldGetter(String name, Class<E> entityClass) {
        this.name = checkNotNull(name);
        this.entityClass = checkNotNull(entityClass);
    }

    public String getName() {
        return name;
    }

    Object get(E entity) {
        ensureGetter();
        final Object property;
        try {
            property = getter.invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
        if (!nullable) {
            checkNotNull(property,
                         "null value of a non-null property %s.",
                         name);
        }
        return property;
    }

    private void ensureGetter() {
        if (getter != null) {
            return;
        }
        lookupGetter(getterName());
        if (getter != null) {
            initialize();
            return;
        }
        lookupGetter(booleanGetterName());
        checkState(getter != null,
                   "Getter for property %s was not found.",
                   name);
        initialize();
    }

    private void lookupGetter(String methodName) {
        checkState(getter == null, "Getter is already initialized.");
        try {
            getter = entityClass.getDeclaredMethod(methodName);
        } catch (NoSuchMethodException e) {
            log().debug(Throwables.getStackTraceAsString(e));
            try {
                getter = entityClass.getMethod(methodName);
            } catch (NoSuchMethodException ignored) {
            }
        }
    }

    private void initialize() {
        checkNotNull(getter);
        final Annotation[] annotations = getter.getDeclaredAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation.annotationType()
                          .equals(Nullable.class)) {
                nullable = true;
                return;
            }
        }
    }

    private String getterName() {
        // TODO:2017-03-13:dmytro.dashenkov: Improve performance through chars and binary operations.
        return GETTER_PREFIX + name.substring(0, 1)
                                   .toUpperCase() + name.substring(1);
    }

    private String booleanGetterName() {
        return BOOLEAN_GETTER_PREFIX + name.substring(0, 1)
                                           .toUpperCase() + name.substring(1);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(EntityFieldGetter.class);
    }
}
