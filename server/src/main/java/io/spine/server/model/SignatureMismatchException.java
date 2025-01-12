/*
 * Copyright 2022, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * Thrown for a {@linkplain Receptor receptor} in case
 * its {@link Receptor#rawMethod() wrapped method} does not match
 * {@linkplain ReceptorSignature method signature}, set for the handler.
 */
public class SignatureMismatchException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    SignatureMismatchException(Method method, Iterable<SignatureMismatch> mismatches) {
        super(formatMsg(method, mismatches));
    }

    private static String formatMsg(Method method, Iterable<SignatureMismatch> mismatches) {
        var list = ImmutableList.copyOf(mismatches);
        var singularOrPlural = list.size() > 1 ? "issues" : "an issue";
        var methodRef = Methods.reference(method);
        var prolog = format(
                "The method `%s` is declared with %s:%n",
                methodRef,
                singularOrPlural
        );
        var issues = buildList(list);
        return prolog + issues;
    }

    private static @NonNull StringBuilder buildList(ImmutableList<SignatureMismatch> list) {
        var stringBuilder = new StringBuilder(100);
        var lastMismatch = list.get(list.size() - 1);
        var newLine = System.lineSeparator();
        list.forEach(mismatch -> {
            var kind = mismatch.isError() ? "Error: " : "Warning: ";
            stringBuilder.append(" - ")
                         .append(kind)
                         .append(mismatch);
            var isLast = mismatch.equals(lastMismatch);
            if (!isLast) {
                stringBuilder.append(newLine);
            }
        });
        return stringBuilder;
    }
}
