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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation which is used to mark getters for {@linkplain EntityColumn entity columns}.
 *
 * <p>The properties of the annotation affect how the column is seen by the storage and clients.
 *
 * <p>The annotation will have effect only if it's applied to a {@code public} instance getter,
 * meaning a method without parameters and with {@code get-} prefix. The {@code is-} prefix is
 * supported for primitive {@code boolean} or boxed {@code Boolean} columns.
 *
 * <p>A {@link #name()} allows to specify a custom column name to be persisted in a {@code Storage}.
 *
 * <p>If there are repeated column names within an {@code Entity},
 * the exception will be raised when a repository serving the entity is added to
 * its {@code BoundedContext}.
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Column {

    /**
     * The custom {@linkplain EntityColumn#name() name} of the column.
     *
     * <p>Defaults to the name extracted from the getter which is used for querying.
     */
    String name() default "";
}
