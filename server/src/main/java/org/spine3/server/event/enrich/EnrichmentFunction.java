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

package org.spine3.server.event.enrich;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.protobuf.Message;
import org.spine3.server.event.EventBus;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code EnrichmentFunction} defines how a source message class can be transformed into a target message class.
 *
 * <p>{@code EnrichmentFunction}s are used by an {@link EventEnricher} to augment events passed to {@link EventBus}.
 *
 * @param <M> a type of the source message to enrich
 * @param <E> a type of the enrichment
 * @author Alexander Yevsyukov
 */
/* package */ abstract class EnrichmentFunction<M extends Message, E extends Message> implements Function<M, E> {

    /**
     * We are having the generified class to be able to bound the types of messages and the
     * translation function when building the {@link EventEnricher}.
     *
     * @see EventEnricher.Builder#addFieldEnrichment(Class, Class, Function)
     **/

    /**
     * An event message class.
     */
    private final Class<M> sourceClass;

    /**
     * A class of a message with enrichment for the event.
     */
    private final Class<E> targetClass;

    /* package */ EnrichmentFunction(Class<M> sourceClass, Class<E> targetClass) {
        this.sourceClass = checkNotNull(sourceClass);
        this.targetClass = checkNotNull(targetClass);
        checkArgument(!sourceClass.equals(targetClass),
                      "Source and target class must not be equal. Passed two values of %", sourceClass);
    }

    public Class<M> getSourceClass() {
        return sourceClass;
    }

    public Class<E> getTargetClass() {
        return targetClass;
    }

    public abstract Function<M, E> getFunction();

    /**
     * Validates the instance.
     *
     * @throws IllegalStateException if the function cannot perform the conversion in its current state
     * or because of the state of its environment
     */
    /* package */ abstract void validate();

    @Override
    public int hashCode() {
        return Objects.hash(sourceClass, targetClass);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        //noinspection ConstantConditions
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EnrichmentFunction other = (EnrichmentFunction) obj;
        return Objects.equals(this.sourceClass, other.sourceClass)
                && Objects.equals(this.targetClass, other.targetClass);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("source", sourceClass)
                          .add("target", targetClass)
                          .toString();
    }
}
