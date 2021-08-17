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

package io.spine.server.model;

import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * An error indicating that two message handlers have clashing
 * {@linkplain io.spine.core.Where @Where filtering} signatures.
 *
 * <p>Two handlers clash if they handle the same type of message but filter messages by different
 * fields. For example:
 * <pre>
 *     {@code
 *     \@Subscribe
 *     public void onProject(@Where(field = "member_count", equals = "0") ProjectStarted event) {
 *     }
 *
 *     \@Subscribe
 *     public void onStarted(@Where(field = "by.admin", equals = "false") ProjectStarted event) {
 *     }
 *     }
 * </pre>
 *
 * <p>Both methods {@code onProject} and {@code onStarted} subscribe to {@code ProjectStarted} but
 * filter the events by {@code member_count} and {@code by.admin} fields respectively. This is
 * an invalid situation, since both filters could be satisfied simultaneously.
 */
public final class HandlerFieldFilterClashError extends ModelError {

    private static final long serialVersionUID = 0L;

    HandlerFieldFilterClashError(Class<?> containingClass,
                                 Method firstMethod,
                                 Method secondMethod) {
        super(format("Handler class %s declares methods %s and %s with clashing field filters.%n" +
                             "It's only allowed to filter by one field per message type.",
                     containingClass.getName(), firstMethod, secondMethod));
    }
}
