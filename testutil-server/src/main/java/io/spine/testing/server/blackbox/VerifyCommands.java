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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Message;
import io.spine.testing.client.blackbox.Count;

import static io.spine.testing.client.blackbox.Count.once;
import static io.spine.testing.server.blackbox.AbstractVerify.classes;
import static io.spine.testing.server.blackbox.AbstractVerify.count;
import static io.spine.testing.server.blackbox.AbstractVerify.countAndClass;

/**
 * Verifies that a {@link MultitenantBlackBoxContext Bounded Context} emitted commands that satisfy
 * criteria defined by a factory method that returns an instance of this class.
 *
 * @author Alexander Yevsyukov
 */
@VisibleForTesting
public class VerifyCommands extends DelegatingVerify<EmittedCommands> {

    private VerifyCommands(VerifyMessages<EmittedCommands> delegate) {
        super(delegate);
    }

    /**
     * Verifies that there was a specific number of commands of the provided class.
     */
    public static VerifyCommands emittedCommand(Class<? extends Message> commandClass,
                                                Count expected) {
        return new VerifyCommands(countAndClass(expected, commandClass));
    }

    /**
     * Verifies that there was an expected amount of commands of any classes.
     */
    public static VerifyCommands emittedCommand(Count expected) {
        return new VerifyCommands(count(expected));
    }

    /**
     * Verifies that the command of the passed class was emitted one time.
     */
    public static VerifyCommands emittedCommand(Class<? extends Message> commandClass) {
        return emittedCommand(commandClass, once());
    }

    /**
     * Verifies that there were events of each of the provided command classes.
     */
    @SafeVarargs
    public static VerifyCommands emittedCommands(Class<? extends Message> commandClass,
                                                 Class<? extends Message>... otherCommandClasses) {
        return new VerifyCommands(classes(commandClass, otherCommandClasses));
    }
}
