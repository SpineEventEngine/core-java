package io.spine.server.entity;

import com.google.protobuf.Message;
import io.spine.core.Version;
import io.spine.server.entity.storage.Column;

/**
 * A server-side persistent object with {@link Column column} definitions.
 *
 * @param <I>
 *         the type of entity identifier
 * @param <S>
 *         the type of entity state
 */
public interface PersistentEntity<I, S extends Message> extends Entity<I, S> {

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to add the {@code Column} annotation.
     */
    @Column
    @Override
    boolean isArchived();

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to add {@code Column} annotation.
     */
    @Column
    @Override
    boolean isDeleted();

    /**
     * {@inheritDoc}
     *
     * <p>Overrides to add {@code Column} annotation.
     */
    @Override
    @Column
    Version getVersion();
}
