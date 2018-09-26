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

package io.spine.server.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as command handler.
 *
 * <p>A command handler method <strong>must</strong>:
 * <ul>
 *     <li>be annotated with {@link Assign @Assign};
 *     <li>have package-private visibility;
 *     <li>return an event message derived from {@link io.spine.base.EventMessage EventMessage}
 *         if there is only one event generated;
 *         <strong>or</strong> an {@code Iterable} of event messages for two or
 *         more events;
 *     <li>accept a command message derived from {@link io.spine.base.CommandMessage CommandMessage}
 *         as the first parameter.
 * </ul>
 *
 * <p>A command handler method <strong>may</strong> accept a {@link io.spine.core.CommandContext
 * CommandContext} as the second parameter, if handling of the command requires its context.

 * <p>If the annotation is applied to a method which doesn't satisfy any of these requirements,
 * this method is not considered a command handler and is <strong>not</strong> registered for
 * command dispatching.
 *
 * <p>A command handler method <strong>should</strong> have package-private access. It will allow
 * calling this method from tests. The method should not be {@code public} because it is not
 * supposed to be called directly.
 *
 * <h1>One Handler per Command</h1>
 *
 * <p>An application must have one and only one handler per command message class.
 * This includes {@linkplain io.spine.server.command.Command the case} of transforming an incoming
 * command into one or more commands that will to be handled instead of the received one.
 *
 * <p>Declaring two methods that handle the same command class will result in run-time error.
 *
 * @author Alexander Yevsyukov
 * @see io.spine.server.tuple Returning Two or More Event Messages
 * @see io.spine.server.command.Command Converting Commands
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Assign {
}
