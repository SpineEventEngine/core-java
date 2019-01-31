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

package io.spine.server.procman.model;

import com.google.common.collect.Sets.SetView;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.server.command.model.CommandReactionMethod;
import io.spine.server.command.model.CommandSubstituteMethod;
import io.spine.server.command.model.CommanderClass;
import io.spine.server.command.model.CommandingClass;
import io.spine.server.entity.model.CommandHandlingEntityClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.event.model.ReactingClass;
import io.spine.server.event.model.ReactorClassDelegate;
import io.spine.server.procman.ProcessManager;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.union;

/**
 * Provides message handling information on a process manager class.
 *
 * @param <P> the type of process managers
 */
public final class ProcessManagerClass<P extends ProcessManager>
        extends CommandHandlingEntityClass<P>
        implements ReactingClass, CommandingClass {

    private static final long serialVersionUID = 0L;

    private final ReactorClassDelegate<P> reactorDelegate;
    private final CommanderClass<P> commanderDelegate;

    private ProcessManagerClass(Class<P> cls) {
        super(cls);
        this.reactorDelegate = new ReactorClassDelegate<>(cls);
        this.commanderDelegate = CommanderClass.delegateFor(cls);
    }

    /**
     * Obtains the process manager class for the passed raw class.
     */
    public static <P extends ProcessManager>
    ProcessManagerClass<P> asProcessManagerClass(Class<P> cls) {
        checkNotNull(cls);
        ProcessManagerClass<P> result = (ProcessManagerClass<P>)
                get(cls, ProcessManagerClass.class, () -> new ProcessManagerClass<>(cls));
        return result;
    }

    @Override
    public Set<CommandClass> getCommands() {
        SetView<CommandClass> result =
                union(super.getCommands(), commanderDelegate.getCommands());
        return result;
    }

    @Override
    public Set<EventClass> getEventClasses() {
        SetView<EventClass> result =
                union(reactorDelegate.getEventClasses(), commanderDelegate.getEventClasses());
        return result;
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        SetView<EventClass> result =
                union(reactorDelegate.getExternalEventClasses(),
                      commanderDelegate.getExternalEventClasses());
        return result;
    }

    public Set<EventClass> getEmittedEventClasses() {
        SetView<EventClass> result = union(getHandleProducts(), getReactProducts());
        return result;
    }

    @Override
    public EventReactorMethod getReactor(EventClass eventClass, MessageClass originClass) {
        return reactorDelegate.getReactor(eventClass, originClass);
    }

    @Override
    public Set<EventClass> getReactProducts() {
        return reactorDelegate.getReactProducts();
    }

    @Override
    public Set<CommandClass> getProducedCommands() {
        return commanderDelegate.getProducedCommands();
    }

    public CommandSubstituteMethod getCommander(CommandClass commandClass) {
        return commanderDelegate.getHandler(commandClass);
    }

    public CommandReactionMethod getCommander(EventClass eventClass) {
        return commanderDelegate.getCommander(eventClass);
    }

    public boolean substitutesCommand(CommandClass commandClass) {
        return commanderDelegate.substitutesCommand(commandClass);
    }

    public boolean reactsOnEvent(EventClass eventClass) {
        return reactorDelegate.contains(eventClass);
    }

    public boolean producesCommandsOn(EventClass eventClass) {
        return commanderDelegate.producesCommandsOn(eventClass);
    }
}
