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

package io.spine.model.verify;

import com.google.protobuf.Any;
import com.google.protobuf.FloatValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import io.spine.server.command.Assign;
import io.spine.server.procman.ProcessManager;
import io.spine.validate.AnyVBuilder;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * A ProcessManager with a valid command handler method.
 *
 * <p>The command handler method handles command of types {@code UInt64Value}.
 *
 * @author Dmytro Dashenkov
 */
public class ValidProcMan extends ProcessManager<String, Any, AnyVBuilder> {

    protected ValidProcMan(String id) {
        super(id);
    }

    @Assign
    List<UInt64Value> handle(UInt64Value command) {
        return singletonList(command);
    }
}
