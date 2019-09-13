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

package io.spine.server.storage.memory.given;

import com.google.protobuf.Any;
import io.spine.protobuf.AnyPacker;
import io.spine.server.entity.storage.Column;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.test.entity.Project;
import io.spine.testdata.Sample;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;

/**
 * The test environment for {@link io.spine.server.storage.memory.EntityQueryMatcher} tests.
 *
 * <p>Provides various types of {@linkplain EntityColumn entity columns} that can be used to
 * emulate a client-side query.
 */
public final class EntityQueryMatcherTestEnv {

    /** Prevents instantiation of this test env class. */
    private EntityQueryMatcherTestEnv() {
    }

    public static EntityColumn anyColumn() {
        return column("getAny");
    }

    public static Any anyValue() {
        Project someMessage = Sample.messageOfType(Project.class);
        Any value = AnyPacker.pack(someMessage);
        return value;
    }

    public static EntityColumn booleanColumn() {
        return column("getBoolean");
    }

    public static boolean booleanValue() {
        return true;
    }

    private static EntityColumn column(String name) {
        try {
            Method method = EntityQueryMatcherTestEnv.class.getDeclaredMethod(name);
            EntityColumn column = EntityColumn.from(method);
            return column;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Column
    public @Nullable Boolean getBoolean() {
        return booleanValue();
    }

    @Column
    public Any getAny() {
        Any actualValue = anyValue();
        return actualValue;
    }
}
