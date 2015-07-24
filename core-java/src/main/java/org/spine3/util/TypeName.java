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

package org.spine3.util;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object for fully-qualified Protobuf type name.
 *
 * @see Descriptors.FileDescriptor#getFullName()
 * @see Message#getDescriptorForType()
 * @author Alexander Yevsyukov
 */
public final class TypeName extends StringValue {

    private TypeName(String value) {
        super(checkNotNull(value));
    }

    private TypeName(Message msg) {
        this(msg.getDescriptorForType().getFullName());
    }

    /**
     * Creates a new type name instance taking its name from the passed message instance.
     * @param msg an instance to get the type name from
     * @return new instance
     */
    public static TypeName of(Message msg) {
        return new TypeName(msg);
    }

    /**
     * Creates a new instance with the passed type name.
     * @param typeName the name of the type
     * @return new instance
     */
    public static TypeName of(String typeName) {
        return new TypeName(typeName);
    }
}
