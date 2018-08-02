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

package io.spine.server.command.model;

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.spine.base.Identifier;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.server.command.AbstractCommandHandler;
import io.spine.server.entity.Entity;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.HandlerKey;
import io.spine.server.model.HandlerMethodFailedException;
import io.spine.server.model.MethodResult;

import java.lang.reflect.Method;
import java.util.Optional;

import static com.google.common.base.Throwables.getRootCause;

/**
 * An abstract base for methods that accept a command message and optionally its context.
 *
 * @param <R> the type of the result object returned by the method
 * @author Alexander Yevsyukov
 */
@Immutable
public abstract class CommandAcceptingMethod<T, R extends MethodResult>
        extends AbstractHandlerMethod<T, CommandClass, CommandContext, R> {

    CommandAcceptingMethod(Method method) {
        super(method);
    }

    /**
     * Obtains ID of the passed object by attempting to cast it to {@link Entity} or
     * {@link AbstractCommandHandler}.
     *
     * @return packed ID or empty optional if the object is of type for which we cannot get ID
     */
    @SuppressWarnings("ChainOfInstanceofChecks")
    private static Optional<Any> idOf(Object target) {
        Any producerId;
        if (target instanceof Entity) {
            producerId = Identifier.pack(((Entity) target).getId());
        } else if (target instanceof AbstractCommandHandler) {
            producerId = Identifier.pack(((AbstractCommandHandler) target).getId());
        } else {
            return Optional.empty();
        }
        return Optional.of(producerId);
    }

    @Override
    public CommandClass getMessageClass() {
        return CommandClass.of(rawMessageClass());
    }

    @Override
    public HandlerKey key() {
        return HandlerKey.of(getMessageClass());
    }

    /**
     * {@inheritDoc}
     *
     * <p>{@linkplain ThrowableMessage#initProducer(Any) Initializes} producer ID if the exception
     * was caused by a thrown rejection.
     */
    @Override
    protected HandlerMethodFailedException whyFailed(Object target,
                                                     Message message,
                                                     CommandContext context,
                                                     Exception cause) {
        HandlerMethodFailedException exception =
                super.whyFailed(target, message, context, cause);

        Throwable rootCause = getRootCause(exception);
        if (rootCause instanceof ThrowableMessage) {
            ThrowableMessage thrownMessage = (ThrowableMessage) rootCause;

            Optional<Any> producerId = idOf(target);
            producerId.ifPresent(thrownMessage::initProducer);
        }

        return exception;
    }
}
