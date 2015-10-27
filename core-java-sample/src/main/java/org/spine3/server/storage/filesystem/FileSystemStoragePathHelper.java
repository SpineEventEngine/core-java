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

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Throwables.propagate;

//TODO:2015-10-27:alexander.yevsyukov: Refactor to move these methods where they really belong.

/*
 * Provides with file system storage paths.
 *
 * @author Alexander Litus
 */
@SuppressWarnings({"DuplicateStringLiteralInspection", "UtilityClass"})
class FileSystemStoragePathHelper {

    protected static final String NOT_CONFIGURED_MESSAGE = "Helper is not configured. Call 'configure' method.";
    protected static final String COMMAND_STORE_FILE_NAME = "/command-store";
    protected static final String EVENT_STORE_FILE_NAME = "/event-store";
    private static final String AGGREGATE_FILE_NAME_PREFIX = "/aggregate/";
    protected static final String PATH_DELIMITER = "/";
    private static final String ENTITY_STORE_DIR = "/entity-store/";

    //TODO:2015-10-27:alexander.yevsyukov: What would happen if we want to use this class with two executors?
    @SuppressWarnings("StaticNonFinalField")
    private static String fileStorePath = null;

    @SuppressWarnings("StaticNonFinalField")
    private static String commandStoreFilePath = null;

    @SuppressWarnings("StaticNonFinalField")
    private static String eventStoreFilePath = null;


    private FileSystemStoragePathHelper() {}

    /**
     * Configures helper with file storage path.
     *
     * @param executorClass execution context class. Is used to choose target directory for storage.
     */
    protected static void configure(Class executorClass) {

        final String tempDir = getTempDir().getAbsolutePath();

        fileStorePath = tempDir + PATH_DELIMITER + executorClass.getSimpleName();
        commandStoreFilePath = fileStorePath + COMMAND_STORE_FILE_NAME;
        eventStoreFilePath = fileStorePath + EVENT_STORE_FILE_NAME;
    }

    protected static String getFileStorePath() {
        return fileStorePath;
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

        final String filePath = fileStorePath + AGGREGATE_FILE_NAME_PREFIX +
                aggregateType + PATH_DELIMITER + aggregateIdString;
        return filePath;
    }

    /**
     * Returns common command store file path.
     *
     * @return absolute path string
     */
    protected static String getCommandStoreFilePath() {
        checkConfigured();
        return commandStoreFilePath;
    }

    /**
     * Returns common event store file path.
     *
     * @return absolute path string
     */
    protected static String getEventStoreFilePath() {
        checkConfigured();
        return eventStoreFilePath;
    }

    protected static String getEntityStoreFilePath(String entityId) {
        checkConfigured();
        final String filePath = fileStorePath + ENTITY_STORE_DIR + entityId;
        return filePath;
    }

    protected static String getBackupFilePath(File sourceFile) {
        checkConfigured();
        final String path = sourceFile.toPath() + "_backup";
        return path;
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    protected static void checkConfigured() {
        if (isNullOrEmpty(fileStorePath)) {
            throw new IllegalStateException(NOT_CONFIGURED_MESSAGE);
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
