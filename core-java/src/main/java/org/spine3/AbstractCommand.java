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

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import org.spine3.base.CommandRequest;
import org.spine3.protobuf.Messages;
import org.spine3.util.MessageValue;

/**
 * Abstract base for command classes.
 *
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("AbstractClassWithoutAbstractMethods") // is OK for value base.
abstract class AbstractCommand extends MessageValue {

    protected AbstractCommand(Message value) {
        super(value);
    }

    @SuppressWarnings("TypeMayBeWeakened") // We use message types for brevity of API.
    public static Message getCommandValue(CommandRequest commandRequest) {
        final Any command = commandRequest.getCommand();
        final Message result = Messages.fromAny(command);
        return result;
    }
}
