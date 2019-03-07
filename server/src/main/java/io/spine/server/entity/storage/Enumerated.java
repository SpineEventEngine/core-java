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

import static io.spine.server.entity.storage.EnumType.ORDINAL;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark an {@linkplain io.spine.server.entity.storage.Column entity column} as an enumerated value.
 *
 * <p>This annotation only has effect when used in conjunction with the {@link
 * io.spine.server.entity.storage.Column} annotation.
 *
 * <p>Entity columns storing enumerated types (i.e. {@linkplain Enum Java Enum}) are persisted
 * differently to all other types. This annotation can be used to control the way the enumerated
 * value is persisted in the data storage.
 *
 * <p>If this annotation is omitted for the column which stores {@link Enum} type, it is still
 * considered enumerated, and is assumed to be of the {@linkplain EnumType#ORDINAL ordinal enum
 * type}.
 *
 * @see EnumType
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Enumerated {

    /**
     * The {@link EnumType} of the enumerated value which defines how the value will be persisted
     * in the data storage.
     *
     * <p>For the detailed information about the persistence methods, see {@link EnumType}.
     */
    EnumType value() default ORDINAL;
}
