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

package io.spine.server.command;

import com.google.common.collect.ImmutableList;
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
import io.spine.server.entity.PropagationOutcome;
import io.spine.server.entity.Success;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;
import java.util.Set;

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
    public void initialize(BoundedContext context) {
        super.initialize(context);
        commandBus = context.commandBus();
    }

    @Override
    public Set<CommandClass> messageClasses() {
        return thisClass.commands();
    }

    @CanIgnoreReturnValue
    @Override
    public void dispatch(CommandEnvelope command) {
        CommandSubstituteMethod method = thisClass.handlerOf(command.messageClass());
        PropagationOutcome outcome = method.invoke(this, command);
        Success success = outcome.getSuccess();
        postCommands(success);
        postRejection(success);
    }

    @Override
    public Set<EventClass> domesticEvents() {
        return thisClass.domesticEvents();
    }

    @Override
    public Set<EventClass> externalEvents() {
        return thisClass.externalEvents();
    }

    @Override
    public void dispatchEvent(EventEnvelope event) {
        CommandReactionMethod method = thisClass.getCommander(event.messageClass());
        PropagationOutcome outcome = method.invoke(this, event);
        postCommands(outcome.getSuccess());
    }

    @Override
    public Version version() {
        return Versions.zero();
    }

    private void postCommands(Success successfulOutcome) {
        if (successfulOutcome.hasProducedCommands()) {
            List<Command> commands = successfulOutcome.getProducedCommands()
                                                      .getCommandList();
            commandBus.post(commands, noOpObserver());
        }
    }

    private void postRejection(Success successfulOutcome) {
        if (successfulOutcome.hasRejection()) {
            ImmutableList<Event> events = ImmutableList.of(successfulOutcome.getRejection());
            postEvents(events);
        }
    }
}
