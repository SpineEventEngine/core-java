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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets.SetView;
import io.spine.server.command.model.CommandReactionMethod;
import io.spine.server.command.model.CommandSubstituteMethod;
import io.spine.server.command.model.CommanderClass;
import io.spine.server.command.model.CommandingClass;
import io.spine.server.entity.model.CommandHandlingEntityClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.event.model.ReactingClass;
import io.spine.server.event.model.ReactorClassDelegate;
import io.spine.server.procman.ProcessManager;
import io.spine.server.type.CommandClass;
import io.spine.server.type.EventClass;
import io.spine.type.MessageClass;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.union;

/**
 * Provides message handling information on a process manager class.
 *
 * @param <P>
 *         the type of process managers
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
    public ImmutableSet<CommandClass> commands() {
        SetView<CommandClass> result =
                union(super.commands(), commanderDelegate.commands());
        return result.immutableCopy();
    }

    @Override
    public ImmutableSet<EventClass> events() {
        SetView<EventClass> result =
                union(reactorDelegate.events(), commanderDelegate.events());
        return result.immutableCopy();
    }

    @Override
    public ImmutableSet<EventClass> domesticEvents() {
        SetView<EventClass> result =
                union(reactorDelegate.domesticEvents(), commanderDelegate.domesticEvents());
        return result.immutableCopy();
    }

    @Override
    public ImmutableSet<EventClass> externalEvents() {
        SetView<EventClass> result =
                union(reactorDelegate.externalEvents(), commanderDelegate.externalEvents());
        return result.immutableCopy();
    }

    /**
     * Obtains event classes produced by this process manager class.
     */
    public ImmutableSet<EventClass> outgoingEvents() {
        SetView<EventClass> result = union(commandOutput(), reactionOutput());
        return result.immutableCopy();
    }

    @Override
    public EventReactorMethod reactorOf(EventClass eventClass, MessageClass originClass) {
        return reactorDelegate.reactorOf(eventClass, originClass);
    }

    @Override
    public ImmutableSet<EventClass> reactionOutput() {
        return reactorDelegate.reactionOutput();
    }

    @Override
    public ImmutableSet<CommandClass> outgoingCommands() {
        return commanderDelegate.outgoingCommands();
    }

    /**
     * Obtains a method which handles the passed class of commands by producing
     * one or more other commands.
     */
    public CommandSubstituteMethod commanderOf(CommandClass commandClass) {
        return commanderDelegate.handlerOf(commandClass);
    }

    /**
     * Obtains a method which may generate one or more commands in response to incoming
     * event with the passed class.
     */
    public CommandReactionMethod commanderOf(EventClass eventClass) {
        return commanderDelegate.commanderOn(eventClass);
    }

    /**
     * Verifies if the process manager class has a method which generates one or more
     * commands in response to a command of the passed class.
     */
    public boolean substitutesCommand(CommandClass commandClass) {
        return commanderDelegate.substitutesCommand(commandClass);
    }

    /**
     * Verifies if the class of process managers react on an event of the passed class.
     */
    public boolean reactsOnEvent(EventClass eventClass) {
        return reactorDelegate.contains(eventClass);
    }

    /**
     * Verifies if the process manager class generates a command in response to
     * an event of the passed class.
     */
    public boolean producesCommandsOn(EventClass eventClass) {
        return commanderDelegate.producesCommandsOn(eventClass);
    }
}
