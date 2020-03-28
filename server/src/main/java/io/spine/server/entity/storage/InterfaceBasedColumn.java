/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage;

import com.google.errorprone.annotations.Immutable;
import io.spine.annotation.Internal;
import io.spine.base.EntityState;
import io.spine.code.proto.FieldDeclaration;
import io.spine.server.entity.Entity;
import io.spine.server.storage.AbstractColumn;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * A column declared with the help of {@link io.spine.base.EntityWithColumns}-derived interface.
 *
 * <p>An interface-based column is:
 * <ol>
 *     <li>Declared in Protobuf with {@code (column)} option:
 *         <pre>
 *         int32 year_of_registration = 8 [(column) = true];
 *         </pre>
 *     <li>Implemented using custom Spine-generated interface:
 *         <pre>
 *         interface UserProfileWithColumns extends EntityWithColumns {
 *             int getYearOfRegistration();
 *         }
 *
 *         // ...
 *         class UserProfileProjection
 *             extends Projection<UserId, UserProfile, UserProfile.Builder>
 *             implements UserProfileWithColumns {
 *
 *            {@literal @}Override
 *             public int getYearOfRegistration() {
 *                 return yearOfRegistration;
 *             }
 *         }
 *         </pre>
 *     <li>Extracted from the entity and propagated to the entity state at the moment of
 *         transaction commit. The column value is finalized by this moment.
 * </ol>
 */
@Internal
public final class InterfaceBasedColumn
        extends AbstractColumn
        implements ColumnDeclaredInProto, ColumnWithCustomGetter {

    /**
     * A getter which obtains the actual column value from the entity.
     */
    private final GetterFromEntity getterFromEntity;

    /**
     * A getter which obtains the column value present in the entity state.
     */
    private final GetterFromState getterFromState;

    /**
     * A corresponding proto field declaration.
     */
    private final FieldDeclaration field;

    InterfaceBasedColumn(ColumnName name,
                         Class<?> type,
                         GetterFromEntity getterFromEntity,
                         GetterFromState getterFromState,
                         FieldDeclaration field) {
        super(name, type);
        this.getterFromEntity = getterFromEntity;
        this.getterFromState = getterFromState;
        this.field = field;
    }

    @Override
    public @Nullable Object valueIn(Entity<?, ?> entity) {
        return getterFromEntity.apply(entity);
    }

    @Override
    public @Nullable Object valueIn(EntityState state) {
        return getterFromState.apply(state);
    }

    @Override
    public FieldDeclaration protoField() {
        return field;
    }

    @Immutable
    interface GetterFromEntity extends Function<Entity<?, ?>, Object> {
    }

    @Immutable
    interface GetterFromState extends Function<EntityState, Object> {
    }
}
