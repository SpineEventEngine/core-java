/*
 * Copyright 2023, TeamDev. All rights reserved.
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

package io.spine.core;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as a subscriber for the command output.
 *
 * <p>Use it to subscribe to either events, business rejections, or entity state updates.
 *
 * <h1>Subscribing to Events</h1>
 *
 * <p>An event subscriber method:
 * <ul>
 *     <li>is annotated with {@link Subscribe};
 *     <li>returns {@code void};
 *     <li>accepts an event derived from {@link io.spine.base.EventMessage EventMessage}
 *          as the first parameter;
 *     <li>(optional) accepts an {@link io.spine.core.EventContext EventContext}
 *          as the second parameter.
 * </ul>
 *
 * <h1>Subscribing to Rejections</h1>
 *
 * <p>A rejection subscriber method:
 * <ul>
 *     <li>is annotated with {@link Subscribe};
 *     <li>returns {@code void};
 *     <li>accepts a rejection message derived from {@link io.spine.base.RejectionMessage
 *         RejectionMessage} as the first parameter;
 *     <li>(optional) accepts a command derived from {@link io.spine.base.CommandMessage
 *         CommandMessage} as the second parameter;
 *     <li>(optional) accepts an {@link io.spine.core.CommandContext CommandContext}
 *          as the second or the third parameter.
 * </ul>
 *
 * <p>Therefore, if the subscriber method specifies both the command message and
 * the command context, it must have the parameters exactly is that order, i.e.
 * {@code (RejectionMessage, CommandMessage, CommandContext)}. Otherwise, an exception may be thrown
 * at runtime.
 *
 * <p>The type of the command argument, if specified, acts as a filter, i.e. the subscriber receives
 * the rejection if:
 * <ul>
 *     <li>the rejection type matches the first argument type;
 *     <li>the command, which processing caused the rejection, has the same type as
 *         the command message argument if it is present;
 *     <li>if the command message argument is absent, any rejection of a matching type is received
 *         by the subscriber.
 * </ul>
 *
 * <h1>Subscribing to Entity State Updates</h1>
 *
 * <p>An entity state subscriber method:
 * <ul>
 *     <li>is annotated with {@link Subscribe};
 *     <li>returns {@code void};
 *     <li>accepts an entity state message marked with the {@code (entity)} option as the only
 *         parameter.
 * </ul>
 *
 * <p>If the annotation is applied to a method which doesn't satisfy either of these requirements,
 * this method is not considered a subscriber and is not registered for the command output delivery.
 *
 * <p>Event subscriber methods are designed to be called by the framework only.
 * Therefore, it is recommended to declare a them as package-private.
 * It discourages a developer from calling these methods directly from anywhere.
 *
 * <p>Package-private access level still declares that an event reactor method is a part
 * of the Bounded Context-level API. See the {@link io.spine.core.BoundedContext
 * BoundedContext} description on how the packages and Bounded Contexts relate.
 *
 * <p>When subscribing to events, {@linkplain Where field filtering} is supported.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Documented
@AcceptsFilters
@AcceptsExternal
public @interface Subscribe {

    /**
     * When {@code true}, the annotated method receives an event generated from outside of the
     * Bounded Context to which the annotated method's class belongs.
     *
     * @deprecated please use {@link External @External} annotation for the first method parameter.
     */
    @Deprecated
    boolean external() default false;

    /**
     * Filter to apply to all the event messages.
     *
     * <p>If an event does not match this filter, it is not passed to the subscriber method.
     *
     * <p>If the {@link ByField#path() @ByField.path} if empty, the filter is not
     * applied.
     *
     * @deprecated please use {@link Where @Where} annotation for the first method parameter.
     */
    @Deprecated
    ByField filter() default @ByField(path = "", value = "");
}
