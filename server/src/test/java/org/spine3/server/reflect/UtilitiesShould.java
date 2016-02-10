/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.reflect;

import org.junit.Test;
import org.spine3.base.Commands;
import org.spine3.base.Events;
import org.spine3.base.Identifiers;
import org.spine3.io.IoUtil;
import org.spine3.test.Tests;
import org.spine3.testutil.*;
import org.spine3.util.Math;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Checks if the utility classes have private constructors.
 *
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings({"InstanceMethodNamingConvention"/*we have another convention in tests*/,
"OverlyCoupledClass"/*ok in this case*/})
public class UtilitiesShould {

    @Test
    @SuppressWarnings("OverlyCoupledMethod"/*ok in this case*/)
    public void have_private_constructors() {

        //TODO:2016-01-18:alexander.yevsyukov: Refactor to move these checks into corresponding tests.
        // util package
        checkPrivateConstructor(Commands.class);
        checkPrivateConstructor(Events.class);
        checkPrivateConstructor(Identifiers.class);
        checkPrivateConstructor(IoUtil.class);
        checkPrivateConstructor(Math.class);
        checkPrivateConstructor(Methods.class);
        checkPrivateConstructor(Tests.class);

        // testutil package
        checkPrivateConstructor(TestAggregateIdFactory.class);
        checkPrivateConstructor(TestAggregateStorageRecordFactory.class);
        checkPrivateConstructor(TestCommands.class);
        checkPrivateConstructor(TestContextFactory.class);
        checkPrivateConstructor(TestEventFactory.class);
        checkPrivateConstructor(TestEventStorageRecordFactory.class);
    }

    protected static void checkPrivateConstructor(Class<?> clazz) {

        Constructor<?> constructor = null;
        try {
            constructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            failWithMessage(clazz);
        }
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            failWithMessage(clazz);
        }
    }

    private static void failWithMessage(Class<?> clazz) {
        fail(clazz.getName() + " does not have private constructor");
    }
}
