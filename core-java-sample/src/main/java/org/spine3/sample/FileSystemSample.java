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
import org.spine3.sample.server.FileSystemHelper;
import org.spine3.server.storage.StorageFactory;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Throwables.propagate;

/**
 * Entry point for core-java sample without gRPC. Works with FileSystem.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class FileSystemSample extends BaseSample {

    public static final String STORAGE_PATH = '/' + FileSystemSample.class.getClass().getName();

    public static File getTempDir() {
        try {
            File tmpFile = File.createTempFile("temp-dir-check", ".tmp");
            File result = new File(tmpFile.getParent());
            if (tmpFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                tmpFile.delete();
            }
            return result;
        } catch (IOException e) {
            propagate(e);
        }
        throw new IllegalStateException("Unable to get temporary directory for storage");
    }

    @Override
    protected StorageFactory createStorageFactory() {
        return new org.spine3.server.storage.filesystem.FileSystemStorageFactory();
    }

    @Override
    protected Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private FileSystemSample() {
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(FileSystemSample.class);

    }

    public static void main(String[] args) {
        final String tempDir = getTempDir().getAbsolutePath();
        FileSystemHelper.configure(tempDir + STORAGE_PATH);

        BaseSample sample = new FileSystemSample();

        sample.execute();
    }

}
