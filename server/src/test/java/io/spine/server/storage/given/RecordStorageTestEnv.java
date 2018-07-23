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

package io.spine.server.storage.given;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.protobuf.Timestamp;
import io.spine.base.Time;
import io.spine.core.Version;
import io.spine.server.entity.EntityWithLifecycle;
import io.spine.server.entity.TestTransaction;
import io.spine.server.entity.TransactionalEntity;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.Enumerated;
import io.spine.test.storage.Project;
import io.spine.test.storage.ProjectVBuilder;

import static io.spine.server.entity.TestTransaction.injectState;
import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.util.Exceptions.illegalStateWithCauseOf;

/**
 * @author Dmytro Dashenkov
 * @author Dmytro Kuzmin
 */
public class RecordStorageTestEnv {

    /** Prevents instantiation of this utility class. */
    private RecordStorageTestEnv() {
    }

    @SuppressWarnings("unused") // Reflective access
    public static class TestCounterEntity<I>
            extends TransactionalEntity<I, Project, ProjectVBuilder> {

        private int counter = 0;

        public TestCounterEntity(I id) {
            super(id);
        }

        @CanIgnoreReturnValue
        @Column
        public int getCounter() {
            counter++;
            return counter;
        }

        @Column
        public long getBigCounter() {
            return getCounter();
        }

        @Column
        public boolean isCounterEven() {
            return counter % 2 == 0;
        }

        @Column
        public String getCounterName() {
            return getId().toString();
        }

        @Column(name = "COUNTER_VERSION" /* Custom name for storing
                                            to check that querying is correct. */)
        public Version getCounterVersion() {
            return Version.newBuilder()
                          .setNumber(counter)
                          .build();
        }

        @Column
        public Timestamp getNow() {
            return Time.getCurrentTime();
        }

        @Column
        public Project getCounterState() {
            return getState();
        }

        @Column
        public int getProjectStatusValue() {
            return getState().getStatusValue();
        }

        @Column
        public Project.Status getProjectStatusOrdinal() {
            return Enum.valueOf(Project.Status.class, getState().getStatus()
                                                                .name());
        }

        @Column
        @Enumerated(STRING)
        public Project.Status getProjectStatusString() {
            return Enum.valueOf(Project.Status.class, getState().getStatus()
                                                                .name());
        }

        public void assignStatus(Project.Status status) {
            Project newState = Project.newBuilder(getState())
                                      .setStatus(status)
                                      .build();
            injectState(this, newState, getCounterVersion());
        }

        public void archive() {
            TestTransaction.archive(this);
        }

        public void delete() {
            TestTransaction.delete(this);
        }
    }

    /**
     * Entity columns representing lifecycle flags, {@code archived} and {@code deleted}.
     *
     * <p>These columns are present in each {@link EntityWithLifecycle} entity. For the purpose of
     * tests being as close to the real production environment as possible, these columns are stored
     * with the entity records, even if an actual entity is missing.
     *
     * <p>Note that there are cases, when a {@code RecordStorage} stores entity records with no such
     * columns, e.g. the {@linkplain io.spine.server.event.EEntity event entity}. Thus, do not rely
     * on these columns being present in all the entities by default when implementing
     * a {@code RecordStorage}.
     */
    public enum LifecycleColumns {

        ARCHIVED("isArchived"),
        DELETED("isDeleted");

        private final EntityColumn column;

        LifecycleColumns(String getterName) {
            try {
                this.column = EntityColumn.from(
                        EntityWithLifecycle.class.getDeclaredMethod(getterName)
                );
            } catch (NoSuchMethodException e) {
                throw illegalStateWithCauseOf(e);
            }
        }

        public EntityColumn column() {
            return column;
        }

        public String columnName() {
            return column.getStoredName();
        }
    }
}
