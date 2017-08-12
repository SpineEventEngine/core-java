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

import javax.annotation.Nullable;
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

    @Nullable
    private Set<CommandClass> commands;

    @Nullable
    private Set<EventClass> stateEvents;

    @Nullable
    private Set<EventClass> eventReactions;

    @Nullable
    private Set<RejectionClass> rejectionReactions;

    private AggregateClass(Class<? extends A> value) {
        super(value);
    }

    public static <A extends Aggregate> AggregateClass<A> of(Class<A> cls) {
        checkNotNull(cls);
        return new AggregateClass<>(cls);
    }

    @Override
    public Set<CommandClass> getCommands() {
        if (commands == null) {
            commands = CommandHandlerMethod.inspect(value());
        }
        return commands;
    }

    public Set<EventClass> getStateEvents() {
        if (stateEvents == null) {
            stateEvents = EventApplierMethod.inspect(value());
        }
        return stateEvents;
    }

    public Set<EventClass> getEventReactions() {
        if (eventReactions == null) {
            eventReactions = EventReactorMethod.inspect(value());
        }
        return eventReactions;
    }

    public Set<RejectionClass> getRejectionReactions() {
        if (rejectionReactions == null) {
            rejectionReactions = RejectionReactorMethod.inspect(value());
        }
        return rejectionReactions;
    }
}
