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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.spine3.base.EventRecord;
import org.spine3.server.storage.EventStorageShould;
import org.spine3.server.storage.EventStoreRecord;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.spine3.server.storage.filesystem.FileSystemHelper.configure;
import static org.spine3.server.storage.filesystem.FileSystemHelper.deleteAll;
import static org.spine3.util.Events.toEventRecord;
import static org.spine3.util.testutil.EventStoreRecordFactory.projectCreated;

/**
 * File system implementation of {@link org.spine3.server.storage.EventStorage} tests.
 *
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class FileSystemEventStorageShould extends EventStorageShould {

    private static final FsEventStorage STORAGE = (FsEventStorage) FsEventStorage.newInstance();

    public FileSystemEventStorageShould() {
        super(STORAGE);
    }

    @Before
    public void setUpTest() {
        configure(FileSystemEventStorageShould.class);
    }

    @After
    public void tearDownTest() {
        STORAGE.releaseResources();
        deleteAll();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void throw_exception_if_try_to_remove_element_via_iterator() {

        final Iterator<EventRecord> iterator = STORAGE.allEvents();
        iterator.remove();
    }

    @Test(expected = NoSuchElementException.class)
    public void throw_exception_if_iteration_has_no_more_elements() {

        final EventStoreRecord recordToStore = projectCreated();
        final EventRecord expected = toEventRecord(recordToStore);
        STORAGE.write(recordToStore);

        final Iterator<EventRecord> iterator = STORAGE.allEvents();

        assertTrue(iterator.hasNext());

        final EventRecord actual = iterator.next();

        assertEventRecordsAreEqual(expected, actual);
        assertFalse(iterator.hasNext());

        iterator.next();
    }
}
