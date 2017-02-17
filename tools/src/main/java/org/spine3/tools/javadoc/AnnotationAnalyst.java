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

package org.spine3.tools.javadoc;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ProgramElementDoc;

import java.lang.annotation.Annotation;

/**
 * {@code AnnotationAnalyst} provides methods to check appurtenance to specified annotation.
 *
 * @param <C> the type of an annotation to analyze
 *
 * @author Dmytro Grankin
 */
public class AnnotationAnalyst<C extends Class<? extends Annotation>> {

    private final C annotationClass;

    AnnotationAnalyst(C annotationClass) {
        this.annotationClass = annotationClass;
    }

    boolean hasAnnotation(ProgramElementDoc doc) {
        return isAnnotationPresent(doc.annotations());
    }

    boolean isAnnotationPresent(AnnotationDesc[] annotations) {
        for (AnnotationDesc annotation : annotations) {
            if (isQualifiedAnnotation(annotation)) {
                return true;
            }
        }

        return false;
    }

    private boolean isQualifiedAnnotation(AnnotationDesc annotation) {
        return annotation.annotationType().qualifiedTypeName().equals(annotationClass.getName());
    }
}
