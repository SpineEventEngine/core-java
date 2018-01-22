/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.entity;

import io.spine.server.entity.given.Given;
import io.spine.test.entity.Project;
import io.spine.testdata.Sample;

import static io.spine.Identifier.newUuid;

/**
 * Extracted from {@link EntityShould}
 *
 * @author Mikhail Mikhaylov
 */
public class TestEntity extends AbstractVersionableEntity<String, Project> {

    static TestEntity newInstance(String id) {
        final TestEntity result = Given.entityOfClass(TestEntity.class)
                                       .withId(id)
                                       .build();
        return result;
    }

    static TestEntity withState() {
        final TestEntity result = Given.entityOfClass(TestEntity.class)
                                       .withId(newUuid())
                                       .withState(Sample.messageOfType(Project.class))
                                       .withVersion(3)
                                       .build();
        return result;
    }

    static TestEntity withStateOf(TestEntity entity) {
        final TestEntity result = Given.entityOfClass(TestEntity.class)
                                       .withId(entity.getId())
                                       .withState(entity.getState())
                                       .withVersion(entity.getVersion()
                                                          .getNumber())
                                       .build();
        return result;
    }

    private TestEntity(String id) {
        super(id);
    }
}
