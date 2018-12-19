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

package io.spine.server.entity.storage.given;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.AbstractVersionableEntity;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.test.entity.Project;
import io.spine.test.entity.ProjectId;
import io.spine.testdata.Sample;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unused") // Lots of entities with reflective access only.
public class ColumnsTestEnv {

    public static final String CUSTOM_COLUMN_NAME = "columnName";

    private ColumnsTestEnv() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Verifies that a column collection contains the specified columns.
     */
    public static void
    assertContainsColumns(Collection<EntityColumn> actual, String... columnNames) {
        assertTrue(containsColumns(actual, columnNames));
    }

    /**
     * Verifies that a column collection does not contain the specified columns.
     */
    public static void
    assertNotContainsColumns(Collection<EntityColumn> actual, String... columnNames) {
        assertFalse(containsColumns(actual, columnNames));
    }

    /**
     * Checks whether a given column collection contains the specified columns.
     *
     * <p>The collection can be safely stored in {@link ImmutableSet} as it will never contain
     * repeated column names.
     */
    private static boolean
    containsColumns(Collection<EntityColumn> actual, String... columnNames) {
        checkNotNull(actual);
        checkNotNull(columnNames);

        ImmutableSet<String> expectedColumns = ImmutableSet.copyOf(columnNames);
        ImmutableSet<String> actualColumns = actual
                .stream()
                .map(EntityColumn::getName)
                .collect(toImmutableSet());
        boolean result = expectedColumns.equals(actualColumns);
        return result;
    }

    public static class EntityWithNoStorageFields extends AbstractEntity<String, Any> {
        public EntityWithNoStorageFields(String id) {
            super(id);
        }

        // A simple getter, which is not an entity column.
        public int getValue() {
            return 0;
        }
    }

    /**
     * An entity type which declares a {@linkplain #setSecretNumber(Integer) mutator method},
     * however doesn't declare a respective accessor method.
     *
     * <p>{@code ColumnReader} should not get confused and assume that the mutator method is
     * a property, and, therefore, a potential column.
     */
    public static class EntityWithASetterButNoGetter extends AbstractEntity<String, Any> {

        private Integer secretNumber;

        protected EntityWithASetterButNoGetter(String id) {
            super(id);
        }

        @SuppressWarnings("WeakerAccess") // Required for a test
        public void setSecretNumber(Integer secretNumber) {
            this.secretNumber = secretNumber;
        }
    }

    public static class EntityWithManyGetters extends AbstractEntity<String, Any> {

        private final Project someMessage = Sample.messageOfType(Project.class);

        public EntityWithManyGetters(String id) {
            super(id);
        }

        @Column
        public boolean isBoolean() {
            return true;
        }

        @Column
        public @Nullable Boolean isBooleanWrapper() {
            return true;
        }

        @Column
        public int isNonBoolean() {
            return 1;
        }

        @Column(name = CUSTOM_COLUMN_NAME)
        public int getIntegerFieldValue() {
            return 0;
        }

        @Column
        public @Nullable Float getFloatNull() {
            return null;
        }

        @Column
        public Project getSomeMessage() {
            return someMessage;
        }

        @Column
        int getSomeNonPublicMethod() {
            throw new AssertionError("getSomeNonPublicMethod invoked");
        }

        @Column
        public void getSomeVoid() {
            throw new AssertionError("getSomeVoid invoked");
        }

        @Column
        public static int getStaticMember() {
            return 1024;
        }
    }

    public static class EntityWithBooleanColumns {

        @Column
        public Boolean isBooleanWrapperColumn() {
            return true;
        }

        @Column
        public Boolean getBooleanWrapperColumn() {
            return true;
        }

        @Column
        public int isNonBoolean() {
            return 1;
        }

        @Column
        public int getNonBoolean() {
            return 1;
        }

        @Column
        public Boolean isBooleanWithParam(int param) {
            return true;
        }

        @Column
        public Boolean getBooleanWithParam(int param) {
            return true;
        }

        @Column
        public int isNonBooleanWithParam(int param) {
            return 1;
        }

        @Column
        public int getNonBooleanWithParam(int param) {
            return 1;
        }
    }

    public static class EntityWithManyGettersDescendant extends EntityWithManyGetters {
        public EntityWithManyGettersDescendant(String id) {
            super(id);
        }

        @Override
        int getSomeNonPublicMethod() {
            return super.getSomeNonPublicMethod();
        }
    }

    public static class EntityWithInvalidGetters extends AbstractEntity<String, Any> {

        protected EntityWithInvalidGetters(String id) {
            super(id);
        }

        @SuppressWarnings("ReturnOfNull") // required for the test
        public Boolean getNonNullBooleanField() {
            return null;
        }
    }

    // Most real-life (non-test) Entities are children of AbstractVersionableEntity,
    // which brings 3 storage fields from the box.
    public static class RealLifeEntity extends AbstractVersionableEntity<ProjectId, Project> {

        public RealLifeEntity(ProjectId id) {
            super(id);
        }

        @Column
        public Timestamp getSomeTime() {
            return Time.getCurrentTime();
        }

        @Column
        public boolean isVisible() {
            return true;
        }
    }

    public interface InterfaceWithEntityColumn {

        // The column annotation from the interface should be taken into account.
        @Column
        int getIntegerFieldValue();
    }

    public static class EntityWithColumnFromInterface extends AbstractEntity<String, Any>
            implements InterfaceWithEntityColumn {
        public EntityWithColumnFromInterface(String id) {
            super(id);
        }

        // The entity column annotation should be `inherited` from the interface.
        @Override
        public int getIntegerFieldValue() {
            return 0;
        }
    }

    public static class EntityWithRepeatedColumnNames
            extends AbstractVersionableEntity<String, Any> {
        protected EntityWithRepeatedColumnNames(String id) {
            super(id);
        }

        private static final String NAME = "COLUMN_NAME";

        @Column(name = NAME)
        public int getValue() {
            return 0;
        }

        @Column(name = NAME)
        public long getLongValue() {
            return 0;
        }
    }
}
