/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.model;

import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.client.ActorRequestFactory;
import io.spine.client.CommandFactory;
import io.spine.core.ActorContext;
import io.spine.core.Command;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.ProducedCommands;
import io.spine.server.entity.Success;
import io.spine.server.type.CommandClass;
import io.spine.server.type.MessageEnvelope;
import io.spine.type.MessageClass;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Immutable
public interface CommandProducingMethod<T,
                                        C extends MessageClass,
                                        E extends MessageEnvelope<?, ?, ?>>
        extends HandlerMethod<T, C, E, CommandClass> {

    @Override
    default Success toSuccessfulOutcome(Object rawResult, T target, MessageEnvelope<?, ?, ?> origin) {
        MethodResult result = MethodResult.from(rawResult);
        ActorContext actorContext = origin.asMessageOrigin()
                                     .getActorContext();
        CommandFactory commandFactory = ActorRequestFactory
                .fromContext(actorContext)
                .command();
        List<Command> commands = result
                .messages()
                .stream()
                .map(msg -> {
                    CommandMessage commandMessage = (CommandMessage) AnyPacker.unpack(msg);
                    return commandFactory.create(commandMessage);
                })
                                      .collect(toList());
        ProducedCommands signals = ProducedCommands
                .newBuilder()
                .addAllCommand(commands)
                .vBuild();
        return Success
                .newBuilder()
                .setProducedCommands(signals)
                .vBuild();
    }
}
