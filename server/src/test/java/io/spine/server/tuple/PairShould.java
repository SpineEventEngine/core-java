package io.spine.server.tuple;

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import io.spine.test.TestValues;
import io.spine.test.Tests;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PairShould {

    @Test(expected = NullPointerException.class)
    public void prohibit_null_input() {
        Pair.of(TestValues.newUuidValue(), Tests.<BoolValue>nullRef());
    }

    @Test
    public void support_equality() {
        StringValue v1 = TestValues.newUuidValue();
        StringValue v2 = TestValues.newUuidValue();

        assertEquals(Pair.of(v1, v2), Pair.of(v1, v2));
    }
}