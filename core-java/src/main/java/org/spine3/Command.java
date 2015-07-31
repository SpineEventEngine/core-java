/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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

package org.spine3;

import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A command in the system.
 *
 * @author Alexander Yevsyukov
 */
public final class Command extends AbstractCommand {

    private Command(Message value) {
        super(value);
    }

    public static Command of(Message value) {
        return new Command(checkNotNull(value));
    }

    public static Command from(CommandRequest cr) {
        return new Command(getCommandValue(checkNotNull(cr)));
    }

    /**
     * @return the class of the command
     */
    public CommandClass getCommandClass() {
        Message value = value();
        return CommandClass.of(value.getClass());
    }

    /**
     * @return contained command message
     */
    @Nonnull
    @Override
    public Message value() {
        final Message value = super.value();
        assert value != null; // as we prevent null value initialization.
        return value;
    }
}
