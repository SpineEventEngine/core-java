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

package org.spine3.server.reflect;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.spine3.test.Tests.nullRef;

/**
 * @author Dmytro Dashenkov.
 */
public class GetterShould {

    @Test
    public void construct_with_getter_method() throws NoSuchMethodException {
        final Class<Bean> beanClass = Bean.class;
        final Method getterMethod = beanClass.getDeclaredMethod("getField");
        final Getter getter = new Getter(getterMethod);
        assertNotNull(getter);
    }

    @Test
    public void construct_with_boolean_getter_method() throws NoSuchMethodException {
        final Class<Bean> beanClass = Bean.class;
        final Method getterMethod = beanClass.getDeclaredMethod("isOtherField");
        final Getter getter = new Getter(getterMethod);
        assertNotNull(getter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void fail_to_construct_with_non_getter_method() throws NoSuchMethodException {
        final Class<Bean> beanClass = Bean.class;
        final Method method = beanClass.getDeclaredMethod("otherMethod");
        final Getter getter = new Getter(method);
        assertNotNull(getter);
    }

    @Test
    public void contain_meta_info_about_the_method() throws NoSuchMethodException {
        final Class<Bean> beanClass = Bean.class;
        final String methodName = "getField";
        final Method getterMethod = beanClass.getDeclaredMethod(methodName);
        final Getter getter = new Getter(getterMethod);
        assertNotNull(getter);

        final String name = getter.getName();
        assertEquals(name, methodName);

        final String propertyName = getter.getPropertyName();
        assertTrue(methodName.toLowerCase().contains(propertyName));

        final Class returnType = getter.getType();
        assertEquals(int.class, returnType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throw_illegal_argument_on_wrong_type() throws NoSuchMethodException {
        final Class<Bean> beanClass = Bean.class;
        final Method getterMethod = beanClass.getDeclaredMethod("isOtherField");
        final Getter getter = new Getter(getterMethod);

        getter.get(new Object());
    }

    @Test(expected = IllegalStateException.class)
    public void throw_illegal_state_on_reflection_error() throws NoSuchMethodException {
        final Class<Bean> beanClass = Bean.class;
        final Method getterMethod = beanClass.getDeclaredMethod("getNullPrivate");
        final Getter getter = new Getter(getterMethod);

        getter.get(new Bean());
    }

    @Test(expected = NullPointerException.class)
    public void throw_NPE_if_non_null_method_returns_null_value() throws NoSuchMethodException {
        final Class<Bean> beanClass = Bean.class;
        final Method getterMethod = beanClass.getDeclaredMethod("getNull");
        final Getter getter = new Getter(getterMethod);

        getter.get(new Bean());
    }

    public static class Bean {

        private final int field = 42;
        private final boolean otherField = true;

        public int getField() {
            return field;
        }

        public boolean isOtherField() {
            return otherField;
        }

        public void otherMethod() {

        }

        private Object getNullPrivate() {
            return nullRef();
        }

        public Object getNull() {
            return nullRef();
        }
    }
}
