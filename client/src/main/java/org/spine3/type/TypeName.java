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

package org.spine3.type;

import com.google.protobuf.Any;
import com.google.protobuf.AnyOrBuilder;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import org.spine3.Internal;
import org.spine3.base.Command;
import org.spine3.protobuf.KnownTypes;
import org.spine3.protobuf.Messages;

import java.util.regex.Pattern;

import static com.google.common.base.Throwables.propagate;
import static org.spine3.validate.Validate.checkNotDefault;
import static org.spine3.validate.Validate.checkNotEmptyOrBlank;

/**
 * A value object for fully-qualified Protobuf type name.
 *
 * <p>The type name is the string coming after the type URL prefix
 * (for example, "type.googleapis.com/") in the URL returned by {@link Any#getTypeUrl()}.
 *
 * @see Descriptors.FileDescriptor#getFullName()
 * @see Message#getDescriptorForType()
 * @author Alexander Yevsyukov
 */
public final class TypeName extends StringTypeValue {

    // TODO:2016-07-08:alexander.litus: consider renaming to TypeUrl

    private static final String TYPE_URL_SEPARATOR = "/";
    private static final Pattern TYPE_URL_SEPARATOR_PATTERN = Pattern.compile(TYPE_URL_SEPARATOR);
    private static final Pattern PROTOBUF_PACKAGE_SEPARATOR = Pattern.compile("\\.");

    private final String typeUrlPrefix;

    private TypeName(String typeUrlPrefix, String typeName) {
        super(checkNotEmptyOrBlank(typeName, "type name"));
        this.typeUrlPrefix = typeUrlPrefix;
    }

    /**
     * Creates a new type name instance taking its name from the passed message instance.
     *
     * @param msg an instance to get the type name from
     * @return new instance
     */
    public static TypeName of(Message msg) {
        return of(msg.getDescriptorForType());
    }

    /**
     * Creates a new instance by the passed descriptor taking full name of the type.
     *
     * @param descriptor the descriptor of the type
     * @return new instance
     */
    public static TypeName of(Descriptor descriptor) {
        final String typeUrlPrefix = Messages.getTypeUrlPrefix(descriptor);
        return new TypeName(typeUrlPrefix, descriptor.getFullName());
    }

    /**
     * Creates a new instance with the passed type name.
     *
     * @param typeNameOrUrl the name of the Protobuf message type or its type URL
     * @return new instance
     */
    @Internal
    public static TypeName of(String typeNameOrUrl) { // TODO:2016-07-08:alexander.litus:  tests
        return isTypeUrl(typeNameOrUrl) ?
               ofTypeUrl(typeNameOrUrl) :
               ofTypeName(typeNameOrUrl);
    }

    private static boolean isTypeUrl(String str) {
        return str.contains(TYPE_URL_SEPARATOR);
    }

    private static TypeName ofTypeUrl(String typeNameOrUrl) {
        final String[] parts = TYPE_URL_SEPARATOR_PATTERN.split(typeNameOrUrl);
        if (parts.length != 2) {
            throw propagate(
                    new InvalidProtocolBufferException("Invalid Protobuf type url encountered: " + typeNameOrUrl));
        }
        return new TypeName(parts[0], parts[1]);
    }

    private static TypeName ofTypeName(String typeName) {
        final String typeUrlPrefix = KnownTypes.getTypeUrlPrefix(typeName);
        return new TypeName(typeUrlPrefix, typeName);
    }

    /**
     * Obtains the type name of the message enclosed into passed instance of {@link Any}.
     *
     * @param any the instance of {@code Any} containing {@code Message} instance of interest
     * @return new instance of {@code TypeName}
     */
    public static TypeName ofEnclosed(AnyOrBuilder any) {
        String typeName;
        try {
            typeName = getTypeName(any.getTypeUrl());
        } catch (InvalidProtocolBufferException e) {
            throw propagate(e);
        }
        assert typeName != null;
        return of(typeName);
    }

    /**
     * Obtains the type name of the command message.
     *
     * <p>The passed command must have non-default message.
     *
     * @param command the command to inspect
     * @return the type name of the command message
     */
    public static TypeName ofCommand(Command command) {
        final Any message = command.getMessage();
        checkNotDefault(message);

        final TypeName commandType = ofEnclosed(message);
        return commandType;
    }

    /**
     * Obtains type name for the passed message class.
     */
    public static TypeName of(Class<? extends Message> clazz) {
        final Message defaultInstance = com.google.protobuf.Internal.getDefaultInstance(clazz);
        final TypeName result = of(defaultInstance);
        return result;
    }

    private static String getTypeName(String typeUrl) throws InvalidProtocolBufferException {
        final String[] parts = TYPE_URL_SEPARATOR_PATTERN.split(typeUrl);
        if (parts.length != 2) {
            throw new InvalidProtocolBufferException("Invalid type url encountered: " + typeUrl);
        }
        return parts[1];
    }

    /**
     * Returns string to be used as a type URL in {@code Any}.
     *
     * @return a Protobuf type URL
     * @see Any#getTypeUrl()
     * @see #ofEnclosed(AnyOrBuilder)
     */
    public String toTypeUrl() {
        final String result = typeUrlPrefix + TYPE_URL_SEPARATOR + value();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value() {
        // Expose method to other packages.
        return super.value();
    }

    /**
     * Returns the name part of the fully qualified name.
     *
     * <p>If there's no package part in the type name, its value is returned.
     *
     * @return string with the type name
     */
    public String nameOnly() {
        final String value = value();
        final String[] parts = PROTOBUF_PACKAGE_SEPARATOR.split(value);
        if (parts.length < 2) {
            // There's only one element in the array since there's no separator found.
            // If this is so, the type name has no package.
            return value;
        }
        final String result = parts[parts.length - 1];
        return result;
    }
}
