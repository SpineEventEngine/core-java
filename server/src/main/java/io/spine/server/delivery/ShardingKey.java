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
package io.spine.server.delivery;

import io.spine.server.entity.model.EntityClass;

import java.io.Serializable;
import java.util.Objects;

/**
 * A key which defines the group of {@code Entity} instances, which must be processed within
 * a single shard.
 *
 * <p>Each shard has a single key.
 *
 * <p>Each key defines the instances of a single shard.
 *
 * @author Alex Tymchenko
 */
public class ShardingKey implements Serializable {

    private static final long serialVersionUID = 0L;

    /**
     * A class of the {@code Entity}, which message dispatching is being sharded.
     */
    private final EntityClass<?> entityClass;

    /**
     * The index of the shard, to which this sharding key corresponds.
     */
    private final ShardIndex index;

    public ShardingKey(EntityClass<?> entityClass, ShardIndex index) {
        this.entityClass = entityClass;
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ShardingKey that = (ShardingKey) o;
        return Objects.equals(entityClass, that.entityClass) &&
                Objects.equals(index, that.index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, index);
    }

    /**
     * Obtains the class of the {@code Entity}, which message dispatching is being sharded.
     *
     * @return the entity class for this shard.
     */
    public EntityClass<?> getEntityClass() {
        return entityClass;
    }

    /**
     * The index of the shard, to which this sharding key corresponds.
     */
    public ShardIndex getIndex() {
        return index;
    }
}
