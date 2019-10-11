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

import io.spine.server.entity.AbstractEntity;
import io.spine.server.entity.storage.Enumerated;
import io.spine.server.entity.storage.TheOldColumn;
import io.spine.test.storage.Project;
import org.checkerframework.checker.nullness.qual.Nullable;

import static io.spine.server.entity.storage.EnumType.STRING;
import static io.spine.server.entity.storage.given.column.TaskStatus.SUCCESS;
import static io.spine.testing.Tests.nullRef;

@SuppressWarnings("unused") // Reflective access
public class TestEntity extends AbstractEntity<String, Project> {

    private @Nullable Integer mutableState = 0;

    public TestEntity(String id) {
        super(id);
    }

    public TestEntity(String id, @Nullable Integer state) {
        super(id);
        this.mutableState = state;
    }

    @TheOldColumn
    public @Nullable Integer getMutableState() {
        return mutableState;
    }

    public void setMutableState(@Nullable Integer mutableState) {
        this.mutableState = mutableState;
    }

    @TheOldColumn
    public boolean isBoolean() {
        return true;
    }

    @TheOldColumn
    public @Nullable Boolean isBooleanWrapper() {
        return true;
    }

    @TheOldColumn
    public int isNonBoolean() {
        return 1;
    }

    @TheOldColumn
    public String getNotNull() {
        return nullRef();
    }

    @TheOldColumn
    public @Nullable String getNull() {
        return null;
    }

    @TheOldColumn
    public long getLong() {
        return 0;
    }

    @TheOldColumn
    public TaskStatus getEnumNotAnnotated() {
        return SUCCESS;
    }

    @TheOldColumn
    @Enumerated
    public TaskStatus getEnumOrdinal() {
        return SUCCESS;
    }

    @TheOldColumn
    @Enumerated(STRING)
    public TaskStatus getEnumString() {
        return SUCCESS;
    }

    @SuppressWarnings("MethodMayBeStatic") // A column method cannot be static.
    @TheOldColumn
    private long getFortyTwoLong() {
        return 42L;
    }

    @TheOldColumn
    public String getParameter(String param) {
        return param;
    }

    @TheOldColumn
    public static String getStatic() {
        return "";
    }
}
