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
import io.spine.server.entity.storage.Enumerated;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.server.entity.storage.given.column.TaskStatus.SUCCESS;
import static io.spine.testing.Tests.nullRef;

@SuppressWarnings("unused") // Reflective access
public class TestEntity extends AbstractEntity<String, Any> {

    private @Nullable Integer mutableState = 0;

    public TestEntity(String id) {
        super(id);
    }

    public TestEntity(String id, @Nullable Integer state) {
        super(id);
        this.mutableState = state;
    }

    @Column
    public @Nullable Integer getMutableState() {
        return mutableState;
    }

    public void setMutableState(@Nullable Integer mutableState) {
        this.mutableState = mutableState;
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

    @Column
    public String getNotNull() {
        return nullRef();
    }

    @Column
    public @Nullable String getNull() {
        return null;
    }

    @Column
    public long getLong() {
        return 0;
    }

    @Column
    public TaskStatus getEnumNotAnnotated() {
        return SUCCESS;
    }

    @Column
    @Enumerated
    public TaskStatus getEnumOrdinal() {
        return SUCCESS;
    }

    @Column
    @Enumerated(STRING)
    public TaskStatus getEnumString() {
        return SUCCESS;
    }

    @SuppressWarnings("MethodMayBeStatic") // A column method cannot be static.
    @Column
    private long getFortyTwoLong() {
        return 42L;
    }

    @Column
    public String getParameter(String param) {
        return param;
    }

    @Column
    public static String getStatic() {
        return "";
    }
}
