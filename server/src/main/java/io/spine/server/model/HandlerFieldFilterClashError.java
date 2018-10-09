/*
 * Copyright 2018, TeamDev. All rights reserved.
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
 * An error indicating that two message handlers have clashing {@link io.spine.core.ByField ByField}
 * signatures.
 *
 * <p>Two handlers clash if they handle the same type of message but filter messages by different
 * fields. For example:
 * <pre>
 *     {@code
 *     \@Subscribe(filter = @ByField(path = "member_count", value = "0"))
 *     public void onProject(ProjectStarted event) {
 *     }
 *
       \@Subscribe(filter = @ByField(path = "by.admin", value = "false"))
 *     public void onStarted(ProjectStarted event) {
 *     }
 *     }
 * </pre>
 *
 * <p>Both methods {@code onProject} and {@code onStarted} subscribe to {@code ProjectStarted} but
 * filter the events by {@code member_count} and {@code by.admin} fields respectively. This is
 * an invalid situation, since both filters could be satisfied simultaneously.
 */
public class HandlerFieldFilterClashError extends ModelError {

    private static final long serialVersionUID = 0L;

    HandlerFieldFilterClashError(Class<?> containingClass,
                                 Method firstMethod,
                                 Method secondMethod) {
        super(format("Handler class %s declares methods %s and %s with clashing field filters.%n" +
                             "It's only allowed to filter by one field per message type.",
                     containingClass.getName(), firstMethod, secondMethod));
    }
}
