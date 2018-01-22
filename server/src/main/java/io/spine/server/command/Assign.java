/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
 *     <li>return an event derived from {@link com.google.protobuf.Message Message}
 *     <strong>or</strong> a {@link java.util.List List} of messages;
 *     <li>accept a command message derived from {@link com.google.protobuf.Message Message}
 *         as the first parameter.
 * </ul>
 *
 * <p>If the annotation is applied to a method which doesn't satisfy any of these requirements,
 * this method is not considered a command handler and is <strong>not</strong> registered for
 * command dispatching.
 *
 * <p>A command handler method <strong>should</strong> have package-private access. It will allow
 * calling this method from tests. The method should not be {@code public} because it is not
 * supposed to be called directly.
 *
 * <p>A command handler method <strong>may</strong> accept a {@link io.spine.core.CommandContext
 * CommandContext} as the second parameter if handling of the command requires its context.
 *
 * <p><strong>IMPORTANT:</strong> an application must have one and only one handler per command
 * message class. Declaring two methods that handle the same command class will result in run-time
 * error.
 *
 * @author Alexander Yevsyukov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Assign {
}
