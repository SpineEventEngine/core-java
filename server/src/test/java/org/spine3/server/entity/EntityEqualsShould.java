/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

import org.junit.Before;
import org.junit.Test;
import org.spine3.test.entity.Project;
import org.spine3.testdata.Sample;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.spine3.base.Identifiers.newUuid;
import static org.spine3.test.Tests.nullRef;

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

        assertTrue(entity.equals(another));
    }

    @SuppressWarnings("EqualsWithItself") // is the purpose of this method.
    @Test
    public void assure_entity_is_equal_to_itself() {
        assertTrue(entity.equals(entity));
    }

    @Test
    public void assure_entity_is_not_equal_to_null() {
        assertFalse(entity.equals(nullRef()));
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes") // is the purpose of this method.
    @Test
    public void assure_entity_is_not_equal_to_object_of_another_class() {
        assertFalse(entity.equals(newUuid()));
    }

    @Test
    public void assure_entities_with_different_ids_are_not_equal() {
        final TestEntity another = TestEntity.newInstance(newUuid());

        assertNotEquals(entity.getId(), another.getId());
        assertFalse(entity.equals(another));
    }

    @Test
    public void assure_entities_with_different_states_are_not_equal() {
        final TestEntity another = TestEntity.withStateOf(entity);
        another.setState(Sample.messageOfType(Project.class), another.getVersion());

        assertNotEquals(entity.getState(), another.getState());
        assertFalse(entity.equals(another));
    }

    @Test
    public void assure_entities_with_different_versions_are_not_equal() {
        final TestEntity another = TestEntity.withStateOf(entity);
        another.incrementVersion();

        assertFalse(entity.equals(another));
    }
}
