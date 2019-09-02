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

package io.spine.core;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Filters events delivered to the {@linkplain Subscribe subscriber method} the first
 * parameter of which has this annotation.
 *
 * <p>For example, the following method would be invoked only if the owner of the created
 * project is {@code mary@ackme.net}:
 * <pre>{@code
 * \@Subscribe
 * void on(@Where(field = "owner.email" equals = "mary@ackme.net") ProjectCreated e) { ... }
 * }</pre>
 *
 * <p><em>NOTE:</em> This syntax applies only to events. Filtering state subscriptions
 * is not supported.
 */
@Retention(RUNTIME)
@Target(PARAMETER)
@Documented
public @interface Where {

    /**
     * The {@linkplain io.spine.base.FieldPath path to the field} of the event message to filter by.
     */
    String field();

    /**
     * The expected value of the field.
     *
     * <p>The value converted with help of {@link io.spine.string.Stringifier Stringifier}s into
     * the type of the actual value of the message field.
     */
    String equals();
}
