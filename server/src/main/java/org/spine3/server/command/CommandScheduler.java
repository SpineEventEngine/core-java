/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

import com.google.common.base.Function;
import org.spine3.base.Command;

import static com.google.common.base.Preconditions.checkState;

/**
 * Schedules commands delivering them to the target according to the scheduling options.
 *
 * @author Alexander Litus
 */
public abstract class CommandScheduler {

    private boolean isActive = true;

    private Function<Command, Command> postFunction;

    /**
     * Schedule a command and deliver it to the target according to the scheduling options.
     *
     * <p>NOTE: check if the command is scheduled already.
     *
     * @param command a command to deliver later
     * @throws IllegalStateException if the scheduler is shut down
     * @see #post(Command)
     */
    public void schedule(Command command) {
        checkState(isActive, "Scheduler is shut down.");
    }

    /**
     * Initiates an orderly shutdown in which previously scheduled commands will be delivered later,
     * but no new commands will be accepted.
     *
     * <p>Invocation has no effect if the scheduler is already shut down.
     */
    public void shutdown() {
        isActive = false;
    }

    /**
     * Delivers a scheduled command to a target.
     *
     * @param command a command to deliver
     */
    protected void post(Command command) {
        postFunction.apply(command);
    }

    /**
     * Sets a function used to post scheduled commands.
     */
    /* package */ void setPostFunction(Function<Command, Command> postFunction) {
        this.postFunction = postFunction;
    }
}
