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

package org.spine3.testutil;

import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.TimeUtil;
import org.spine3.base.CommandRequest;

import org.spine3.base.UserId;
import org.spine3.protobuf.Messages;
import org.spine3.test.order.command.CreateOrder;

/**
 * The utility class which is used for creating CommandRequests for tests.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class CommandRequestFactory {

    private CommandRequestFactory() {
    }

    public static CommandRequest create(Timestamp when) {
        final UserId userId = UserId.getDefaultInstance();
        final Any command = Messages.toAny(CreateOrder.newBuilder().setOrderId(
                AggregateIdFactory.createCommon()).build());

        final CommandRequest dummyCommandRequest = CommandRequest.newBuilder()
                .setContext(ContextFactory.getCommandContext(userId, when))
                .setCommand(command)
                .build();
        return dummyCommandRequest;
    }

    public static CommandRequest create() {
        return create(TimeUtil.getCurrentTime());
    }

}
