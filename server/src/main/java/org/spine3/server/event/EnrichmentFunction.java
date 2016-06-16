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

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.protobuf.Message;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@code EnrichmentFunction} defines a couple of classes that participate in the event enrichment process
 * and the function which transforms the instance of a source class into the the instance of the target class.
 *
 * <p>The source class is the class of an event message, which needs to be enriched.
 * The target class is the class of the enrichment.
 *
 * <p>{@code EnrichmentFunction}s are used by an {@link Enricher} to augment events passed to {@link EventBus}.
 *
 * @param <M> a type of the event message to enrich
 * @param <E> a type of the enrichment
 * @author Alexander Yevsyukov
 */
public final class EnrichmentFunction<M extends Message, E extends Message> implements Function<M, E> {

    /**
     * We are having the generified class (instead of working with {@link org.spine3.server.type.EventClass})
     * to be able to bound the types of messages and the translation function when building the {@link EnricherImpl}.
     *
     * @see EnricherImpl.Builder#add(EnrichmentFunction, Function)
     **/

    /**
     * An event message class.
     */
    private final Class<M> sourceClass;

    /**
     * A class of a message with enrichment for the event.
     */
    private final Class<E> targetClass;

    /**
     * A function, which performs the enrichment.
     */
    private final Function<M, E> translator;

    /**
     * Creates a new instance of the enrichment type.
     */
    public static <M extends Message, E extends Message>
    EnrichmentFunction<M, E> createDefault(Class<M> source, Class<E> target) {
        final EnrichmentFunction<M, E> result = new EnrichmentFunction<>(source, target, new DefaultTranslator<M, E>());
        return result;
    }

    public static <M extends Message, E extends Message>
    EnrichmentFunction<M, E> createCustom(Class<M> source, Class<E> target, Function<M, E> translator) {
        final EnrichmentFunction<M, E> result = new EnrichmentFunction<>(source, target, translator);
        return result;
    }

    private EnrichmentFunction(Class<M> sourceClass, Class<E> targetClass, Function<M, E> translator) {
        this.sourceClass = checkNotNull(sourceClass);
        this.targetClass = checkNotNull(targetClass);
        this.translator = checkNotNull(translator);
        checkArgument(!sourceClass.equals(targetClass),
                      "Source and target class must not be equal. Passed two values of %", sourceClass);
    }

    public Class<M> getSourceClass() {
        return sourceClass;
    }

    public Class<E> getTargetClass() {
        return targetClass;
    }

    public Function<M, E> getTranslator() {
        return translator;
    }

    @Override
    @Nullable
    public E apply(@Nullable M message) {
        if (message == null) {
            return null;
        }
        final E result = translator.apply(message);
        return result;
    }

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
                && Objects.equals(this.targetClass, other.targetClass)
                && Objects.equals(this.translator, other.translator);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("source", sourceClass)
                          .add("target", targetClass)
                          .add("function", translator)
                          .toString();
    }

}
