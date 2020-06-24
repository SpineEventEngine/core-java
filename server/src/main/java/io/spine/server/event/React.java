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

package io.spine.server.event;

import io.spine.core.AcceptsExternal;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method of an entity as one that <em>may</em> modify the state of the entity in
 * response to some domain event.
 *
 * <p>A reacting method must be annotated {@link React @React}.
 *
 * <p>Like other message-handling methods, event reactors are designed to be called by
 * the framework only. Therefore, it is recommended to declare a them as package-private.
 * It discourages a developer from calling these methods directly from anywhere.
 *
 * <p>Package-private access level still declares that an event reactor method is a part
 * of the Bounded Context-level API. See the {@link io.spine.core.BoundedContext
 * BoundedContext} description on how the packages and Bounded Contexts relate.
 *
 * <h1>Accepted Parameters</h1>
 *
 * <p>Each reacting method <strong>must</strong> accept an event message derived
 * from {@link io.spine.base.EventMessage EventMessage} as the first parameter.
 * Optionally, one may pass some additional parameters and put the incoming message into
 * some perspective.
 *
 * <p>Here are the available sets of parameters:
 *
 * <ul>
 *
 * <li>single event message:
 * <pre>
 *
 * {@literal @}React
 *  EngineStopped on(CarStopped event) { ... }
 * </pre>
 *
 * <li>an event message along with its {@link io.spine.core.EventContext context}; the context
 * brings some system properties related to event, such as the actor ID and the timestamp of
 * the event emission:
 * <pre>
 *
 * {@literal @}React
 *  ProjectOwnerAssigned on(ProjectCreated event, EventContext context) { ... }
 * </pre>
 *
 * <li>if an event is a rejection event, one may additionally specify the command message, which
 * led to this event; this will act like a filter:
 * <pre>
 *
 * // Only rejections of `CannotAllocateCargo` type caused by the rejected `DeliverOrder` command will be dispatched.
 * {@literal @}React
 *  OrderDeliveryFailed on(CannotAllocateCargo event, DeliverOrder command) { ... }
 * </pre>
 *
 * <p>It is also possible to add the context of the origin command to access even more properties:
 * <pre>
 *
 * {@literal @}React
 *  ProjectRenameFailed on(ProjectAlreadyCompleted event, RenameProject command, CommandContext ctx) { ... }
 * </pre>
 *
 * </ul>
 *
 * <h1>Returning Values</h1>
 *
 * <p>The essence of a reacting method is an emission of one or several events in a reaction to
 * the dispatched event. The emitted events must derive from {@link io.spine.base.EventMessage
 * EventMessage} in order to make the code less error-prone.
 *
 * <p>As long as an entity may have a complex logic of determining which event to emit in reaction,
 * the {@code React}-marked methods allow a variety of options for the returning values.
 *
 * <p>A reacting method must return either
 * <ul>
 *
 *  <li>an event message:
 *  <pre>
 *
 * {@literal @}React
 *  TaskReassigned on(UserDeactivated event) { ... }
 *  </pre>
 *
 *
 *  <li>an {@code Optional} event message:
 *  <pre>
 *
 * {@literal @}React
 * {@literal Optional<PersonAllowedToBuyAlcohol>} on(PersonAgeChanged event) { ... }
 *  </pre>
 *
 *
 *  <li>{@linkplain io.spine.server.tuple.Either one of} particular events;
 *  it also allows to use a special {@link io.spine.server.model.Nothing Nothing} event stating
 *  that the entity may choose not to react at all:
 *  <pre>
 *
 * {@literal @}React
 * {@literal EitherOf3<ProjectCompleted, ProjectEstimateUpdated, Nothing>} on(TaskCompleted event) { ... }
 *  </pre>
 *
 *
 *  <li>an {@code Iterable} of event messages:
 *  <pre>
 *
 * {@literal @}React
 * {@literal Iterable<StoryMovedToBacklog>} on(SprintCompleted event) { ... }
 *  </pre>
 *
 *
 *  <li>a {@link io.spine.server.tuple.Tuple tuple} of event messages; being similar
 *  to {@code Iterable}, tuples allow to declare the exact types of returning values, including
 *  {@code Optional} values:
 *  <pre>
 *
 * {@literal @}React
 * {@literal Pair<ProjectOwnerAssigned, ProjectDueDateChanged>} on(ProjectCreated event) { ... }
 *
 * {@literal @}React
 * {@literal Pair<TaskAssigned, Optional<TaskStarted>>} on(TaskAdded event) { ... }
 *  </pre>
 *
 * </ul>
 *
 * <p>If the annotation is applied to a method which does not satisfy either of these requirements,
 * this method will not be registering for receiving events.
 */
@Retention(RUNTIME)
@Target(METHOD)
@Documented
@AcceptsExternal
public @interface React {

    /**
     * When {@code true}, the annotated method of the entity reacts on the event generated from
     * outside of the Bounded Context to which this entity belongs.
     *
     * @deprecated please use {@link io.spine.core.External @External} annotation for the first
     *         method parameter.
     */
    @Deprecated
    boolean external() default false;
}
