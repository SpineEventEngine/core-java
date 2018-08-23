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

package io.spine.server.entity;

import io.spine.base.Identifier;
import io.spine.client.EntityId;
import io.spine.core.Command;
import io.spine.core.CommandId;
import io.spine.core.Event;
import io.spine.core.EventId;
import io.spine.system.server.EntityHistoryId;
import io.spine.system.server.SystemGateway;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A copy of recent history of an {@linkplain TransactionalEntity
 * event-sourced entity}.
 * // TODO:2018-08-08:dmytro.dashenkov: Update doc.
 * <p>Any modifications to this object will not affect the real history of the entity.
 *
 * @author Mykhailo Drachuk
 * @author Alexander Yevsyukov
 * @author Dmytro Dashenkov
 */
public final class RecentHistory implements HistoryLog {

    private final SystemGateway gateway;
    private final EntityHistoryId historyId;

    private RecentHistory(Builder builder) {
        this.gateway = builder.gateway;
        this.historyId = builder.entityId;
    }

    @Override
    public boolean contains(Command command) {
        checkNotNull(command);
        CommandId commandId = command.getId();
        return gateway.hasHandled(historyId, commandId);
    }

    @Override
    public boolean contains(Event event) {
        checkNotNull(event);
        EventId eventId = event.getId();
        return gateway.hasHandled(historyId, eventId);
    }

    @Override
    public EntityHistoryId id() {
        return historyId;
    }

    /**
     * Creates a new instance of {@code Builder} for {@code RecentHistory} instances.
     *
     * @return new instance of {@code Builder}
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * A builder for the {@code RecentHistory} instances.
     */
    public static final class Builder {

        private SystemGateway gateway;
        private @MonotonicNonNull EntityHistoryId entityId;

        private TransactionalEntity<?, ?, ?> entity;

        /**
         * Prevents direct instantiation.
         */
        private Builder() {
        }

        public Builder of(TransactionalEntity<?, ?, ?> entity) {
            this.entity = checkNotNull(entity);
            return this;
        }

        public Builder readingFrom(SystemGateway system) {
            this.gateway = checkNotNull(system);
            return this;
        }

        /**
         * Creates a new instance of {@code RecentHistory}.
         *
         * @return new instance of {@code RecentHistory}
         */
        public RecentHistory build() {
            checkNotNull(gateway);
            checkNotNull(entity);

            initEntityId();

            return new RecentHistory(this);
        }

        private void initEntityId() {
            Object id = entity.getId();
            EntityId entityId = EntityId
                    .newBuilder()
                    .setId(Identifier.pack(id))
                    .build();
            TypeUrl type = TypeUrl.of(entity.getState());
            this.entityId = EntityHistoryId
                    .newBuilder()
                    .setEntityId(entityId)
                    .setTypeUrl(type.value())
                    .build();
        }
    }
}
