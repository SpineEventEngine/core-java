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

import io.spine.test.entity.Project;
import io.spine.testdata.Sample;
import org.junit.Before;
import org.junit.Test;

import static io.spine.base.Identifier.newUuid;
import static io.spine.test.Tests.nullRef;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * This test suite tests {@link AbstractVersionableEntity#equals(Object)}.
 *
 * <p>When migrating to JUnit 5, this class may become a
 * {@code @Nested} class of {@link EntityShould}.
 *
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
public class EntityEqualsShould {

    private TestEntity entity;

    @Before
    public void setUp() {
        entity = TestEntity.withState();
    }

    @Test
    public void assure_same_entities_are_equal() {
        final TestEntity another = TestEntity.withStateOf(entity);

        assertEquals(entity, another);
    }

    @SuppressWarnings("EqualsWithItself") // is the purpose of this method.
    @Test
    public void assure_entity_is_equal_to_itself() {
        assertEquals(entity, entity);
    }

    @Test
    public void assure_entity_is_not_equal_to_null() {
        assertNotEquals(entity, nullRef());
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes") // is the purpose of this method.
    @Test
    public void assure_entity_is_not_equal_to_object_of_another_class() {
        assertNotEquals(entity, newUuid());
    }

    @Test
    public void assure_entities_with_different_ids_are_not_equal() {
        final TestEntity another = TestEntity.newInstance(newUuid());

        assertNotEquals(entity.getId(), another.getId());
        assertNotEquals(entity, another);
    }

    @Test
    public void assure_entities_with_different_states_are_not_equal() {
        final TestEntity another = TestEntity.withStateOf(entity);
        another.updateState(Sample.messageOfType(Project.class), another.getVersion());

        assertNotEquals(entity.getState(), another.getState());
        assertNotEquals(entity, another);
    }

    @SuppressWarnings("CheckReturnValue") // The entity version can be ignored in this test.
    @Test
    public void assure_entities_with_different_versions_are_not_equal() {
        final TestEntity another = TestEntity.withStateOf(entity);
        another.incrementVersion();

        assertNotEquals(entity, another);
    }
}
