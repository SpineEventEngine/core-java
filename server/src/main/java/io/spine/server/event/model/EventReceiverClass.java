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

package io.spine.server.event.model;

import io.spine.core.EventClass;

import java.util.Set;

/**
 * Describes a class of objects that receive events.
 *
 * <p>A class can declare methods to receive events from the same Bounded Context (“domestic”
 * events), or events originated in another Bounded Context (“external” events).
 */
public interface EventReceiverClass {

    /**
     * Obtains a set of event classes which this class receives.
     *
     * <p>The returned set contains only event classes of the {@code BoundedContext}
     * to which the class belongs.
     *
     * <p>For external events, please see {@link #getExternalEventClasses()}.
     */
    Set<EventClass> getEventClasses();

    /**
     * Obtains a set of external events which this class receives.
     *
     * <p>External events are those that are delivered to the {@code BoundedContext}
     * to which this class belongs from outside.
     *
     * <p>For domestic events, please see {@link #getEventClasses()}.
     */
    Set<EventClass> getExternalEventClasses();
}
