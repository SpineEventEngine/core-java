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

import com.google.common.base.Strings;
import org.spine3.sample.server.FileSystemHelper;
import org.spine3.server.aggregate.AggregateId;

import java.io.File;

/**
 * @author Mikhail Mikhaylov
 */
public class Helper {

    @SuppressWarnings("StaticNonFinalField")
    private static String fileStoragePath = null;
    private static final String COMMAND_STORE_FILE_NAME = "/command-store";
    private static final String AGGREGATE_FILE_NAME_PREFIX = "/aggregate/";
    private static final String PATH_DELIMITER = "/";

    private Helper() {
    }

    /**
     * Configures helper with file storage path.
     *
     * @param storagePath file storage path
     */
    public static void configure(String storagePath) {
        fileStoragePath = storagePath;
    }

    public static String getAggregateFilePath(String aggregateType, String aggregateIdString) {
        checkConfigured();

        final String filePath = fileStoragePath + AGGREGATE_FILE_NAME_PREFIX +
                aggregateType + PATH_DELIMITER + aggregateIdString;
        return filePath;
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    private static void checkConfigured() {
        if (Strings.isNullOrEmpty(fileStoragePath)) {
            throw new IllegalStateException(FileSystemHelper.STORAGE_PATH_IS_NOT_SET);
        }
    }
}
