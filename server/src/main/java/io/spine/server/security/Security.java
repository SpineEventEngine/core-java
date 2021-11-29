/*
 * Copyright 2021, TeamDev. All rights reserved.
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

package io.spine.server.security;

import io.spine.code.java.PackageName;
import io.spine.server.Server;
import io.spine.system.server.SystemContext;

import static java.lang.String.format;

/**
 * Controls which class is allowed to call a method.
 *
 * @implNote The generic parameter of {@link Class} is of no importance for access restriction
 *         checking performed by this class.
 */
@SuppressWarnings("rawtypes") // see @implNote
public final class Security extends SecurityManager {

    private static final Security INSTANCE = new Security();

    /** Prevents instantiation of this class from outside. */
    private Security() {
        super();
    }

    /**
     * Obtains the class preceding in call chain the class which calls the
     * method from which this method is being called.
     */
    private Class previousCallerClass() {
        Class[] context = getClassContext();
        var result = context[3];
        return result;
    }

    /**
     * Throws {@link SecurityException} of the calling class does not belong to
     * the Spine Event Engine framework or its tests.
     */
    public static void allowOnlyFrameworkServer() {
        var callingClass = INSTANCE.previousCallerClass();
        if (!belongsToServer(callingClass)) {
            throw nonAllowedCaller(callingClass);
        }
    }

    private static boolean belongsToServer(Class callingClass) {
        var serverPackage = PackageName.of(Server.class);
        var systemServerPackage = PackageName.of(SystemContext.class);
        var callingClassName = callingClass.getName();
        var result =
                callingClassName.startsWith(serverPackage.value())
                || callingClassName.startsWith(systemServerPackage.value())
                || callingClassName.startsWith("io.spine.testing.server");
        return result;
    }

    private static SecurityException nonAllowedCaller(Class callingClass) {
        var msg = format(
                "The class `%s` is not allowed to make this call.", callingClass.getName()
        );
        throw new SecurityException(msg);
    }
}
