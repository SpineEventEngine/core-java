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

package io.spine.server.command;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as command handler.
 *
 * <p>A command handler method <em>must</em>:
 * <ul>
 *     <li>be annotated with {@link Assign @Assign};
 *     <li>return an event message derived from {@link io.spine.base.EventMessage EventMessage}
 *         if there is only one event generated;
 *         <strong>or</strong> an {@code Iterable} of event messages for two or
 *         more events;
 *     <li>accept a command message derived from {@link io.spine.base.CommandMessage CommandMessage}
 *         as the first parameter.
 * </ul>
 *
 * <p>Like other message-handling methods, command handlers are designed to be called by
 * the framework only. Therefore, it is recommended to declare a them as package-private.
 * It discourages a developer from calling these methods directly from anywhere.
 *
 * <p>Package-private access level still declares that a command handler method is a part
 * of the Bounded Context-level API. See the {@link io.spine.core.BoundedContext
 * BoundedContext} description on how the packages and Bounded Contexts relate.
 *
 * <h1>Accepted Parameters</h1>
 *
 * <p>The first parameter of the command handler always declares a type of the handled command.
 *
 * <p>A command handler method <strong>may</strong> accept a {@link io.spine.core.CommandContext
 * CommandContext} as the second parameter, if handling of the command requires its context.
 *
 * <pre>
 *
 * {@literal @}Assign
 *  TaskCreated handler(CreateTask command) { ... }
 *
 * {@literal @}Assign
 *  TaskCompleted handler(CompleteTask command, CommandContext context) { ... }
 * </pre>
 *
 * <p>In case a command may be rejected, a corresponding {@code Throwable} should be declared:
 *
 * <pre>
 *
 * {@literal @}Assign
 *  TaskStarted handler(StartTask command) throws TaskAlreadyInProgress { ... }
 * </pre>
 *
 * <p>If the annotation is applied to a method which doesn't satisfy any of these requirements,
 * this method is not considered a command handler and is <strong>not</strong> registered for
 * command dispatching.
 *
 * <h1>Returning Values</h1>
 *
 * <p>As a command is an imperative, it must lead to some outcome. Typically, a command results
 * in an emission of one or more events. Each of them must derive
 * from {@link io.spine.base.EventMessage EventMessage} in order to make the code less error-prone.
 *
 * <p>A command handler method must return either
 * <ul>
 *
 *  <li>an event message:
 *  <pre>
 *
 * {@literal @}Assign
 *  TaskReassigned on(ReassignTask command) { ... }
 *  </pre>
 *
 *
 *  <li>an {@code Iterable} of event messages:
 *  <pre>
 *
 * {@literal @}Assign
 * {@literal Iterable<TaskCompleted>} handler(CompleteProject event) { ... }
 *  </pre>
 *
 *
 *  <li>a {@link io.spine.server.tuple.Tuple tuple} of event messages; being similar
 *  to {@code Iterable}, tuples allow to declare the exact types of returning values, including
 *  {@code Optional} values:
 *  <pre>
 *
 * {@literal @}Assign
 * {@literal Pair<ProjectCreated, ProjectAssigned>} handlerCreateProject event) { ... }
 *
 * {@literal @}Assign
 * {@literal Pair<TaskCreated, Optional<TaskAssigned>>} handler(CreateTask event) { ... }
 *  </pre>
 *
 *
 *  <li>{@linkplain io.spine.server.tuple.Either one of} particular events:
 *  <pre>
 *
 * {@literal @}Assign
 * {@literal EitherOf2<TaskRemovedFromProject, TaskDeleted>} handler(RemoveTask command) { ... }
 *  </pre>
 * </ul>
 *
 * <h1>One Handler per Command</h1>
 *
 * <p>An application must have one and only one handler per command message class.
 * This includes {@linkplain io.spine.server.command.Command the case} of transforming an incoming
 * command into one or more commands that will to be handled instead of the received one.
 *
 * <p>Declaring two methods that handle the same command class will result in run-time error.
 *
 * @see io.spine.server.tuple.Tuple Returning Two or More Event Messages
 * @see io.spine.server.tuple.Either Returning One of Event Messages
 * @see io.spine.server.command.Command Transforming Commands
 */
@Retention(RUNTIME)
@Target(METHOD)
public @interface Assign {
}
