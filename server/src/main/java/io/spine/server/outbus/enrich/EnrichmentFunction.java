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

package io.spine.server.outbus.enrich;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Streams;
import com.google.protobuf.Message;
import io.spine.server.event.EventBus;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * {@code EnrichmentFunction} defines how a source message class can be transformed
 * into a target message class.
 *
 * <p>{@code EnrichmentFunction}s are used by an {@link Enricher} to augment events
 * passed to {@link EventBus}.
 *
 * @param <S> a type of the source object to enrich
 * @param <T> a type of the target enrichment
 * @param <C> a type of the message context
 * @author Alexander Yevsyukov
 */
abstract class EnrichmentFunction<S, T, C extends Message> {

    /**
     * We are having the generified class to be able to bound the types of messages and the
     * translation function when building the {@link Enricher}.
     *
     * @see Enricher.AbstractBuilder#add(Class, Class, Function)
     */

    private final Class<S> sourceClass;
    private final Class<T> enrichmentClass;

    EnrichmentFunction(Class<S> sourceClass, Class<T> enrichmentClass) {
        this.sourceClass = checkNotNull(sourceClass);
        this.enrichmentClass = checkNotNull(enrichmentClass);
        checkArgument(
                !sourceClass.equals(enrichmentClass),
                "Source and enrichment class must not be equal. Passed two values of %",
                sourceClass
        );
    }

    /**
     * Performs the calculation of target enrichment type.
     *
     * @param  input the source of enrichment
     * @param  context the context of the message
     * @return enrichment result object
     */
    public abstract T apply(S input, C context);

    Class<S> getSourceClass() {
        return sourceClass;
    }

    Class<T> getEnrichmentClass() {
        return enrichmentClass;
    }

    /**
     * Activates the function.
     *
     * <p>During the activation the internal state of the function may be adjusted.
     *
     * <p>A typical example of such an adjustment would be parsing and validation of the relations
     * between {@code eventClass} and {@code enrichmentClass} from the corresponding {@code .proto}
     * definitions. The function internal state in this case is appended with the parsed data, which
     * is later used at runtime.
     *
     * <p>After the function is activated, the {@link #isActive()} returns {@code true}.
     *
     * <p>If an activation cannot be performed flawlessly, the {@code IllegalStateException}
     * should be thrown. In this case {@link #isActive()} should return {@code false}.
     *
     * @throws IllegalStateException if the function cannot perform the conversion in its
     *                               current state or because of the state of its environment
     */
    abstract void activate();

    /**
     * Checks whether this instance of {@code EnrichmentFunction} is active
     * and available to use for the conversion.
     *
     * @return {@code true} if the function is eligible for the conversion, {@code false} otherwise.
     */
    abstract boolean isActive();

    /**
     * A helper predicate to filter the active functions only.
     */
    static Predicate<EnrichmentFunction<?, ?, ?>> activeOnly() {
        return input -> {
            checkNotNull(input);
            return input.isActive();
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceClass, enrichmentClass);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EnrichmentFunction other = (EnrichmentFunction) obj;
        return Objects.equals(this.sourceClass, other.sourceClass)
                && Objects.equals(this.enrichmentClass, other.enrichmentClass);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("sourceClass", sourceClass)
                          .add("enrichmentClass", enrichmentClass)
                          .toString();
    }

    /**
     * Checks whether this instance of {@code EnrichmentFunction} is active.
     *
     * <p>Throws {@link IllegalStateException} if the instance is not active.
     */
    protected void ensureActive() {
        if (!isActive()) {
            throw newIllegalStateException(
                    "Enrichment function %s is not active. Please use `activate()` first.", this
            );
        }
    }

    /**
     * Obtains first function that matches the passed predicate.
     */
    static Optional<EnrichmentFunction<?, ?, ?>>
    firstThat(Iterable<EnrichmentFunction<?, ?, ?>> functions,
              Predicate<? super EnrichmentFunction<?, ?, ?>> predicate) {
        Optional<EnrichmentFunction<?, ?, ?>> optional = Streams.stream(functions)
                                                                .filter(predicate)
                                                                .findFirst();
        return optional;
    }
}
