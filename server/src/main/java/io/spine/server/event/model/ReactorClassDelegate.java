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

package io.spine.server.event.model;

import com.google.common.collect.ImmutableSet;
import io.spine.core.CommandClass;
import io.spine.core.EventClass;
import io.spine.core.RejectionClass;
import io.spine.server.model.HandlerMethod;
import io.spine.server.model.MessageHandlerMap;
import io.spine.server.rejection.model.RejectionReactorMethod;

import java.util.Set;

/**
 * The helper class for holding messaging information on behalf of another model class.
 *
 * @param <T> the type of the raw class for obtaining messaging information
 * @author Alex Tymchenko
 * @author Alexander Yevsyukov
 */
public final class ReactorClassDelegate<T>
        extends EventReceivingClassDelegate<T, EventReactorMethod>
        implements ReactingClass {

    private static final long serialVersionUID = 0L;

    private final MessageHandlerMap<RejectionClass, RejectionReactorMethod> rejectionReactions;

    private final ImmutableSet<RejectionClass> domesticRejections;
    private final ImmutableSet<RejectionClass> externalRejections;

    public ReactorClassDelegate(Class<T> cls) {
        super(cls, EventReactorMethod.factory());
        this.rejectionReactions = new MessageHandlerMap<>(cls, RejectionReactorMethod.factory());
        this.domesticRejections =
                rejectionReactions.getMessageClasses(HandlerMethod::isDomestic);
        this.externalRejections =
                rejectionReactions.getMessageClasses(HandlerMethod::isExternal);
    }

    @Override
    public Set<RejectionClass> getRejectionClasses() {
        return domesticRejections;
    }

    @Override
    public Set<RejectionClass> getExternalRejectionClasses() {
        return externalRejections;
    }

    @Override
    public EventReactorMethod getReactor(EventClass eventClass) {
        return getMethod(eventClass);
    }

    @Override
    public RejectionReactorMethod getReactor(RejectionClass rejCls, CommandClass cmdCls) {
        return rejectionReactions.getMethod(rejCls, cmdCls);
    }
}
