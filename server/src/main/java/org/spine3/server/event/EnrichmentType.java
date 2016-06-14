/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.event;

import com.google.common.base.MoreObjects;
import com.google.protobuf.Message;

import java.util.Objects;

/**
 * {@code EnrichmentType} defines a couple of classes that participate in the event enrichment
 * process.
 *
 * <p>The first class is a event message class, which needs to be enriched. The second class is
 * a class of message enrichment.
 *
 * <p>Instances of {@code EnrichmentType} serves as an instruction for {@link Enricher} to augment
 * passed events.
 *
 * @param <M> a type of an event message to enrich
 * @param <E> a type of an enrichment
 * @author Alexander Yevsyukov
 */
public final class EnrichmentType<M extends Class<? extends Message>, E extends Class<? extends Message>> {

    private final M source;
    private final E target;

    public static <M extends Class<? extends Message>, E extends Class<? extends Message>> EnrichmentType<M, E>
        of(M source, E target) {
        return new EnrichmentType<>(source, target);
    }

    private EnrichmentType(M source, E target) {
        this.source = source;
        this.target = target;
    }

    public M getSource() {
        return source;
    }

    public E getTarget() {
        return target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        //noinspection ConstantConditions
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EnrichmentType other = (EnrichmentType) obj;
        return Objects.equals(this.source, other.source)
                && Objects.equals(this.target, other.target);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("source", source)
                          .add("target", target)
                          .toString();
    }
}
