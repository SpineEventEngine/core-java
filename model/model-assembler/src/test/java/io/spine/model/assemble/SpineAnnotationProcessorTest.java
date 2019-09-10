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

package io.spine.model.assemble;

import io.spine.model.assemble.given.MemoizingMessager;
import io.spine.model.assemble.given.MemoizingMessager.MemoizedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

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
        Set<String> supportedAnnotations = processor.getSupportedAnnotationTypes();

        assertNotNull(supportedAnnotations);
        assertEquals(targetType.getName(), supportedAnnotations.iterator().next());
    }

    @Test
    @DisplayName("print error message")
    void printErrorMessage() {
        MemoizingMessager messager = new MemoizingMessager();
        processor.setMessager(messager);

        String errorMessage = "custom error";
        processor.error(errorMessage);

        MemoizedMessage received = messager.firstMessage();
        assertThat(received.kind()).isEqualTo(ERROR);
        assertThat(received.messageAsString()).isEqualTo(errorMessage);
    }

    @Test
    @DisplayName("print warning message")
    void printWarningMessage() {
        MemoizingMessager messager = new MemoizingMessager();
        processor.setMessager(messager);

        String message = "custom warning";
        processor.warn(message);

        MemoizedMessage received = messager.firstMessage();
        assertThat(received.kind()).isEqualTo(WARNING);
        assertThat(received.messageAsString()).isEqualTo(message);
    }
}
