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

package io.spine.server.aggregate.given;

import com.google.common.base.Splitter;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.route.CommandRoute;
import io.spine.validate.StringValueVBuilder;

import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.storage.LifecycleFlagField.archived;
import static io.spine.server.storage.LifecycleFlagField.deleted;
import static java.lang.String.format;

/**
 * Test environment for {@link io.spine.server.aggregate.AggregateRepositoryTest}.
 *
 * @author Alexander Yevsyukov
 */
public class AggregateRepositoryViewTestEnv {

    private AggregateRepositoryViewTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /**
     * The aggregate that can handle status flags.
     *
     * <p>We use {@code StringValue} for messages to save on code generation
     * in the tests. Real aggregates should use generated messages.
     */
    public static class AggregateWithLifecycle
            extends Aggregate<Long, StringValue, StringValueVBuilder> {

        private AggregateWithLifecycle(Long id) {
            super(id);
        }

        @Assign
        StringValue handle(StringValue commandMessage) {
            String msg = commandMessage.getValue();
            // Transform the command to the event (the fact in the past).
            return toMessage(msg + 'd');
        }

        @Apply
        void on(StringValue eventMessage) {
            String msg = RepoOfAggregateWithLifecycle.getMessage(eventMessage);
            if (archived.name()
                        .equalsIgnoreCase(msg)) {
                setArchived(true);
            }
            if (deleted.name()
                       .equalsIgnoreCase(msg)) {
                setDeleted(true);
            }
            getBuilder().setValue(msg);
        }
    }

    /**
     * The aggregate repository under tests.
     *
     * <p>The repository accepts commands as {@code StringValue}s in the form:
     * {@code AggregateId-CommandMessage}.
     */
    public static class RepoOfAggregateWithLifecycle
            extends AggregateRepository<Long, AggregateWithLifecycle> {

        private static final char SEPARATOR = '-';
        /**
         * Custom {@code IdCommandFunction} that parses an aggregate ID from {@code StringValue}.
         */
        private static final CommandRoute<Long, Message> parsingRoute =
                new CommandRoute<Long, Message>() {

                    private static final long serialVersionUID = 0L;

                    @Override
                    public Long apply(Message message, CommandContext context) {
                        Long result = getId((StringValue) message);
                        return result;
                    }
                };

        public RepoOfAggregateWithLifecycle() {
            super();
            getCommandRouting().replaceDefault(parsingRoute);
        }

        /**
         * Creates a command message in the form {@code <id>-<action>}.
         *
         * @see #getId(StringValue)
         * @see #getMessage(StringValue)
         */
        public static StringValue createCommandMessage(Long id, String msg) {
            return toMessage(format("%d%s%s", id, SEPARATOR, msg));
        }

        private static Long getId(StringValue commandMessage) {
            return Long.valueOf(Splitter.on(SEPARATOR)
                                        .splitToList(commandMessage.getValue())
                                        .get(0));
        }

        static String getMessage(StringValue commandMessage) {
            return Splitter.on(SEPARATOR)
                           .splitToList(commandMessage.getValue())
                           .get(1);
        }
    }
}
