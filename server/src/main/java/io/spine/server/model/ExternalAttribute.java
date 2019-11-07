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
package io.spine.server.model;

import com.google.errorprone.annotations.Immutable;
import io.spine.core.Subscribe;
import io.spine.server.command.Command;
import io.spine.server.event.React;

import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A meta-attribute of the {@code Method}, telling whether this method handles the objects,
 * produced outside of the current bounded context.
 *
 * @see Subscribe#external()
 * @see React#external()
 * @see Command#external()
 */
@Immutable
enum ExternalAttribute implements Attribute<Boolean> {

    /** An attribute value for the methods, designed to handle external objects only. */
    EXTERNAL(true),

    /** An attribute value for the methods, designed to handle domestic objects only. */
    DOMESTIC(false);

    private final boolean value;

    ExternalAttribute(boolean value) {
        this.value = value;
    }

    @Override
    public String parameter() {
        return "external";
    }

    @Override
    public Boolean value() {
        return value;
    }

    /**
     * Obtains the value of this attribute for a given method.
     *
     * @param method
     *         the method to inspect
     * @return the value of {@code ExternalAttribute} for the given {@code Method}
     */
    public static ExternalAttribute of(Method method) {
        checkNotNull(method);
        boolean isExternal =
                isExternalReactor(method)
                        || isExternalSubscriber(method)
                        || isExternalCommander(method);
        return isExternal ? EXTERNAL : DOMESTIC;
    }

    private static boolean isExternalReactor(Method method) {
        React reactAnnotation = method.getAnnotation(React.class);
        boolean result = (reactAnnotation != null && reactAnnotation.external());
        return result;
    }

    private static boolean isExternalSubscriber(Method method) {
        Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
        boolean result = (subscribeAnnotation != null && subscribeAnnotation.external());
        return result;
    }

    private static boolean isExternalCommander(Method method) {
        Command commandAnnotation = method.getAnnotation(Command.class);
        boolean result = (commandAnnotation != null && commandAnnotation.external());
        return result;
    }
}