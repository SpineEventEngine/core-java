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

package org.spine3.server.entity;

import com.google.protobuf.Timestamp;
import org.spine3.protobuf.Timestamps;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Version information on an entity.
 *
 * <p>Numbering of versions starts from zero, which means a “pre-initialization”
 * version of an entity.
 *
 * <p>Each modification of an entity should call {@link #increment()} for its version.
 *
 * @author Alexander Yevsyukov
 */
public final class Version {

    private final AtomicInteger number;
    private final AtomicReference<Timestamp> timestamp;

    private Version(int number, Timestamp timestamp) {
        this.number = new AtomicInteger(number);
        this.timestamp = new AtomicReference<>(timestamp);
    }

    /**
     * Creates new instance with the zero number and current system time.
     */
    public static Version create() {
        return new Version(0, currentTime());
    }

    private static Timestamp currentTime() {
        return Timestamps.getCurrentTime();
    }

    /**
     * Creates a new instance with the passed number and the timestamp.
     */
    public static Version of(int number, Timestamp timestamp) {
        checkNotNull(timestamp);
        return new Version(number, timestamp);
    }

    /**
     * Obtains the version number.
     */
    public int getNumber() {
        final int result = number.get();
        return result;
    }

    /**
     * Obtains the timestamp of the version.
     */
    public Timestamp getTimestamp() {
        final Timestamp result = timestamp.get();
        return result;
    }

    /**
     * Increments the version number by one and updates the timestamp
     * with the system time.
     */
    public void increment() {
        number.incrementAndGet();
        timestamp.set(currentTime());
    }

    void copyFrom(Version another) {
        number.set(another.getNumber());
        timestamp.set(another.getTimestamp());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Version)) {
            return false;
        }
        Version version = (Version) o;
        return Objects.equals(getNumber(), version.getNumber()) &&
               Objects.equals(getTimestamp(), version.getTimestamp());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNumber(), getTimestamp());
    }
}
