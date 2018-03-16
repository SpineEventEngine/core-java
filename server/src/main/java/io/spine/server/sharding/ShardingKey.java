/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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
package io.spine.server.sharding;

import io.spine.server.entity.EntityClass;

import java.io.Serializable;
import java.util.Objects;

import static io.spine.util.Exceptions.newIllegalArgumentException;

/**
 * @author Alex Tymchenko
 */
public class ShardingKey implements Serializable {

    private static final long serialVersionUID = 0L;

    private final EntityClass<?> entityClass;
    private final IdPredicate idPredicate;


    ShardingKey(EntityClass<?> entityClass, IdPredicate idPredicate) {
        this.entityClass = entityClass;
        this.idPredicate = idPredicate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ShardingKey that = (ShardingKey) o;
        return Objects.equals(entityClass, that.entityClass) &&
                Objects.equals(idPredicate, that.idPredicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityClass, idPredicate);
    }

    public EntityClass<?> getEntityClass() {
        return entityClass;
    }

    public IdPredicate getIdPredicate() {
        return idPredicate;
    }

    public boolean applyToId(Object id) {

        final boolean result;
        final IdPredicate.PredicateCase predicateCase = idPredicate.getPredicateCase();
        switch (predicateCase) {
            case ALL_IDS:
                result = true; break;
            case UNIFORM_BY_IDS:
                result = apply(idPredicate.getUniformByIds(), id.hashCode()); break;
            case PREDICATE_NOT_SET:
                throw newIllegalArgumentException("Unset ID hash Predicate type detected: %s",
                                                  predicateCase);
            default:
                throw newIllegalArgumentException("Unsupported ID hash Predicate type detected: %s",
                                                  predicateCase);
        }

        return result;
    }

    private static boolean apply(UniformByIdHash uniformByIds, int idHash) {
        final int actualRemainder = idHash % uniformByIds.getDivisor();
        final int expectedRemainder = uniformByIds.getRemainder();
        return actualRemainder == expectedRemainder;
    }
}
