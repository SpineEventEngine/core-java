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

package io.spine.server.command.model;

import io.spine.core.EventClass;
import io.spine.server.command.AbstractCommander;
import io.spine.server.event.model.EventReceiverClass;
import io.spine.server.event.model.EventReceivingClassDelegate;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides information on message handling for a class of {@link AbstractCommander}s.
 *
 * @param <C> the type of commanders
 * @author Alexander Yevsyukov
 */
public final class CommanderClass<C extends AbstractCommander>
        extends AbstractCommandHandlingClass<C, CommandSubstituteMethod>
        implements EventReceiverClass {

    private static final long serialVersionUID = 0L;
    private final EventReceivingClassDelegate<C, CommandReactionMethod> delegate;

    private CommanderClass(Class<C> cls) {
        super(cls, CommandSubstituteMethod.factory());
        this.delegate = new EventReceivingClassDelegate<>(cls, CommandReactionMethod.factory());
    }

    public static <C extends AbstractCommander>
    CommanderClass<C> asCommanderClass(Class<C> cls) {
        checkNotNull(cls);
        CommanderClass<C> result = (CommanderClass<C>)
                get(cls, CommanderClass.class, () -> new CommanderClass<>(cls));
        return result;
    }

    @Override
    public Set<EventClass> getEventClasses() {
        return delegate.getEventClasses();
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return delegate.getExternalEventClasses();
    }

    /**
     * Obtains the method which reacts on the passed event class.
     */
    public CommandReactionMethod getReaction(EventClass eventClass) {
        return delegate.getMethod(eventClass);
    }
}
