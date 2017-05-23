/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.projection;

import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Serializable comparator of Protobuf {@link Timestamp}s.
 *
 * @author Alexander Yevsyukov
 */
final class TimestampComparator implements Comparator<Timestamp>, Serializable {

    private static final long serialVersionUID = 0L;

    private static final TimestampComparator timestampComparator = new TimestampComparator();

    static TimestampComparator getInstance() {
        return timestampComparator;
    }

    @Override
    public int compare(Timestamp t1, Timestamp t2) {
        return Timestamps.comparator()
                         .compare(t1, t2);
    }

    @Override
    public String toString() {
        return "TimestampComparator";
    }
}
