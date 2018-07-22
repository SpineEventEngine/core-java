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

package io.spine.server.event.model;

import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.rejection.model.RejectionReactorMethod;

import java.util.Set;

/**
 * Provides message handling information on a class that reacts on messages.
 *
 * @author Alexander Yevsyukov
 */
public interface ReactorClass {

    /**
     * Obtains a set of event classes to which this class reacts.
     *
     * <p>The returned set contains only event classes of the {@code BoundedContext}
     * to which the class belongs.
     *
     * <p>For reactions on external events, please see {@link #getExternalEventReactions()}.
     */
    Set<EventClass> getEventReactions();

    /**
     * Obtains a set of external events to which this class reacts.
     *
     * <p>External events are those that are delivered to the {@code BoundedContext}
     * to which this class belongs from outside.
     *
     * <p>For reactions on domestic events, please see {@link #getEventReactions()}.
     */
    Set<EventClass> getExternalEventReactions();

    /**
     * Obtains a set of rejection classes to which this class reacts.
     *
     * <p>The returned set contains only rejection classes of the {@code BoundedContext}
     * to which the class belongs.
     *
     * <p>For reactions on external rejections, please see {@link #getExternalRejectionReactions()}.
     */
    Set<RejectionClass> getRejectionReactions();

    /**
     * Obtains a set of external rejections to which this class reacts.
     *
     * <p>External rejections are those that are delivered to the {@code BoundedContext}
     * to which this class belongs from outside.
     *
     * <p>For reactions on domestic rejections, please see {@link #getRejectionReactions()}.
     */
    Set<RejectionClass> getExternalRejectionReactions();

    /**
     * Obtains the method that reacts on the passed event class.
     */
    EventReactorMethod getReactor(EventClass eventClass);

    /**
     * Obtains the method that reacts on the passed projection class.
     */
    RejectionReactorMethod getReactor(RejectionClass rejCls, CommandClass cmdCls);
}
