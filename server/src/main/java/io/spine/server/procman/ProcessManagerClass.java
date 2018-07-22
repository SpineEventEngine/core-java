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

package io.spine.server.procman;

import com.google.common.collect.ImmutableSet;
import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.command.CommandHandlerMethod;
import io.spine.server.command.CommandHandlingClass;
import io.spine.server.entity.EntityClass;
import io.spine.server.event.EventReactorMethod;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.rejection.RejectionReactorMethod;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.HandlerMethods.domestic;
import static io.spine.server.model.HandlerMethods.external;

/**
 * Provides message handling information on a process manager class.
 *
 * @param <P> the type of process managers
 * @author Alexander Yevsyukov
 */
@Internal
@SuppressWarnings("ReturnOfCollectionOrArrayField") // returning an immutable impl.
public final class ProcessManagerClass<P extends ProcessManager>
        extends EntityClass<P>
        implements CommandHandlingClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<CommandClass, CommandHandlerMethod> commands;
    private final MessageHandlerMap<EventClass, EventReactorMethod> eventReactors;
    private final MessageHandlerMap<RejectionClass, RejectionReactorMethod> rejectionReactors;

    private final ImmutableSet<EventClass> domesticEventReactions;
    private final ImmutableSet<EventClass> externalEventReactions;
    private final ImmutableSet<RejectionClass> domesticRejectionReactions;
    private final ImmutableSet<RejectionClass> externalRejectionReactions;


    private ProcessManagerClass(Class<? extends P> cls) {
        super(cls);
        this.commands = new MessageHandlerMap<>(cls, CommandHandlerMethod.factory());
        this.eventReactors =  new MessageHandlerMap<>(cls, EventReactorMethod.factory());
        this.rejectionReactors =  new MessageHandlerMap<>(cls, RejectionReactorMethod.factory());

        this.domesticEventReactions = eventReactors.getMessageClasses(domestic());
        this.externalEventReactions = eventReactors.getMessageClasses(external());

        this.domesticRejectionReactions = rejectionReactors.getMessageClasses(domestic());
        this.externalRejectionReactions = rejectionReactors.getMessageClasses(external());
    }

    public static <P extends ProcessManager> ProcessManagerClass<P> of(Class<P> cls) {
        checkNotNull(cls);
        return new ProcessManagerClass<>(cls);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<CommandClass> getCommands() {
        return commands.getMessageClasses();
    }

    Set<EventClass> getEventReactions() {
        return domesticEventReactions;
    }

    Set<EventClass> getExternalEventReactions() {
        return externalEventReactions;
    }

    Set<RejectionClass> getRejectionReactions() {
        return domesticRejectionReactions;
    }

    Set<RejectionClass> getExternalRejectionReactions() {
        return externalRejectionReactions;
    }

    CommandHandlerMethod getHandler(CommandClass commandClass) {
        return commands.getMethod(commandClass);
    }

    EventReactorMethod getReactor(EventClass eventClass) {
        return eventReactors.getMethod(eventClass);
    }

    RejectionReactorMethod getReactor(RejectionClass cls, CommandClass commandCls) {
        return rejectionReactors.getMethod(cls, commandCls);
    }
}
