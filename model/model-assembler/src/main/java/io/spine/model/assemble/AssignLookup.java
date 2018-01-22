/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.model.assemble;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ProtocolStringList;
import io.spine.annotation.Internal;
import io.spine.model.CommandHandlers;
import io.spine.server.command.Assign;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.io.Files.createParentDirs;
import static io.spine.validate.Validate.isDefault;

/**
 * An annotation processor for the {@link Assign @Assign} annotation.
 *
 * <p>Collects the types which contain command handler methods (marked with {@code @Assign}
 * annotation) and writes them into the {@code ${spineDirRoot}/.spine/spine_model.ser} file, where
 * "{@code spineDirRoot}" is the value of the <b>spineDirRoot</b> annotator option.
 *
 * <p><b>spineDirRoot</b> is the only supported option of the processor.
 * Use {@code javac -AspineDirRoot=/path/to/project/root [...]} to set the value of the option.
 * If none is set, the option will default to current directory (denoted with "{@code ./}").
 *
 * @author Dmytro Dashenkov
 */
public class AssignLookup extends SpineAnnotationProcessor {

    @Internal
    public static final String DESTINATION_PATH = ".spine/spine_model.ser";
    @VisibleForTesting
    static final String OUTPUT_OPTION_NAME = "spineDirRoot";
    private static final String DEFAULT_OUTPUT_OPTION = ".";

    private final CommandHandlers.Builder commandHandlers = CommandHandlers.newBuilder();

    @Override
    protected Class<? extends Annotation> getAnnotationType() {
        return Assign.class;
    }

    @Override
    public Set<String> getSupportedOptions() {
        final Set<String> result = newHashSet(super.getSupportedOptions());
        result.add(OUTPUT_OPTION_NAME);
        return result;
    }

    @Override
    protected void processElement(Element element) {
        final TypeElement enclosingTypeElement = (TypeElement) element.getEnclosingElement();
        final String typeName = enclosingTypeElement.getQualifiedName().toString();
        commandHandlers.addCommandHandlingTypes(typeName);
    }

    @Override
    protected void onRoundFinished() {
        final String spineOutput = getOption(OUTPUT_OPTION_NAME).or(DEFAULT_OUTPUT_OPTION);
        final String fileName = spineOutput + '/' + DESTINATION_PATH;
        final File serializedModelStorage = new File(fileName);
        mergeOldHandlersFrom(serializedModelStorage);
        writeHandlersTo(serializedModelStorage);
    }

    /**
     * Merges the currently built {@linkplain CommandHandlers commandHandlers}
     * with the pre-built one.
     *
     * <p>If the file exists and is not empty, the message of type {@link CommandHandlers} is
     * read from it and merged with the current commandHandlers by the rules of
     * {@link com.google.protobuf.Message.Builder#mergeFrom(com.google.protobuf.Message)
     * Message.Builder.mergeFrom()}.
     *
     * @param file the file which may or may not contain the pre-assembled commandHandlers
     */
    private void mergeOldHandlersFrom(File file) {
        final boolean fileWithData = existsNonEmpty(file);
        if (fileWithData) {
            final CommandHandlers preexistingModel = readExisting(file);
            commandHandlers.mergeFrom(preexistingModel);
        }
    }

    /**
     * Writes the {@link CommandHandlers} to the given file.
     *
     * <p>If the given file does not exist, this method creates it.
     *
     * <p>The written commandHandlers will be cleaned from duplications in the repeated fields.
     *
     * <p>The I/O errors are handled by rethrowing them as {@link IllegalStateException}.
     *
     * @param file an existing file to write the commandHandlers into
     */
    private void writeHandlersTo(File file) {
        ensureFile(file);
        removeDuplicates();
        final CommandHandlers serializedModel = commandHandlers.build();
        if (!isDefault(serializedModel)) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                serializedModel.writeTo(out);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Cleans the currently built commandHandlers from the duplicates.
     *
     * <p>Calling this method will cause the {@linkplain #commandHandlers current commandHandlers}
     * not to contain duplicate entries in any {@code repeated} field.
     */
    private void removeDuplicates() {
        final ProtocolStringList handlingTypesList = commandHandlers.getCommandHandlingTypesList();
        final Set<String> commandHandlingTypes = newTreeSet(handlingTypesList);
        commandHandlers.clearCommandHandlingTypes()
                       .addAllCommandHandlingTypes(commandHandlingTypes);
    }

    /**
     * Ensures the given file existence.
     *
     * <p>Performs no action if the given file {@linkplain File#exists() exists}.
     *
     * <p>If the given file does not exist, it is created (with the parent dirs if required).
     *
     * @param file a file to create
     */
    private static void ensureFile(File file) {
        try {
            if (!file.exists()) {
                createParentDirs(file);
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean existsNonEmpty(File file) {
        return file.exists() && file.length() > 0;
    }

    /**
     * Reads the existing {@link CommandHandlers} from the given file.
     *
     * <p>The given file should exist.
     *
     * <p>If the given file is empty,
     * the {@link CommandHandlers#getDefaultInstance() CommandHandlers.getDefaultInstance()} is
     * returned.
     *
     * @param file an existing file with a {@link CommandHandlers} message
     * @return the read commandHandlers
     */
    private static CommandHandlers readExisting(File file) {
        if (file.length() == 0) {
            return CommandHandlers.getDefaultInstance();
        } else {
            try (InputStream in = new FileInputStream(file)) {
                final CommandHandlers preexistingModel = CommandHandlers.parseFrom(in);
                return preexistingModel;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    protected boolean isFinished() {
        return true;
    }
}
