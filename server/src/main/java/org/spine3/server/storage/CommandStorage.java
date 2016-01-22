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

package org.spine3.server.storage;

import com.google.protobuf.Any;
import org.spine3.SPI;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.client.CommandRequest;
import org.spine3.server.aggregate.AggregateId;
import org.spine3.server.util.CommandIdentifiers;
import org.spine3.type.TypeName;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A storage used by {@link org.spine3.server.CommandStore} for keeping command data.
 *
 * @author Alexander Yevsyukov
 */
@SPI
public abstract class CommandStorage extends AbstractStorage<CommandId, CommandStorageRecord> {

    public void store(AggregateId aggregateId, CommandRequest request) {
        checkNotNull(aggregateId, "aggregateId");
        checkNotNull(request, "request");
        checkNotClosed("Cannot store to closed storage");

        final Any command = request.getCommand();
        final CommandContext context = request.getContext();
        final TypeName commandType = TypeName.ofEnclosed(command);
        final CommandId commandId = context.getCommandId();
        final String commandIdStr = CommandIdentifiers.idToString(commandId);
        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setTimestamp(context.getTimestamp())
                .setCommandType(commandType.nameOnly())
                .setCommandId(commandIdStr)
                .setAggregateIdType(aggregateId.getShortTypeName())
                .setAggregateId(aggregateId.toString())
                .setCommand(command)
                .setContext(context);

        write(commandId, builder.build());
    }

}
