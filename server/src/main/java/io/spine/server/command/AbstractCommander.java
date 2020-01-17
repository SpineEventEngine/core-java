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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.concurrent.LazyInit;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.BoundedContext;
import io.spine.server.command.model.CommandReactionMethod;
import io.spine.server.command.model.CommandSubstituteMethod;
import io.spine.server.command.model.CommanderClass;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.dispatch.DispatchOutcomeHandler;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.server.command.model.CommanderClass.asCommanderClass;

/**
 * The abstract base for classes that generate commands in response to incoming messages.
 */
public abstract class AbstractCommander
        extends AbstractCommandDispatcher
        implements Commander, EventDispatcherDelegate {

    private final CommanderClass<?> thisClass = asCommanderClass(getClass());
    @LazyInit
    private @MonotonicNonNull CommandBus commandBus;

    @Override
    public void registerWith(BoundedContext context) {
        super.registerWith(context);
        commandBus = context.commandBus();
    }

    private CommandBus commandBus() {
        return checkNotNull(commandBus, "`%s` does not have `CommandBus` assigned.", this);
    }

    @Override
    public ImmutableSet<CommandClass> messageClasses() {
        return thisClass.commands();
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(CommandEnvelope command) {
        CommandSubstituteMethod method = thisClass.handlerOf(command.messageClass());
        DispatchOutcomeHandler
                .from(method.invoke(this, command))
                .onCommands(this::postCommands)
                .onRejection(this::postRejection)
                .handle();
    }

    @Override
    public ImmutableSet<EventClass> events() {
        return thisClass.events();
    }

    @Override
    public ImmutableSet<EventClass> externalEvents() {
        return thisClass.externalEvents();
    }

    @Override
    public ImmutableSet<EventClass> domesticEvents() {
        return thisClass.domesticEvents();
    }

    @Override
    public void dispatchEvent(EventEnvelope event) {
        CommandReactionMethod method = thisClass.commanderOn(event.messageClass());
        DispatchOutcomeHandler
                .from(method.invoke(this, event))
                .onCommands(this::postCommands)
                .handle();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Always returns a version with number {@code 0} and current time.
     */
    @Override
    public Version version() {
        return Versions.zero();
    }

    private void postCommands(List<Command> commands) {
        commandBus().post(commands, noOpObserver());
    }

    private void postRejection(Event rejectionEvent) {
        ImmutableList<Event> events = ImmutableList.of(rejectionEvent);
        postEvents(events);
    }
}
