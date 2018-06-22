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

package io.spine.server.entity;

import com.google.protobuf.StringValue;
import io.spine.core.Version;
import io.spine.core.Versions;
import io.spine.server.entity.given.EntityTestEnv.BareBonesEntity;
import io.spine.server.entity.given.EntityTestEnv.EntityWithMessageId;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdInteger;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdLong;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdMessage;
import io.spine.server.entity.given.EntityTestEnv.TestEntityWithIdString;
import io.spine.test.Tests;
import io.spine.test.TimeTests;
import io.spine.test.entity.Project;
import io.spine.testdata.Sample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Time.getCurrentTime;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.server.entity.given.EntityTestEnv.isBetween;
import static io.spine.test.Tests.assertSecondsEqual;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Litus
 */
@DisplayName("Entity should")
class EntityTest {

    private Project state = Sample.messageOfType(Project.class);
    private TestEntity entityNew;
    private TestEntity entityWithState;

    @BeforeEach
    void setUp() {
        state = Sample.messageOfType(Project.class);
        entityNew = TestEntity.newInstance(newUuid());
        entityWithState = TestEntity.withState();
    }

    @SuppressWarnings("ResultOfObjectAllocationIgnored") // Because we expect the exception.
    @Test
    @DisplayName("not accept null ID")
    void notAcceptNullId() {
        assertThrows(NullPointerException.class, () -> new BareBonesEntity(Tests.<Long>nullRef()));
    }

    @Test
    @DisplayName("return default state")
    void returnDefaultState() {
        final Project state = entityNew.getDefaultState();
        assertEquals(Project.getDefaultInstance(), state);
    }

    @Test
    @DisplayName("return default state for different entities")
    void returnDefaultStateForDifferentEntities() {
        assertEquals(Project.getDefaultInstance(), entityNew.getDefaultState());

        final EntityWithMessageId entityWithMessageId = new EntityWithMessageId();
        final StringValue expected = StringValue.getDefaultInstance();
        assertEquals(expected, entityWithMessageId.getDefaultState());
    }

    @Test
    @DisplayName("accept String ID to constructor")
    void acceptStringIdToConstructor() {
        final String stringId = "stringId";
        final TestEntityWithIdString entityWithStringId = new TestEntityWithIdString(stringId);

        assertEquals(stringId, entityWithStringId.getId());
    }

    @Test
    @DisplayName("accept Long ID to constructor")
    void acceptLongIdToConstructor() {
        final Long longId = 12L;
        final TestEntityWithIdLong entityWithLongId = new TestEntityWithIdLong(longId);

        assertEquals(longId, entityWithLongId.getId());
    }

    @Test
    @DisplayName("accept Integer ID to constructor")
    void acceptIntegerIdToConstructor() {
        final Integer integerId = 12;
        final TestEntityWithIdInteger entityWithIntegerId = new TestEntityWithIdInteger(integerId);

        assertEquals(integerId, entityWithIntegerId.getId());
    }

    @Test
    @DisplayName("accept Message ID to constructor")
    void acceptMessageIdToConstructor() {
        final StringValue messageId = toMessage("messageId");
        final TestEntityWithIdMessage entityWithMessageID = new TestEntityWithIdMessage(messageId);

        assertEquals(messageId, entityWithMessageID.getId());
    }

    @Test
    @DisplayName("have state")
    void haveState() {
        final Version ver = Versions.newVersion(3, getCurrentTime());

        entityNew.updateState(state, ver);

        assertEquals(state, entityNew.getState());
        assertEquals(ver, entityNew.getVersion());
    }

    @Test
    @DisplayName("check entity state when setting it")
    void checkEntityStateWhenSetIt() {
        final TestEntity spyEntityNew = spy(entityNew);
        spyEntityNew.updateState(state, Versions.zero());
        verify(spyEntityNew).checkEntityState(eq(state));
    }

    @Test
    @DisplayName("throw exception if try to set null state")
    void throwExceptionIfTryToSetNullState() {
        assertThrows(NullPointerException.class,
                     () -> entityNew.updateState(Tests.nullRef(), Versions.zero()));
    }

    @Test
    @DisplayName("have zero version by default")
    void haveZeroVersionByDefault() {
        assertEquals(0, entityNew.getVersion()
                                 .getNumber());
    }

    @Test
    @DisplayName("increment version by one")
    void incrementVersionByOne() {
        final int version = entityNew.incrementVersion();
        assertEquals(1, version);
    }

    @SuppressWarnings("CheckReturnValue") // New entity version number can be ignored in this test.
    @Test
    @DisplayName("record modification time when incrementing version")
    void recordModificationTimeOnVersionIncrement() {
        final long timeBeforeincrement = TimeTests.currentTimeSeconds();
        entityNew.incrementVersion();
        final long timeAfterIncrement = TimeTests.currentTimeSeconds();
        assertThat(entityNew.whenModified()
                            .getSeconds(),
                   isBetween(timeBeforeincrement, timeAfterIncrement));
    }

    @Test
    @DisplayName("update state")
    void updateState() {
        entityNew.incrementState(state);

        assertEquals(state, entityNew.getState());
    }

    @Test
    @DisplayName("increment version when updating state")
    void incrementVersionOnStateUpdate() {
        entityNew.incrementState(state);

        assertEquals(1, entityNew.getVersion()
                                 .getNumber());
    }

    @Test
    @DisplayName("record modification time when updating state")
    void recordModificationTimeOnStateUpdate() {
        entityNew.incrementState(state);
        final long expectedTimeSec = TimeTests.currentTimeSeconds();

        assertSecondsEqual(expectedTimeSec, entityNew.whenModified()
                                                     .getSeconds(), 1);
    }

    @Test
    @DisplayName("generate non-zero hash code if entity has non empty id and state")
    void generateNonZeroHashCodeIfEntityHasNonEmptyIdAndState() {
        assertFalse(entityWithState.getId()
                                   .trim()
                                   .isEmpty());

        final int hashCode = entityWithState.hashCode();

        assertTrue(hashCode != 0);
    }

    @Test
    @DisplayName("generate same hash code for one instance")
    void generateSameHashCodeForOneInstance() {
        assertEquals(entityWithState.hashCode(), entityWithState.hashCode());
    }

    @Test
    @DisplayName("generate unique hash code for different instances")
    void generateUniqueHashCodeForDifferentInstances() {
        final TestEntity another = TestEntity.withState();

        assertNotEquals(entityWithState.hashCode(), another.hashCode());
    }
}

