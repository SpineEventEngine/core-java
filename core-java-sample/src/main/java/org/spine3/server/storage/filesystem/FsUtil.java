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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.spine3.util.FileNameEscaper;
import org.spine3.util.Identifiers;

import java.io.*;
import java.util.Map;

import static com.google.protobuf.Descriptors.FieldDescriptor.JavaType.STRING;
import static java.nio.file.Files.copy;
import static org.spine3.util.IoUtil.flushAndCloseSilently;

/**
 * A utility class which contains common util methods used by file system storages.
 *
 * @author Mikhail Mikhaylov
 * @author Alexander Litus
 */
@SuppressWarnings("UtilityClass")
class FsUtil {

    private FsUtil() {
    }

    /**
     * Writes a {@link Message} into a {@link File} using {@link Message#writeDelimitedTo}.
     *
     * @param file the {@link File} to write data in (gets created if does not exist)
     * @param message the data to write to the file
     */
    @SuppressWarnings({"TypeMayBeWeakened", "ResultOfMethodCallIgnored", "OverlyBroadCatchBlock"})
    public static void writeMessage(File file, Message message) {

        FileOutputStream fileOutputStream = null;
        OutputStream bufferedOutputStream = null;
        File backup = null;

        try {
            if (file.exists()) {
                backup = makeBackupCopy(file);
            } else {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            fileOutputStream = new FileOutputStream(file, true);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            message.writeDelimitedTo(bufferedOutputStream);

            if (backup != null) {
                backup.delete();
            }
        } catch (IOException ignored) {
            // restore from backup
            final boolean isDeleted = file.delete();
            if (isDeleted && backup != null) {
                //noinspection ResultOfMethodCallIgnored
                backup.renameTo(file);
            }
        } finally {
            flushAndCloseSilently(fileOutputStream, bufferedOutputStream);
        }
    }

    private static File makeBackupCopy(File sourceFile) throws IOException {

        final String backupFilePath = sourceFile.toPath() + "_backup";
        final File backupFile = new File(backupFilePath);

        copy(sourceFile.toPath(), backupFile.toPath());

        return backupFile;
    }

    /**
     * Creates string representation of the passed ID escaping characters
     * that are not allowed in file names.
     *
     * @param id the ID to convert
     * @return string representation of the ID
     * @see Identifiers#idToString(Object)
     * @see FileNameEscaper#escape(String)
     */
    public static <I> String idToStringWithEscaping(I id) {

        final I idNormalized = escapeStringFieldsIfIsMessage(id);
        String result = Identifiers.idToString(idNormalized);
        result = FileNameEscaper.getInstance().escape(result);

        return result;
    }

    private static <I> I escapeStringFieldsIfIsMessage(I input) {
        I result = input;
        if (input instanceof Message) {
            final Message message = escapeStringFields((Message) input);
            @SuppressWarnings("unchecked") final
            I castedMessage = (I) message; // cast is safe because input is Message
            result = castedMessage;
        }
        return result;
    }

    private static Message escapeStringFields(Message message) {

        final Message.Builder result = message.toBuilder();
        final Map<Descriptors.FieldDescriptor, Object> fields = message.getAllFields();

        final FileNameEscaper escaper = FileNameEscaper.getInstance();

        for (Descriptors.FieldDescriptor descriptor : fields.keySet()) {

            Object value = fields.get(descriptor);

            if (descriptor.getJavaType() == STRING) {
                value = escaper.escape(value.toString());
            }

            result.setField(descriptor, value);
        }

        return result.build();
    }
}
