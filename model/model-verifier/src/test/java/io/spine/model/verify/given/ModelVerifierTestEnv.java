/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.model.verify.given;

import com.google.protobuf.Any;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.command.Assign;
import io.spine.server.event.EventBus;
import io.spine.server.procman.ProcessManager;
import io.spine.test.model.verify.ChangeTitle;
import io.spine.test.model.verify.EditPhoto;
import io.spine.test.model.verify.PhotoEdited;
import io.spine.test.model.verify.PhotoUploaded;
import io.spine.test.model.verify.TitleChanged;
import io.spine.test.model.verify.UploadPhoto;
import io.spine.validate.AnyVBuilder;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

import static com.google.protobuf.Any.pack;

/**
 * @author Dmytro Dashenkov
 */
public final class ModelVerifierTestEnv {

    /** Prevents instantiation of this utility class. */
    private ModelVerifierTestEnv() {
    }

    public static Project actualProject() {
        Project result = ProjectBuilder.builder().build();
        result.getPluginManager().apply("java");
        return result;
    }

    public static class UploadCommandHandler extends AbstractCommandHandler {

        protected UploadCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        PhotoUploaded handle(UploadPhoto command) {
            return PhotoUploaded
                    .newBuilder()
                    .setPhoto(command.getPhoto())
                    .build();
        }
    }

    public static class EditAggregate extends Aggregate<String, Any, AnyVBuilder> {

        protected EditAggregate(String id) {
            super(id);
        }

        @Assign
        PhotoEdited handle(EditPhoto command) {
            return PhotoEdited
                    .newBuilder()
                    .setNewPhoto(command.getNewPhoto())
                    .build();
        }

        @Apply
        private void on(PhotoEdited event) {
            getBuilder().setOriginalState(pack(event));
        }
    }

    public static class RenameProcMan extends ProcessManager<String, Any, AnyVBuilder> {

        protected RenameProcMan(String id) {
            super(id);
        }

        @Assign
        TitleChanged handle(ChangeTitle command) {
            return TitleChanged
                    .newBuilder()
                    .setNewTitle(command.getNewTitle())
                    .build();
        }

        @Apply
        private void on(TitleChanged event) {
            getBuilder().setOriginalState(pack(event));
        }
    }

    public static class DuplicateCommandHandler extends AbstractCommandHandler {

        protected DuplicateCommandHandler(EventBus eventBus) {
            super(eventBus);
        }

        @Assign
        PhotoUploaded handle(UploadPhoto command) {
            return PhotoUploaded
                    .newBuilder()
                    .setPhoto(command.getPhoto())
                    .build();
        }
    }
}
