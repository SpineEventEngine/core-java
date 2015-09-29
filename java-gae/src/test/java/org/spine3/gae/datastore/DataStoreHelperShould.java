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

package org.spine3.gae.datastore;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spine3.TypeName;
import org.spine3.base.CommandRequest;

@SuppressWarnings("InstanceMethodNamingConvention")
public class DataStoreHelperShould {


    private static final String SOME_RANDOM_STRING = "123qwe";

    private static final LocalServiceTestHelper testHelper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    private static final DataStoreHelperOldImpl dataStoreHelper = new DataStoreHelperOldImpl();

    private final TypeName commandTypeName = TypeName.of(CommandRequest.getDescriptor());

    @BeforeClass
    public static void setUp() {
        testHelper.setUp();
    }

    @AfterClass
    public static void tearDown() {
        testHelper.tearDown();
    }

    //todo:2015-08-17:mikhail.mikhaylov: resolve testsources dependency
//    @Test
//    public void write_correctly() {
//        final CommandRequest commandRequest = CommandRequestFactory.create();
//        final String stringId = JsonFormat.printToString(commandRequest.getContext().getCommandId());
//
//        dataStoreHelper.put(Converters.convert(commandRequest));
//        final Message readCommandRequest = dataStoreHelper.read(commandTypeName, stringId);
//        assertEquals(commandRequest, readCommandRequest);
//    }
//
//    @Test
//    public void read_all() {
//        final CommandRequest commandRequest1 = CommandRequestFactory.create();
//        final CommandRequest commandRequest2 = CommandRequestFactory.create();
//
//        dataStoreHelper.put(Converters.convert(commandRequest1));
//        dataStoreHelper.put(Converters.convert(commandRequest2));
//
//        final List<Message> readCommands = dataStoreHelper.read(commandTypeName.toString());
//
//        assertTrue(readCommands.contains(commandRequest1));
//        assertTrue(readCommands.contains(commandRequest2));
//    }

    @Test(expected = MissingEntityException.class)
    public void fail_read_on_wrong_key() {
        dataStoreHelper.read(commandTypeName, SOME_RANDOM_STRING);
    }

    @Test
    public void read_by_filter() {
        ////todo:2015-08-10:mikhail.mikhaylov: implement
    }
}