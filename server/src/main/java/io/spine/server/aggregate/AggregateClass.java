/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.server.aggregate;

import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.command.CommandHandlerClass;
import io.spine.server.model.EntityClass;
import io.spine.server.reflect.CommandHandlerMethod;
import io.spine.server.reflect.EventApplierMethod;
import io.spine.server.reflect.EventReactorMethod;
import io.spine.server.reflect.RejectionReactorMethod;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides type information on an aggregate class.
 *
 * @author Alexander Yevsyukov
 */
@Internal
@SuppressWarnings("ReturnOfCollectionOrArrayField") // impl. is immutable
public final class AggregateClass<A extends Aggregate>
        extends EntityClass<A>
        implements CommandHandlerClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<CommandClass, CommandHandlerMethod> commands;
    private final MessageHandlerMap<EventClass, EventApplierMethod> stateEvents;
    private final MessageHandlerMap<EventClass, EventReactorMethod> eventReactions;
    private final MessageHandlerMap<RejectionClass, RejectionReactorMethod> rejectionReactions;

    private AggregateClass(Class<? extends A> cls) {
        super(cls);
        this.commands = new MessageHandlerMap<>(cls, CommandHandlerMethod.factory());
        this.stateEvents = new MessageHandlerMap<>(cls, EventApplierMethod.factory());
        this.eventReactions = new MessageHandlerMap<>(cls, EventReactorMethod.factory());
        this.rejectionReactions = new MessageHandlerMap<>(cls, RejectionReactorMethod.factory());
    }

    public static <A extends Aggregate> AggregateClass<A> of(Class<A> cls) {
        checkNotNull(cls);
        return new AggregateClass<>(cls);
    }

    @Override
    public Set<CommandClass> getCommands() {
        return commands.getMessageClasses();
    }

    public Set<EventClass> getStateEvents() {
        return stateEvents.getMessageClasses();
    }

    public Set<EventClass> getEventReactions() {
        return eventReactions.getMessageClasses();
    }

    public Set<RejectionClass> getRejectionReactions() {
        return rejectionReactions.getMessageClasses();
    }

    public CommandHandlerMethod getHandler(CommandClass commandClass) {
        return commands.getMethod(commandClass);
    }

    public EventApplierMethod getApplier(EventClass eventClass) {
        return stateEvents.getMethod(eventClass);
    }
}
