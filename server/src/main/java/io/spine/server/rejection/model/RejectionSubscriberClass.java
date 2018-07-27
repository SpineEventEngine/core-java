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

package io.spine.server.rejection.model;

import com.google.common.collect.ImmutableSet;
import io.spine.core.CommandClass;
import io.spine.core.RejectionClass;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.model.ModelClass;
import io.spine.server.rejection.RejectionSubscriber;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides type information on a {@link RejectionSubscriber} class.
 *
 * @param <S> the type of rejection subscribers
 * @author Alexander Yevsyukov
 */
public final class RejectionSubscriberClass<S extends RejectionSubscriber> extends ModelClass<S> {

    private static final long serialVersionUID = 0L;

    private final
    MessageHandlerMap<RejectionClass, RejectionSubscriberMethod> rejectionSubscriptions;
    private final ImmutableSet<RejectionClass> domesticSubscriptions;
    private final ImmutableSet<RejectionClass> externalSubscriptions;

    private RejectionSubscriberClass(Class<? extends S> cls) {
        super(cls);
        rejectionSubscriptions = new MessageHandlerMap<>(cls, RejectionSubscriberMethod.factory());
        this.domesticSubscriptions =
                rejectionSubscriptions.getMessageClasses(HandlerMethod::isDomestic);
        this.externalSubscriptions =
                rejectionSubscriptions.getMessageClasses(HandlerMethod::isExternal);
    }

    /**
     * Obtains a model class for the passed raw class.
     */
    public static <S extends RejectionSubscriber>
    RejectionSubscriberClass<S> asRejectionSubscriber(Class<S> cls) {
        checkNotNull(cls);
        RejectionSubscriberClass<S> result = (RejectionSubscriberClass<S>)
                get(cls, RejectionSubscriberClass.class, () -> new RejectionSubscriberClass<>(cls));
        return result;
    }

    public Set<RejectionClass> getRejectionSubscriptions() {
        return domesticSubscriptions;
    }

    @SuppressWarnings("InstanceMethodNamingConvention")     // it's long to reflect the aim.
    public Set<RejectionClass> getExternalRejectionSubscriptions() {
        return externalSubscriptions;
    }

    public RejectionSubscriberMethod getSubscriber(RejectionClass cls, CommandClass commandCls) {
        return rejectionSubscriptions.getMethod(cls, commandCls);
    }
}
