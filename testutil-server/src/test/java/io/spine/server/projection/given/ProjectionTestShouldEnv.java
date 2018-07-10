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

package io.spine.server.projection.given;

import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.core.Subscribe;
import io.spine.server.entity.Repository;
import io.spine.server.entity.given.Given;
import io.spine.server.projection.Projection;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.projection.ProjectionTest;
import io.spine.validate.StringValueVBuilder;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Vladyslav Lubenskyi
 */
public class ProjectionTestShouldEnv {

    private static final long ID = 1L;

    /**
     * Prevents direct instantiation.
     */
    private ProjectionTestShouldEnv() {
    }

    public static TestProjection projection() {
        TestProjection result =
                Given.projectionOfClass(TestProjection.class)
                     .withId(ID)
                     .withVersion(64)
                     .build();
        return result;
    }

    /**
     * A dummy projection that is subscribed to a {@code StringValue} event.
     */
    public static final class TestProjection
            extends Projection<Long, StringValue, StringValueVBuilder> {

        TestProjection(Long id) {
            super(id);
        }

        @Subscribe
        public void on(StringValue command) {
            getBuilder().setValue(command.getValue());
        }
    }

    private static class TestProjectionRepository
            extends ProjectionRepository<Long, TestProjection, StringValue> {
    }

    /**
     * The test class for the {@code StringValue} event handler in {@code TestProjection}.
     */
    public static class TestProjectionTest
            extends ProjectionTest<Long, StringValue, StringValue, TestProjection> {

        public static final StringValue TEST_EVENT = StringValue.newBuilder()
                                                                .setValue("test projection event")
                                                                .build();

        @BeforeEach
        @Override
        public void setUp() {
            super.setUp();
        }

        @Override
        protected Long newId() {
            return ID;
        }

        @Override
        protected StringValue createMessage() {
            return TEST_EVENT;
        }

        @Override
        public Expected<StringValue> expectThat(TestProjection entity) {
            return super.expectThat(entity);
        }

        @Override
        protected Repository<Long, TestProjection> createEntityRepository() {
            return new TestProjectionRepository();
        }

        public Message storedMessage() {
            return message();
        }

        /**
         * Exposes internal configuration method.
         */
        public void init() {
            configureBoundedContext();
        }
    }
}
