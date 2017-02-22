/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.command;

import com.google.common.base.Joiner;
import org.spine3.base.CommandClass;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utilities for command-related registries.
 *
 * @author Alexander Yevsyukov
 */
class RegistryUtil {

    private RegistryUtil() {}

    /**
     * Ensures that the passed set of classes is empty.
     *
     * <p>This is a convenience method for checking registration of handling dispatching.
     *
     * @param alreadyRegistered the set of already registered classes or an empty set
     * @param registeringObject the object which tries to register dispatching or handling
     * @param singularFormat the message format for the case if the {@code alreadyRegistered} set contains only one element
     * @param pluralFormat the message format if {@code alreadyRegistered} set has more than one element
     * @throws IllegalArgumentException if the set is not empty
     */
    protected static void checkNotAlreadyRegistered(Set<? extends CommandClass> alreadyRegistered,
                                                        Object registeringObject,
                                                        String singularFormat,
                                                        String pluralFormat) {
        final String format = alreadyRegistered.size() > 1 ? pluralFormat : singularFormat;
        checkArgument(alreadyRegistered.isEmpty(), format, registeringObject, Joiner.on(", ").join(alreadyRegistered));
    }
}
