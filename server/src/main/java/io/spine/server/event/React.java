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

package io.spine.server.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of an entity as one that <em>may</em> modify the state of the entity in
 * response to some domain event.
 *
 * <p>A reacting method <strong>must:</strong>
 * <ul>
 *     <li>be annotated with {@link React @React};
 *     <li>have package-private visibility;
 *     <li>accept an event message (derived from {@link io.spine.base.EventMessage EventMessage})
 *         as the first parameter.
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
 * <li>an event message:
 * <pre>
 * {@literal @}React
 *  TaskReassigned on(UserDeactivated event) { ... }
 * </pre>
 *
 *  <li>an {@code Optional} event message:
 *  <pre>
 * {@literal @}React
 * {@literal Optional<PersonAllowedToBuyAlcohol>} on(PersonAgeChanged event) { ... }
 *  </pre>
 *
 *  <li>{@linkplain io.spine.server.tuple.Either one of} particular events;
 *  it also allows to use a special {@link io.spine.server.model.Nothing Nothing} event stating
 *  that the entity may choose not to react at all:
 *  <pre>
 * {@literal @}React
 * {@literal EitherOf3<ProjectCompleted, ProjectEstimateUpdated, Nothing>} on(TaskCompleted event) { ... }
 *  </pre>
 *
 *
 *  <li>an {@code Iterable} of event messages:
 *  <pre>
 * {@literal @}React
 * {@literal Iterable<StoryMovedToBacklog>} on(SprintCompleted event) { ... }
 *  </pre>
 *
 *  <li>a {@link io.spine.server.tuple.Tuple tuple} of event messages; being similar
 *  to {@code Iterable}, tuples allow to declare the exact types of returning values:
 *  <pre>
 * {@literal @}React
 * {@literal Pair<ProjectOwnerAssigned, ProjectDueDateChanged>} on(ProjectCreated event) { ... }
 *  </pre>
 *
 * </ul>
 *
 * <p>If the annotation is applied to a method which does not satisfy either of these requirements,
 * this method will not be registering for receiving events.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface React {

    /**
     * When {@code true}, the annotated method of the entity reacts on the event generated from
     * outside of the Bounded Context to which this entity belongs.
     */
    boolean external() default false;
}
