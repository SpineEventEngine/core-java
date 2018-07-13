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

package io.spine.core;

import com.google.protobuf.Any;
import io.spine.protobuf.Attribute;

import java.util.Map;

/**
 * Abstract base for attributes extending {@link CommandContext}.
 *
 * <p>Example of usage: <pre>
 *     {@code
 *      // Init attribute.
 *      CommandAttribute<Long> attr = new CommandAttribute<Long>("attrName") {};
 *
 *      // Setting value.
 *      CommandContext.Builder builder = ...;
 *      attr.setValue(builder, 100L);
 *
 *      // Getting value.
 *      CommandContext context = builder.build();
 *      Long value = attr.getValue(context);
 * }</pre>
 * @author Alexander Yevsyukov
 */
public abstract class CommandAttribute<T>
        extends Attribute<T, CommandContext, CommandContext.Builder> {

    /**
     * Creates a new instance for the passed name.
     */
    public CommandAttribute(String name) {
        super(name);
    }

    @Override
    protected final Map<String, Any> getMap(CommandContext context) {
        return context.getAttributesMap();
    }

    @Override
    protected final Map<String, Any> getMutableMap(CommandContext.Builder builder) {
        return builder.getMutableAttributes();
    }
}
