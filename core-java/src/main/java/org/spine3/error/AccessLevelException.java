/*
 * Copyright 2015, TeamDev Ltd. All rights reserved.
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
package org.spine3.error;

import org.spine3.server.AggregateRoot;
import org.spine3.Repository;
import org.spine3.util.Methods;

import java.lang.reflect.Method;

/**
 * This exception is thrown if a handler method is defined with wrong access level modifier.
 *
 * @author Alexander Yevsyukov
 */
public class AccessLevelException extends RuntimeException {

    private AccessLevelException(String message) {
        super(message);
    }

    public static AccessLevelException forAggregateCommandHandler(AggregateRoot aggregate, Method method) {
        return new AccessLevelException(messageForAggregateCommandHandler(aggregate, method));
    }

    public static AccessLevelException forCommandHandler(Object handler, Method method) {
        return new AccessLevelException(messageForCommandHandler(handler, method));
    }

    public static AccessLevelException forRepositoryCommandHandler(Repository repository, Method method) {
        return new AccessLevelException(messageForRepositryCommandHandler(repository, method));
    }

    public static AccessLevelException forEventApplier(AggregateRoot aggregate, Method method) {
        return new AccessLevelException(messageForEventApplier(aggregate, method));
    }

    public static AccessLevelException forEventHandler(Object handler, Method method) {
        return new AccessLevelException(messageForEventHandler(handler, method));
    }

    public static AccessLevelException forRepositoryEventHandler(Repository repository, Method method) {
        return new AccessLevelException(messageForRepositoryEventHandler(repository, method));
    }

    private static String messageForAggregateCommandHandler(Object aggregate, Method method) {
        return "Command handler of the aggregate " + Methods.getFullMethodName(aggregate, method) +
                " must be declared 'public'. It is part of the public API of the aggregate.";
    }

    private static final String MUST_BE_PUBLIC_FOR_COMMAND_DISPATCHER = " must be declared 'public' to be called by CommandDispatcher.";

    private static String messageForCommandHandler(Object handler, Method method) {
        return "Command handler " + Methods.getFullMethodName(handler, method) +
                MUST_BE_PUBLIC_FOR_COMMAND_DISPATCHER;
    }

    private static String messageForRepositryCommandHandler(Object repository, Method method) {
        return "Command handler of the repository " + Methods.getFullMethodName(repository, method) +
                MUST_BE_PUBLIC_FOR_COMMAND_DISPATCHER;
    }

    private static String messageForEventApplier(AggregateRoot aggregate, Method method) {
        return "Event applier method of the aggregate " + Methods.getFullMethodName(aggregate, method) +
                " must be declared 'private'. It is not supposed to be called from outside the aggregate.";
    }

    private static final String MUST_BE_PUBLIC_FOR_EVENT_BUS = " must be declared 'public' to be called by EventBus.";

    private static String messageForEventHandler(Object handler, Method method) {
        return "Event handler " + Methods.getFullMethodName(handler, method) +
                MUST_BE_PUBLIC_FOR_EVENT_BUS;
    }

    private static String messageForRepositoryEventHandler(Object repository, Method method) {
        return "Event handler of the repository " + Methods.getFullMethodName(repository, method) +
                MUST_BE_PUBLIC_FOR_EVENT_BUS;
    }

    private static final long serialVersionUID = 0L;
}
