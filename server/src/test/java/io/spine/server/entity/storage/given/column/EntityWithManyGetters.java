/*
 * Copyright 2019, TeamDev. All rights reserved.
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

package io.spine.server.entity.storage.given.column;

import com.google.protobuf.Any;
import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.storage.Column;
import io.spine.test.entity.Project;
import io.spine.testdata.Sample;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unused")
public class EntityWithManyGetters extends AbstractEntity<String, Any> {

    public static final String CUSTOM_COLUMN_NAME = "columnName";

    private final Project someMessage = Sample.messageOfType(Project.class);

    public EntityWithManyGetters(String id) {
        super(id);
    }

    @Column
    public boolean isBoolean() {
        return true;
    }

    @Column
    public @Nullable Boolean isBooleanWrapper() {
        return true;
    }

    @Column
    public int isNonBoolean() {
        return 1;
    }

    @Column(name = CUSTOM_COLUMN_NAME)
    public int getIntegerFieldValue() {
        return 0;
    }

    @Column
    public @Nullable Float getFloatNull() {
        return null;
    }

    @Column
    public Project getSomeMessage() {
        return someMessage;
    }

    @Column
    int getSomeNonPublicMethod() {
        throw new AssertionError("getSomeNonPublicMethod invoked");
    }

    @Column
    public void getSomeVoid() {
        throw new AssertionError("getSomeVoid invoked");
    }

    @Column
    public static int getStaticMember() {
        return 1024;
    }
}
