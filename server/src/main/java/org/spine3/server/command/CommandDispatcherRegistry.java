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

package org.spine3.server.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import org.spine3.base.CommandClass;
import org.spine3.server.bus.DispatcherRegistry;
import org.spine3.server.procman.ProcessManagerRepository;

import java.util.Map;
import java.util.Set;

import static org.spine3.util.Exceptions.newIllegalArgumentException;

/**
 * The registry of objects dispatching command request to where they are processed.
 *
 * <p>There can be only one dispatcher per command class.
 *
 * @author Alexander Yevsyukov
 */
class CommandDispatcherRegistry extends DispatcherRegistry<CommandClass, CommandDispatcher> {

    /**
     * {@inheritDoc}
     *
     * <p>If an instance of {@link ProcessManagerRepository} is passed to registration
     * and it does not {@linkplain ProcessManagerRepository#getMessageClasses() expose}
     * command classes (because the corresponding {@link org.spine3.server.procman.ProcessManager
     * ProcessManager} does not handle commands), the repository is not registered.
     * No exceptions or log messages will be produces in this case.
     *
     * @param dispatcher the dispatcher to register
     */
    @Override
    protected void register(CommandDispatcher dispatcher) {
        if (dispatcher instanceof ProcessManagerRepository
            && dispatcher.getMessageClasses().isEmpty()) {
            return;
        }
        super.register(dispatcher);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Ensures that there are not dispatchers already registered for
     * the commands of this dispatcher.
     *
     * @param dispatcher the dispatcher to check
     * @throws IllegalStateException if there is at least one command of the passed dispatcher
     *                               that already has a registered dispatcher
     */
    @Override
    protected void checkDispatcher(CommandDispatcher dispatcher) throws IllegalArgumentException {
        super.checkDispatcher(dispatcher);
        checkNotAlreadyRegistered(dispatcher);
    }

    Optional<CommandDispatcher> getDispatcher(CommandClass commandClass) {
        final Set<CommandDispatcher> dispatchers = getDispatchers(commandClass);
        if (dispatchers.isEmpty()) {
            return Optional.absent();
        }

        // Since there can be only one dispatcher per command the returned set
        // contains only one element.
        final CommandDispatcher result = FluentIterable.from(dispatchers)
                                                       .get(0);
        return Optional.of(result);
    }

    /**
     * Ensures that all of the commands of the passed dispatcher are not
     * already registered for dispatched in this command bus.
     *
     * @throws IllegalArgumentException if at least one command class already has
     *                                  a registered dispatcher
     */
    private void checkNotAlreadyRegistered(CommandDispatcher dispatcher) {
        final Set<CommandClass> commandClasses = dispatcher.getMessageClasses();
        final Map<CommandClass, CommandDispatcher> alreadyRegistered = Maps.newHashMap();
        // Gather command classes from this dispatcher that are registered.
        for (CommandClass commandClass : commandClasses) {
            final Optional<CommandDispatcher> registeredDispatcher = getDispatcher(commandClass);
            if (registeredDispatcher.isPresent()) {
                alreadyRegistered.put(commandClass, registeredDispatcher.get());
            }
        }

        doCheck(alreadyRegistered, dispatcher);
    }

    /**
     * Ensures that the passed set of classes is empty.
     *
     * <p>This is a convenience method for checking registration of handling dispatching.
     *
     * @param alreadyRegistered the map of already registered classes or an empty set
     * @param registeringObject the object which tries to register dispatching or handling
     * @throws IllegalArgumentException if the set is not empty
     */
    private static void doCheck(Map<CommandClass, CommandDispatcher> alreadyRegistered,
                                Object registeringObject) {
        if (!alreadyRegistered.isEmpty()) {
            throw newIllegalArgumentException(
                    "Cannot register dispatcher (%s) because there are " +
                    "already registered dispatchers for the same command classes (%s).",
                    registeringObject,
                    alreadyRegistered);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to
     * {@link CommandBus#getRegisteredCommandClasses() CommandBus}.
     */
    @Override
    protected Set<CommandClass> getRegisteredMessageClasses() {
        return super.getRegisteredMessageClasses();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to the tests.
     */
    @VisibleForTesting
    @Override
    protected void unregister(CommandDispatcher dispatcher) {
        super.unregister(dispatcher);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to expose the method to
     * {@link CommandBus#close() CommandBus}.
     */
    @Override
    protected void unregisterAll() {
        super.unregisterAll();
    }
}
