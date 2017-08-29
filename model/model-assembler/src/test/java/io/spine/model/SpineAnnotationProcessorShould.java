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

package io.spine.model;

import org.junit.Before;
import org.junit.Test;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.WARNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Dmytro Dashenkov
 */
public abstract class SpineAnnotationProcessorShould {

    private SpineAnnotationProcessor processor;

    protected abstract SpineAnnotationProcessor processor();

    @Before
    public void setUp() {
        processor = processor();
    }

    @Test
    public void have_constant_annotation_type() {
        final Class<?> target = processor.getAnnotationType();
        assertNotNull(target);
        assertTrue(target.isAnnotation());

        // Some actions that may change the processor's internal state.
        processor.init(mock(ProcessingEnvironment.class));
        processor.onRoundStarted();
        processor.onRoundFinished();

        assertEquals(target, processor.getAnnotationType());
    }

    @Test
    public void generate_supported_annotation_names_based_of_target_annotation() {
        final Class<?> targetType = processor.getAnnotationType();
        final Set<String> supportedAnnotations = processor.getSupportedAnnotationTypes();

        assertNotNull(supportedAnnotations);
        assertEquals(targetType.getName(), supportedAnnotations.iterator().next());
    }

    @Test
    public void print_error_message() {
        final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
        final Messager messager = mock(Messager.class);
        when(environment.getMessager()).thenReturn(messager);

        processor.init(environment);

        final String errorMessage = "custom error";
        processor.error(errorMessage);
        verify(messager).printMessage(eq(ERROR), eq(errorMessage));
    }

    @Test
    public void print_warning_message() {
        final ProcessingEnvironment environment = mock(ProcessingEnvironment.class);
        final Messager messager = mock(Messager.class);
        when(environment.getMessager()).thenReturn(messager);

        processor.init(environment);

        final String message = "custom warning";
        processor.warn(message);
        verify(messager).printMessage(eq(WARNING), eq(message));
    }
}
