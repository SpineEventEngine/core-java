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
import org.spine3.server.storage.EntityStorage;
import org.spine3.server.storage.EntityStorageShould;
import org.spine3.test.TestIdWithStringField;

/**
 * File system implementation of {@link EntityStorage} tests.
 *
 * @author Alexander Litus
 */
public class FsEntityStorageShould extends EntityStorageShould {

    private static final FileSystemDepository DEPOSITORY = FileSystemDepository.newInstance(FsEntityStorageShould.class);

    private static final EntityStorage<String, TestIdWithStringField> STORAGE = FsEntityStorage.newInstance(DEPOSITORY);

    public FsEntityStorageShould() {
        super(STORAGE);
    }


    @After
    public void tearDownTest() {
        DEPOSITORY.deleteAll();
    }
}
