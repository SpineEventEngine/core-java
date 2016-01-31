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

package org.spine3.server.reflect;

import org.junit.Test;
import org.spine3.base.CommandContext;
import org.spine3.server.Assign;
import org.spine3.server.CommandHandler;
import org.spine3.server.aggregate.Aggregate;
import org.spine3.test.project.Project;
import org.spine3.test.project.command.CreateProject;
import org.spine3.test.project.event.ProjectCreated;

import static org.junit.Assert.assertFalse;

public class MethodMapShould {

    /**
     * Test aggregate in which methods are scanned.
     */
    private static final class TestAggregate extends Aggregate<Long, Project> {

        public TestAggregate(Long id) {
            super(id);
        }

        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }

        @Assign
        public ProjectCreated handle(CreateProject command, CommandContext context) {
            return ProjectCreated.getDefaultInstance();
        }
    }

    @Test
    public void expose_key_set() {
        final MethodMap methodMap = new MethodMap(TestAggregate.class, CommandHandler.METHOD_PREDICATE);
        assertFalse(methodMap.keySet().isEmpty());
    }
}
