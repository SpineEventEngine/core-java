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

package io.spine.server.procman.model;

import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.server.entity.model.CommandHandlingEntityClass;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.event.model.ReactorClass;
import io.spine.server.event.model.ReactorClassDelegate;
import io.spine.server.procman.ProcessManager;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides message handling information on a process manager class.
 *
 * @param <P> the type of process managers
 * @author Alexander Yevsyukov
 */
public final class ProcessManagerClass<P extends ProcessManager>
        extends CommandHandlingEntityClass<P>
        implements ReactorClass {

    private static final long serialVersionUID = 0L;

    private final ReactorClassDelegate<P> delegate;

    private ProcessManagerClass(Class<P> cls) {
        super(cls);
        this.delegate = new ReactorClassDelegate<>(cls);
    }

    public static <P extends ProcessManager> ProcessManagerClass<P> of(Class<P> cls) {
        checkNotNull(cls);
        return new ProcessManagerClass<>(cls);
    }

    @Override
    public Set<EventClass> getEventReactions() {
        return delegate.getEventReactions();
    }

    @Override
    public Set<EventClass> getExternalEventReactions() {
        return delegate.getExternalEventReactions();
    }

    @Override
    public EventReactorMethod getReactor(EventClass eventClass, CommandClass commandClass) {
        return delegate.getReactor(eventClass, commandClass);
    }
}
