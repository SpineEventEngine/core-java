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

package org.spine3.server.storage.filesystem;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spine3.base.CommandRequest;
import org.spine3.server.aggregate.AggregateId;
import org.spine3.test.project.ProjectId;
import org.spine3.util.testutil.CommandRequestFactory;

import static org.spine3.server.storage.filesystem.FileSystemDepository.configure;
import static org.spine3.server.storage.filesystem.FileSystemDepository.deleteAll;

/**
 * File system implementation of {@link org.spine3.server.storage.EventStorage} tests.
 *
 * @author Alexander Litus
 */
@SuppressWarnings({"InstanceMethodNamingConvention", "DuplicateStringLiteralInspection", "ConstantConditions"})
public class FsCommandStorageShould {

    private static final FsCommandStorage STORAGE = (FsCommandStorage) FsCommandStorage.newInstance();

    private static final ProjectId ID = ProjectId.newBuilder().setId("project_id").build();;

    @BeforeClass
    public static void setUp() {
        configure(FsCommandStorageShould.class);
    }

    @Before
    public void setUpTest() {
        deleteAll();
    }

    @AfterClass
    public static void tearDownClass() {
        deleteAll();
    }

    @Test(expected = NullPointerException.class)
    public void throw_exception_if_try_to_write_null() {
        STORAGE.write(null);
    }

    @Test
    public void save_command() {
        final CommandRequest commandRequest = CommandRequestFactory.create();
        STORAGE.store(AggregateId.of(ID), commandRequest);
    }
}
