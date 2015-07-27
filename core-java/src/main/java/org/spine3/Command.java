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
import org.spine3.base.CommandRequestOrBuilder;
import org.spine3.util.MessageValue;
import org.spine3.util.Messages;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A command in the system.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OverloadedMethodsWithSameNumberOfParameters") // is OK as we want many factory methods.
public class Command extends MessageValue {

    protected Command(Message value) {
        super(value);
    }

    public static Command of(Message value) {
        return new Command(checkNotNull(value));
    }

    public static Command of(CommandRequest cr) {
        return new Command(getCommandValue(checkNotNull(cr)));
    }

    public static Message getCommandValue(CommandRequestOrBuilder commandRequest) {
        return Messages.fromAny(commandRequest.getCommand());
    }
}
