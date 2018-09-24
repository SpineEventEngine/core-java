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

package io.spine.server.entity.storage;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation, which is used to mark getters for {@linkplain EntityColumn entity columns}.
 *
 * <p>The properties of the annotation affect how the column will be persisted.
 *
 * <p>The annotation will have affect only if it applied to a {@code public} instance getter,
 * i.e. a method without parameters with {@code get-} or {@code is-} prefix.
 *
 * <p>The class declaring an entity column <b>must</b> as well be {@code public}.
 *
 * <p>A {@link #name()} allows to specify a custom column name to be persisted in a {@code Storage}.
 *
 * <p>If there are repeated column names within an {@code Entity},
 * the exception will be raised on a repository
 * {@linkplain io.spine.server.BoundedContext#register(io.spine.server.entity.Repository)
 * registration} for the entity.
 *
 * @author Dmytro Grankin
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Column {

    /**
     * (Optional) The custom {@linkplain EntityColumn#getStoredName() name} of the column
     * to be persisted.
     *
     * <p>Defaults to the {@linkplain EntityColumn#getName() name} extracted from the getter,
     * which is used for querying.
     *
     * <p>This value does not changes a {@linkplain EntityColumn#getName() name} of column,
     * that should be used for {@linkplain EntityQueries querying}.
     */
    String name() default "";
}
