/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package io.spine.util;

import com.google.protobuf.Descriptors.FileDescriptor;
import io.spine.annotation.Internal;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for Spine-specific files.
 *
 * @author Alexander Yevsyukov
 */
@Internal
public class CodeLayout {

    private static final String COMMANDS_FILE_SUFFIX = "commands";
    private static final char EXTENSION_SEPARATOR = '.';
    private static final char PATH_SEPARATOR = '/';

    private CodeLayout() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Checks if the file is for commands.
     *
     * @param file a descriptor of a {@code .proto} file to check
     * @return {@code true} if the file name ends with {@code "commands"},
     * {@code false} otherwise
     */
    public static boolean isCommandsFile(FileDescriptor file) {
        checkNotNull(file);

        final String fqn = file.getName();
        final int startIndexOfFileName = fqn.lastIndexOf(PATH_SEPARATOR) + 1;
        final int endIndexOfFileName = fqn.lastIndexOf(EXTENSION_SEPARATOR);
        final String fileName = fqn.substring(startIndexOfFileName, endIndexOfFileName);
        final boolean isCommandsFile = fileName.endsWith(COMMANDS_FILE_SUFFIX);
        return isCommandsFile;
    }
}
