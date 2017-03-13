/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.aggregate;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.Command;
import org.spine3.base.CommandContext;
import org.spine3.client.CommandFactory;
import org.spine3.server.BoundedContext;
import org.spine3.server.command.Assign;
import org.spine3.server.entity.idfunc.IdCommandFunction;
import org.spine3.test.TestCommandFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.protobuf.Values.newStringValue;
import static org.spine3.test.Tests.emptyObserver;

/**
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OptionalGetWithoutIsPresent") // we do get() in assertions.
public class AggregateRepositoryViewsShould {

    private final CommandFactory commandFactory = TestCommandFactory.newInstance(getClass());
    private BoundedContext boundedContext;
    /**
     * The default behaviour of an {@code AggregateRepository}.
     */
    private AggregateRepository<Long, SHAggregate> repository;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType") // It's on purpose for tests.
    private Optional<SHAggregate> aggregate;

    /** The Aggregate ID used in all tests */
    private static final Long id = 100L;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        repository = new SHRepository(boundedContext);
        boundedContext.register(repository);

        // Create the aggregate instance.
        postCommand("createCommand");
    }

    /** Creates a command and posts it to {@code CommandBus} for being processed by the repository. */
    private void postCommand(String cmd) {
        final Command command = commandFactory.createCommand(SHRepository.createCommandMessage(id, cmd));
        boundedContext.getCommandBus().post(command, emptyObserver());
    }

    @Test
    public void load_aggregate_if_no_status_flags_set() {
        aggregate = repository.load(id);

        assertTrue(aggregate.isPresent());
        final SHAggregate agg = aggregate.get();
        assertFalse(agg.isArchived());
        assertFalse(agg.isDeleted());
    }

    @Test
    public void not_load_aggregates_with_archived_status() {
        postCommand("archive");

        aggregate = repository.load(id);

        assertFalse(aggregate.isPresent());
    }

    @Test
    public void not_load_aggregates_with_deleted_status() {
        postCommand("delete");

        aggregate = repository.load(id);

        assertFalse(aggregate.isPresent());
    }

    /**
     * The aggregate that can handle lifecycle flags.
     *
     * <p>We use {@code StringValue} for messages to save on code generation
     * in the tests. Real aggregates should use generated messages.
     */
    @SuppressWarnings("RedundantMethodOverride") // We expose methods to the tests.
    private static class SHAggregate extends Aggregate<Long, StringValue, StringValue.Builder> {
        private SHAggregate(Long id) {
            super(id);
        }

        @Assign
        StringValue handle(StringValue commandMessage) {
            final String msg = commandMessage.getValue();
            // Transform the command to the event (the fact in the past).
            return newStringValue(msg + 'd');
        }

        @Apply
        private void on(StringValue eventMessage) {
            final String msg = SHRepository.getMessage(eventMessage);
            if ("archived".equalsIgnoreCase(msg)) {
                setArchived(true);
            }
            if ("deleted".equalsIgnoreCase(msg)) {
                setDeleted(true);
            }
            getBuilder().setValue(msg);
        }

        @Override
        public boolean isArchived() {
            return super.isArchived();
        }

        @Override
        public boolean isDeleted() {
            return super.isDeleted();
        }
    }

    /**
     * The aggregate repository under tests.
     *
     * <p>The repository accepts commands as {@code StringValue}s in the form:
     * {@code AggregateId-CommandMessage}.
     */
    private static class SHRepository extends AggregateRepository<Long, SHAggregate> {

        private static final char SEPARATOR = '-';

        private static StringValue createCommandMessage(Long id, String msg) {
            return newStringValue("%d%s" + msg, id, SEPARATOR);
        }

        private static Long getId(StringValue commandMessage) {
            return Long.valueOf(Splitter.on(SEPARATOR)
                                        .splitToList(commandMessage.getValue())
                                        .get(0));
        }

        private static String getMessage(StringValue commandMessage) {
            return Splitter.on(SEPARATOR)
                           .splitToList(commandMessage.getValue())
                           .get(1);
        }

        /**
         * Custom {@code IdCommandFunction} that parses an aggregate ID from {@code StringValue}.
         */
        private static final IdCommandFunction<Long, Message> parsingFunc = new IdCommandFunction<Long, Message>() {
            @Override
            public Long apply(Message message, CommandContext context) {
                final Long result = getId((StringValue)message);
                return result;
            }
        };

        private SHRepository(BoundedContext boundedContext) {
            super(boundedContext);
        }

        /**
         * Provides custom function for obtaining aggregate IDs from commands.
         */
        @SuppressWarnings("MethodDoesntCallSuperMethod") // This is the purpose of the method.
        @Override
        protected IdCommandFunction<Long, Message> getIdFunction() {
            return parsingFunc;
        }
    }
}
