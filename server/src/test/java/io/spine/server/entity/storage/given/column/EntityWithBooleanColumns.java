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

import io.spine.server.entity.storage.TheOldColumn;

public class EntityWithBooleanColumns {

    @TheOldColumn
    public Boolean isBooleanWrapperColumn() {
        return true;
    }

    @TheOldColumn
    public Boolean getBooleanWrapperColumn() {
        return true;
    }

    @TheOldColumn
    public int isNonBoolean() {
        return 1;
    }

    @TheOldColumn
    public int getNonBoolean() {
        return 1;
    }

    @TheOldColumn
    public Boolean isBooleanWithParam(int param) {
        return true;
    }

    @TheOldColumn
    public Boolean getBooleanWithParam(int param) {
        return true;
    }

    @TheOldColumn
    public int isNonBooleanWithParam(int param) {
        return 1;
    }

    @TheOldColumn
    public int getNonBooleanWithParam(int param) {
        return 1;
    }
}
