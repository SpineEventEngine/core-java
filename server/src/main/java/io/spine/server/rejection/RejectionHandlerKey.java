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

package io.spine.server.rejection;

import com.google.common.base.MoreObjects;
import io.spine.core.CommandClass;
import io.spine.core.RejectionClass;
import io.spine.server.model.HandlerKey;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A key for a rejection handler method.
 *
 * <p>The key always contains {@link RejectionClass}, but {@link CommandClass} is optional
 * because a rejection handler doesn't necessarily has a command message as a parameter.
 *
 * @author Dmytro Grankin
 */
public class RejectionHandlerKey implements HandlerKey<RejectionClass> {

    @Nullable
    private final CommandClass commandClass;
    private final RejectionClass rejectionClass;

    private RejectionHandlerKey(RejectionClass rejectionClass,
                                @Nullable CommandClass commandClass) {
        this.rejectionClass = checkNotNull(rejectionClass);
        this.commandClass = commandClass;
    }

    public static RejectionHandlerKey of(RejectionClass rejectionClass, CommandClass commandClass) {
        return new RejectionHandlerKey(rejectionClass, checkNotNull(commandClass));
    }

    public static RejectionHandlerKey of(RejectionClass rejectionClass) {
        return new RejectionHandlerKey(rejectionClass, null);
    }

    @Override
    public RejectionClass getHandledMessageCls() {
        return rejectionClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RejectionHandlerKey that = (RejectionHandlerKey) o;
        return Objects.equals(rejectionClass, that.rejectionClass) &&
                Objects.equals(commandClass, that.commandClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rejectionClass, commandClass);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("rejectionClass", rejectionClass)
                          .add("commandClass", commandClass)
                          .toString();
    }
}
