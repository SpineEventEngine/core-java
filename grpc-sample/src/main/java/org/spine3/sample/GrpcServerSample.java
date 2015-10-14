package org.spine3.sample;/*
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

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spine3.base.CommandRequest;
import org.spine3.server.Engine;
import org.spine3.server.storage.datastore.DatastoreStorageFactory;

import java.util.List;

import static org.spine3.sample.BaseSample.*;
import static org.spine3.sample.BaseSample.SUCCESS_MESSAGE;
import static org.spine3.sample.BaseSample.generateRequests;

/**
 * Simple Spine framework example. Uses server implementation, but invokes requests on it manually.
 *
 * @author Mikhail Melnik
 * @author Mikhail Mikhaylov
 */
@SuppressWarnings("UtilityClass")
public class GrpcServerSample {

    private static final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

    public static void main(String[] args) {

        // as we don't start server, we have to manually setup DataStore helper
        helper.setUp();

        setupEnvironment(new DatastoreStorageFactory());
        final EventLogger eventLogger = setupEventLogger();

        List<CommandRequest> requests = generateRequests();
        for (CommandRequest request : requests) {
            Engine.getInstance().process(request);
        }

        log().info(SUCCESS_MESSAGE);

        tearDownEventLogger(eventLogger);
        tearDownEnvironment();

        helper.tearDown();
    }

    private GrpcServerSample() {
    }

    private enum LogSingleton {
        INSTANCE;

        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(GrpcServerSample.class);
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }
}
