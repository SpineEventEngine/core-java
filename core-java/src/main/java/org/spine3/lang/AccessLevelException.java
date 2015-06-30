/*
 * Copyright (c) 2000-2015 TeamDev Ltd. All rights reserved.
 * TeamDev PROPRIETARY and CONFIDENTIAL.
 * Use is subject to license terms.
 */
package org.spine3.lang;

import org.spine3.AggregateRoot;
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
