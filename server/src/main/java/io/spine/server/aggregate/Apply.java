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

package io.spine.server.aggregate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of an aggregate as one that modifies the state of the aggregate with data
 * from the passed event.
 *
 * <p>As we apply the event to the aggregate state, we call such a method <i>Event Applier</i>.
 *
 * <p>An event applier method:
 * <ul>
 *     <li>is annotated with {@link Apply};
 *     <li>is {@code private};
 *     <li>is {@code void};
 *     <li>accepts an event derived from {@link io.spine.base.EventMessage EventMessage}
 *         as the only parameter.
 * </ul>
 *
 * <p>To update the state of the aggregate, the {@link Aggregate#builder()} method
 * should be used.
 *
 * <p>If the annotation comes with the attribute {@link #allowImport() allowImport} set to
 * {@code true}, the aggregate can receive incoming events as if they were produced
 * by the aggregate.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Apply {

    /**
     * If {@code true} the aggregate supports importing of events with the messages
     * defined as the first parameter of the annotated method.
     *
     * @see ImportBus
     * @see AggregateRepository#setupImportRouting(io.spine.server.route.EventRouting)
     */
    boolean allowImport() default false;
}

