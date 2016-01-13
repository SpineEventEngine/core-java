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

package org.spine3.server;

import org.junit.Test;
import org.spine3.test.project.Project;
import org.spine3.test.project.ProjectId;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Litus
 */
@SuppressWarnings("InstanceMethodNamingConvention")
public class RepositoryBaseShould {

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // OK in this case
    public void throw_exception_if_entity_constructor_is_protected() {
        try {
            new RepositoryForEntitiesWithProtectedConstructor();
        } catch (RuntimeException e) {
            assertEquals(e.getCause().getClass(), IllegalAccessException.class);
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // OK in this case
    public void throw_exception_if_entity_constructor_is_private() {
        try {
            new RepositoryForEntitiesWithPrivateConstructor();
        } catch (RuntimeException e) {
            assertEquals(e.getCause().getClass(), IllegalAccessException.class);
        }
    }

    @Test
    @SuppressWarnings("ResultOfObjectAllocationIgnored") // OK in this case
    public void throw_exception_if_entity_has_no_required_constructor() {
        try {
            new RepositoryForEntitiesWithoutRequiredConstructor();
        } catch (RuntimeException e) {
            assertEquals(e.getCause().getClass(), NoSuchMethodException.class);
        }
    }

    public static class RepositoryForEntitiesWithProtectedConstructor extends RepositoryBase<ProjectId, EntityWithProtectedConstructor> {
        @Override
        protected void checkStorageClass(Object storage) {
        }
        @Override
        public void store(EntityWithProtectedConstructor obj) {
        }
        @Nullable
        @Override
        public EntityWithProtectedConstructor load(ProjectId id) {
            return null;
        }
    }

    public static class EntityWithProtectedConstructor extends Entity<ProjectId, Project> {

        protected EntityWithProtectedConstructor(ProjectId id) {
            super(id);
        }
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class RepositoryForEntitiesWithPrivateConstructor extends RepositoryBase<ProjectId, EntityWithPrivateConstructor> {
        @Override
        protected void checkStorageClass(Object storage) {
        }
        @Override
        public void store(EntityWithPrivateConstructor obj) {
        }
        @Nullable
        @Override
        public EntityWithPrivateConstructor load(ProjectId id) {
            return null;
        }
    }

    public static class EntityWithPrivateConstructor extends Entity<ProjectId, Project> {

        private EntityWithPrivateConstructor(ProjectId id) {
            super(id);
        }
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }

    public static class RepositoryForEntitiesWithoutRequiredConstructor extends RepositoryBase<ProjectId, EntityWithoutRequiredConstructor> {
        @Override
        protected void checkStorageClass(Object storage) {
        }
        @Override
        public void store(EntityWithoutRequiredConstructor obj) {
        }
        @Nullable
        @Override
        public EntityWithoutRequiredConstructor load(ProjectId id) {
            return null;
        }
    }

    public static class EntityWithoutRequiredConstructor extends Entity<ProjectId, Project> {

        private EntityWithoutRequiredConstructor() {
            super(ProjectId.getDefaultInstance());
        }
        @Override
        protected Project getDefaultState() {
            return Project.getDefaultInstance();
        }
    }
}
