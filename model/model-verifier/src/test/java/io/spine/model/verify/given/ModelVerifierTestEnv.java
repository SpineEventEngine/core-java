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

import io.spine.core.EventContext;
import io.spine.core.Events;
import io.spine.core.UserId;
import io.spine.core.UserIdVBuilder;
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
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;

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

    public static class EditAggregate extends Aggregate<String, UserId, UserIdVBuilder> {

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

        @SuppressWarnings("unused") // Just an event context is used to fetch the editor.
        @Apply
        private void on(PhotoEdited event, EventContext context) {
            getBuilder().mergeFrom(Events.getActor(context));
        }
    }

    public static class RenameProcMan extends ProcessManager<String, UserId, UserIdVBuilder> {

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

        @SuppressWarnings("unused") // Just an event context is used to fetch the editor.
        @Apply
        private void on(TitleChanged event, EventContext context) {
            getBuilder().mergeFrom(Events.getActor(context));
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
