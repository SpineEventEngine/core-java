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

import com.google.protobuf.Any;
import io.spine.server.BoundedContext;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.system.server.EntityHistoryId;
import io.spine.type.TypeUrl;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.base.Identifier.unpack;

/**
 * @author Dmytro Dashenkov
 */
public abstract class DeliveryEventSubscriber<I> extends AbstractEventSubscriber {

    private final TypeUrl targetType;

    protected DeliveryEventSubscriber(TypeUrl targetType) {
        this.targetType = checkNotNull(targetType);
    }

    protected boolean correctType(EntityHistoryId historyId) {
        String typeUrlRaw = historyId.getTypeUrl();
        TypeUrl typeUrl = TypeUrl.parse(typeUrlRaw);
        return typeUrl.equals(targetType);
    }

    protected I idFrom(EntityHistoryId historyId) {
        Any id = historyId.getEntityId()
                          .getId();
        return unpack(id);
    }

    public final void registerAt(BoundedContext context) {
        context.getIntegrationBus()
               .register(this);
    }
}
