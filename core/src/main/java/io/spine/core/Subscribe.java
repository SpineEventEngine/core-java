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

package io.spine.core;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as a subscriber for the command output.
 *
 * <p>Use it to subscribe to either events or business rejections
 *
 * <h1>Subscribing to Events</h1>
 *
 * <p>An event subscriber method:
 * <ul>
 *     <li>is annotated with {@link Subscribe};
 *     <li>is {@code public};
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
 *     <li>is {@code public};
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
 * // TODO:2018-10-08:dmytro.dashenkov: Simplify next paragraph.
 * <p>If a {@linkplain ByField field filter} is defined, only the events matching this filter are
 * passed to the subscriber. A single event subscriber class may define a number of subscribing
 * methods which would differ from each other only by the filter. In this case, it is allowed to
 * define several methods with the same filtered field and without a filter at all, but it's illegal
 * to define multiple methods with different filtered fields. For example, the next event handling
 * is valid:
 * <pre>
 *     {@code
 *     \@Subscribe(filter = @ByField(path = "subscription.status", value = "EXPIRED"))
 *     public void onExpired(UserLoggedIn event) {
 *         // Handle expired subscription.
 *     }
 *
 *     \@Subscribe(filter = @ByField(path = "subscription.status", value = "INACTIVE"))
 *     public void onInactive(UserLoggedIn event) {
 *         // Handle inactive subscription.
 *     }
 *
 *     \@Subscribe
 *     public void on(UserLoggedIn event) {
 *         // Handle other cases.
 *     }
 *     }
 * </pre>
 * <p>And this is not valid:
 * <pre>
 *     {@code
 *     \@Subscribe(filter = @ByField(path = "subscription.status", value = "EXPIRED"))
 *     public void onExpired(UserLoggedIn event) {
 *     }
 *
 *     \@Subscribe(filter = @ByField(path = "payment_method.status", value = "UNSET"))
 *     public void onUnknownBilling(UserLoggedIn event) {
 *         // Error, different field paths used in the same class for the same event type.
 *     }
 *     }
 * </pre>
 *
 * <p>If the annotation is applied to a method which doesn't satisfy any of these requirements,
 * this method is not considered as a subscriber and is not registered for the command output
 * delivery.
 *
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
@Retention(RUNTIME)
@Target(METHOD)
@Documented
public @interface Subscribe {

    /**
     * When {@code true}, the annotated method of receives an event generated from outside of the
     * Bounded Context to which the annotated method's class belongs.
     */
    boolean external() default false;

    /**
     * Filter to apply to all the event messages.
     *
     * <p>If an event does not match this filter, it is not passed to the subscriber method.
     *
     * <p>If the {@link ByField#path() @ByField.path} if empty, the filter is not
     * applied.
     */
    ByField filter() default @ByField(path = "", value = "");
}
