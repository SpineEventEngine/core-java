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
import com.google.protobuf.StringValue;
import io.spine.base.CommandMessage;
import io.spine.core.CommandContext;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.AggregateRepository;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.Assign;
import io.spine.server.route.CommandRoute;
import io.spine.test.aggregate.archiver.Evaluate;
import io.spine.test.aggregate.archiver.Evaluated;
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
        StringValue handle(Evaluate commandMessage) {
            String command = commandMessage.getCmd();
            // Transform the command to the event (the fact in the past).
            return toMessage(command + 'd');
        }

        @Apply
        void on(Evaluated eventMessage) {
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
        private static final CommandRoute<Long, CommandMessage> parsingRoute =
                new CommandRoute<Long, CommandMessage>() {

                    private static final long serialVersionUID = 0L;

                    @Override
                    public Long apply(CommandMessage message, CommandContext context) {
                        Long result = getId((Evaluate) message);
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
         * @see #getId(Evaluate)
         * @see #getMessage(Evaluated)
         */
        public static Evaluate createCommandMessage(Long id, String msg) {
            return toMessage(format("%d%s%s", id, SEPARATOR, msg));
        }

        private static Long getId(Evaluate commandMessage) {
            return Long.valueOf(Splitter.on(SEPARATOR)
                                        .splitToList(commandMessage.getCmd())
                                        .get(0));
        }

        static String getMessage(Evaluated commandMessage) {
            return Splitter.on(SEPARATOR)
                           .splitToList(commandMessage.getCmd())
                           .get(1);
        }
    }
}
