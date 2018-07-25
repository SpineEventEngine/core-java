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

package io.spine.testing.server.entity;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.StringValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.server.entity.InvalidEntityStateException;
import io.spine.server.entity.VersionableEntity;
import io.spine.testing.server.User;
import io.spine.testing.server.entity.EntityBuilderTestEnv.TestEntity;
import io.spine.testing.server.entity.EntityBuilderTestEnv.TestEntityBuilder;
import io.spine.testing.server.entity.EntityBuilderTestEnv.UserAggregate;
import io.spine.validate.ConstraintViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Verify.assertSize;
import static io.spine.testing.server.entity.given.Given.aggregateOfClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
@SuppressWarnings({"ConstantConditions" /* Some of the methods test `null` arguments. */,
        "ResultOfMethodCallIgnored" /* We ignore when we test for `null`s. */})
@DisplayName("EntityBuilder should")
class EntityBuilderTest {

    /**
     * Convenience method that mimics the way tests would call creation of an entity.
     */
    private static EntityBuilder<TestEntity, Long, StringValue> givenEntity() {
        EntityBuilder<TestEntity, Long, StringValue> builder = new TestEntityBuilder();
        return builder.setResultClass(TestEntity.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .testAllPublicStaticMethods(EntityBuilder.class);
    }

    @SuppressWarnings("DuplicateStringLiteralInspection") // Common test case.
    @Test
    @DisplayName("not accept null ID")
    void notAcceptNullID() {
        assertThrows(NullPointerException.class, () -> givenEntity().withId(null));
    }

    @Test
    @DisplayName("not accept null state")
    void notAcceptNullState() {
        assertThrows(NullPointerException.class, () -> givenEntity().withState(null));
    }

    @Test
    @DisplayName("not accept null timestamp")
    void notAcceptNullTimestamp() {
        assertThrows(NullPointerException.class, () -> givenEntity().modifiedOn(null));
    }

    @Test
    @DisplayName("obtain entity ID class")
    void getEntityIdClass() {
        assertEquals(Long.class, givenEntity().getIdClass());
    }

    @Test
    @DisplayName("create entity")
    void createEntity() {
        long id = 1024L;
        int version = 100500;
        StringValue state = toMessage(getClass().getName());
        Timestamp timestamp = Time.getCurrentTime();

        VersionableEntity entity = givenEntity()
                .withId(id)
                .withVersion(version)
                .withState(state)
                .modifiedOn(timestamp)
                .build();

        assertEquals(TestEntity.class, entity.getClass());
        assertEquals(id, entity.getId());
    }

    @Test
    @DisplayName("create entity with default values")
    void createWithDefaultValues() {
        VersionableEntity entity = givenEntity().build();

        assertEquals(TestEntity.class, entity.getClass());
        assertEquals(0L, entity.getId());
        assertEquals(toMessage(""), entity.getState());
        assertEquals(0, entity.getVersion().getNumber());
    }

    @SuppressWarnings("CheckReturnValue") // Method called to throw exception.
    @Test
    @DisplayName("throw InvalidEntityStateException if entity state is invalid")
    void throwOnInvalidState() {
        User user = User.newBuilder()
                        .setFirstName("|")
                        .setLastName("|")
                        .build();
        try {
            aggregateOfClass(UserAggregate.class).withId(getClass().getName())
                                                 .withVersion(1)
                                                 .withState(user)
                                                 .build();
            fail("Should have thrown InvalidEntityStateException.");
        } catch (InvalidEntityStateException e) {
            List<ConstraintViolation> violations = e.getError()
                                                    .getValidationError()
                                                    .getConstraintViolationList();
            assertSize(user.getAllFields()
                           .size(), violations);
        }
    }

    @Test
    @DisplayName("update valid entity state")
    void updateEntityState() {
        User user = User.newBuilder()
                        .setFirstName("Fname")
                        .setLastName("Lname")
                        .build();
        UserAggregate aggregate = aggregateOfClass(UserAggregate.class)
                .withId(getClass().getName())
                .withVersion(1)
                .withState(user)
                .build();

        assertEquals(user, aggregate.getState());
    }
}
