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

package io.spine.testlogging;

import io.spine.logging.Logging;
import io.spine.server.command.CommandErrorHandler;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.SubstituteLogger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

/**
 * A JUnit {@link org.junit.jupiter.api.extension.Extension Extension} which mutes all the logs from
 * failed command handling for a given test case.
 *
 * @author Dmytro Dashenkov
 */
public class MuteLogging implements BeforeEachCallback, AfterEachCallback {

    private static final Class<?> LOG_TAG = CommandErrorHandler.class;

    @Override
    public void beforeEach(ExtensionContext context) {
        substitute().setDelegate(NOP_LOGGER);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        Logger actualLogger = LoggerFactory.getLogger(LOG_TAG);
        substitute().setDelegate(actualLogger);
    }

    private static SubstituteLogger substitute() {
        Logger logger = Logging.get(LOG_TAG);
        assertTrue(logger instanceof SubstituteLogger);
        SubstituteLogger substituteLogger = (SubstituteLogger) logger;
        return substituteLogger;
    }
}
