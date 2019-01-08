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

import com.google.protobuf.Message;
import io.spine.base.CommandMessage;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.server.BoundedContext;

import java.util.Set;

import static io.spine.core.CommandClass.setOf;

/**
 * Utility class that remembers all commands issued by a commander class.
 *
 * @author Alexander Yevsyukov
 */
public class CommandInterceptor extends AbstractCommandHandler {

    private final Set<CommandClass> intercept;
    private final CommandHistory history = new CommandHistory();

    @SafeVarargs
    @SuppressWarnings("ThisEscapedInObjectConstruction") // Already configured.
    CommandInterceptor(BoundedContext context, Class<? extends CommandMessage>... commandClasses) {
        super(context.getEventBus());
        this.intercept = setOf(commandClasses);
        context.getCommandBus()
               .register(this);
    }

    @Override
    public String dispatch(CommandEnvelope envelope) {
        history.add(envelope);
        return getClass().getName();
    }

    @Override
    public Set<CommandClass> getMessageClasses() {
        return intercept;
    }

    public boolean contains(Class<? extends Message> commandClass) {
        return history.contains(commandClass);
    }
}
