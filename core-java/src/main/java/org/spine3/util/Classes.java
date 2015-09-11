/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import static com.google.common.base.Throwables.propagate;

/**
 * Utilities for working with classes.
 *
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("UtilityClass")
public class Classes {

    private Classes() {}

    public static <T> Class<T> getGenericParameterType(Object object, int paramNumber) {
        try {
            Type genericSuperclass = object.getClass().getGenericSuperclass();
            Field actualTypeArguments = genericSuperclass.getClass().getDeclaredField("actualTypeArguments");

            actualTypeArguments.setAccessible(true);
            @SuppressWarnings("unchecked")
            Class<T> result = (Class<T>) ((Type[]) actualTypeArguments.get(genericSuperclass))[paramNumber];
            actualTypeArguments.setAccessible(false);

            return result;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw propagate(e);
        }
    }
}
