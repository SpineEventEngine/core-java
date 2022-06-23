/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.bus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.Signal;
import io.spine.server.BoundedContext;
import io.spine.server.tenant.TenantIndex;
import io.spine.server.type.SignalEnvelope;
import io.spine.system.server.SystemWriteSide;
import io.spine.type.MessageClass;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * The implementation base for the bus builders.
 *
 * @param <E> the type of {@link SignalEnvelope} posted by the bus
 * @param <T> the type of {@link Signal} posted by the bus
 * @param <B> the own type of the builder
 */
@Internal
@CanIgnoreReturnValue
public abstract class BusBuilder<B extends BusBuilder<B, T, E, C, D>,
                                 T extends Signal<?, ?, ?>,
                                 E extends SignalEnvelope<?, T, ?>,
                                 C extends MessageClass<? extends Message>,
                                 D extends MessageDispatcher<C, E>> {

    private final List<BusFilter<E>> filters = new ArrayList<>();
    private final Set<Listener<E>> listeners = new HashSet<>();

    private @Nullable SystemWriteSide systemWriteSide;
    private @Nullable TenantIndex tenantIndex;
    private BoundedContext context;

    /**
     * Creates a new instance of the bus builder.
     */
    protected BusBuilder() {
    }

    /**
     * Adds the given {@linkplain BusFilter filter} to the builder.
     *
     * <p>The order of appending the filters to the builder is the order of the filters in
     * the resulting bus.
     *
     * @param filter the filter to add
     */
    public final B appendFilter(BusFilter<E> filter) {
        checkNotNull(filter);
        filters.add(filter);
        return self();
    }

    /**
     * Obtains the filters added to this this builder by the time of the call.
     *
     * @see #appendFilter(BusFilter)
     */
    public final Iterable<BusFilter<E>> filters() {
        return ImmutableList.copyOf(filters);
    }

    /**
     * Adds a listener of the message posted to the bus being build.
     *
     * <p>When a message is posted to the bus, the listeners are notified before invoking filters.
     *
     * <p>If an exception is thrown by a {@linkplain Consumer#accept(Object) listener code}, it
     * will be ignored by the bus.
     */
    public final B addListener(Listener<E> listener) {
        checkNotNull(listener);
        listeners.add(listener);
        return self();
    }

    /**
     * Removes the listener. If the listener was not added before, the method has no effect.
     */
    public final B removeListener(Listener<E> listener) {
        checkNotNull(listener);
        listeners.remove(listener);
        return self();
    }

    /**
     * Obtains immutable set of listeners added to the builder by the time of the call.
     */
    public final Set<Listener<E>> listeners() {
        return ImmutableSet.copyOf(listeners);
    }

    @Internal
    public B injectContext(BoundedContext context) {
        this.context = context;
        return self();
    }

    protected final BoundedContext context() {
        return checkNotNull(
                context,
                "%s does not have BoundedContext assigned." +
                        " Please call `injectContext(BoundedContext)`.",
                getClass().getName()
        );
    }

    /**
     * Inject the {@link SystemWriteSide} of the Bounded Context to which the built bus belongs.
     *
     * @apiNote This method is {@link Internal} to the framework. The name of the method starts
     *          with the {@code inject} prefix so that this method does not appear in an
     *          auto-complete hint for the {@code set} prefix.
     */
    @Internal
    public B injectSystem(SystemWriteSide writeSide) {
        this.systemWriteSide = checkNotNull(writeSide);
        return self();
    }

    /**
     * Inject the {@link TenantIndex} of the Bounded Context to which the built bus belongs.
     *
     * @apiNote This method is {@link Internal} to the framework. The name of the method starts
     *          with the {@code inject} prefix so that this method does not appear in an
     *          auto-complete hint for the {@code set} prefix.
     */
    @Internal
    public B injectTenantIndex(TenantIndex index) {
        this.tenantIndex = checkNotNull(index);
        return self();
    }

    /**
     * Obtains a {@link SystemWriteSide} set in the builder.
     */
    @Internal
    public Optional<SystemWriteSide> system() {
        return ofNullable(systemWriteSide);
    }

    /**
     * Obtains a {@link TenantIndex} set in the builder.
     */
    @Internal
    public Optional<TenantIndex> tenantIndex() {
        return ofNullable(tenantIndex);
    }

    protected abstract DispatcherRegistry<C, E, D> newRegistry();

    /**
     * Creates new instance of {@code Bus} with the set parameters.
     *
     * <p>It is recommended to specify the exact resulting type of the bus in the return type
     * when overriding this method.
     */
    @CheckReturnValue
    public abstract Bus<?, E, ?, ?> build();

    protected void checkFieldsSet() {
        FieldCheck.check(this);
    }

    /**
     * Returns {@code this} reference to avoid redundant casts.
     */
    protected abstract B self();

    /**
     * Verifies if required fields of a {@link BusBuilder} are set.
     */
    public static final class FieldCheck {

        private static final String SYSTEM_METHOD = "injectSystem";
        private static final String TENANT_INDEX_METHOD = "injectTenantIndex";
        private static final String ERROR_FORMAT = "`%s` must be set. Please call `%s()`.";

        /** Prevents instantiation of this utility class. */
        private FieldCheck() {
        }

        private static void check(BusBuilder builder) {
            checkSet(builder.systemWriteSide, SystemWriteSide.class, SYSTEM_METHOD);
            checkSet(builder.tenantIndex, TenantIndex.class, TENANT_INDEX_METHOD);
        }

        public static void checkSet(@Nullable Object field,
                                    Class<?> fieldType,
                                    String setterName) {
            checkState(field != null, ERROR_FORMAT, fieldType.getSimpleName(), setterName);
        }

        public static Supplier<IllegalStateException> systemNotSet() {
            return () -> newException(SystemWriteSide.class, SYSTEM_METHOD);
        }

        public static Supplier<IllegalStateException> tenantIndexNotSet() {
            return () -> newException(TenantIndex.class, TENANT_INDEX_METHOD);
        }

        private static IllegalStateException newException(Class<?> fieldClass, String setterName) {
            String errorMessage = format(ERROR_FORMAT, fieldClass.getSimpleName(), setterName);
            return new IllegalStateException(errorMessage);
        }
    }
}
