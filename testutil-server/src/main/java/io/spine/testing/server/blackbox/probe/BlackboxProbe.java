/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.testing.server.blackbox.probe;

import com.google.common.collect.ImmutableSet;
import io.spine.server.BoundedContext;
import io.spine.server.event.EventDispatcher;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of {@link BoundedContext.Probe} which is used by
 * {@link io.spine.testing.server.blackbox.BlackBox BlackBox} to collect
 * commands and events produced by a {@link BoundedContext} being tested.
 */
public final class BlackboxProbe implements BoundedContext.Probe {

    private @Nullable BoundedContext context;

    /**
     * Collects all commands, including posted to the context during its setup or
     * generated by the entities in response to posted or generated events or commands.
     */
    private final CommandCollector commands;

    /**
     * Collects all events, including posted to the context during its setup or
     * generated by the entities in response to posted or generated events or commands.
     */
    private final EventCollector events;

    /**
     * Handles runtime exceptions thrown from signal handlers.
     */
    private final FailedHandlerGuard failedHandlerGuard;

    /**
     * Creates a new instance.
     */
    public BlackboxProbe() {
        this.commands = new CommandCollector();
        this.events = new EventCollector();
        this.failedHandlerGuard = new FailedHandlerGuard();
    }

    @Override
    public void registerWith(BoundedContext context) {
        checkNotRegistered();
        this.context = checkNotNull(context);
    }

    @Override
    public boolean isRegistered() {
        return context != null;
    }

    /**
     * Obtains a {@link FailedHandlerGuard} instance used by this probe.
     */
    public FailedHandlerGuard failedHandlerGuard() {
        return failedHandlerGuard;
    }

    @Override
    public CommandCollector commandListener() {
        return commands;
    }

    @Override
    public EventCollector eventListener() {
        return events;
    }

    @Override
    public Set<EventDispatcher> eventDispatchers() {
        checkRegistered();
        assert(context != null);
        var commandGuard = new UnsupportedCommandGuard(context.name().value());
        return ImmutableSet.of(
                failedHandlerGuard(),
                commandGuard,
                DiagnosticLog.instance()
        );
    }
}