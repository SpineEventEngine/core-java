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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.base.CommandId;
import org.spine3.base.Commands;
import org.spine3.server.aggregate.AggregateId;
import org.spine3.testdata.TestContextFactory;
import org.spine3.type.TypeName;

import static com.google.protobuf.util.TimeUtil.getCurrentTime;
import static org.junit.Assert.*;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Messages.toAny;
import static org.spine3.testdata.TestCommands.createProject;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public abstract class CommandStorageShould {

    private static final int TEST_ITERATION_COUNT = 500;
    private CommandStorage storage;

    @Before
    public void setUpTest() {
        storage = getStorage();
    }

    @After
    public void tearDownTest() throws Exception {
        storage.close();
    }

    /**
     * Used to initialize the storage before each test.
     *
     * @return an empty storage instance
     */
    protected abstract CommandStorage getStorage();

    @Test
    public void override_read_method_and_do_not_throw_exception() {
        try {
            storage.read(CommandId.getDefaultInstance());
        } catch (UnsupportedOperationException e) {
            fail("read() method must be overridden if you want to use these tests.");
        }
    }

    @Test
    public void return_null_if_no_record_with_such_id_exists() {
        final CommandStorageRecord record = storage.read(CommandId.getDefaultInstance());
        assertNull(record);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_read_by_null_id() {
        //noinspection ConstantConditions
        storage.read(null);
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_by_null_id() {
        //noinspection ConstantConditions
        storage.write(null, newCommandStorageRecord());
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = NullPointerException.class)
    public void throw_exception_if_write_null_record() {
        storage.write(Commands.generateId(), null);
    }

    @Test
    public void write_and_read_record() {
        writeAndReadRecordTest();
    }

    @Test
    public void write_and_read_several_records_by_different_ids() {
        writeAndReadRecordTest();
        writeAndReadRecordTest();
        writeAndReadRecordTest();
    }

    private void writeAndReadRecordTest() {
        final CommandStorageRecord expected = newCommandStorageRecord();
        final CommandId id = expected.getContext().getCommandId();
        storage.write(id, expected);

        final CommandStorageRecord actual = storage.read(id);

        assertEquals(expected, actual);
    }

    private static CommandStorageRecord newCommandStorageRecord() {
        final String aggregateIdString = newUuid();
        final AggregateId<String> aggregateId = AggregateId.of(aggregateIdString);
        final Any command = toAny(createProject());
        final TypeName commandType = TypeName.ofEnclosed(command);
        final CommandContext context = TestContextFactory.createCommandContext();
        final CommandStorageRecord.Builder builder = CommandStorageRecord.newBuilder()
                .setTimestamp(getCurrentTime())
                .setCommandType(commandType.nameOnly())
                .setCommandId(context.getCommandId().getUuid())
                .setAggregateIdType(aggregateId.getShortTypeName())
                .setAggregateId(aggregateIdString)
                .setMessage(command)
                .setContext(context);
        return builder.build();
    }
}
