/*
 * Copyright 2020, TeamDev. All rights reserved.
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
 * Filters events delivered to a handler method.
 *
 * <p>To apply filtering to an event handler method, annotate the first parameter of the method.
 *
 * <p>For example, the following method would be invoked only if the owner of the created
 * project is {@code mary@ackme.net}:
 * <pre>
 *{@literal @Subscribe }
 * void on(@Where(field = "owner.email", equals = "mary@ackme.net") ProjectCreated e) { ... }
 * </pre>
 *
 * <p>Annotations for handler methods which support {@code @Where} should be marked with
 * {@link AcceptsFilters}.
 *
 * <h1>Filtering Events by a Field Value</h1>
 * <p>If a field filter is defined, only the events matching this filter are passed to the handler
 * method.
 *
 * <p>A single class may define a number of handler methods with different field filters. Though,
 * all the field filters must target the same field. For example, this event handling is valid:
 * <pre>
 *    {@literal @Subscribe}
 *     void{@literal onExpired(@Where(field = "subscription.status", equals = "EXPIRED")
 *                      UserLoggedIn event)} {
 *         // Handle expired subscription.
 *     }
 *
 *    {@literal @Subscribe}
 *     void{@literal onInactive(@Where(field = "subscription.status", equals = "INACTIVE")
 *                       UserLoggedIn event)} {
 *         // Handle inactive subscription.
 *     }
 *
 *    {@literal @Subscribe}
 *     void on(UserLoggedIn event) {
 *         // Handle other cases.
 *     }
 * </pre>
 * <p>And this one is not:
 * <pre>
 *    {@literal @Subscribe}
 *     void{@literal onExpired(@Where(field = "subscription.status", equals = "EXPIRED")
 *                      UserLoggedIn event)} {
 *     }
 *
 *    {@literal @Subscribe}
 *     void{@literal onUnknownBilling(@Where(field = "payment_method.status", equals = "UNSET")
 *                             UserLoggedIn event)} {
 *         // Error, different field paths used in the same class for the same event type.
 *     }
 * </pre>
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
