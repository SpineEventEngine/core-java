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

package io.spine.server.command;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.server.command.model.CommandReactionMethod;
import io.spine.server.command.model.CommandSubstituteMethod;
import io.spine.server.command.model.CommanderClass;
import io.spine.server.command.model.CommandingMethod;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcherDelegate;
import io.spine.string.Stringifiers;
import io.spine.type.MessageClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.command.model.CommanderClass.asCommanderClass;
import static java.lang.String.format;

/**
 * The abstract base for classes that generate commands in response to incoming messages.
 *
 * @author Alexander Yevsyukov
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
    public Set<CommandClass> getMessageClasses() {
        return thisClass.getCommands();
    }

    @CanIgnoreReturnValue
    @Override
    public String dispatch(CommandEnvelope command) {
        CommandSubstituteMethod method = thisClass.getHandler(command.getMessageClass());
        CommandingMethod.Result result =
                method.invoke(this, command.getMessage(), command.getCommandContext());
        result.transformOrSplitAndPost(command, commandBus);
        return getId();
    }

    @Override
    public Set<EventClass> getEventClasses() {
        return thisClass.getEventClasses();
    }

    @Override
    public Set<EventClass> getExternalEventClasses() {
        return thisClass.getExternalEventClasses();
    }

    @Override
    public Set<String> dispatchEvent(EventEnvelope event) {
        CommandReactionMethod method = thisClass.getCommander(event.getMessageClass());
        CommandingMethod.Result result =
                method.invoke(this, event.getMessage(), event.getEventContext());
        result.produceAndPost(event, commandBus);
        return identity();
    }

    @Override
    public void onError(EventEnvelope envelope, RuntimeException exception) {
        checkNotNull(envelope);
        checkNotNull(exception);
        MessageClass messageClass = envelope.getMessageClass();
        String messageId = Stringifiers.toString(envelope.getId());
        String errorMessage =
                format("Unable to create a command from event (class: %s id: %s).",
                       messageClass, messageId);
        log().error(errorMessage, exception);
    }
}
