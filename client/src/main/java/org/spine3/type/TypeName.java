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

package org.spine3.type;

import com.google.common.base.Splitter;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Message;
import org.spine3.base.Command;
import org.spine3.base.Event;
import org.spine3.type.error.UnknownTypeException;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.spine3.util.Exceptions.wrappedCause;

/**
 * Utility class for obtaining a type name from a {@code Message}.
 *
 * @author Alexander Yevsyukov
 */
public class TypeName extends StringTypeValue {

    private static final Splitter packageSplitter = Splitter.on('.');
    
    private TypeName(String value) {
        super(value);
    }

    private static TypeName create(String value) {
        return new TypeName(value);
    }

    /**
     * Creates new instance by the passed type name value.
     */
    public static TypeName of(String typeName) {
        checkNotNull(typeName);
        checkArgument(!typeName.isEmpty());
        return create(typeName);
    }

    /**
     * Creates instance from the passed type URL.
     */
    public static TypeName from(TypeUrl typeUrl) {
        checkNotNull(typeUrl);
        return create(typeUrl.getTypeName());
    }

    /**
     * Obtains type name for the passed message.
     */
    public static TypeName of(Message message) {
        checkNotNull(message);
        return from(TypeUrl.of(message));
    }

    /**
     * Obtains type name for the passed message class.
     */
    public static TypeName of(Class<? extends Message> cls) {
        checkNotNull(cls);
        return from(TypeUrl.of(cls));
    }

    /**
     * Obtains type name from the message of the passed command.
     */
    public static TypeName ofCommand(Command command) {
        checkNotNull(command);
        return from(TypeUrl.ofCommand(command));
    }

    /**
     * Obtains type name from the message of the passed event.
     */
    public static TypeName ofEvent(Event event) {
        checkNotNull(event);
        return from(TypeUrl.ofEvent(event));
    }

    /**
     * Obtains type name for the message type by its descriptor.
     */
    public static TypeName from(Descriptor descriptor) {
        checkNotNull(descriptor);
        return from(TypeUrl.from(descriptor));
    }

    /**
     * Returns the unqualified name of the Protobuf type, for example: {@code StringValue}.
     */
    public String getSimpleName() {
        final String typeName = value();
        final List<String> tokens = packageSplitter.splitToList(typeName);
        final String result = tokens.get(tokens.size() - 1);
        return result;
    }

    /**
     * Creates URL instance corresponding to this type name.
     */
    public TypeUrl toUrl() {
        return TypeUrl.from(this);
    }

    /**
     * Returns a message {@link Class} corresponding to the Protobuf type represented
     * by this type name.
     *
     * @return the message class
     * @throws UnknownTypeException wrapping {@link ClassNotFoundException} if
     *         there is no corresponding Java class
     */
    public <T extends Message> Class<T> getJavaClass() throws UnknownTypeException {
        return KnownTypes.getJavaClass(toUrl());
    }

    /**
     * Obtains descriptor for the type.
     */
    public Descriptor getDescriptor() {
        try {
            return DescriptorUtil.getDescriptorForType(value());
        } catch (IllegalArgumentException e) {
            throw wrappedCause(e);
        }
    }
}
