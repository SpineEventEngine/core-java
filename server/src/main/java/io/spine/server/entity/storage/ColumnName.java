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

package io.spine.server.entity.storage;

import io.spine.code.java.MethodName;
import io.spine.server.storage.StorageField;
import io.spine.value.StringTypeValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.util.Preconditions2.checkNotEmptyOrBlank;

public final class ColumnName extends StringTypeValue {

    private static final long serialVersionUID = 0L;

    private ColumnName(String value) {
        super(value);
    }

    public static ColumnName of(String value) {
        checkNotEmptyOrBlank(value);
        return new ColumnName(value);
    }

    public static ColumnName of(StorageField storageField) {
        checkNotNull(storageField);
        return of(storageField.name());
    }

    public static ColumnName from(Method method) {
        checkNotNull(method);
        MethodName methodName = MethodName.of(method);
        checkIsGetter(methodName);
        String name = underscoredNameWithoutPrefix(methodName);
        return of(name);
    }

    private static String underscoredNameWithoutPrefix(MethodName methodName) {
        List<String> words = new ArrayList<>(methodName.words());
        words.remove(0);
        String result = words.stream()
                             .map(String::toLowerCase)
                             .collect(Collectors.joining("_"));
        return result;
    }

    private static void checkIsGetter(MethodName methodName) {
        checkState(methodName.isGetter(),
                   "A column name can only be extracted from getter method, " +
                           "the name of the method `%s` is unsuitable for column extraction.",
                   methodName.fullyQualifiedName());
    }
}
