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

package io.spine.server.entity.storage.given;

import com.google.protobuf.Timestamp;
import io.spine.server.entity.storage.EntityColumn;
import io.spine.server.entity.storage.TheOldColumn;

import java.lang.reflect.Method;

public final class SimpleColumn {

    /** Prevents instantiation of this test env class. */
    private SimpleColumn() {
    }

    public static EntityColumn column() {
        return stringColumn();
    }

    public static EntityColumn stringColumn() {
        return column("getString");
    }

    public static EntityColumn doubleColumn() {
        return column("getDouble");
    }

    public static EntityColumn doublePrimitiveColumn() {
        return column("getDoublePrimitive");
    }

    public static EntityColumn timestampColumn() {
        return column("getTimestamp");
    }

    private static EntityColumn column(String name) {
        try {
            Method method = SimpleColumn.class.getDeclaredMethod(name);
            EntityColumn column = EntityColumn.from(method);
            return column;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @TheOldColumn
    public String getString() {
        return "42";
    }

    @TheOldColumn
    public Double getDouble() {
        return 42.0;
    }

    @TheOldColumn
    public double getDoublePrimitive() {
        return 42.0;
    }

    @TheOldColumn
    public Timestamp getTimestamp() {
        return Timestamp.getDefaultInstance();
    }
}
