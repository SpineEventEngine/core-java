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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.command.model.CommandReactionMethod;
import io.spine.server.command.model.CommandSubstituteMethod;
import io.spine.server.command.model.CommanderClass;
import io.spine.server.command.model.CommandingMethod;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.command.model.CommanderClass.asCommanderClass;

/**
 * The abstract base for classes that generate commands in response to incoming messages.
 */
public abstract class AbstractCommander
        extends AbstractCommandDispatcher
        implements Commander, EventDispatcherDelegate<String> {

    private final CommanderClass<?> thisClass = asCommanderClass(getClass());
    private final CommandBus commandBus;

    protected AbstractCommander(CommandBus commandBus, EventBus eventBus) {
        super(eventBus);
        this.commandBus = commandBus;
    }

    @Override
    public Set<CommandClass> messageClasses() {
        return thisClass.commands();
    }

    @CanIgnoreReturnValue
    @Override
    public String dispatch(CommandEnvelope command) {
        CommandSubstituteMethod method = thisClass.handlerOf(command.messageClass());
        CommandingMethod.Result result = method.invoke(this, command);
        result.transformOrSplitAndPost(command, commandBus);
        return getId();
    }

    @Override
    public Set<EventClass> eventClasses() {
        return thisClass.incomingEvents();
    }

    @Override
    public Set<EventClass> externalEventClasses() {
        return thisClass.externalEvents();
    }

    @Override
    public Set<String> dispatchEvent(EventEnvelope event) {
        CommandReactionMethod method = thisClass.getCommander(event.messageClass());
        CommandingMethod.Result result = method.invoke(this, event);
        result.produceAndPost(event, commandBus);
        return identity();
    }

    @Override
    public void onError(EventEnvelope event, RuntimeException exception) {
        checkNotNull(event);
        checkNotNull(exception);
        _error(exception,
               "Unable to create a command from event (class: `{}` id: `{}`).",
               event.messageClass(),
               event.idAsString());
    }
}
