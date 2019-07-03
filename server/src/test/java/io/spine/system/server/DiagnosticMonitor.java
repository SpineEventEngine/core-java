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

package io.spine.system.server;

import com.google.common.collect.ImmutableList;
import io.spine.core.Subscribe;
import io.spine.server.event.AbstractEventSubscriber;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public final class DiagnosticMonitor extends AbstractEventSubscriber {

    private final List<ConstraintViolated> violations = newArrayList();
    private final List<CannotDispatchCommandTwice> duplicateCommands = newArrayList();
    private final List<CannotDispatchEventTwice> duplicateEvents = newArrayList();
    private final List<HandlerFailedUnexpectedly> handlerFailures = newArrayList();

    @Subscribe
    void on(ConstraintViolated event) {
        violations.add(event);
    }

    @Subscribe
    void on(CannotDispatchCommandTwice event) {
        duplicateCommands.add(event);
    }

    @Subscribe
    void on(CannotDispatchEventTwice event) {
        duplicateEvents.add(event);
    }

    @Subscribe
    void on(HandlerFailedUnexpectedly event) {
        handlerFailures.add(event);
    }

    public ImmutableList<ConstraintViolated> constraintViolatedEvents() {
        return ImmutableList.copyOf(violations);
    }

    public List<CannotDispatchCommandTwice> duplicateCommandEvents() {
        return ImmutableList.copyOf(duplicateCommands);
    }

    public List<CannotDispatchEventTwice> duplicateEventEvents() {
        return ImmutableList.copyOf(duplicateEvents);
    }

    public List<HandlerFailedUnexpectedly> handlerFailureEvents() {
        return ImmutableList.copyOf(handlerFailures);
    }
}
