/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.model.assemble;

import io.spine.model.assemble.given.MemoizingMessager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.truth.Truth.assertThat;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SpineAnnotationProcessor should")
abstract class SpineAnnotationProcessorTest {

    private SpineAnnotationProcessor processor;

    protected abstract SpineAnnotationProcessor processor();

    @BeforeEach
    void setUp() {
        processor = processor();
    }

    @Test
    @DisplayName("have constant annotation type")
    void getAnnotationType() {
        Class<?> target = processor.getAnnotationType();
        assertNotNull(target);
        assertTrue(target.isAnnotation());

        processor.setOptions(newHashMap());

        // Some actions that may change the processor's internal state.
        processor.onRoundStarted();
        processor.onRoundFinished();

        assertEquals(target, processor.getAnnotationType());
    }

    @Test
    @DisplayName("generate supported annotation names based on target annotation")
    void getSupportedAnnotationTypes() {
        Class<?> targetType = processor.getAnnotationType();
        var supportedAnnotations = processor.getSupportedAnnotationTypes();

        assertNotNull(supportedAnnotations);
        assertEquals(targetType.getName(), supportedAnnotations.iterator().next());
    }

    @Test
    @DisplayName("print error message")
    void printErrorMessage() {
        var messager = new MemoizingMessager();
        processor.setMessager(messager);

        var errorMessage = "custom error";
        processor.error(errorMessage);

        var received = messager.firstMessage();
        assertThat(received.kind()).isEqualTo(ERROR);
        assertThat(received.message()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("print warning message")
    void printWarningMessage() {
        var messager = new MemoizingMessager();
        processor.setMessager(messager);

        var message = "custom warning";
        processor.warn(message);

        var received = messager.firstMessage();
        assertThat(received.kind()).isEqualTo(WARNING);
        assertThat(received.message()).isEqualTo(message);
    }
}
