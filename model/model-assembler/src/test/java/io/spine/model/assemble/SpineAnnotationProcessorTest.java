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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
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

        // Some actions that may change the processor's internal state.
        processor.init(mock(ProcessingEnvironment.class));
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
        ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
        Messager messager = mock(Messager.class);
        when(environment.getMessager()).thenReturn(messager);

        processor.init(environment);

        String errorMessage = "custom error";
        processor.error(errorMessage);
        verify(messager).printMessage(eq(ERROR), eq(errorMessage));
    }

    @Test
    @DisplayName("print warning message")
    void printWarningMessage() {
        ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
        Messager messager = mock(Messager.class);
        when(environment.getMessager()).thenReturn(messager);

        processor.init(environment);

        String message = "custom warning";
        processor.warn(message);
        verify(messager).printMessage(eq(WARNING), eq(message));
    }
}
