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
import io.spine.server.command.model.CommandReactionMethod;
import io.spine.server.command.model.CommandSubstituteMethod;
import io.spine.server.command.model.CommanderClass;
import io.spine.server.command.model.CommandingClass;
import io.spine.server.entity.model.CommandHandlingEntityClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.event.model.ReactingClass;
import io.spine.server.event.model.ReactorClassDelegate;
import io.spine.server.model.ExternalCommandReceiverMethodError;
import io.spine.server.model.HandlerMethod;
import io.spine.server.procman.ProcessManager;
import io.spine.server.type.CommandClass;
import io.spine.server.type.EventClass;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.union;
import static java.util.stream.Collectors.toSet;

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
        checkExternalCommanders(result);
        return result;
    }

    @Override
    public Set<CommandClass> commands() {
        SetView<CommandClass> result =
                union(super.commands(), commanderDelegate.commands());
        return result;
    }

    @Override
    public Set<EventClass> domesticEvents() {
        SetView<EventClass> result =
                union(reactorDelegate.domesticEvents(), commanderDelegate.domesticEvents());
        return result;
    }

    @Override
    public Set<EventClass> externalEvents() {
        SetView<EventClass> result =
                union(reactorDelegate.externalEvents(),
                      commanderDelegate.externalEvents());
        return result;
    }

    /**
     * Obtains event classes produced by this process manager class.
     */
    public Set<EventClass> outgoingEvents() {
        SetView<EventClass> result = union(commandOutput(), reactionOutput());
        return result;
    }

    @Override
    public EventReactorMethod reactorOf(EventClass eventClass, MessageClass originClass) {
        return reactorDelegate.reactorOf(eventClass, originClass);
    }

    @Override
    public Set<EventClass> reactionOutput() {
        return reactorDelegate.reactionOutput();
    }

    @Override
    public Set<CommandClass> outgoingCommands() {
        return commanderDelegate.outgoingCommands();
    }

    public CommandSubstituteMethod commanderOf(CommandClass commandClass) {
        return commanderDelegate.handlerOf(commandClass);
    }

    public CommandReactionMethod commanderOf(EventClass eventClass) {
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

    /**
     * Makes sure no command substitution methods are marked as
     * {@linkplain io.spine.server.command.Command#external()} external} in the class.
     *
     * <p>Command substitution methods accept {@linkplain io.spine.base.CommandMessage commands} as
     * input and there is no notion of "external" commands in the system. Thus, such method
     * declarations, although technically possible, should be avoided to prevent confusion.
     *
     * @throws ExternalCommandReceiverMethodError
     *         in case external command substitution methods are found within the class
     */
    private static void checkExternalCommanders(ProcessManagerClass<?> candidate) {
        Set<CommandSubstituteMethod> methods =
                candidate.commanderDelegate
                        .commands()
                        .stream()
                        .map(candidate::commanderOf)
                        .filter(HandlerMethod::isExternal)
                        .collect(toSet());
        if (!methods.isEmpty()) {
            throw new ExternalCommandReceiverMethodError(candidate, methods);
        }
    }
}
