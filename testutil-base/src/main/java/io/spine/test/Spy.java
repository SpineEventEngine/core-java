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

package io.spine.test;

import org.mockito.Mockito;

import java.lang.reflect.Field;

import static io.spine.util.Exceptions.illegalArgumentWithCauseOf;

/**
 * Utility for injecting a Mockito spy in a private field of an object.
 *
 * <p>Although, it's a not recommended way of testing, in some cases such an approach is needed for
 * testing interactions in complex objects.
 *
 * @author Alexander Yevsyukov
 */
public class Spy<T> {

    private final Class<T> classOfSpy;

    public static <T> Spy<T> ofClass(Class<T> spyClass) {
        return new Spy<>(spyClass);
    }

    private Spy(Class<T> classOfSpy) {
        this.classOfSpy = classOfSpy;
    }

    public T on(Object obj) {
        final String className = classOfSpy.getSimpleName();
        final String fieldName = className.substring(0, 1).toLowerCase() + className.substring(1);
        return on(obj, fieldName);
    }

    public T on(Object obj, String fieldName) {
        final Class<?> cls = obj.getClass();
        final Object spy;
        try {
            final Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);
            final Object fieldValue = field.get(obj);
            spy = Mockito.spy(fieldValue);
            field.set(obj, spy);
            final T result = classOfSpy.cast(spy);
            return result;
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw illegalArgumentWithCauseOf(e);
        }
    }
}
