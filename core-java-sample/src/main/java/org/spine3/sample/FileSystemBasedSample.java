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

package org.spine3.sample;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.server.storage.StorageFactory;
import org.spine3.server.storage.filesystem.FileSystemHelper;
import org.spine3.server.storage.filesystem.FileSystemStorageFactory;

/**
 * Entry point for core-java sample without gRPC. Works with FileSystem.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class FileSystemBasedSample extends BaseSample {

    public static void main(String[] args) {

        FileSystemHelper.configure(FileSystemBasedSample.class);

        BaseSample sample = new FileSystemBasedSample();

        sample.execute();
    }

    @Override
    protected StorageFactory storageFactory() {
        return new FileSystemStorageFactory();
    }

    @Override
    protected Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FileSystemBasedSample.class);

    }

    private FileSystemBasedSample() {}
}
