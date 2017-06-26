/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a subscriber for the command output.
 *
 * <p>Use it to subscribe to either events or business failures
 *
 * <h2>Subscribing to Events</h2>
 *
 * <p>An event subscriber method:
 * <ul>
 *     <li>is annotated with {@link Subscribe};
 *     <li>is {@code public};
 *     <li>returns {@code void};
 *     <li>accepts an event derived from {@link com.google.protobuf.Message Message}
 *          as the first parameter;
 *     <li>(optional) accepts an {@link io.spine.core.EventContext EventContext}
 *          as the second parameter.
 * </ul>
 *
 * <h2>Subscribing to Failures</h2>
 *
 * <p>A failure subscriber method:
 * <ul>
 *     <li>is annotated with {@link Subscribe};
 *     <li>is {@code public};
 *     <li>returns {@code void};
 *     <li>accepts a failure derived from {@link com.google.protobuf.Message Message}
 *          as the first parameter;
 *     <li>(optional) accepts a command derived from {@link com.google.protobuf.Message Message}
 *          as the second parameter;
 *     <li>(optional) accepts an {@link io.spine.core.CommandContext CommandContext}
 *          as the second or the third parameter.
 * </ul>
 *
 * <p>Therefore, if the subscriber method specifies both the command message and
 * the command context, it must have the parameters exactly is that order, i.e.
 * {@code (FailureMessage, CommandMessage, CommandContext)}. Otherwise, an exception may be thrown
 * at runtime.
 *
 * <p>The type of the command argument, if specified, acts as a filter, i.e.
 * the subscriber receives the failure if:
 * <ul>
 *     <li>the failure type matches the first argument type;
 *     <li>the command, which processing caused the failure, has the same type as
 *          the command message argument if it is present;
 *     <li>if the command message argument is absent, any failure of a matching type is received by
 *          the subscriber.
 * </ul>
 *
 * <p>If the annotation is applied to a method which doesn't satisfy any of these requirements,
 * this method is not considered as a subscriber and is not registered for the command output
 * delivery.
 *
 * @author Alexander Yevsyukov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
}
