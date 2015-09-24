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

import org.spine3.server.Entity;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.propagate;
import static org.spine3.util.Identifiers.idToString;

/*
 * Provides with file system storage paths
 */
@SuppressWarnings("DuplicateStringLiteralInspection")
class FileSystemStoragePathHelper {

    protected static final String STORAGE_PATH_IS_NOT_SET = "Storage path is not set.";
    protected static final String COMMAND_STORE_FILE_NAME = "/command-store";
    protected static final String EVENT_STORE_FILE_NAME = "/event-store";
    private static final String AGGREGATE_FILE_NAME_PREFIX = "/aggregate/";
    protected static final String PATH_DELIMITER = "/";
    private static final String ENTITY_STORE_DIR = "/entity-store/";
    @SuppressWarnings("StaticNonFinalField")
    private static String fileStoragePath = null;

    private FileSystemStoragePathHelper() {}

    protected static String getFileStoragePath() {
        return fileStoragePath;
    }

    protected static void configure(Class executorClass) {
        final String tempDir = getTempDir().getAbsolutePath();

        fileStoragePath = tempDir + PATH_DELIMITER + executorClass.getSimpleName();
    }

    /**
     * Returns path for aggregate storage file.
     *
     * @param aggregateType     the type of an aggregate
     * @param aggregateIdString String representation of aggregate id
     * @return absolute path string
     */
    protected static String getAggregateFilePath(String aggregateType, String aggregateIdString) {
        checkConfigured();

        final String filePath = fileStoragePath + AGGREGATE_FILE_NAME_PREFIX +
                aggregateType + PATH_DELIMITER + aggregateIdString;
        return filePath;
    }

    protected static String getCommandStoreFilePath() {
        checkConfigured();
        return fileStoragePath + COMMAND_STORE_FILE_NAME;
    }

    /**
     * Returns common event store file path.
     *
     * @return absolute path string
     */
    protected static String getEventStoreFilePath() {
        checkConfigured();
        return fileStoragePath + EVENT_STORE_FILE_NAME;
    }

    protected static String getEntityFilePath(Entity entity) {
        checkConfigured();
        checkNotNull(entity.getId(), "Entity id shouldn't be null");
        final String idString = idToString(entity.getId());
        final String filePath = fileStoragePath + ENTITY_STORE_DIR + idString;
        return filePath;
    }

    protected static String getBackupFilePath(File sourceFile) {
        checkConfigured();
        final String path = sourceFile.toPath() + "_backup";
        return path;
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    protected static void checkConfigured() {
        if (isNullOrEmpty(fileStoragePath)) {
            throw new IllegalStateException(STORAGE_PATH_IS_NOT_SET);
        }
    }

    private static File getTempDir() {
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
}
