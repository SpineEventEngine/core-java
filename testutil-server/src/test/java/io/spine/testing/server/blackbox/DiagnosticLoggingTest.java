/*
 * Copyright 2023, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.protobuf.Empty;
import io.spine.core.MessageId;
import io.spine.type.TypeUrl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.base.Identifier.newUuid;
import static io.spine.base.Identifier.pack;

/**
 * Provides API for testing {@link DiagnosticLogging} functionality.
 */
abstract class DiagnosticLoggingTest {

    private ByteArrayOutputStream output;
    private PrintStream saveErrorStream;
    private PrintStream substErrorStream;

    @BeforeEach
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    void setUpStderr() {
        saveErrorStream = System.err;
        output = new ByteArrayOutputStream();
        substErrorStream = new PrintStream(output, true);
        System.setErr(substErrorStream);
        logger().setLevel(Level.OFF);
    }

    @AfterEach
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    void resetStderr() {
        System.err.close();
        System.setErr(saveErrorStream);
        logger().setLevel(Level.ALL);
    }

    /**
     * Ensures the {@code messagePart} has beed logged.
     */
    protected void assertLogged(String messagePart) {
        substErrorStream.flush();
        assertThat(output.toString())
                .contains(messagePart);
    }

    /**
     * Sets up the logger to intercept.
     */
    protected abstract Logger logger();

    /**
     * Creates a random test entity ID of an {@link Empty} type.
     */
    protected static MessageId entity() {
        return MessageId
                .newBuilder()
                .setId(pack(newUuid()))
                .setTypeUrl(TypeUrl.of(Empty.class).value())
                .vBuild();
    }
}
