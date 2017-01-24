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

package org.spine3.test;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;

/**
 * @author Illia Shepilov
 */
public class NullToleranceTestBuilderShould {

    @Test(expected = NullPointerException.class)
    @SuppressWarnings("ConstantConditions") // Need to test the builder.
    public void not_accept_null_target_class() {
        NullToleranceTest.newBuilder()
                         .setClass(null);
    }

    @Test
    public void return_target_class() {
        final Class<?> targetClass = NullToleranceTest.newBuilder()
                                                      .setClass(NullToleranceTestBuilderShould.class)
                                                      .getTargetClass();
        assertEquals(NullToleranceTestBuilderShould.class, targetClass);
    }

    @Test
    public void return_default_values_map() {
        final NullToleranceTest.Builder builder = NullToleranceTest.newBuilder();

        int expectedSize = 0;
        int actualSize = builder.getDefaultValues()
                                .size();
        assertEquals(expectedSize, actualSize);

        builder.addDefaultValue(int.class, 0);
        builder.addDefaultValue(char.class, '\u000f');
        expectedSize = 2;
        actualSize = builder.getDefaultValues()
                            .size();
        assertEquals(expectedSize, actualSize);

        final Map<? super Class, ? super Object> expectedMap = newHashMap();
        expectedMap.put(int.class, 0);
        expectedMap.put(char.class, '\u000f');
        assertEquals(expectedMap, builder.getDefaultValues());
    }

    @Test
    public void return_excluded_method_names_set() {
        final NullToleranceTest.Builder builder = NullToleranceTest.newBuilder();

        int expectedSize = 0;
        int actualSize = builder.getExcludedMethods()
                                .size();
        assertEquals(expectedSize, actualSize);

        final String firstMethodName = "method";
        final String secondMethodName = "secondMethod";
        builder.excludeMethod(firstMethodName);
        builder.excludeMethod(secondMethodName);
        expectedSize = 2;
        actualSize = builder.getExcludedMethods()
                            .size();
        assertEquals(expectedSize, actualSize);

        final Set<String> expectedMethodNames = newHashSet(firstMethodName, secondMethodName);
        final Set<String> actualMethodNames = builder.getExcludedMethods();
        assertEquals(expectedMethodNames, actualMethodNames);
    }

    @Test(expected = NullPointerException.class)
    public void do_not_build_null_tolerance_instance_without_target_class() {
        NullToleranceTest.newBuilder()
                         .build();
    }

    @Test
    public void build_null_tolerance_class_instance() {
        final String excludedMethodName = "excludedMethod";
        final NullToleranceTest nullToleranceTest = NullToleranceTest.newBuilder()
                                                                     .setClass(NullToleranceTestBuilderShould.class)
                                                                     .addDefaultValue(long.class, 0L)
                                                                     .excludeMethod(excludedMethodName)
                                                                     .build();
        final Map<? super Class, ? super Object> expectedMap = newHashMap();
        expectedMap.put(long.class, 0L);
        assertEquals(NullToleranceTestBuilderShould.class, nullToleranceTest.getTargetClass());
        assertEquals(newHashSet(excludedMethodName), nullToleranceTest.getExcludedMethods());
        assertEquals(expectedMap, nullToleranceTest.getDefaultValuesMap());
    }
}
