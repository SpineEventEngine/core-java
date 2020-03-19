/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package com.example;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.server.BoundedContext;
import io.spine.server.BoundedContextBuilder;
import io.spine.server.DefaultRepository;
import io.spine.server.bc.given.ProjectAggregate;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.event.EventDispatcher;
import io.spine.server.type.CommandClass;
import io.spine.server.type.CommandEnvelope;
import io.spine.server.type.EventClass;
import io.spine.server.type.EventEnvelope;

/**
 * Test environment class for testing {@code BoundedContext} configuration from
 * outside the framework.
 *
 * @see io.spine.server.BoundedContextTest.RestrictRegistrationCalls
 */
public final class ForeignContextConfig {

    private ForeignContextConfig() {}

    public static void repositoryRegistration() {
        context().internalAccess().register(DefaultRepository.of(ProjectAggregate.class));
    }

    public static void commandDispatcherRegistration() {
        context().internalAccess().registerCommandDispatcher(newCommandDispatcher());
    }

    public static void eventDispatcherRegistration() {
        context().internalAccess().registerEventDispatcher(EmptyEventDispatcher.newInstance());
    }

    private static BoundedContext context() {
        return BoundedContextBuilder.assumingTests().build();
    }

    private static CommandDispatcher newCommandDispatcher() {
        return new CommandDispatcher() {
            @Override
            public ImmutableSet<CommandClass> messageClasses() {
                return ImmutableSet.of();
            }

            @CanIgnoreReturnValue
            @Override
            public void dispatch(CommandEnvelope envelope) {
                // Do nothing.
            }
        };
    }

    private static class EmptyEventDispatcher implements EventDispatcher {

        private static EventDispatcher newInstance() {
            return new EmptyEventDispatcher();
        }

        @Override
        public ImmutableSet<EventClass> externalEventClasses() {
            return ImmutableSet.of();
        }

        @Override
        public ImmutableSet<EventClass> domesticEventClasses() {
            return eventClasses();
        }

        @Override
        public ImmutableSet<EventClass> messageClasses() {
            return ImmutableSet.of();
        }

        @CanIgnoreReturnValue
        @Override
        public void dispatch(EventEnvelope envelope) {
            // Do nothing.
        }
    }
}
