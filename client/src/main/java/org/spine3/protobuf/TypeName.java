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

package org.spine3.protobuf;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import org.spine3.base.Command;

/**
 * Utility class for obtaining a type name from a {@code Message}.
 *
 * @author Alexander Yevsyukov
 */
public class TypeName {

    private TypeName() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Obtains type name for the passed message.
     */
    public static String of(Message message) {
        final String result = TypeUrl.of(message)
                                     .getTypeName();
        return result;
    }

    /**
     * Obtains type name for the passed message class.
     */
    public static String of(Class<? extends Message> clazz) {
        return TypeUrl.of(clazz).getTypeName();
    }

    /**
     * Obtains type name from the message of the passed command.
     */
    public static String ofCommand(Command command) {
        final String result = TypeUrl.ofCommand(command)
                                     .getTypeName();
        return result;
    }

    /**
     * Obtains type name for the message type by its descriptor.
     */
    public static String from(Descriptor descriptor) {
        final String result = TypeUrl.from(descriptor)
                                     .getTypeName();
        return result;
    }
}
