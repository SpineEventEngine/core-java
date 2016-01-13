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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A value object holding a fully-qualified Java class name.
 *
 * @author Mikhail Mikhaylov
 */
public final class ClassName extends StringTypeValue {

    private ClassName(String value) {
        super(checkNotNull(value));
    }

    private ClassName(Class clazz) {
        this(clazz.getName());
    }

    /**
     * Creates a new instance with the name of the passed class.
     * @param clazz the class to get name from
     * @return new instance
     */
    public static ClassName of(Class clazz) {
        return new ClassName(checkNotNull(clazz));
    }

    /**
     * Creates a new instance with the passed class name value.
     * @param className a fully-qualified Java class name
     * @return new
     */
    public static ClassName of(String className) {
        checkNotNull(className);
        checkArgument(className.length() > 0, "Class name cannot me empty");
        return new ClassName(className);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value() {
        // Open access to other packages.
        return super.value();
    }
}
