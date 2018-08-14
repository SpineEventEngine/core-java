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

package io.spine.server.command.model;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.core.CommandEnvelope;
import io.spine.core.EventEnvelope;
import io.spine.core.MessageEnvelope;
import io.spine.server.command.Command;
import io.spine.server.command.Commander;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.SeveralCommands;
import io.spine.server.commandbus.SingleCommand;
import io.spine.server.commandbus.Split;
import io.spine.server.commandbus.Transform;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.MethodResult;
import io.spine.server.model.MethodSignature;
import io.spine.type.MessageClass;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.of;
import static io.spine.server.commandbus.CommandSequence.inResponseTo;
import static io.spine.server.commandbus.CommandSequence.respondMany;
import static io.spine.server.commandbus.CommandSequence.split;
import static io.spine.server.commandbus.CommandSequence.transform;

/**
 * Base interface for methods that generate one or more command messages in response to
 * an incoming message.
 *
 * @param <M> the type of the message class
 * @param <R> the type of the method result
 *
 * @author Alexander Yevsyukov
 */
@Immutable
public interface CommandingMethod<M extends MessageClass,
                                  E extends MessageEnvelope<?, ?, ?>,
                                  R extends MethodResult>
        extends HandlerMethod<Commander, M, E, R> {

    /**
     * The implementation base for {@link CommandingMethod} factories.
     *
     * @param <H> the type of created methods
     * @param <S> the type of {@link MethodSignature}
     */
    abstract class Factory<H extends CommandingMethod,
                           S extends MethodSignature<?>> extends MethodFactory<H, S> {

        protected Factory() {
            super(Command.class, of(Message.class, Iterable.class));
        }
    }

    /**
     * A commanding method returns one or more command messages.
     */
    final class Result extends MethodResult<Message> {

        private final boolean optional;

        /**
         * Creates new instance of method result.
         *
         * @param rawMethodOutput
         *        output of the raw method call
         * @param optional
         *        {@code true} if this kind of methods may not return a value,
         *        {@code false} otherwise
         */
        Result(Object rawMethodOutput, boolean optional) {
            super(rawMethodOutput);
            this.optional = optional;
            List<Message> messages = toMessages(rawMethodOutput);
            checkMessages(messages);
            setMessages(messages);
        }

        private void checkMessages(List<Message> messages) {
            if (messages.isEmpty() && !optional) {
                throw new IllegalStateException(
                        "Commanding method did not produce command messages"
                );
            }
        }

        /**
         * Transforms the passed command into one or more, and posts new command(s) to the passed
         * {@code CommandBus}.
         *
         * @implNote The number of commands generated is the same as the number of
         *           {@linkplain #asMessages()} command messages} produced by the method.
         */
        public void transformOrSplitAndPost(CommandEnvelope cmd, CommandBus bus) {
            checkNotNull(cmd);
            checkNotNull(bus);
            List<? extends Message> messages = asMessages();
            if (messages.size() == 1) {
                Transform transform = transform(cmd).to(messages.get(0));
                transform.post(bus);
            } else {
                Split split = split(cmd).addAll(messages);
                split.postAll(bus);
            }
        }

        /**
         * Creates one or more commands in response to the event and posts the to the passed
         * {@code CommandBus}.
         *
         * @implNote The number of commands generated is the same as the number of
         *           {@linkplain #asMessages()} command messages} produced by the method.
         */
        public void produceAndPost(EventEnvelope event, CommandBus bus) {
            checkNotNull(event);
            checkNotNull(bus);
            List<? extends Message> messages = asMessages();
            if (messages.size() == 1) {
                SingleCommand seq = inResponseTo(event).produce(messages.get(0));
                seq.post(bus);
            } else {
                SeveralCommands seq = respondMany(event);
                seq.addAll(messages);
            }
        }
    }
}
