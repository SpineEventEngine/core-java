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

package io.spine.system.server.given;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Message;
import io.spine.system.server.CommandAcknowledged;
import io.spine.system.server.CommandDispatched;
import io.spine.system.server.CommandErrored;
import io.spine.system.server.CommandHandled;
import io.spine.system.server.CommandReceived;
import io.spine.system.server.CommandRejected;

import java.util.Collection;

/**
 * The environment for the {@link io.spine.system.server.CommandLifecycleAggregate CommandLifecycle}
 * tests.
 *
 * @author Dmytro Dashenkov
 */
public final class CommandLifecycleTestEnv {

    /**
     * Prevents the utility class instantiation.
     */
    private CommandLifecycleTestEnv() {
    }

    public static class CommandLifecycleWatcher extends AbstractEventWatcher {

        @Override
        protected Collection<Class<? extends Message>> getEventClasses() {
            return ImmutableSet.of(CommandReceived.class,
                                   CommandAcknowledged.class,
                                   CommandDispatched.class,
                                   CommandHandled.class,
                                   CommandErrored.class,
                                   CommandRejected.class);
        }
    }
}
