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

package io.spine.testing.server.procman;

import io.spine.server.BoundedContext;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.procman.ProcessManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * Injects the given {@link CommandBus command bus} into the process manager for unit tests.
 *
 * @author Vladyslav Lubenskyi
 */
public class InjectCommandBus {

    private final CommandBus bus;

    private InjectCommandBus(CommandBus bus) {
        this.bus = bus;
    }

    /**
     * Creates new instance for injecting the passed Command Bus.
     */
    public static InjectCommandBus of(CommandBus bus) {
        checkNotNull(bus);
        return new InjectCommandBus(bus);
    }

    /**
     * Creates new instance for injecting a Command Bus of the passed Bounded Context.
     */
    public static InjectCommandBus of(BoundedContext boundedContext) {
        checkNotNull(boundedContext);
        CommandBus bus = boundedContext.getCommandBus();
        checkNotNull(bus);
        return of(bus);
    }

    public void to(ProcessManager processManager) {
        inject(processManager, bus);
    }

    /**
     * Injects {@link CommandBus} instance into the process manager via reflection.
     *
     * @param processManager tested process manager.
     * @see ProcessManager#setCommandBus(CommandBus)
     */
    static void inject(ProcessManager processManager, CommandBus commandBus) {
        try {
            Method method = ProcessManager.class.getDeclaredMethod("setCommandBus",
                                                                   CommandBus.class);
            method.setAccessible(true);
            method.invoke(processManager, commandBus);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw illegalStateWithCauseOf(e);
        }
    }
}
