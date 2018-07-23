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

import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventBus;

import java.util.Set;

/**
 * The abstract base for classes that post one or more command in response to an incoming one.
 *
 * <p>Example of use case scenarios:
 * <ul>
 *     <li>Converting a command into another (e.g. because of command API changes).
 *     <li>Splitting a command which holds data for a bigger aggregate into several commands
 *     set to corresponding aggregate parts.
 * </ul>
 *
 * @author Alexander Yevsyukov
 */
public abstract class Commander extends AbstractCommandDispatcher {

    private final CommandBus commandBus;

    protected Commander(CommandBus commandBus, EventBus eventBus) {
        super(eventBus);
        this.commandBus = commandBus;
    }

    @Override
    public Set<CommandClass> getMessageClasses() {
        //TODO:2018-07-20:alexander.yevsyukov: This should be obtained by inspecting methods in the model class.
        return Sets.newHashSet();
    }

    @CanIgnoreReturnValue
    @Override
    public String dispatch(CommandEnvelope envelope) {
        //TODO:2018-07-20:alexander.yevsyukov: Dispatch the envelope to the method.
        // Post resulting events of command transformations to the EventBus.
        return getId();
    }
}
