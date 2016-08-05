/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.entity;

import com.google.protobuf.Timestamp;
import org.spine3.test.entity.Project;
import org.spine3.test.entity.ProjectId;
import org.spine3.test.entity.command.CreateProject;

import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.protobuf.Timestamps.getCurrentTime;

/* package */ class Given {

    private Given() {}

    /* package */ static Project newProject() {
        final Project.Builder project = Project.newBuilder()
                                               .setId(AggregateId.newProjectId())
                                               .setName("Project-" + newUuid())
                                               .setStatus(Project.Status.CREATED);
        return project.build();
    }

    /* package */ static class AggregateId {

        private AggregateId() {}

        /* package */ static ProjectId newProjectId() {
            final String uuid = newUuid();
            return ProjectId.newBuilder()
                            .setId(uuid)
                            .build();
        }
    }

    /* package */ static class CommandMessage {

        private CommandMessage() {}

        /* package */ static CreateProject createProject() {
            return CreateProject.newBuilder()
                                .setProjectId(AggregateId.newProjectId())
                                .build();
        }
    }

    /**
     * Extracted from {@link org.spine3.server.entity.EntityShould}
     *
     * @author Mikhail Mikhaylov
     */
    /* package */ static class TestEntity extends Entity<String, Project> {

        private boolean isValidateMethodCalled = false;

        /* package */ static TestEntity newInstance(String id) {
            final TestEntity entity = new TestEntity(id);
            return entity;
        }

        /* package */ static TestEntity withState() {
            final Project state = newProject();
            final int version = 3;
            final Timestamp whenModified = getCurrentTime();
            final TestEntity entity = new TestEntity(newUuid());
            entity.setState(state, version, whenModified);
            return entity;
        }

        /* package */ static TestEntity withState(TestEntity entity) {
            final TestEntity result = new TestEntity(entity.getId());
            result.setState(entity.getState(), entity.getVersion(), entity.whenModified());
            return result;
        }

        private TestEntity(String id) {
            super(id);
        }

        @Override
        protected void validate(Project state) throws IllegalStateException {
            super.validate(state);
            isValidateMethodCalled = true;
        }

        /* package */ boolean isValidateMethodCalled() {
            return isValidateMethodCalled;
        }
    }
}
